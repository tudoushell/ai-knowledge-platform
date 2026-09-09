package com.elliot.ai.rag.evaluation;

import com.elliot.ai.rag.retrieval.hybrid.HybridRetrievalService;
import com.elliot.ai.rag.retrieval.model.HybridCandidate;
import com.elliot.ai.rag.retrieval.model.RerankCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;
import com.elliot.ai.rag.retrieval.pipeline.RagRetrievalPipeline;
import com.elliot.ai.rag.retrieval.pipeline.model.RagRetrievalPipelineResult;
import com.elliot.ai.rag.retrieval.rerank.RerankService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
public class RagRetrievalEvaluationTest {

    @Autowired
    private RagRetrievalPipeline retrievalPipeline;

    @Autowired
    private HybridRetrievalService hybridRetrievalService;

    @Autowired
    private RerankService rerankService;


    @Test
    void shouldEvaluateRetrievalPipeline() {
        UUID knowledgeBaseId = UUID.fromString("d83415c9-c26c-4762-bfe9-4767efc71b36");
        List<RetrievalEvaluationCase> cases = RetrievalEvaluationDataset.cases();
        RetrievalEvaluationRunner runner = new RetrievalEvaluationRunner(new RetrievalMetricsEvaluator());
        RetrievalEvaluationResult enhancedSummary = runner.run(
                knowledgeBaseId,
                cases,
                5,
                0.5,
                this::retrieveEnhanced
        );
        RetrievalEvaluationResult baselineSummary = runner.run(
                knowledgeBaseId,
                cases,
                5,
                0.5,
                this::retrieveBaseline
        );
        Map<RetrievalQueryCategory, RetrievalCategorySummary> baselineByCategory =
                summarizeByCategory(baselineSummary);

        Map<RetrievalQueryCategory, RetrievalCategorySummary> enhancedByCategory =
                summarizeByCategory(enhancedSummary);

        log.info("Baseline categories:");
        baselineByCategory.forEach(
                (category, summary) ->
                        log.info("{}", summary)
        );

        log.info("Enhanced categories:");
        enhancedByCategory.forEach(
                (category, summary) ->
                        log.info("{}", summary)
        );

        List<RetrievalCaseComparison> comparisons = compare(baselineSummary, enhancedSummary);
        for (RetrievalCaseComparison comparison : comparisons) {
            log.info(
                    """
                            [{}]
                            case={}
                            query={}
                            baseline: hit={} recall={} rr={}
                            enhanced: hit={} recall={} rr={}
                            """,
                    comparison.status(),
                    comparison.caseId(),
                    comparison.query(),
                    comparison.baseline().hit(),
                    comparison.baseline().recall(),
                    comparison.baseline().reciprocalRank(),
                    comparison.enhanced().hit(),
                    comparison.enhanced().recall(),
                    comparison.enhanced().reciprocalRank()
            );
        }

        long improvedCount =
                comparisons.stream()
                        .filter(item ->
                                item.status()
                                        == ComparisonStatus.IMPROVED
                        )
                        .count();

        long sameCount =
                comparisons.stream()
                        .filter(item ->
                                item.status()
                                        == ComparisonStatus.SAME
                        )
                        .count();

        long regressedCount =
                comparisons.stream()
                        .filter(item ->
                                item.status()
                                        == ComparisonStatus.REGRESSED
                        )
                        .count();
        log.info(
                """
                        Case Comparison Summary
                        Improved: {}
                        Same: {}
                        Regressed: {}
                        """,
                improvedCount,
                sameCount,
                regressedCount
        );

    }

