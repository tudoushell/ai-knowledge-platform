package com.elliot.ai.rag.retrieval.model;

import java.util.UUID;

public record RetrievalQuery (
        UUID knowledgeBaseId,
        String query,
        int topK,
        double similarityThreshold
){
}
