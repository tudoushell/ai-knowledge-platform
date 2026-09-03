package com.elliot.ai.rag.retrieval.pipeline;

import com.elliot.ai.rag.AiRagServiceApplication;
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
import com.elliot.ai.rag.retrieval.rerank.RerankService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 检索流水线的 Spring 集成测试。
 *
 * <p>测试真实创建 Pipeline Bean，并以 mock 隔离模型调用、数据库检索和 Rerank 外部服务，
 * 验证流水线各阶段的编排和参数传递。</p>
 */
@Tag("integration")
@SpringBootTest(classes = AiRagServiceApplication.class)
@ActiveProfiles("test")
class RagRetrievalPipelineIntegrationTest {

    @Autowired
    private RagRetrievalPipeline ragRetrievalPipeline;

    @MockitoBean
    private QueryRewriteService queryRewriteService;

    @MockitoBean
    private QueryExpansionService queryExpansionService;

    @MockitoBean
    private MultiQueryRetrievalService multiQueryRetrievalService;

    @MockitoBean
    private RerankService rerankService;

    @Test
    void shouldExecuteRetrievalPipelineAndReturnTrace() {
        UUID knowledgeBaseId = UUID.randomUUID();
        String originalQuery = "Spring Boot 如何配置 PostgreSQL 数据库连接";
        String rewrittenQuery = "Spring Boot 配置 PostgreSQL 数据源";
        double similarityThreshold = 0.6;

        QueryRewriteResult rewriteResult = new QueryRewriteResult(
                originalQuery,
                rewrittenQuery,
                true
        );
        QueryExpansionResult expansionResult = new QueryExpansionResult(
                rewrittenQuery,
                List.of("Spring Boot PostgreSQL datasource 配置", "Spring Boot JDBC PostgreSQL 连接")
        );
        List<MultiQueryCandidate> multiQueryCandidates = List.of(
                multiQueryCandidate("Chunk A", 1, 0.05, 3),
                multiQueryCandidate("Chunk B", 2, 0.04, 2),
                multiQueryCandidate("Chunk C", 3, 0.03, 1)
        );
        List<RerankCandidate> rerankCandidates = List.of(
                rerankCandidate("Chunk B", 1, 0.95),
                rerankCandidate("Chunk A", 2, 0.89)
        );

        when(queryRewriteService.rewrite(originalQuery)).thenReturn(rewriteResult);
        when(queryExpansionService.expand(rewrittenQuery)).thenReturn(expansionResult);
        when(multiQueryRetrievalService.retrieve(anyList())).thenAnswer(invocation -> {
            List<RetrievalQuery> queries = invocation.getArgument(0);
            assertThat(queries).extracting(RetrievalQuery::query)
                    .containsExactly(
                            rewrittenQuery,
                            "Spring Boot PostgreSQL datasource 配置",
                            "Spring Boot JDBC PostgreSQL 连接"
                    );
            assertThat(queries).allSatisfy(query -> {
                assertThat(query.knowledgeBaseId()).isEqualTo(knowledgeBaseId);
                assertThat(query.topK()).isEqualTo(2);
                assertThat(query.similarityThreshold()).isEqualTo(similarityThreshold);
            });
            return multiQueryCandidates;
        });
        when(rerankService.rerank(any(RetrievalQuery.class), anyList())).thenAnswer(invocation -> {
            RetrievalQuery query = invocation.getArgument(0);
            List<HybridCandidate> candidates = invocation.getArgument(1);
            assertThat(query).isEqualTo(new RetrievalQuery(
                    knowledgeBaseId,
                    rewrittenQuery,
                    2,
                    similarityThreshold
            ));
            assertThat(candidates).extracting(HybridCandidate::getContent)
                    .containsExactly("Chunk A", "Chunk B", "Chunk C");
            assertThat(candidates).extracting(HybridCandidate::getRrfRank)
                    .containsExactly(1, 2, 3);
            return rerankCandidates;
        });

        RagRetrievalPipelineResult result = ragRetrievalPipeline.retrieve(
                knowledgeBaseId,
                originalQuery,
                2,
                similarityThreshold
        );

        assertThat(result.candidates()).containsExactlyElementsOf(rerankCandidates);
        assertThat(result.trace().originalQuery()).isEqualTo(originalQuery);
        assertThat(result.trace().rewrittenQuery()).isEqualTo(rewrittenQuery);
        assertThat(result.trace().rewritten()).isTrue();
        assertThat(result.trace().retrievalQueries()).containsExactly(
                rewrittenQuery,
                "Spring Boot PostgreSQL datasource 配置",
                "Spring Boot JDBC PostgreSQL 连接"
        );
        assertThat(result.trace().multiQueryCandidateCount()).isEqualTo(3);
        assertThat(result.trace().rerankInputCount()).isEqualTo(3);
        assertThat(result.trace().rerankOutCount()).isEqualTo(2);
        verify(queryRewriteService).rewrite(originalQuery);
        verify(queryExpansionService).expand(rewrittenQuery);
    }

    private MultiQueryCandidate multiQueryCandidate(
            String content,
            int rank,
            double score,
            int hitCount
    ) {
        HybridCandidate candidate = HybridCandidate.builder()
                .chunkId(UUID.randomUUID())
                .documentId(UUID.randomUUID())
                .documentName("test.md")
                .chunkIndex(rank)
                .content(content)
                .rrfRank(rank)
                .rrfScore(score)
                .build();
        return new MultiQueryCandidate(candidate, score, hitCount, rank);
    }

    private RerankCandidate rerankCandidate(String content, int rank, double score) {
        return new RerankCandidate(
                rank,
                score,
                true,
                rank,
                score,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "test.md",
                rank,
                null,
                null,
                content
        );
    }
}
