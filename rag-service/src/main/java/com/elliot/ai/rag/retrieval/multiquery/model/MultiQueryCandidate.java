package com.elliot.ai.rag.retrieval.multiquery.model;

import com.elliot.ai.rag.retrieval.model.HybridCandidate;

public record MultiQueryCandidate(
        HybridCandidate candidate,
        double multiQueryRrfScore,
        int hitCount,
        int multiQueryRrfRank
) {
}
