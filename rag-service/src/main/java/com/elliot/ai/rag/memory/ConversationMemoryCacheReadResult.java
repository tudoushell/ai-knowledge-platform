package com.elliot.ai.rag.memory;

import com.elliot.ai.rag.query.conversation.model.ConversationMessage;

import java.util.List;

public record ConversationMemoryCacheReadResult(
        ConversationMemoryCacheStatus cacheStatus,
        List<ConversationMessage> messages
) {

    public static ConversationMemoryCacheReadResult hit(List<ConversationMessage> messages) {
        return new ConversationMemoryCacheReadResult(ConversationMemoryCacheStatus.HIT, messages);
    }

    public static ConversationMemoryCacheReadResult miss() {
        return new ConversationMemoryCacheReadResult(ConversationMemoryCacheStatus.MISS, List.of());
    }

    public static ConversationMemoryCacheReadResult error() {
        return new ConversationMemoryCacheReadResult(ConversationMemoryCacheStatus.ERROR, List.of());
    }
}
