package com.elliot.ai.rag.service;

import com.elliot.ai.rag.entity.Conversation;

import java.util.UUID;

/**
 * 会话管理服务。
 *
 * <p>负责会话的创建、归属查询和删除，并在删除会话时清理关联消息及会话缓存。</p>
 */
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
     * @return 会话信息；不存在时返回 {@code null}
     */
    Conversation getConversation(UUID conversationId);

    /**
     * 在指定知识库范围内查询会话。
     *
     * @param conversationId  会话 ID
     * @param knowledgeBaseId 知识库 ID，用于校验会话归属
     * @return 匹配指定知识库归属的会话；不存在或归属不匹配时返回 {@code null}
     */
    Conversation getConversation(UUID conversationId, UUID knowledgeBaseId);

    /**
     * 删除指定知识库下的会话。
     *
     * <p>删除会话主体及其全部历史消息，并使对应的会话历史缓存失效。</p>
     *
     * @param conversationId  会话 ID
     * @param knowledgeBaseId 知识库 ID，用于校验会话归属
     * @throws com.elliot.ai.common.exception.BusinessException 会话不存在或不属于指定知识库时抛出
     */
    void deleteConversation(UUID conversationId, UUID knowledgeBaseId);
}
