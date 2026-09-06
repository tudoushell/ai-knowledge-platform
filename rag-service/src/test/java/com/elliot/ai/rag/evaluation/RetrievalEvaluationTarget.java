package com.elliot.ai.rag.evaluation;

import java.util.List;
import java.util.UUID;

@FunctionalInterface
public interface RetrievalEvaluationTarget {

    List<UUID> retrieve(
            UUID knowledgeBaseId,
            String query,
            int topK,
            double similarityThreshold
    );
}
