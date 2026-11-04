package com.elliot.ai.rag.evaluation.conversation;

import com.elliot.ai.rag.evaluation.conversation.judge.ConversationRewriteJudge;
import com.elliot.ai.rag.evaluation.conversation.judge.LlmConversationRewriteJudge;
import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteEvaluationCase;
import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteJudgeResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(LlmConversationRewriteJudge.class)
public class ConversationRewriteJudgeIT {

    @Autowired
    private ConversationRewriteJudge judge;

    @Test
    void conversationRewriteJudgeResultTest() throws Exception {
        List<ConversationRewriteEvaluationCase> cases = new ConversationRewriteEvaluationDatasetLoader().load();
        ConversationRewriteEvaluationCase evaluationCase = cases.stream()
                .filter(item -> "pronoun-002".equals(item.caseId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing evaluation case: pronoun-002"));
        ConversationRewriteJudgeResult result = judge.judge(evaluationCase, "Rerank 一般在 RAG 检索流程的什么时候执行？");
        System.out.println(result);
        assertThat(result).isNotNull();
        assertThat(result.reason()).isNotBlank();
        assertThat(result.passed()).as("Judge reason: %s", result.reason()).isTrue();
    }
}
