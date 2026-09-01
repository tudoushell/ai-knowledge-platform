package com.elliot.ai.rag.dto;

import java.util.UUID;

public record KeywordSearchHitDto(
        Integer rank,
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
