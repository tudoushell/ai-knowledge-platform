package com.elliot.ai.rag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.rag.conversation.memory")
public class ConversationMemoryProperties {
    private int maxMessage = 10;
    private int maxHistoryChars = 12000;
    private Duration ttl = Duration.ofMinutes(30);
}
