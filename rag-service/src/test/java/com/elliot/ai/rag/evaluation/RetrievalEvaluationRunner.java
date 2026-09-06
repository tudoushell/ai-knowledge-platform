package com.elliot.ai.rag.evaluation;

import com.elliot.ai.rag.retrieval.model.RerankCandidate;
import com.elliot.ai.rag.retrieval.pipeline.RagRetrievalPipeline;
import com.elliot.ai.rag.retrieval.pipeline.model.RagRetrievalPipelineResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
public class RetrievalEvaluationRunner {

    private final RetrievalMetricsEvaluator metricsEvaluator;

    public RetrievalEvaluationRunner(RetrievalMetricsEvaluator metricsEvaluator) {
        this.metricsEvaluator = metricsEvaluator;
    }

    public RetrievalEvaluationSummary run(
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
            return new RetrievalEvaluationSummary(0,0.0,0.0, 0.0);
        }
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
        }
        int caseCount = cases.size();
        return new RetrievalEvaluationSummary(
                caseCount,
                hitSum/caseCount,
                recallSum/caseCount,
                reciprocalRankSum / caseCount
        );
    }
}
