package com.elliot.ai.rag.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 与 knowledge_base.status 的数据库约束保持一致。 */
@Getter
@RequiredArgsConstructor
public enum KnowledgeBaseStatus {

    ENABLED("ENABLED"),
    DISABLED("DISABLED");

    @EnumValue
    private final String value;
}
