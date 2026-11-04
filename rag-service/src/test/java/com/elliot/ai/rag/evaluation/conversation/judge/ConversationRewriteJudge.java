package com.elliot.ai.rag.evaluation.conversation.judge;

import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteEvaluationCase;
import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteJudgeResult;

public interface ConversationRewriteJudge {
    ConversationRewriteJudgeResult judge(ConversationRewriteEvaluationCase evaluationCase, String actualStandaloneQuery);

}
