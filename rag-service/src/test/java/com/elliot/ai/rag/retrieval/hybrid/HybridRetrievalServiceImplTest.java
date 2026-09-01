package com.elliot.ai.rag.retrieval.hybrid;

import com.elliot.ai.rag.AiRagServiceApplication;
import com.elliot.ai.rag.retrieval.model.HybridCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Hybrid 检索集成测试。
 *
 * <p>该测试启动 Spring 上下文，并通过 test profile 连接配置的数据库，验证真实的
 * 向量检索、关键词检索和 Hybrid 检索服务链路。</p>
 */
@Slf4j
@Tag("integration")
@SpringBootTest(classes = AiRagServiceApplication.class)
@ActiveProfiles("test")
@Transactional
class HybridRetrievalServiceImplTest {

    private static final UUID TEST_KNOWLEDGE_BASE_ID =
            UUID.fromString("d83415c9-c26c-4762-bfe9-4767efc71b36");

    @Autowired
    private HybridRetrievalService hybridRetrievalService;


    @Test
    void shouldRetrieveCandidatesFromConfiguredDatabase() {
        RetrievalQuery query = new RetrievalQuery(
                TEST_KNOWLEDGE_BASE_ID,
                "汽车",
                5,
                0.5
        );
        List<HybridCandidate> retrieve = hybridRetrievalService.retrieve(query);
        assertThat(retrieve).isNotNull();
        assertThat(retrieve).allSatisfy(candidate -> {
            log.info(
                    "hybrid result rank={}, chunkId={}, rrfScore={}, vectorRank={}, vectorScore={}, keywordRank={}, keywordScore={}",
                    candidate.getHybridRank(),
                    candidate.getChunkId(),
                    candidate.getRrfScore(),
                    candidate.getVectorRank(),
                    candidate.getVectorScore(),
                    candidate.getKeywordRank(),
                    candidate.getKeywordScore()
            );
        });
    }
}
