package com.elliot.ai.rag.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class EmbeddingConfig {

    /**
     * Embedding 请求专用重试策略。
     *
     * <p>单次请求失败后立即将异常交给文档任务处理。文档任务会按
     * {@code RETRY_WAIT} 状态进行持久化的退避重试，避免 Spring AI 默认的
     * 长时间指数退避与任务重试策略叠加。</p>
     */
    private RetryTemplate embeddingRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(1)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("http://localhost:11434")
                .apiKey("ollama")
                .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model("nomic-embed-text")
                .build();

        return new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.NONE,
                options,
                embeddingRetryTemplate()
        );
    }
}
