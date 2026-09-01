package com.elliot.ai.rag.retrieval.rerank.cohere.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CohereRerankRequest(
        String model,
        String query,
        List<String> documents,
        @JsonProperty("top_n")
        Integer topN
) {
}
