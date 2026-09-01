package com.elliot.ai.rag.dto;

import com.elliot.ai.rag.entity.DocumentChunk;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 文档文本片段响应对象。
 */
@Schema(description = "文档文本片段响应对象")
public record DocumentChunkDto(
        @Schema(description = "Chunk ID")
        UUID id,

        @Schema(description = "所属文档 ID")
        UUID documentId,

        @Schema(description = "Chunk 在文档中的顺序，从 0 开始")
        Integer chunkIndex,

        @Schema(description = "Chunk 文本内容")
        String content,

        @Schema(description = "文本字符数")
        Integer charCount,

        @Schema(description = "文本 Token 数；未统计时为空")
        Integer tokenCount,

        @Schema(description = "所属章节标题")
        String sectionTitle,

        @Schema(description = "所在页码")
        Integer pageNumber,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {

    public static DocumentChunkDto from(DocumentChunk chunk) {
        return new DocumentChunkDto(
                chunk.getId(),
                chunk.getDocumentId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getCharCount(),
                chunk.getTokenCount(),
                chunk.getSectionTitle(),
                chunk.getPageNumber(),
                chunk.getCreatedAt()
        );
    }
}
