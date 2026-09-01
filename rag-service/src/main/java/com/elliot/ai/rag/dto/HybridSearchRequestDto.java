package com.elliot.ai.rag.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record HybridSearchRequestDto(
        @NotNull(message = "知识库id不能为空")
        UUID knowledgeBaseId,

        @NotBlank(message = "查询内容不能为空")
        @Size(max = 1000, message = "查询内容不能超过 1000 个字符")
        String query,

        /**
         * Hybrid 最终返回数量。
         *
         * 注意：不是 Vector / Keyword 单路召回数量。
         */
        @Min(value = 1, message = "topK 不能小于 1")
        @Max(value = 20, message = "topK 不能超过 20")
        Integer topK,
        /**
         * Vector Retriever 使用的相似度阈值。
         */
        @DecimalMin(
                value = "0.0",
                message = "相似度阈值不能小于 0"
        )
        @DecimalMax(
                value = "1.0",
                message = "相似度阈值不能大于 1"
        )
        Double similarityThreshold
) {
}
