package com.elliot.ai.rag.dto;

import java.util.UUID;

public record RagSourceChunkDto(
        UUID chunkId,
        Integer chunkIndex,
        String selectTitle,
        Integer pageNumber,
        String content,
        boolean matched
) {
}
