package com.elliot.ai.common.prompt;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从当前应用 Classpath 的 {@code prompts/{scene}/} 目录加载并缓存提示词模板。
 */
public class PromptTemplateConfig {

    private final Map<String, PromptTemplate> cache = new ConcurrentHashMap<>();

    public PromptTemplate getTemplate(String sceneCode) {
        String scene = resolveScene(sceneCode);
        return cache.computeIfAbsent(scene, this::loadPromptTemplate);
    }

    private String resolveScene(String sceneCode) {
        if (sceneCode == null || sceneCode.isBlank()) {
            return "general";
        }
        return sceneCode;
    }

    private PromptTemplate loadPromptTemplate(String scene) {
        String systemPrompt = readPromptTemplate(scene, "system.md");
        String userTemplate = readPromptTemplate(scene, "user.md");
        return new PromptTemplate(scene, systemPrompt, userTemplate);
    }

    private String readPromptTemplate(String scene, String fileName) {
        String path = "prompts/" + scene + "/" + fileName;
        ClassPathResource resource = new ClassPathResource(path);

        if (!resource.exists()) {
            throw new PromptTemplateException(
                    "PROMPT_NOT_FOUND",
                    "Prompt 文件不存在：" + path
            );
        }

        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        }
        catch (IOException exception) {
            throw new PromptTemplateException(
                    "PROMPT_READ_ERROR",
                    "读取 Prompt 文件失败：" + path
            );
        }
    }
}
