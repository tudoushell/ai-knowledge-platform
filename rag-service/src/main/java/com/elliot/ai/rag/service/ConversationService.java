package com.elliot.ai.rag.service;

import com.elliot.ai.rag.entity.Conversation;

import java.util.UUID;

public interface ConversationService {

    /**
     * 在指定知识库下创建会话。
     *
     * @param knowledgeBaseId 会话所属的知识库 ID
     * @param title           会话标题
     * @return 创建后的会话
     */
    Conversation createConversation(UUID knowledgeBaseId, String title);

    /**
     * 根据会话 ID 查询会话。
     *
     * @param conversationId 会话 ID
     * @return 会话信息
     */
    Conversation getConversation(UUID conversationId);

    /**
     * 在指定知识库范围内查询会话。
     *
     * @param conversationId 会话 ID
     * @param knowledgeBaseId 知识库 ID，用于校验会话归属
     * @return 会话信息
     */
    Conversation getConversation(UUID conversationId, UUID knowledgeBaseId);
}
