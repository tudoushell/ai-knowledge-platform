package com.elliot.ai.rag.query.conversation.model;

import com.elliot.ai.rag.memory.ConversationHistoryResult;

import java.util.UUID;

public record ConversationContext(
        UUID conversationId,
        ConversationHistoryResult historyResult,
        String originalQuery,
        String standaloneQuery,
        boolean rewritten,
        boolean historyUsed
) {
}
