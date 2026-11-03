package com.elliot.ai.rag.evaluation.conversation;

import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteEvaluationCase;
import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteEvaluationResult;
import com.elliot.ai.rag.query.conversation.ConversationQueryRewriteService;
import com.elliot.ai.rag.query.conversation.model.ConversationQueryRewriteResult;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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


}
