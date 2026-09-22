package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elliot.ai.rag.config.ConversationMemoryProperties;
import com.elliot.ai.rag.entity.ConversationMessageEntity;
import com.elliot.ai.rag.mapper.ConversationMessageMapper;
import com.elliot.ai.rag.memory.ConversationHistoryResult;
import com.elliot.ai.rag.memory.ConversationMemoryCache;
import com.elliot.ai.rag.memory.ConversationMemoryCacheReadResult;
import com.elliot.ai.rag.memory.ConversationMemoryCacheStatus;
import com.elliot.ai.rag.memory.ConversationMemorySource;
import com.elliot.ai.rag.query.conversation.model.ConversationMessage;
import com.elliot.ai.rag.service.ConversationHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


@RequiredArgsConstructor
@Service
public class ConversationHistoryServiceImpl implements ConversationHistoryService {

    private final ConversationMessageMapper conversationMessageMapper;

    private final ConversationMemoryProperties properties;

    private final ConversationMemoryCache conversationMemoryCache;

    @Override
    public void saveUserMessage(UUID conversationId, String content) {
        saveMessage(conversationId, content, ConversationMessage.Role.USER);
    }

    @Override
    public void saveAssistantMessage(UUID conversationId, String content) {
        saveMessage(conversationId, content, ConversationMessage.Role.ASSISTANT);
    }

    @Override
    public ConversationHistoryResult getHistory(UUID conversationId) {
        ConversationMemoryCacheReadResult readResult = conversationMemoryCache.get(conversationId);
        List<ConversationMessage> conversationMessages;
        ConversationMemorySource memorySource = ConversationMemorySource.DATABASE;

        if (readResult.cacheStatus() == ConversationMemoryCacheStatus.HIT) {
            conversationMessages = readResult.messages();
            memorySource = ConversationMemorySource.REDIS;
        } else {
            conversationMessages = loadRecentMessagesFromDatabase(conversationId);
            conversationMemoryCache.put(conversationId, conversationMessages);
        }
        int rawMessageCount = conversationMessages.size();
        List<ConversationMessage> window = applyCharacterWindow(conversationMessages);
        List<ConversationMessage> history = alignConversationTurns(window);
        return new ConversationHistoryResult(history,
                memorySource,
                readResult.cacheStatus(),
                rawMessageCount,
                history.size(),
                calculateHistoryChars(history)
        );
    }

    private int calculateHistoryChars(List<ConversationMessage> history) {
        return history.stream().mapToInt(message -> message.content().length()).sum();
    }


    private List<ConversationMessage> loadRecentMessagesFromDatabase(UUID conversationId) {
        int maxMessage = properties.getMaxMessage();
        List<ConversationMessageEntity> entities = conversationMessageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessageEntity>()
                        .eq(ConversationMessageEntity::getConversationId, conversationId)
                        .orderByDesc(ConversationMessageEntity::getCreatedAt)
                        .last("limit " + maxMessage)

        );
        List<ConversationMessageEntity> sortedMessages = new ArrayList<>(entities);
        Collections.reverse(sortedMessages);
        return sortedMessages.stream().map(this::toConversationMessage).toList();
    }


    private List<ConversationMessage> alignConversationTurns(List<ConversationMessage> conversationMessages) {
        if (conversationMessages.isEmpty()) {
            return conversationMessages;
        }
        if (conversationMessages.get(0).role().equals(ConversationMessage.Role.USER)) {
            return conversationMessages;
        }
        for (int i = 0; i < conversationMessages.size(); i++) {
            if (ConversationMessage.Role.USER.equals(conversationMessages.get(i).role())) {
                return new ArrayList<>(conversationMessages.subList(i, conversationMessages.size()));
            }
        }
        return conversationMessages;
    }


    private List<ConversationMessage> applyCharacterWindow(List<ConversationMessage> conversationMessages) {
        int maxHistoryChars = properties.getMaxHistoryChars();
        if (maxHistoryChars <= 0) {
            return List.of();
        }
        List<ConversationMessage> result = new ArrayList<>(conversationMessages.size());
        int characterLength = 0;
        for (int i = conversationMessages.size() - 1; i >= 0; i--) {
            ConversationMessage conversationMessage = conversationMessages.get(i);
            characterLength += conversationMessage.content().length();
            if (characterLength > maxHistoryChars) {
                if (result.isEmpty()) {
                    result.add(truncateMessage(conversationMessage, maxHistoryChars));
                }
                break;
            }
            result.add(conversationMessage);
        }
        Collections.reverse(result);
        return result;
    }

    private ConversationMessage truncateMessage(ConversationMessage conversationMessage, int maxHistoryChars) {
        String content = conversationMessage.content();
        if (content.length() <= maxHistoryChars) {
            return conversationMessage;
        }
        String truncatedContent = content.substring(content.length() - maxHistoryChars);
        return new ConversationMessage(conversationMessage.role(), truncatedContent);
    }

    private void saveMessage(UUID conversationId, String content, ConversationMessage.Role role) {
        ConversationMessageEntity conversationMessageEntity = new ConversationMessageEntity();
        conversationMessageEntity.setId(UUID.randomUUID());
        conversationMessageEntity.setRole(role.name());
        conversationMessageEntity.setConversationId(conversationId);
        conversationMessageEntity.setContent(content);
        conversationMessageMapper.insert(conversationMessageEntity);
        conversationMemoryCache.append(conversationId, new ConversationMessage(role, content));
    }

    private ConversationMessage toConversationMessage(ConversationMessageEntity conversationMessageEntity) {
        return new ConversationMessage(
                ConversationMessage.Role.valueOf(conversationMessageEntity.getRole()),
                conversationMessageEntity.getContent()
        );
    }
}
