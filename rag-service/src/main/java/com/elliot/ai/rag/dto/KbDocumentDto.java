package com.elliot.ai.rag.dto;

import com.elliot.ai.rag.enums.KbDocumentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 知识库文档响应对象。
 *
 * <p>用于向客户端返回文档所属知识库、文件存储信息、内容摘要及处理状态。</p>
 */
@Schema(description = "知识库文档响应对象")
@Setter
@Getter
public class KbDocumentDto {

    /** 文档 ID。 */
    @Schema(description = "文档 ID")
    private UUID id;

    /** 文档所属的知识库 ID。 */
    @Schema(description = "文档所属的知识库 ID")
    private UUID knowledgeBaseId;

    /** 上传时的原始文件名。 */
    @Schema(description = "上传时的原始文件名")
    private String originalName;

    /** 文件在存储系统中的名称。 */
    @Schema(description = "文件在存储系统中的名称")
    private String storedName;

    /** 文件相对于存储根目录的路径。 */
    @Schema(description = "文件相对于存储根目录的路径")
    private String storagePath;

    /** 文件的 MIME 类型。 */
    @Schema(description = "文件的 MIME 类型")
    private String contentType;

    /** 文件扩展名。 */
    @Schema(description = "文件扩展名", example = "md")
    private String fileExtension;

    /** 文件大小，单位为字节。 */
    @Schema(description = "文件大小，单位为字节", example = "1024")
    private Long sizeBytes;

    /** 文件内容的 SHA-256 摘要。 */
    @Schema(description = "文件内容的 SHA-256 摘要")
    private String sha256;

    /** 文档当前的处理状态。 */
    @Schema(description = "文档当前的处理状态")
    private KbDocumentStatus status;

    /** 已生成的 Chunk 数量。 */
    @Schema(description = "已生成的 Chunk 数量")
    private Integer chunkCount;

    /** 已写入向量库的向量数量。 */
    @Schema(description = "已写入向量库的向量数量")
    private Integer vectorCount;

    /** 文档创建时间。 */
    @Schema(description = "文档创建时间")
    private OffsetDateTime createdAt;

    /** 文档更新时间。 */
    @Schema(description = "文档更新时间")
    private OffsetDateTime updatedAt;
}
