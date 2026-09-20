package com.elliot.ai.rag.service;

import com.elliot.ai.rag.query.conversation.model.ConversationMessage;

import java.util.List;
import java.util.UUID;

public interface ConversationHistoryService {

    /**
     * 保存用户在指定会话中发送的消息。
     *
     * @param conversationId 会话唯一标识
     * @param content 用户消息内容
     */
    void saveUserMessage(UUID conversationId, String content);

    /**
     * 保存助手在指定会话中回复的消息。
     *
     * @param conversationId 会话唯一标识
     * @param content 助手消息内容
     */
    void saveAssistantMessage(UUID conversationId, String content);

    /**
     * 获取指定会话的历史消息。
     *
     * @param conversationId 会话唯一标识
     * @return 按会话记录顺序返回的历史消息列表
     */
    List<ConversationMessage> getHistory(UUID conversationId);
}
