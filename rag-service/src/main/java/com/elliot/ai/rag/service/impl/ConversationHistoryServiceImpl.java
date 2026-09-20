package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elliot.ai.rag.entity.ConversationMessageEntity;
import com.elliot.ai.rag.mapper.ConversationMessageMapper;
import com.elliot.ai.rag.query.conversation.model.ConversationMessage;
import com.elliot.ai.rag.service.ConversationHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@RequiredArgsConstructor
@Service
public class ConversationHistoryServiceImpl implements ConversationHistoryService {

    private final ConversationMessageMapper conversationMessageMapper;

    @Override
    public void saveUserMessage(UUID conversationId, String content) {
        saveMessage(conversationId,content, ConversationMessage.Role.USER);
    }

    @Override
    public void saveAssistantMessage(UUID conversationId, String content) {
        saveMessage(conversationId,content, ConversationMessage.Role.ASSISTANT);
    }

    @Override
    public List<ConversationMessage> getHistory(UUID conversationId) {
        List<ConversationMessageEntity> entities = conversationMessageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessageEntity>()
                        .eq(ConversationMessageEntity::getConversationId, conversationId)
                        .orderByAsc(ConversationMessageEntity::getCreatedAt)

        );
        return entities.stream().map(this::toConversationMessage).toList();
    }

    private void saveMessage(UUID conversationId, String content, ConversationMessage.Role role) {
        ConversationMessageEntity conversationMessageEntity = new ConversationMessageEntity();
        conversationMessageEntity.setId(UUID.randomUUID());
        conversationMessageEntity.setRole(role.name());
        conversationMessageEntity.setConversationId(conversationId);
        conversationMessageEntity.setContent(content);
        conversationMessageMapper.insert(conversationMessageEntity);
    }

    private ConversationMessage toConversationMessage(ConversationMessageEntity conversationMessageEntity) {
        return new ConversationMessage(
                ConversationMessage.Role.valueOf(conversationMessageEntity.getRole()),
                conversationMessageEntity.getContent()
        );
    }
}
