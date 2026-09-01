package com.elliot.ai.rag.dto;

import java.util.List;
import java.util.UUID;

public record KeywordSearchDto(
        UUID knowledgeBaseId,

        String query,

        Integer topK,

        Integer resultCount,

        List<KeywordSearchHitDto> results
) {
}
