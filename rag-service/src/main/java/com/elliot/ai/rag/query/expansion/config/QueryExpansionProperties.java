package com.elliot.ai.rag.query.expansion.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.rag.query-expansion")
public class QueryExpansionProperties {
    /**
     * 是否启用Query Expansion
     */
    private boolean enabled;

    private String modelCode;

    /**
     * 额外生成多少个扩展Query
     */
    private int count = 2;

}
