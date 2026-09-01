package com.elliot.ai.rag.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChunkConfiguration {

    @Bean
    public TokenTextSplitter tokenTextSplitter(ChunkProperties properties) {
       return TokenTextSplitter.builder()
               // 单个 Chunk 的目标最大 Token 数，不是字符数。
               .withChunkSize(properties.getChunkSize())
               // 仅用于优先按标点断句：标点位于该字符数之后时，才在标点处提前截断。
               .withMinChunkSizeChars(properties.getMinChunkChars())
               // 小于或等于该字符数的 Chunk 不生成向量，避免无意义的短文本入库。
               .withMinChunkLengthToEmbed(properties.getMinChunkChars())
               // 单次传入 splitter 的一个 Document 最多执行的常规切分次数，防止超大文本过度切分。
               .withMaxNumChunks(1000)
               // 是否保留原文换行符；false 时换行符会被替换为空格。
               .withKeepSeparator(true)
               .build();
    }
}
