package com.elliot.ai.rag.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.elliot.ai.rag.enums.KbDocumentStatus;
import com.elliot.ai.rag.enums.ParsedFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 对应 kb_document 表。 */
@Getter
@Setter
@TableName("kb_document")
public class KbDocument {

    /** 文档唯一标识。 */
    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    /** 所属知识库唯一标识。 */
    private UUID knowledgeBaseId;

    /** 用户上传时的原始文件名。 */
    private String originalName;

    /** 存储后的文件名。 */
    private String storedName;

    /** 原始文件在存储根目录下的相对路径。 */
    private String storagePath;

    /** 文件的 MIME 类型。 */
    private String contentType;

    /** 文件扩展名。 */
    private String fileExtension;

    /** 文件大小，单位为字节。 */
    private Long sizeBytes;

    /** 文件内容的 SHA-256 摘要。 */
    private String sha256;

    /** 解析后文件的存储格式，例如纯文本或结构化 JSONL。 */
    private ParsedFormat parsedFormat;
    /**
     * 解析后的PDF页数
     */
    private Integer pageCount;

    /**
     * 解析后的结构化块数量
     */
    private Integer parsedBlockCount;

    /**
     * 分片数量
     */
    private Integer chunkCount;

    /** 文本切分策略，例如 TOKEN、按段落或按标题切分。 */
    private String chunkStrategy;

    /** 切分时每个 Chunk 的目标大小，具体单位由切分策略决定。 */
    private Integer chunkSize;

    /** 相邻 Chunk 的重叠大小，用于保留跨片段上下文。 */
    private Integer chunkOverlap;

    /** 已成功写入向量库的向量数量。 */
    private Integer vectorCount;

    /** 生成向量时使用的 embedding 模型名称。 */
    private String embeddingModel;

    /** 最近一次成功完成向量索引的时间。 */
    private OffsetDateTime indexedAt;

    /** 文档当前处理状态。 */
    private KbDocumentStatus status;

    /** 解析后文本文件在解析目录下的相对路径。 */
    private String parsedStoragePath;

    /** 解析文本的预览内容。 */
    private String parsedPreview;

    /** 解析文本的字符数。 */
    private Long parsedCharCount;

    /** 文档处理失败时的错误信息。 */
    private String errorMessage;

    /** 创建时间，插入记录时自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /** 更新时间，插入和更新记录时自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
