package com.elliot.ai.rag.query.rewrite.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.rag.query-rewrite")
public class QueryRewriteProperties {
    /**
     * 是否启用 Query Rewrite。
     */
    private boolean enabled = true;

    /**
     * Query Rewrite 使用的 ChatClient Bean 名称。
     */
    private String modelCode = "qwen3.7";
}
