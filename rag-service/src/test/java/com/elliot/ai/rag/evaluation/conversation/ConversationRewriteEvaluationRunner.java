package com.elliot.ai.rag.evaluation.conversation;

import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteCategory;
import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteEvaluationCase;
import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteEvaluationResult;
import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteEvaluationSummary;
import com.elliot.ai.rag.query.conversation.ConversationQueryRewriteService;
import com.elliot.ai.rag.query.conversation.model.ConversationQueryRewriteResult;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ConversationRewriteEvaluationRunner {

    private final ConversationQueryRewriteService rewriteService;


    public List<ConversationRewriteEvaluationResult> run(List<ConversationRewriteEvaluationCase> cases) {
        List<ConversationRewriteEvaluationResult> results =
                new ArrayList<>();
        for (ConversationRewriteEvaluationCase evaluationCase : cases) {
            results.add(evaluate(evaluationCase));
        }
        return results;
    }

    private ConversationRewriteEvaluationResult evaluate(ConversationRewriteEvaluationCase evaluationCase) {
        ConversationQueryRewriteResult rewriteResult = rewriteService.rewrite(evaluationCase.history(), evaluationCase.currentQuery());
        boolean exactMatch = rewriteResult.standaloneQuery().equals(evaluationCase.expectedStandaloneQuery());

        boolean rewriteDecisionCorrect = evaluationCase.rewriteRequired() == rewriteResult.rewritten();
        return new ConversationRewriteEvaluationResult(
                evaluationCase.caseId(),
                evaluationCase.category(),
                evaluationCase.currentQuery(),
                evaluationCase.expectedStandaloneQuery(),
                rewriteResult.standaloneQuery(),
                evaluationCase.rewriteRequired(),
                rewriteResult.rewritten(),
                rewriteDecisionCorrect,
                exactMatch
        );
    }


    public ConversationRewriteEvaluationSummary summarized(
            List<ConversationRewriteEvaluationResult> results
    ) {
        if (CollectionUtils.isEmpty(results)) {
            throw new IllegalArgumentException("Evaluation results must not be empty");
        }
        int totalCases = results.size();
        long decisionCorrectCount = results.stream()
                .filter(ConversationRewriteEvaluationResult::rewriteDecisionCorrect)
                .count();
        long exactMatchCount = results.stream()
                .filter(ConversationRewriteEvaluationResult::exactMatch)
                .count();
        Map<ConversationRewriteCategory, ConversationRewriteEvaluationSummary.CategorySummary> categories =
                summarizeCategories(results);
        return new ConversationRewriteEvaluationSummary(
                totalCases,
                percentage(decisionCorrectCount, totalCases),
                percentage(exactMatchCount, totalCases),
                categories);

    }

    private Map<ConversationRewriteCategory,
            ConversationRewriteEvaluationSummary.CategorySummary> summarizeCategories(
            List<ConversationRewriteEvaluationResult> results
    ) {
        Map<ConversationRewriteCategory,
                ConversationRewriteEvaluationSummary.CategorySummary> categories = new EnumMap<>(ConversationRewriteCategory.class);
        for (ConversationRewriteCategory category : ConversationRewriteCategory.values()) {
            List<ConversationRewriteEvaluationResult> categoryResults = results.stream()
                    .filter(result -> result.category().equals(category))
                    .toList();
            if (CollectionUtils.isEmpty(categoryResults)) {
                continue;
            }
            long decisionCorrectCount = categoryResults.stream()
                    .filter(ConversationRewriteEvaluationResult::rewriteDecisionCorrect)
                    .count();
            long exactMatchCount = categoryResults.stream()
                    .filter(ConversationRewriteEvaluationResult::exactMatch)
                    .count();
            int total = categoryResults.size();
            categories.put(category, new ConversationRewriteEvaluationSummary.CategorySummary(
                    total,
                    percentage(decisionCorrectCount, total),
                    percentage(exactMatchCount, total)
            ));

        }
        return categories;
    }

    private double percentage(long decisionCorrectCount, int total) {
        return decisionCorrectCount * 100.0 / total;
    }


}
