package com.elliot.ai.common.prompt;

/** 提示词模板加载失败时抛出的异常。 */
public class PromptTemplateException extends RuntimeException {

    private final String code;

    public PromptTemplateException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
