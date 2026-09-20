package com.elliot.ai.rag.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@TableName("conversation_message")
public class ConversationMessageEntity {
    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    private UUID conversationId;

    private String role;

    private String content;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
