package com.elliot.ai.rag.evaluation;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RetrievalEvaluationDataset {

    public static List<RetrievalEvaluationCase> cases() {
        return List.of(
                new RetrievalEvaluationCase(
                        "redis-001",
                        RetrievalQueryCategory.COLLOQUIAL,
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
}
