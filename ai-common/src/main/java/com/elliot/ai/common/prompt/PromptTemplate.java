package com.elliot.ai.common.prompt;

/**
 * 一个场景对应的系统提示词与用户提示词模板。
 *
 * @param scene        提示词场景标识
 * @param systemPrompt 系统提示词内容
 * @param userTemplate 用户提示词模板内容
 */
public record PromptTemplate(
        String scene,
        String systemPrompt,
        String userTemplate
) {
}
