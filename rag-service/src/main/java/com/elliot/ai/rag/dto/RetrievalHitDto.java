package com.elliot.ai.rag.dto;

import java.util.UUID;

public record RetrievalHitDto(
        int rank,
        Double score,
        UUID chunkId,
        UUID documentId,
        String documentName,
        Integer chunkIndex,
        String sectionTitle,
        Integer pageNumber,
        String content
) {
}
