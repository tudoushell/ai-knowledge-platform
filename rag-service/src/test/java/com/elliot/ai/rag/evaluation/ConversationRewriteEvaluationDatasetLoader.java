package com.elliot.ai.rag.evaluation;

import com.elliot.ai.common.util.JsonUtils;
import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteEvaluationCase;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 从 classpath 中加载会话查询改写评估数据集。
 */
public class ConversationRewriteEvaluationDatasetLoader {
    private static final String DATASET_PATH = "evaluation/conversation-rewrite-cases.json";

    /**
     * 以 UTF-8 读取 JSON 数据集，并转换为评估用例列表。
     *
     * @return 数据集中的评估用例
     * @throws IOException 数据集不存在或读取失败时抛出
     * @throws IllegalArgumentException JSON 不合法或无法转换为评估用例时抛出
     */
    public List<ConversationRewriteEvaluationCase> load() throws IOException {
        ClassPathResource resource = new ClassPathResource(DATASET_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return JsonUtils.fromJson(json, new TypeReference<List<ConversationRewriteEvaluationCase>>() {});
        }
    }
}
