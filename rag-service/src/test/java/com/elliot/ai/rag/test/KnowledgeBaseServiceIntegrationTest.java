package com.elliot.ai.rag.test;

import com.elliot.ai.rag.service.KnowledgeBaseService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class KnowledgeBaseServiceIntegrationTest {
    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private ChatClient chatClient;


    @Test
    void chat() {
        String content = chatClient.prompt().user("你是什么模型").call().content();
        System.out.println(content);
    }


    @Test
    void shouldCreateAndQueryKnowledgeBase() {
                knowledgeBaseService.create(
                        "Java技术知识库",
                        "保存 Java、Spring 和数据库相关文档"
                );
    }
}
