package com.elliot.ai.rag.retrieval.rerank.cohere.model;

import java.util.List;

public record CohereRerankResponse(
        List<CohereRerankResult> results
) {
}
