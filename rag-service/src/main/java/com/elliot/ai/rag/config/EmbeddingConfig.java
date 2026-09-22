package com.elliot.ai.rag.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .baseUrl("http://localhost:11434/v1")
                .apiKey("ollama")
                .model("nomic-embed-text")
                // 仅请求一次，失败后交由文档任务的 RETRY_WAIT 状态做持久化退避重试。
                .maxRetries(0)
                .build();

        return OpenAiEmbeddingModel.builder()
                .metadataMode(MetadataMode.NONE)
                .options(options)
                .build();
    }
}
