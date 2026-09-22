package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.entity.Conversation;
import com.elliot.ai.rag.entity.ConversationMessageEntity;
import com.elliot.ai.rag.mapper.ConversationMapper;
import com.elliot.ai.rag.mapper.ConversationMessageMapper;
import com.elliot.ai.rag.memory.ConversationMemoryCache;
import com.elliot.ai.rag.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final ConversationMemoryCache conversationMemoryCache;

    @Override
    public Conversation createConversation(UUID knowledgeBaseId, String title) {
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setKnowledgeBaseId(knowledgeBaseId);
        conversation.setTitle(title);
        conversationMapper.insert(conversation);
        return conversation;
    }

    @Override
    public Conversation getConversation(UUID conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    @Override
    public Conversation getConversation(UUID conversationId, UUID knowledgeBaseId) {
        return conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>().eq(Conversation::getId, conversationId)
                .eq(Conversation::getKnowledgeBaseId, knowledgeBaseId));

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteConversation(UUID conversationId, UUID knowledgeBaseId) {
        Conversation conversation = getConversation(conversationId, knowledgeBaseId);
        if (conversation ==  null) {
            throw new BusinessException(ResultCode.FAIL,
                    "会话不存在");
        }
        conversationMapper.deleteById(conversation.getId());
        conversationMessageMapper.delete(new LambdaUpdateWrapper<ConversationMessageEntity>()
                .eq(ConversationMessageEntity::getConversationId, conversation.getId()));
        conversationMemoryCache.evict(conversationId);
    }
}
