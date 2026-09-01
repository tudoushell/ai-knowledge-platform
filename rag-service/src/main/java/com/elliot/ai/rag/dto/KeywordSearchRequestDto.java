package com.elliot.ai.rag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record KeywordSearchRequestDto(
        @NotNull
        UUID knowledgeBaseId,
        @NotBlank
        @Size(max = 500)
        String query,
        @Min(1)
        @Max(20)
        Integer topK
) {
}
