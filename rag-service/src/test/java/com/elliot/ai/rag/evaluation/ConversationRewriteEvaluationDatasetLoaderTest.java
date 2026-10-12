package com.elliot.ai.rag.evaluation;

import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteCategory;
import com.elliot.ai.rag.query.conversation.model.ConversationMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ConversationRewriteEvaluationDatasetLoaderTest {

    @Test
    void loadsDatasetWithTypedHistoryAndRewriteFlags() throws IOException {
        var cases = new ConversationRewriteEvaluationDatasetLoader().load();

        assertEquals(8, cases.size());
        var first = cases.get(0);
        assertEquals("pronoun-001", first.caseId());
        assertEquals(ConversationRewriteCategory.PRONOUN, first.category());
        assertEquals(ConversationMessage.Role.USER, first.history().get(0).role());
        assertEquals("Spring AI ChatClient 是什么？", first.history().get(0).content());
        assertEquals("它有什么优点？", first.currentQuery());
        assertEquals("Spring AI ChatClient 有什么优点？", first.expectedStandaloneQuery());
        assertTrue(first.rewriteRequired());
        assertFalse(cases.get(4).rewriteRequired());
    }
}
