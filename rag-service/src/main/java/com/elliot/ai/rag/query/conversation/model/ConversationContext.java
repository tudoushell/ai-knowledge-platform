package com.elliot.ai.rag.query.conversation.model;

import java.util.List;
import java.util.UUID;

public record ConversationContext(
        UUID conversationId,
        List<ConversationMessage> history,
        String originalQuery,
        String standaloneQuery,
        boolean rewritten,
        boolean historyUsed
) {
}
