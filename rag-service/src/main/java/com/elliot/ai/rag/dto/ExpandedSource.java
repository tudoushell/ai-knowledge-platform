package com.elliot.ai.rag.dto;

import java.util.List;
import java.util.UUID;

public record ExpandedSource(
        Integer rank,
        Double score,
        UUID matchedChunkId,
        Integer matchedChunkIndex,
        UUID documentId,
        String documentName,
        Integer contextStartIndex,
        Integer contextEndIndex,
        List<RagSourceChunkDto> chunks
) {
}
