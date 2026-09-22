package com.elliot.ai.rag.memory;

public enum ConversationMemoryCacheStatus {
    /**
     * Redis 有缓存
     */
    HIT,
    /**
     * Redis 正常，但 Key 不存在
     */
    MISS,
    /**
     * Redis 连接失败 / JSON 损坏
     */
    ERROR
}
