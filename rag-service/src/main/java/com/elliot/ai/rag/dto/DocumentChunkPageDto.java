package com.elliot.ai.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 文档 Chunk 分页响应对象。
 */
@Schema(description = "文档 Chunk 分页响应对象")
public record DocumentChunkPageDto(
        @Schema(description = "当前页码，从 1 开始", example = "1")
        int page,

        @Schema(description = "每页数量", example = "20")
        int size,

        @Schema(description = "Chunk 总数", example = "56")
        long total,

        @Schema(description = "当前页 Chunk 列表")
        List<DocumentChunkDto> records
) {
}
