package com.elliot.ai.rag.evaluation;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class RetrievalEvaluationRunner {

    private final RetrievalMetricsEvaluator metricsEvaluator;

    public RetrievalEvaluationRunner(RetrievalMetricsEvaluator metricsEvaluator) {
        this.metricsEvaluator = metricsEvaluator;
    }

    public RetrievalEvaluationResult run(
            UUID knowledgeBaseId,
            List<RetrievalEvaluationCase> cases,
            int k,
            double similarityThreshold,
            RetrievalEvaluationTarget target
    ) {
        if (k <= 0) {
            throw new IllegalArgumentException("k should be greater than 0");
        }
        if (cases.isEmpty()) {
            return new RetrievalEvaluationResult(
                    new RetrievalEvaluationSummary(0, 0.0, 0.0, 0.0),
                    List.of());
        }
        List<RetrievalEvaluationCaseResult> caseResults = new ArrayList<>();

        double hitSum = 0.0;
        double recallSum = 0.0;
        double reciprocalRankSum = 0.0;
        for (RetrievalEvaluationCase evaluationCase : cases) {
            List<UUID> rankedChunkIds = target.retrieve(
                    knowledgeBaseId,
                    evaluationCase.query(),
                    k,
                    similarityThreshold
            );
            RetrievalMetrics metrics = metricsEvaluator
                    .evaluate(rankedChunkIds, evaluationCase.relevantChunkIds(), k);
            log.info("case id={},hit={},recall={}, rr={}, retrieved={}",
                    evaluationCase.id(),
                    metrics.hit(),
                    metrics.recall(),
                    metrics.reciprocalRank(),
                    rankedChunkIds);
            hitSum += metrics.hit();
            recallSum += metrics.recall();
            reciprocalRankSum += metrics.reciprocalRank();
            caseResults.add(
                    new RetrievalEvaluationCaseResult(evaluationCase.id(),
                            evaluationCase.category(),
                            evaluationCase.query(),
                            List.copyOf(rankedChunkIds),
                            metrics)
            );
        }
        int caseCount = cases.size();
        RetrievalEvaluationSummary summary = new RetrievalEvaluationSummary(
                caseCount,
                hitSum / caseCount,
                recallSum / caseCount,
                reciprocalRankSum / caseCount
        );
        return new RetrievalEvaluationResult(summary, caseResults);
    }
}
