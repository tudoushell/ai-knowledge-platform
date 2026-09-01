package com.elliot.ai.rag.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文档解析结果的存储格式。
 */
@Getter
@RequiredArgsConstructor
public enum ParsedFormat {

    /** 解析结果是连续的纯文本内容。 */
    PLAIN_TEXT("PLAIN_TEXT"),

    /** 解析结果按行存储，每行是一条独立的 JSON 结构化记录。 */
    STRUCTURED_JSONL("STRUCTURED_JSONL");

    /** 持久化到 {@code kb_document.parsed_format} 的枚举值。 */
    @EnumValue
    private final String value;
}
