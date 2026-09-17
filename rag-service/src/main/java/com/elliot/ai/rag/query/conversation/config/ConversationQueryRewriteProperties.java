package com.elliot.ai.rag.query.conversation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "app.rag.conversation.query-rewrite"
)
public record ConversationQueryRewriteProperties(
        boolean enabled,
        String modelCode
) {
}