package com.elliot.ai.rag.dto;

import com.elliot.ai.rag.entity.KbDocument;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 知识库文档详情响应对象。
 *
 * <p>用于展示文件元数据、解析结果、切分配置、向量索引信息及处理错误；
 * 不暴露原文件与解析文件的内部存储路径。</p>
 */
@Schema(description = "知识库文档详情响应对象")
public record KbDocumentDetailDto(
        @Schema(description = "文档 ID")
        UUID id,

        @Schema(description = "所属知识库 ID")
        UUID knowledgeBaseId,

        @Schema(description = "上传时的原始文件名")
        String originalName,

        @Schema(description = "文件 MIME 类型")
        String contentType,

        @Schema(description = "文件扩展名", example = "pdf")
        String fileExtension,

        @Schema(description = "文件大小，单位为字节")
        Long sizeBytes,

        @Schema(description = "文件内容 SHA-256 摘要")
        String sha256,

        @Schema(description = "文档处理状态", example = "INDEXED")
        String status,

        @Schema(description = "解析结果存储格式", example = "STRUCTURED_JSONL")
        String parsedFormat,

        @Schema(description = "解析文本预览")
        String parsedPreview,

        @Schema(description = "解析后文本字符数")
        Long parsedCharCount,

        @Schema(description = "解析后的结构化块数量")
        Integer parsedBlockCount,

        @Schema(description = "PDF 等分页文档的页数")
        Integer pageCount,

        @Schema(description = "已生成的 Chunk 数量")
        Integer chunkCount,

        @Schema(description = "文本切分策略", example = "TOKEN")
        String chunkStrategy,

        @Schema(description = "每个 Chunk 的目标大小")
        Integer chunkSize,

        @Schema(description = "相邻 Chunk 的重叠大小")
        Integer chunkOverlap,

        @Schema(description = "已写入向量库的向量数量")
        Integer vectorCount,

        @Schema(description = "生成向量使用的 embedding 模型")
        String embeddingModel,

        @Schema(description = "最近一次完成向量索引的时间")
        OffsetDateTime indexedAt,

        @Schema(description = "处理失败时的错误信息")
        String errorMessage,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {

    /**
     * 将文档实体转换为详情响应对象。
     *
     * @param document 文档实体
     * @return 文档详情
     */
    public static KbDocumentDetailDto from(KbDocument document) {
        return new KbDocumentDetailDto(
                document.getId(),
                document.getKnowledgeBaseId(),
                document.getOriginalName(),
                document.getContentType(),
                document.getFileExtension(),
                document.getSizeBytes(),
                document.getSha256(),
                document.getStatus() == null ? null : document.getStatus().getValue(),
                document.getParsedFormat() == null ? null : document.getParsedFormat().getValue(),
                document.getParsedPreview(),
                document.getParsedCharCount(),
                document.getParsedBlockCount(),
                document.getPageCount(),
                document.getChunkCount(),
                document.getChunkStrategy(),
                document.getChunkSize(),
                document.getChunkOverlap(),
                document.getVectorCount(),
                document.getEmbeddingModel(),
                document.getIndexedAt(),
                document.getErrorMessage(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
