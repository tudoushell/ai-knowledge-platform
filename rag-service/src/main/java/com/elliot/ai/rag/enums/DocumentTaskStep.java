package com.elliot.ai.rag.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum DocumentTaskStep {
    PARSE("PARSE"),
    CHUNK("CHUNK"),
    INDEX("INDEX"),
    COMPLETE("COMPLETE"),;

    @EnumValue
    private final String value;
}
