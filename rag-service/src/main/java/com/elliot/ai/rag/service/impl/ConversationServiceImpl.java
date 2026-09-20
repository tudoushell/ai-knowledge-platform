package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elliot.ai.rag.entity.Conversation;
import com.elliot.ai.rag.mapper.ConversationMapper;
import com.elliot.ai.rag.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;

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
}
