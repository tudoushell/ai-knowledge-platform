package com.elliot.ai.rag.evaluation;

public record RetrievalCategorySummary(
        RetrievalQueryCategory category,
        int caseCount,
        double hitRate,
        double meanRecall,
        double mrr
) {
}
