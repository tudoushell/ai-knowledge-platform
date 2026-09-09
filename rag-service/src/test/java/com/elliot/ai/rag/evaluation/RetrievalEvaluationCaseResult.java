package com.elliot.ai.rag.evaluation;

import java.util.List;
import java.util.UUID;

public record RetrievalEvaluationCaseResult(
        String caseId,
        RetrievalQueryCategory category,
        String query,
        List<UUID> rankedChunkIds,
        RetrievalMetrics metrics
) {
}
