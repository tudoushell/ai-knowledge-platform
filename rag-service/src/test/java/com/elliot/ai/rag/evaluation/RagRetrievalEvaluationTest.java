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

import java.util.List;
import java.util.Set;
import java.util.UUID;

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
        List<RetrievalEvaluationCase> cases = buildCase();
        RetrievalEvaluationRunner runner = new RetrievalEvaluationRunner(new RetrievalMetricsEvaluator());
        RetrievalEvaluationSummary summary = runner.run(
                knowledgeBaseId,
                cases,
                5,
                0.5,
                this::retrieveEnhanced
        );
        RetrievalEvaluationSummary baselineSummary = runner.run(
                knowledgeBaseId,
                cases,
                5,
                0.5,
                this::retrieveBaseline
        );

        logEvaluationComparison(5, baselineSummary, summary);
    }


    List<RetrievalEvaluationCase> buildCase() {
        return List.of(
                new RetrievalEvaluationCase(
                        "redis-001",
                        "redis 是什么？",
                        Set.of(
                                UUID.fromString("0ffc0a84-28e4-409d-8088-822f1c1eeb82"),
                                UUID.fromString("a7c9d96f-a5e5-483b-a6cb-0f570a3e1344"),
                                UUID.fromString("f406bda2-6940-49c7-a13e-5034cf64679d"),
                                UUID.fromString("04667980-8f97-4051-a4e8-c5d33b65a2b2"),
                                UUID.fromString("aacd4b1e-024d-48ca-b8ec-9e0da69c3e2f")
                        )
                )
        );
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

    private void logEvaluationComparison(
            int k,
            RetrievalEvaluationSummary baselineSummary,
            RetrievalEvaluationSummary enhancedSummary
    ) {
        log.info(
                "retrieval evaluation baseline, k={}, caseCount={}, hitRate={}, meanRecall={}, mrr={}",
                k,
                baselineSummary.caseCount(),
                baselineSummary.hitRate(),
                baselineSummary.meanRecall(),
                baselineSummary.mrr()
        );
        log.info(
                "retrieval evaluation enhanced, k={}, caseCount={}, hitRate={}, meanRecall={}, mrr={}",
                k,
                enhancedSummary.caseCount(),
                enhancedSummary.hitRate(),
                enhancedSummary.meanRecall(),
                enhancedSummary.mrr()
        );
        log.info(
                "retrieval evaluation delta (enhanced - baseline), hitRate={}, meanRecall={}, mrr={}",
                enhancedSummary.hitRate() - baselineSummary.hitRate(),
                enhancedSummary.meanRecall() - baselineSummary.meanRecall(),
                enhancedSummary.mrr() - baselineSummary.mrr()
        );
    }
}
