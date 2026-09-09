package com.elliot.ai.rag.evaluation;

public record RetrievalCaseComparison(
        String caseId,
        String query,
        RetrievalMetrics baseline,
        RetrievalMetrics enhanced,
        ComparisonStatus status
) {
}
