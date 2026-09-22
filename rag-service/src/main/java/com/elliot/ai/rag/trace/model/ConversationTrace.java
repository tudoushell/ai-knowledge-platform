package com.elliot.ai.rag.trace.model;

import com.elliot.ai.rag.memory.ConversationHistoryResult;
import com.elliot.ai.rag.memory.ConversationMemoryCacheStatus;
import com.elliot.ai.rag.query.conversation.model.ConversationContext;

import java.util.UUID;

public record ConversationTrace(
        UUID conversationId,
        String originalQuery,
        String standaloneQuery,
        boolean rewritten,
        boolean historyUsed,
        String memorySource,
        String cacheStatus,
        int rawMessageCount,
        int windowMessageCount,
        int historyChars
) {

    public static ConversationTrace from(
            ConversationContext context
    ) {

        ConversationHistoryResult historyResult =
                context.historyResult();

        return new ConversationTrace(
                context.conversationId(),
                context.originalQuery(),
                context.standaloneQuery(),
                context.rewritten(),
                context.historyUsed(),
                historyResult.memorySource().name(),
                historyResult.cacheStatus().name(),
                historyResult.rawMessageCount(),
                historyResult.windowMessageCount(),
                historyResult.historyChars()
        );
    }

    public boolean cacheFallback() {
        return ConversationMemoryCacheStatus.ERROR.name()
                .equals(cacheStatus);
    }
}
