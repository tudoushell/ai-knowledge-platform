package com.elliot.ai.rag.service;

import com.elliot.ai.rag.memory.ConversationHistoryResult;

import java.util.UUID;

/**
 * 会话历史消息服务。
 *
 * <p>负责持久化用户和助手消息，并为对话上下文提供经过消息数量、字符数及
 * 起始轮次边界约束的历史记录。缓存的读写细节由具体实现负责，对调用方透明。</p>
 */
public interface ConversationHistoryService {

    /**
     * 持久化用户在指定会话中发送的消息，并同步更新会话历史缓存。
     *
     * @param conversationId 会话唯一标识
     * @param content        用户消息内容
     */
    void saveUserMessage(UUID conversationId, String content);

    /**
     * 持久化助手在指定会话中的回复消息，并同步更新会话历史缓存。
     *
     * @param conversationId 会话唯一标识
     * @param content        助手消息内容
     */
    void saveAssistantMessage(UUID conversationId, String content);

    /**
     * 获取用于构建对话上下文的会话历史。
     *
     * <p>优先从缓存读取；缓存未命中或访问异常时降级到数据库。结果中的消息
     * 按时间从早到晚排列，并已应用消息数量、最大字符数和以用户消息为起点的
     * 窗口约束。</p>
     *
     * <p>返回结果同时包含实际数据来源、缓存读取状态、窗口处理前后的消息数量
     * 以及最终历史消息字符数；没有历史消息时，其中的消息列表为空。</p>
     *
     * @param conversationId 会话唯一标识
     * @return 会话历史消息及其缓存、来源和窗口统计信息
     */
    ConversationHistoryResult getHistory(UUID conversationId);
}
