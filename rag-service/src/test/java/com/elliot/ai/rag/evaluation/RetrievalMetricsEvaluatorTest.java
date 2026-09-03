package com.elliot.ai.rag.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


public class RetrievalMetricsEvaluatorTest {

    @Test
    void shouldCalculateRetrievalMetrics() {
        UUID relevantA = UUID.randomUUID();
        UUID relevantB = UUID.randomUUID();

        UUID irrelevantA = UUID.randomUUID();
        UUID irrelevantB = UUID.randomUUID();


        RetrievalMetrics evaluate = new RetrievalMetricsEvaluator().evaluate(
                List.of(
                        irrelevantA,
                        relevantA,
                        irrelevantB,
                        relevantB
                ),
                Set.of(
                        relevantA,
                        relevantB
                ),
                4
        );
        assertThat(evaluate.hit()).isEqualTo(1.0);
        assertThat(evaluate.recall()).isEqualTo(1.0);
        assertThat(evaluate.reciprocalRank()).isEqualTo(0.5);
    }
}
