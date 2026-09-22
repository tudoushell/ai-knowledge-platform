package com.elliot.ai.rag.memory;

import com.elliot.ai.rag.query.conversation.model.ConversationMessage;

import java.util.List;
import java.util.UUID;

/**
 * 会话历史缓存访问接口。
 *
 * <p>用于按会话 ID 缓存和读取参与上下文构建的历史消息，具体的缓存介质、
 * 序列化方式及过期策略由实现类负责。</p>
 */
public interface ConversationMemoryCache {

    /**
     * 获取指定会话的历史消息缓存。
     *
     * <p>返回结果通过状态区分以下情况：</p>
     * <ul>
     *     <li>{@link ConversationMemoryCacheStatus#HIT}：缓存命中，消息由结果携带；</li>
     *     <li>{@link ConversationMemoryCacheStatus#MISS}：缓存服务正常，但会话缓存不存在；</li>
     *     <li>{@link ConversationMemoryCacheStatus#ERROR}：缓存访问或数据解析失败。</li>
     * </ul>
     * <p>对于 {@code MISS} 和 {@code ERROR}，调用方可降级到持久化存储读取。</p>
     *
     * @param conversationId 会话 ID
     * @return 包含缓存状态及历史消息的读取结果
     */
    ConversationMemoryCacheReadResult get(UUID conversationId);

    /**
     * 写入或更新指定会话的历史消息缓存。
     *
     * @param conversationId 会话 ID
     * @param messages       待缓存的历史消息列表
     */
    void put(UUID conversationId, List<ConversationMessage> messages);

    /**
     * 将一条消息追加到指定会话的现有历史缓存中。
     *
     * <p>如果该会话尚未建立缓存，则不创建不完整的缓存，后续读取时应从
     * 持久化存储加载完整的历史消息。</p>
     *
     * @param conversationId 会话 ID
     * @param message        待追加的消息
     */
    void append(UUID conversationId, ConversationMessage message);

    /**
     * 使指定会话的历史缓存失效。
     *
     * <p>缓存删除后，后续读取应从持久化存储重新加载并构建缓存。</p>
     *
     * @param conversationId 会话 ID
     */
    void evict(UUID conversationId);
}
