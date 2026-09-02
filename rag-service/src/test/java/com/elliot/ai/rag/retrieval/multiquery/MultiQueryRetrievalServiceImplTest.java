package com.elliot.ai.rag.retrieval.multiquery;

import com.elliot.ai.rag.retrieval.hybrid.HybridRetrievalService;
import com.elliot.ai.rag.retrieval.model.HybridCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class MultiQueryRetrievalServiceImplTest {
    @Mock
    private HybridRetrievalService hybridRetrievalService;

    @InjectMocks
    private MultiQueryRetrievalServiceImpl multiQueryRetrievalService;

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

    private static final UUID CHUNK_D =
            UUID.fromString(
                    "00000000-0000-0000-0000-000000000004"
            );

    private static final UUID CHUNK_E =
            UUID.fromString(
                    "00000000-0000-0000-0000-000000000005"
            );

    @Test
    void shouldDeduplicateCandidatesByChunkIdAndKeepFirstOccurrence() {
        UUID knowledgeId = UUID.randomUUID();

        List<RetrievalQuery> queries = IntStream.range(0, 3).mapToObj(
                index -> {
                    return new RetrievalQuery(knowledgeId, "q" + index, 10, 0.5);
                }
        ).toList();

        HybridCandidate candidateA = candidate(randomRrfRank(), randomRrfScore(), CHUNK_A, "CHUNK A");
        HybridCandidate firstCandidateB = candidate(randomRrfRank(), randomRrfScore(), CHUNK_B, "CHUNK B");
        HybridCandidate candidateC = candidate(randomRrfRank(), randomRrfScore(), CHUNK_C, "CHUNK C");
        HybridCandidate duplicateCandidateB = candidate(randomRrfRank(), randomRrfScore(), CHUNK_B, "CHUNK B duplicated");
        HybridCandidate candidateD = candidate(randomRrfRank(), randomRrfScore(), CHUNK_D, "CHUNK D");
        HybridCandidate duplicateCandidateA = candidate(randomRrfRank(), randomRrfScore(), CHUNK_A, "CHUNK A duplicated");
        HybridCandidate candidateE = candidate(randomRrfRank(), randomRrfScore(), CHUNK_E, "CHUNK E");

        Map<String, List<HybridCandidate>> candidatesByQuery = Map.of(
                "q0", List.of(candidateA, firstCandidateB, candidateC),
                "q1", List.of(duplicateCandidateB, candidateD),
                "q2", List.of(duplicateCandidateA, candidateE)
        );
        when(hybridRetrievalService.retrieve(any()))
                .thenAnswer(invocation ->
                        candidatesByQuery.get(
                                invocation.getArgument(0, RetrievalQuery.class).query())
                );

        List<HybridCandidate> result = multiQueryRetrievalService.retrieve(queries);

        assertThat(result).containsExactly(candidateA, firstCandidateB, candidateC, candidateD, candidateE);
        verify(hybridRetrievalService).retrieve(queries.get(0));
        verify(hybridRetrievalService).retrieve(queries.get(1));
        verify(hybridRetrievalService).retrieve(queries.get(2));
    }

    private int randomRrfRank() {
        return ThreadLocalRandom.current().nextInt(1, 10);
    }

    private double randomRrfScore() {
        return ThreadLocalRandom.current().nextDouble();
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
