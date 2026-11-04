package com.elliot.ai.rag.evaluation.conversation;

import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteJudgeResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class ConversationRewriteJudgeResultTest {


    @Test
    void rewriteJudgeResultTest() {
        ConversationRewriteJudgeResult pass = new ConversationRewriteJudgeResult(true, true, true, true, "pass");
        assertThat(pass.passed()).isTrue();
        ConversationRewriteJudgeResult contextFalse =
                new ConversationRewriteJudgeResult(false, true, true, true, "false");
        assertThat(contextFalse.passed()).isFalse();
        ConversationRewriteJudgeResult intentPreservedFalse =
                new ConversationRewriteJudgeResult(true, false, true, true, "false");
        assertThat(intentPreservedFalse.passed()).isFalse();
        ConversationRewriteJudgeResult standaloneFalse =
                new ConversationRewriteJudgeResult(true, true, false, true, "false");
        assertThat(standaloneFalse.passed()).isFalse();
        ConversationRewriteJudgeResult noUnsupportedInfoFalse =
                new ConversationRewriteJudgeResult(true, true, true, false, "false");
        assertThat(noUnsupportedInfoFalse.passed()).isFalse();
    }

}
