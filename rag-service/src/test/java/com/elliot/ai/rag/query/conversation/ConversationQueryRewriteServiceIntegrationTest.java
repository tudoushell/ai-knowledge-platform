package com.elliot.ai.rag.query.conversation;

import com.elliot.ai.rag.AiRagServiceApplication;
import com.elliot.ai.rag.query.conversation.model.ConversationMessage;
import com.elliot.ai.rag.query.conversation.model.ConversationQueryRewriteResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.elliot.ai.rag.query.conversation.model.ConversationMessage.Role.ASSISTANT;
import static com.elliot.ai.rag.query.conversation.model.ConversationMessage.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用已配置的 Qwen 客户端验证会话查询改写链路。
 *
 * <p>该测试会访问真实模型服务，仅在显式设置 {@code RUN_LLM_INTEGRATION_TESTS=true}
 * 且提供 {@code alibaba_key} 环境变量时执行。</p>
 */
@Tag("integration")
@SpringBootTest(classes = AiRagServiceApplication.class)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_LLM_INTEGRATION_TESTS", matches = "true")
@EnabledIfEnvironmentVariable(named = "alibaba_key", matches = ".+")
class ConversationQueryRewriteServiceIntegrationTest {

    @Autowired
    private ConversationQueryRewriteService conversationQueryRewriteService;

    @Test
    void shouldResolveContextAndRewriteAsStandaloneQuery() {
        List<ConversationMessage> history = List.of(
                new ConversationMessage(USER, "Spring AI 支持哪些向量数据库？"),
                new ConversationMessage(ASSISTANT, "支持 Milvus、Elasticsearch、Redis 等。")
        );
        String currentQuery = "它怎么连接 Elasticsearch？";

        ConversationQueryRewriteResult result = conversationQueryRewriteService.rewrite(history, currentQuery);

        assertThat(result.originQuery()).isEqualTo(currentQuery);
        assertThat(result.standaloneQuery())
                .isNotBlank()
                .isNotEqualTo(currentQuery)
                .containsIgnoringCase("Spring AI")
                .containsIgnoringCase("Elasticsearch");
        assertThat(result.rewritten()).isTrue();
        assertThat(result.historyUsed()).isTrue();
    }

    @Test
    void shouldReturnOriginalQueryWhenHistoryIsEmpty() {
        String currentQuery = "Spring AI 如何连接 Elasticsearch？";

        ConversationQueryRewriteResult result = conversationQueryRewriteService.rewrite(
                List.of(),
                currentQuery
        );

        assertThat(result.originQuery()).isEqualTo(currentQuery);
        assertThat(result.standaloneQuery()).isEqualTo(currentQuery);
        assertThat(result.rewritten()).isFalse();
        assertThat(result.historyUsed()).isFalse();
    }
}
