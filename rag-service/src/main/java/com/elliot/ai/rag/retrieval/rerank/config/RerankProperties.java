package com.elliot.ai.rag.retrieval.rerank.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.rag.rerank")
public class RerankProperties {

    /**
     * 是否启用Rerank
     */
    private boolean enabled;

    /**
     * 无法调通 rerank 服务，是否进行降级
     */
    private boolean fallbackEnabled;

    /**
     * Rerank 服务地址
     */
    private String baseUrl;

    /**
     * Rerank API path
     */
    private String path;

    /**
     * Rerank 模型名称
     */
    private String model;

    /**
     * API Key
     *
     */
    private String apiKey;

    private Duration connectTimeout = Duration.ofSeconds(1);
    private Duration readTimeout = Duration.ofSeconds(3);


}
