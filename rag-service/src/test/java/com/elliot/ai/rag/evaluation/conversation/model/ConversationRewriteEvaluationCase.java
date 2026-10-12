package com.elliot.ai.rag.evaluation.conversation.model;

import com.elliot.ai.rag.query.conversation.model.ConversationMessage;

import java.util.List;

public record ConversationRewriteEvaluationCase(
        String caseId,
        ConversationRewriteCategory category,
        List<ConversationMessage> history,
        String currentQuery,
        boolean rewriteRequired,
        String expectedStandaloneQuery
) {
}
