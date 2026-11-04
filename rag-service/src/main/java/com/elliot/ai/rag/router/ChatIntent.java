package com.elliot.ai.rag.router;

/**
 * 聊天路由，针对于文档数量，需调用Tool去查询数据库
 */
public enum ChatIntent {
    DOCUMENT_COUNT,
    RAG;

    public static ChatIntent from(String intent) {
        return ChatIntent.valueOf(intent.toUpperCase());
    }
}
