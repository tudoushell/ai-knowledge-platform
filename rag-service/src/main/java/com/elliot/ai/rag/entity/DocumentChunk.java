package com.elliot.ai.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 对应 document_chunk 表，保存文档切分后的文本片段。 */
@Getter
@Setter
@TableName("document_chunk")
public class DocumentChunk {

    /** 文本片段唯一标识。 */
    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    /** 所属知识库唯一标识。 */
    private UUID knowledgeBaseId;

    /** 所属文档唯一标识。 */
    private UUID documentId;

    /** 片段在原文档中的顺序，从零开始。 */
    private Integer chunkIndex;

    /** 片段的文本内容。 */
    private String content;

    /** 片段文本的字符数。 */
    private Integer charCount;

    /** 片段文本的 Token 数，未统计时为空。 */
    private Integer tokenCount;

    /** 片段文本内容的 SHA-256 摘要。 */
    private String contentHash;

    /** 片段所属章节标题。 */
    private String sectionTitle;

    /** 片段所在页码。 */
    private Integer pageNumber;

    /** 片段创建时间。 */
    private OffsetDateTime createdAt;
}
