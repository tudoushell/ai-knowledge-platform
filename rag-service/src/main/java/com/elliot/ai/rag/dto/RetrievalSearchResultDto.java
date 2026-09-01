package com.elliot.ai.rag.dto;

import java.util.List;
import java.util.UUID;

public record RetrievalSearchResultDto(
        UUID knowledgeBaseId,
        String query,
        int topK,
        double similarityThreshold,
        int resultCount,
        List<RetrievalHitDto> hits
) {
}
