package com.elliot.ai.rag.dto;

import java.util.List;
import java.util.UUID;

public record RagChatResultDto(
    UUID knowledgeBaseId,
    String question,
    String answer,
    boolean knowledgeFound,
    List<RagSourceDto>  sources,
    TokenUsageDto tokenUsage
) {
}
