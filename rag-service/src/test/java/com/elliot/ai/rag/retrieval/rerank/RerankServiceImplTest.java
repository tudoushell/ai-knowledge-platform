package com.elliot.ai.rag.retrieval.rerank;

import com.elliot.ai.rag.config.RerankProperties;
import com.elliot.ai.rag.retrieval.model.HybridCandidate;
import com.elliot.ai.rag.retrieval.model.RerankCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;
import com.elliot.ai.rag.retrieval.rerank.model.RerankModelResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class RerankServiceImplTest {

    @Mock
    private RerankModelClient rerankModelClient;

    @Mock
    private RerankProperties rerankProperties;

    @InjectMocks
    private RerankServiceImpl rerankService;

    private static final UUID CHUNK_A =
            UUID.fromString(
                    "00000000-0000-0000-0000-000000000001"
            );

    private static final UUID CHUNK_B =
            UUID.fromString(
                    "00000000-0000-0000-0000-000000000002"
            );

    private static final UUID CHUNK_C =
            UUID.fromString(
                    "00000000-0000-0000-0000-000000000003"
            );


    @BeforeEach
    void setUp() {
        when(rerankProperties.isEnabled()).thenReturn(true);
//        when(rerankProperties.isFallbackEnabled()).thenReturn(false);
    }


    @Test
    void shouldReorderCandidatesByRerankScore() {

        List<HybridCandidate> candidates =
                List.of(
                        candidate(
                                1,
                                0.032,
                                CHUNK_A,
                                "chunk A"
                        ),
                        candidate(
                                2,
                                0.030,
                                CHUNK_B,
                                "chunk B"
                        ),
                        candidate(
                                3,
                                0.028,
                                CHUNK_C,
                                "chunk C"
                        )
                );

        when(
                rerankModelClient.rerank(
                        any(), any()
                )
        ).thenReturn(
                List.of(
                        new RerankModelResult(
                                0,
                                0.50
                        ),
                        new RerankModelResult(
                                1,
                                0.70
                        ),
                        new RerankModelResult(
                                2,
                                0.95
                        )
                )
        );

        RetrievalQuery query =
                new RetrievalQuery(
                        UUID.randomUUID(),
                        "测试问题",
                        3,
                        0.5
                );

        List<RerankCandidate> result =
                rerankService.rerank(
                        query,
                        candidates
                );
        result.forEach(this::logRerankCandidate);
        assertThat(result.get(0).chunkId()).isEqualTo(CHUNK_C);
    }

    private void logRerankCandidate(RerankCandidate candidate) {
        log.info("""
                        RerankCandidate:
                          rerankRank={}
                          rerankScore={}
                          reranked={}
                          rrfRank={}
                          rrfScore={}
                          effectiveScore={}
                          chunkId={}
                          documentId={}
                          documentName={}
                          chunkIndex={}
                          sectionTitle={}
                          pageNumber={}
                          content={}
                        """,
                candidate.rerankRank(),
                candidate.rerankScore(),
                candidate.reranked(),
                candidate.rrfRank(),
                candidate.rrfScore(),
                candidate.effectiveScore(),
                candidate.chunkId(),
                candidate.documentId(),
                candidate.documentName(),
                candidate.chunkIndex(),
                candidate.sectionTitle(),
                candidate.pageNumber(),
                candidate.content());
    }


    private HybridCandidate candidate(
            int rrfRank,
            double rrfScore,
            UUID chunkId,
            String content
    ) {

        return HybridCandidate.builder()
                .chunkId(chunkId)
                .documentId(UUID.randomUUID())
                .documentName("test.md")
                .chunkIndex(rrfRank)
                .sectionTitle("测试标题")
                .content(content)
                .rrfRank(rrfRank)
                .rrfScore(rrfScore)
                .build();
    }
}
