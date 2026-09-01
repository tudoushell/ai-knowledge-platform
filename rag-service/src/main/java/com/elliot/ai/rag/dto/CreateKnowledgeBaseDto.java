package com.elliot.ai.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建知识库请求参数。
 *
 * @param name 知识库名称，不能为空，最大长度 128 个字符
 * @param description 知识库描述，可为空，最大长度 500 个字符
 */
public record CreateKnowledgeBaseDto(
        @NotBlank(message = "知识库名称不能为空")
        @Size(max = 128, message = "知识库名称不能超过 128 个字符")
        String name,

        @Size(max = 500, message = "知识库描述不能超过 500 个字符")
        String description
) {
}
