package com.elliot.ai.rag.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum DocumentTaskStatus {
    PENDING("PENDING"),
    RUNNING("RUNNING"),
    RETRY_WAIT("RETRY_WAIT"),
    SUCCEEDED("SUCCEEDED"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED");


    @EnumValue
    private final String value;


    public static List<String> listActiveStatuses() {
        return List.of(PENDING.value, RUNNING.value, RETRY_WAIT.value);
    }
}