    private List<RetrievalCaseComparison> compare(
            RetrievalEvaluationResult baseline,
            RetrievalEvaluationResult enhanced
    ) {

        Map<String, RetrievalEvaluationCaseResult> enhancedById =
                enhanced.caseResults()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        RetrievalEvaluationCaseResult::caseId,
                                        Function.identity()
                                )
                        );

        List<RetrievalCaseComparison> comparisons =
                new ArrayList<>();

        for (RetrievalEvaluationCaseResult baselineCase
                : baseline.caseResults()) {

            RetrievalEvaluationCaseResult enhancedCase =
                    enhancedById.get(
                            baselineCase.caseId()
                    );

            if (enhancedCase == null) {
                throw new IllegalStateException(
                        "Missing enhanced result for case: "
                                + baselineCase.caseId()
                );
            }

            comparisons.add(
                    new RetrievalCaseComparison(
                            baselineCase.caseId(),
                            baselineCase.query(),
                            baselineCase.metrics(),
                            enhancedCase.metrics(),
                            compareMetrics(
                                    baselineCase.metrics(),
                                    enhancedCase.metrics()
                            )
                    )
            );
        }

        return List.copyOf(comparisons);
    }

    private ComparisonStatus compareMetrics(
            RetrievalMetrics baseline,
            RetrievalMetrics enhanced
    ) {

        if (enhanced.hit() > baseline.hit()) {
            return ComparisonStatus.IMPROVED;
        }

        if (enhanced.hit() < baseline.hit()) {
            return ComparisonStatus.REGRESSED;
        }

        int rrCompare =
                Double.compare(
                        enhanced.reciprocalRank(),
                        baseline.reciprocalRank()
                );

        if (rrCompare > 0) {
            return ComparisonStatus.IMPROVED;
        }

        if (rrCompare < 0) {
            return ComparisonStatus.REGRESSED;
        }

        int recallCompare =
                Double.compare(
                        enhanced.recall(),
                        baseline.recall()
                );

        if (recallCompare > 0) {
            return ComparisonStatus.IMPROVED;
        }

        if (recallCompare < 0) {
            return ComparisonStatus.REGRESSED;
        }

        return ComparisonStatus.SAME;
    }

    private Map<RetrievalQueryCategory, RetrievalCategorySummary> summarizeByCategory(
            RetrievalEvaluationResult result) {

        Map<RetrievalQueryCategory, List<RetrievalEvaluationCaseResult>> grouped =
                result.caseResults().stream()
                        .collect(Collectors.groupingBy(
                                RetrievalEvaluationCaseResult::category
                        ));

        return grouped.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> summarizeCategory(
                                entry.getKey(),
                                entry.getValue()
                        )
                ));
    }

    private RetrievalCategorySummary summarizeCategory(RetrievalQueryCategory category, List<RetrievalEvaluationCaseResult> cases) {
        double hitRate = cases.stream().mapToDouble(item -> item.metrics().hit()).average().orElse(0.0);
        double meanRecall = cases.stream().mapToDouble(item -> item.metrics().recall()).average().orElse(0.0);
        double mrr = cases.stream().mapToDouble(item -> item.metrics().reciprocalRank()).average().orElse(0.0);

        return new RetrievalCategorySummary(
                category,
                cases.size(),
                hitRate,
                meanRecall,
                mrr);
    }


    private List<UUID> retrieveEnhanced(
            UUID knowledgeBaseId,
            String query,
            int topK,
            double similarityThreshold
    ) {
        RagRetrievalPipelineResult result =
                retrievalPipeline.retrieve(
                        knowledgeBaseId,
                        query,
                        topK,
                        similarityThreshold
                );

        return result.candidates()
                .stream()
                .map(RerankCandidate::chunkId)
                .toList();
    }

    private List<UUID> retrieveBaseline(
            UUID knowledgeBaseId,
            String query,
            int topK,
            double similarityThreshold
    ) {
        RetrievalQuery retrievalQuery = new RetrievalQuery(knowledgeBaseId, query, topK, similarityThreshold);
        List<HybridCandidate> hybridCandidates = hybridRetrievalService.retrieve(retrievalQuery);
        if (hybridCandidates.isEmpty()) {
            return List.of();
        }
        List<RerankCandidate> rerankCandidates = rerankService.rerank(retrievalQuery, hybridCandidates);
        return rerankCandidates.stream()
                .map(RerankCandidate::chunkId)
                .toList();
    }
}
