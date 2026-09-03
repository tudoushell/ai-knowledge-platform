package com.elliot.ai.rag.retrieval.pipeline;

import com.elliot.ai.rag.config.RagProperties;
import com.elliot.ai.rag.query.expansion.QueryExpansionService;
import com.elliot.ai.rag.query.model.QueryExpansionResult;
import com.elliot.ai.rag.query.model.QueryRewriteResult;
import com.elliot.ai.rag.query.rewrite.QueryRewriteService;
import com.elliot.ai.rag.retrieval.model.HybridCandidate;
import com.elliot.ai.rag.retrieval.model.RerankCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;
import com.elliot.ai.rag.retrieval.multiquery.MultiQueryRetrievalService;
import com.elliot.ai.rag.retrieval.multiquery.model.MultiQueryCandidate;
import com.elliot.ai.rag.retrieval.pipeline.model.RagRetrievalPipelineResult;
import com.elliot.ai.rag.retrieval.pipeline.model.RetrievalTrace;
import com.elliot.ai.rag.retrieval.rerank.RerankService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RagRetrievalPipelineImpl implements RagRetrievalPipeline {

    private final QueryRewriteService queryRewriteService;
    private final QueryExpansionService queryExpansionService;
    private final MultiQueryRetrievalService multiQueryRetrievalService;
    private final RerankService rerankService;
    private final RagProperties ragProperties;

    @Override
    public RagRetrievalPipelineResult retrieve(UUID knowledgeBaseId, String originalQuery, int topK, double similarityThreshold) {
        //1.Query Rewrite
        QueryRewriteResult rewriteResult = queryRewriteService.rewrite(originalQuery);
        //2. 构建基本检索
        String retrievalQuery = rewriteResult.retrievalQuery();
        RetrievalQuery baseRetrievalQuery = new RetrievalQuery(
                knowledgeBaseId,
                retrievalQuery,
                topK,
                similarityThreshold
        );
        //3. 扩展base Retrieval Query
        QueryExpansionResult expansionResult = queryExpansionService.expand(retrievalQuery);
        List<String> retrievalQuestions = expansionResult.retrievalQueries();

        //4. 构建多个Query
        List<RetrievalQuery> retrievalQueries = expansionResult.retrievalQueries()
                .stream()
                .map(query -> new RetrievalQuery(
                        knowledgeBaseId,
                        query,
                        topK,
                        similarityThreshold
                ))
                .toList();
        /**
         * 5. Multi Query Retrieval + RRF
         * 内部执行
         * Q1 ->  Vector Recall + Keyword Recall + RRF  -> Hybrid Candidates Q1
         * Q2 ->  Vector Recall + Keyword Recall + RRF  -> Hybrid Candidates Q2
         *
         *  Hybrid Candidates Q1 +  Hybrid Candidates Q2 -> Group by chunkId -> RRF(rrfRank)
         *
         */
        List<MultiQueryCandidate> multiQueryCandidates = multiQueryRetrievalService.retrieve(retrievalQueries);
        if (multiQueryCandidates.isEmpty()) {
            buildTrace(originalQuery,
                    rewriteResult,
                    retrievalQuestions,
                    0,
                    0,
                    0);
        }
        //6. Global TopN
        List<HybridCandidate> rerankInputCandidates = buildRerankCandidates(multiQueryCandidates);
        List<RerankCandidate> rerankCandidates = rerankService.rerank(baseRetrievalQuery, rerankInputCandidates);
        RetrievalTrace retrievalTrace = buildTrace(originalQuery,
                rewriteResult,
                retrievalQuestions,
                multiQueryCandidates.size(),
                rerankInputCandidates.size(),
                rerankCandidates.size());
        return new RagRetrievalPipelineResult(
                rerankCandidates,
                retrievalTrace);
    }

    private List<HybridCandidate> buildRerankCandidates(List<MultiQueryCandidate> candidates) {
        return candidates.stream()
                .limit(
                        ragProperties.getRerankCandidateTopK()
                )
                .map(this::toRerankInputCandidate)
                .toList();
    }


    private HybridCandidate toRerankInputCandidate(MultiQueryCandidate multiQueryCandidate) {
        HybridCandidate source = multiQueryCandidate.candidate();
        return HybridCandidate.builder()
                .chunkId(source.getChunkId())
                .documentId(source.getDocumentId())
                .documentName(source.getDocumentName())
                .chunkIndex(source.getChunkIndex())
                .sectionTitle(source.getSectionTitle())
                .pageNumber(source.getPageNumber())
                .content(source.getContent())
                .vectorRank(source.getVectorRank())
                .vectorScore(source.getVectorScore())
                .keywordRank(source.getKeywordRank())
                .keywordScore(source.getKeywordScore())
                .rrfRank(multiQueryCandidate.multiQueryRrfRank())
                .rrfScore(multiQueryCandidate.multiQueryRrfScore())
                .build();
    }

    private RetrievalTrace buildTrace(
            String originalQuery,
            QueryRewriteResult rewriteResult,
            List<String> retrievalQueries,
            int multiQueryCandidateCount,
            int rerankInputCount,
            int rerankOutputCount
    ) {
        return new RetrievalTrace(
                originalQuery,
                rewriteResult.rewrittenQuery(),
                rewriteResult.rewritten(),
                List.copyOf(retrievalQueries),
                multiQueryCandidateCount,
                rerankInputCount,
                rerankOutputCount
        );

    }

}
