package com.elliot.ai.rag.tool;

import com.elliot.ai.rag.router.ChatClientRouter;
import com.elliot.ai.rag.router.ChatIntent;
import com.elliot.ai.rag.router.ChatIntentRouter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
public class ToolCallingIntegrationTest {

    @Autowired
    private ChatClientRouter chatClientRouter;
    @Autowired
    private KnowledgeBaseTools knowledgeBaseTools;
    @Autowired
    private ChatIntentRouter intentRouter;

    @Test
    void shouldRouteQuestion() {
        ChatIntent documentIntent = intentRouter.route("qwen3.8-flash", "当前知识库有多少篇文档");
        assertThat(documentIntent).isEqualTo(ChatIntent.DOCUMENT_COUNT);
        ChatIntent ragIntent = intentRouter.route("qwen3.8-flash", "HashMap 是什么");
        assertThat(ragIntent).isEqualTo(ChatIntent.RAG);
    }


    @Test
    void shouldCallCountDocuments() {
        String content = chatClientRouter.get("qwen3.8-flash")
                .prompt()
                .system("你是知识库助手。回答文档数量问题时必须调用工具，不能猜测。")
                .user("当前知识库有多少篇文档？")
                .toolContext(Map.of("knowledgeBaseId", UUID.fromString("d83415c9-c26c-4762-bfe9-4767efc71b36")))
                .tools(knowledgeBaseTools)
                .call()
                .content();
        System.out.println(content);
    }

}
