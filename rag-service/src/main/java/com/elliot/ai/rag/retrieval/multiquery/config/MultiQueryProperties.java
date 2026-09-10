package com.elliot.ai.rag.retrieval.multiquery.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.rag.multi-query")
public class MultiQueryProperties {
    /**
     * 多 Query 结果融合时第二层 RRF 使用的K
     */
    private int rrk = 60;
}
