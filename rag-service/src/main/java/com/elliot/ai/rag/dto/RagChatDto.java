package com.elliot.ai.rag.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RagChatDto(
        @NotNull(message = "知识库 ID 不能为空")
        UUID knowledgeBaseId,
        @NotBlank(message = "问题不能为空")
        String question,
        @Min(value = 1, message = "topK 不能小于1")
        @Max(value = 20, message = "topK 不能超过20")
        Integer topK,
        @DecimalMin(value = "0.0", message = "相似度阀值不能小于 0")
        @DecimalMax(value = "1.0", message = "相似度阀值不能大于 1")
        Double similarityThreshold,
        @NotBlank(message = "模型不能为空")
        String modelCode) {

}
