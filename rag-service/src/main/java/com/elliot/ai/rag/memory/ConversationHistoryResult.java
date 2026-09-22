package com.elliot.ai.rag.memory;

import com.elliot.ai.rag.query.conversation.model.ConversationMessage;

import java.util.List;

public record ConversationHistoryResult(
        List<ConversationMessage> messages,
        ConversationMemorySource memorySource,
        ConversationMemoryCacheStatus cacheStatus,
        int rawMessageCount,
        int windowMessageCount,
        int historyChars
) {
}
