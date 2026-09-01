package com.elliot.ai.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.elliot.ai.rag.enums.KnowledgeBaseStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 对应 knowledge_base 表。 */
@Getter
@Setter
@TableName("knowledge_base")
public class KnowledgeBase {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    private String name;

    private String description;

    private KnowledgeBaseStatus status;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
