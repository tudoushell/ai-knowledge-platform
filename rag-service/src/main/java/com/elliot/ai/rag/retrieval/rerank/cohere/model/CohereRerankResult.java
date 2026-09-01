package com.elliot.ai.rag.retrieval.rerank.cohere.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CohereRerankResult(
        int index,
        @JsonProperty("relevance_score")
        double relevanceScore
) {
}
