package com.elliot.ai.rag.evaluation;

public enum RetrievalQueryCategory {
    /**
     * 口语化表达
     */
    COLLOQUIAL,
    /**
     * 精确技术关键词
     */
    EXACT_KEYWORD,
    /**
     * 很短，上下文缺少的 Query
     */
    SHORT_QUERY,
    /**
     * 需要多个 Chunk 才能较完整回答
     */
    MULTI_CHUNK,
    /**
     * 普通技术问题
     */
    GENERAL
}
