package com.elliot.ai.rag.evaluation.conversation;

import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteCategory;
import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteEvaluationResult;
import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteEvaluationSummary;
import com.elliot.ai.rag.query.conversation.ConversationQueryRewriteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class ConversationRewriteEvaluationIT {

    @Autowired
    private ConversationQueryRewriteService rewriteService;

    @Test
    void runEvaluation() throws Exception {
        ConversationRewriteEvaluationDatasetLoader loader =
                new ConversationRewriteEvaluationDatasetLoader();

        ConversationRewriteEvaluationRunner runner =
                new ConversationRewriteEvaluationRunner(
                        rewriteService
                );

        List<ConversationRewriteEvaluationResult> results =
                runner.run(loader.load());
        ConversationRewriteEvaluationSummary summary = runner.summarized(results);

        System.out.println("========== Evaluation Summary ==========");

        System.out.printf(
                "Total Cases: %d%n",
                summary.totalCases()
        );

        System.out.printf(
                "Rewrite Decision Accuracy: %.1f%%%n",
                summary.rewriteDecisionAccuracy()
        );

        System.out.printf(
                "Exact Match Rate: %.1f%%%n",
                summary.exactMatchRate()
        );

        for (ConversationRewriteCategory category
                : ConversationRewriteCategory.values()) {

            ConversationRewriteEvaluationSummary.CategorySummary categorySummary =
                    summary.categories().get(category);

            if (categorySummary == null) {
                continue;
            }

            System.out.printf(
                    "%s | cases=%d | decision=%.1f%% | exact=%.1f%%%n",
                    category,
                    categorySummary.totalCases(),
                    categorySummary.rewriteDecisionAccuracy(),
                    categorySummary.exactMatchRate()
            );
        }
        

        for (ConversationRewriteEvaluationResult result : results) {

            System.out.printf(
                    "[%s] %s | decision=%s | exact=%s%n",
                    result.caseId(),
                    result.category(),
                    result.rewriteDecisionCorrect(),
                    result.exactMatch()
            );

            System.out.println(
                    "Expected: " + result.expectedStandaloneQuery()
            );

            System.out.println(
                    "Actual:   " + result.actualStandaloneQuery()
            );

            System.out.println();
        }
    }

}
