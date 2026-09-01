package com.elliot.ai.rag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.rag.chunk")
public class ChunkProperties {
    private int chunkSize = 500;
    private int readBufferChars = 10000;
    private int databaseBatchSize = 100;
    private int minChunkChars = 50;
    private int overlap = 0;
}
