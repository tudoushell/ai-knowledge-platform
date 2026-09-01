package com.elliot.ai.rag.dto;

public record TokenUsageDto(
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {
    
}
