package com.elliot.ai.rag.service.impl;

import com.elliot.ai.rag.config.RagProperties;
import com.elliot.ai.rag.retrieval.model.HybridCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;
import com.elliot.ai.rag.service.KeywordRetrievalService;
import com.elliot.ai.rag.service.VectorRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class HybridRetrievalServiceImpl2Test {
    @Mock
    private VectorRetrievalService vectorRetrievalService;

    @Mock
    private KeywordRetrievalService keywordRetrievalService;

    @Mock
    private RagProperties ragProperties;

    @InjectMocks
    private HybridRetrievalServiceImpl hybridRetrievalServiceImpl;


    @BeforeEach
    void initMocks() {

        when(ragProperties.getRecallTopK()).thenReturn(20);
        when(ragProperties.getRrfK()).thenReturn(60);
        when(ragProperties.getRerankCandidateTopK()).thenReturn(10);
    }

    @Test
    void shouldLimitCandidatesForRerank() {
        UUID documentId =
                UUID.randomUUID();

        List<RetrievalCandidate> vectorCandidates =
                IntStream.rangeClosed(1, 15)
                        .mapToObj(rank ->
                                candidate(
                                        rank,
                                        1.0 - rank * 0.01,
                                        UUID.randomUUID(),
                                        documentId,
                                        rank,
                                        "chunk-" + rank
                                )
                        )
                        .toList();

        when(
                vectorRetrievalService.retrieve(any())
        ).thenReturn(vectorCandidates);

        when(
                keywordRetrievalService.retrieve(any())
        ).thenReturn(List.of());

        RetrievalQuery query =
                new RetrievalQuery(
                        UUID.randomUUID(),
                        "test",
                        5,
                        0.5
                );

        List<HybridCandidate> result =
                hybridRetrievalServiceImpl.retrieve(query);

        /*
         * Final TopK 是 5，
         * 但是 Hybrid 阶段应该给 Reranker 10 个 Candidate。
         */
        assertThat(result.size()).isEqualTo(10);
    }


    @Test
    void shouldSupportSingleRetrievalCandidate() {
        UUID knowledgeBaseId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        UUID chunkA = UUID.randomUUID();
        UUID chunkB = UUID.randomUUID();

        List<RetrievalCandidate> vectorCandidates = List.of(candidate(
                1,
                9.0,
                chunkA,
                documentId,
                1, "chunk A"
        ));

        List<RetrievalCandidate> keywordCandidates = List.of(candidate(
                1,
                9.0,
                chunkB,
                documentId,
                2, "chunk B"
        ));

        when(vectorRetrievalService.retrieve(any())).thenReturn(vectorCandidates);
        when(keywordRetrievalService.retrieve(any())).thenReturn(keywordCandidates);
        RetrievalQuery retrievalQuery = new RetrievalQuery(knowledgeBaseId, "hash map", 5, 0.5);
        List<HybridCandidate> retrieve = hybridRetrievalServiceImpl.retrieve(retrievalQuery);
        assertThat(retrieve).hasSize(2);
        assertThat(retrieve).allSatisfy(candidate -> {
            log.info("hybridRank {} chunkId {} chunkIndex {}  rrfScore {}", candidate.getHybridRank(),
                    candidate.getChunkId(), candidate.getChunkIndex(), candidate.getRrfScore());
        });

    }


    @Test
    void shouldMergeSameChunkFromVectorAndKeyword() {
        UUID knowledgeBaseId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        UUID chunkA = UUID.randomUUID();
        UUID chunkB = UUID.randomUUID();
        UUID chunkC = UUID.randomUUID();
        List<RetrievalCandidate> vectorCandidates = List.of(
                candidate(
                        1,
                        0.90,
                        chunkA,
                        documentId,
                        1,
                        "chunk A"
                ),
                candidate(
                        2,
                        0.80,
                        chunkB,
                        documentId,
                        2,
                        "chunk B"
                )
        );

        List<RetrievalCandidate> keywordCandidates =
                List.of(
                        candidate(
                                1,
                                10.0,
                                chunkA,
                                documentId,
                                1,
                                "chunk A"
                        ),
                        candidate(
                                2,
                                8.0,
                                chunkC,
                                documentId,
                                3,
                                "chunk C"
                        )
                );

        when(vectorRetrievalService.retrieve(any())).thenReturn(vectorCandidates);

        when(keywordRetrievalService.retrieve(any())).thenReturn(keywordCandidates);

        RetrievalQuery query =
                new RetrievalQuery(
                        knowledgeBaseId,
                        "Spring事务",
                        5,
                        0.5
                );
        List<HybridCandidate> result = hybridRetrievalServiceImpl.retrieve(query);
        assertThat(result).hasSize(3);
        HybridCandidate candidateA = result.stream().filter(candidate -> chunkA.equals(candidate.getChunkId())).findFirst().orElseThrow();
        assertThat(candidateA.getVectorRank()).isEqualTo(1);
        assertThat(candidateA.getKeywordRank()).isEqualTo(1);
    }


    private RetrievalCandidate candidate(
            int rank,
            double score,
            UUID chunkId,
            UUID documentId,
            int chunkIndex,
            String content
    ) {

        return new RetrievalCandidate(
                rank,
                score,
                chunkId,
                documentId,
                "test.md",
                chunkIndex,
                "测试标题",
                null,
                content
        );
    }
}
