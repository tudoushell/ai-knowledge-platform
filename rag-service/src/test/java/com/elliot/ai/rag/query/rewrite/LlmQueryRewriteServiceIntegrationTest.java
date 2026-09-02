package com.elliot.ai.rag.query.rewrite;

import com.elliot.ai.rag.AiRagServiceApplication;
import com.elliot.ai.rag.query.model.QueryRewriteResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用已配置的 Qwen 客户端验证查询改写链路。
 *
 * <p>该测试会访问真实模型服务，仅在显式设置 {@code RUN_LLM_INTEGRATION_TESTS=true}
 * 且提供 {@code alibaba_key} 环境变量时执行。</p>
 */
@Slf4j
@Tag("integration")
@SpringBootTest(classes = AiRagServiceApplication.class)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_LLM_INTEGRATION_TESTS", matches = "true")
@EnabledIfEnvironmentVariable(named = "alibaba_key", matches = ".+")
class LlmQueryRewriteServiceIntegrationTest {

    @Autowired
    private QueryRewriteService queryRewriteService;

    @Test
    void shouldRewriteQueryThroughConfiguredLlm() {
        String originalQuery = "hash map Put 流程";

        QueryRewriteResult result = queryRewriteService.rewrite(originalQuery);
        log.info("original query: {}, rewritten query： {}", result.originalQuery(), result.rewrittenQuery());
        assertThat(result.originalQuery()).isEqualTo(originalQuery);
        assertThat(result.rewrittenQuery()).isNotBlank();
        assertThat(result.rewritten()).isEqualTo(!originalQuery.equals(result.rewrittenQuery()));
    }
}
