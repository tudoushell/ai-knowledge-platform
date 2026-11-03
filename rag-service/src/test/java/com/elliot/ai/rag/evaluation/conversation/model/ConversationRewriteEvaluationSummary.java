package com.elliot.ai.rag.evaluation.conversation.model;

import java.util.Map;

public record ConversationRewriteEvaluationSummary(
        int totalCases,
        double rewriteDecisionAccuracy,
        double exactMatchRate,
        Map<ConversationRewriteCategory, CategorySummary> categories
) {
    public record CategorySummary(
            int totalCases,
            double rewriteDecisionAccuracy,
            double exactMatchRate
    ) {

    }
}
