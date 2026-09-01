package com.elliot.ai.rag.query.model;

public record QueryRewriteResult(
        String originalQuery,
        String rewrittenQuery,
        boolean rewritten
) {
}
