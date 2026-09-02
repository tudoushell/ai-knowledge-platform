package com.elliot.ai.rag.query.model;

public record QueryRewriteResult(
        String originalQuery,
        String rewrittenQuery,
        boolean rewritten
) {

    public String retrievalQuery() {
        return rewritten ? rewrittenQuery : originalQuery;
    }

    public static QueryRewriteResult fallback(String originalQuery) {
        return new QueryRewriteResult(
                originalQuery,
                originalQuery,
                false
        );
    }
}
