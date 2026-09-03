package com.elliot.ai.rag.evaluation;

import java.util.Set;
import java.util.UUID;

/**
 *
 * @param id
 * @param query
 * @param relevantChunkIds
 */
public record RetrievalEvaluationCase(
        String id,
        String query,
        Set<UUID> relevantChunkIds
) {
}
