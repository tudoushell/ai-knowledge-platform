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

    /**
     * 在相同数据集和检索参数下，对比增强检索流水线与基线检索的效果。
     *
     * <p>增强方案执行问题改写、查询扩展、多查询混合检索、融合及精排；
     * 基线方案直接使用原始问题执行单查询混合检索及精排。
     * 根据数据集标注的相关 Chunk，计算 Hit@5、Recall@5 和 RR@5，
     * 按问题类别汇总 HitRate、平均 Recall 和 MRR，并输出逐例差异及改善、持平、退化数量。</p>
     *
     * <p>运行前需准备指定知识库、与数据集标注一致的 Chunk 以及所需的数据库和模型服务。
     * 本测试通过日志报告评测结果，未对检索质量设置断言阈值。</p>
     */
    @Test
    void shouldEvaluateRetrievalPipeline() {
        // 1. 准备两组评测共用的会话标识、知识库及带相关 Chunk 标注的问题集。
        UUID conversationId = UUID.randomUUID();
        UUID knowledgeBaseId = UUID.fromString("d83415c9-c26c-4762-bfe9-4767efc71b36");
        List<RetrievalEvaluationCase> cases = RetrievalEvaluationDataset.cases();
        // Runner 逐例调用检索方法，再根据返回的 Chunk 排名计算指标并汇总。
        RetrievalEvaluationRunner runner = new RetrievalEvaluationRunner(new RetrievalMetricsEvaluator());
        // 2. 评测增强方案；TopK 固定为 5，向量相似度阈值为 0.5。
        RetrievalEvaluationResult enhancedSummary = runner.run(
                conversationId,
                knowledgeBaseId,
                cases,
                5,
                0.5,
                this::retrieveEnhanced
        );
        // 3. 使用相同问题集和参数评测基线，便于观察新增检索步骤带来的效果变化。
        RetrievalEvaluationResult baselineSummary = runner.run(
                conversationId,
                knowledgeBaseId,
                cases,
                5,
                0.5,
                this::retrieveBaseline
        );
        // 4. 按问题类别汇总，观察增强方案对不同类型问题的影响，避免整体均值掩盖局部退化。
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

        // 5. 按用例 ID 对齐结果；依次比较 Hit、RR、Recall，首个不相等的指标决定改善或退化。
        // 日志中的 rr 是首个相关 Chunk 排名的倒数，越大表示正确资料排得越靠前。
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

        // 6. 统计改善、持平和退化的用例数，结合上面的逐例日志定位收益和回归问题。
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
            UUID conversationId,
            UUID knowledgeBaseId,
            String query,
            int topK,
            double similarityThreshold
    ) {
        RagRetrievalPipelineResult result =
                retrievalPipeline.retrieve(
                        conversationId,
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
            UUID conversationId,
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
