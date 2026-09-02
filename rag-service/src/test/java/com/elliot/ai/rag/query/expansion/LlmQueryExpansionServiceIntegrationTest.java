package com.elliot.ai.rag.query.expansion;

import com.elliot.ai.rag.AiRagServiceApplication;
import com.elliot.ai.rag.query.model.QueryExpansionResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用已配置的模型客户端验证查询扩展链路。
 *
 * <p>该测试会访问真实模型服务，仅在显式设置 {@code RUN_LLM_INTEGRATION_TESTS=true}
 * 且提供 {@code alibaba_key} 环境变量时执行。</p>
 */

@Tag("integration")
@SpringBootTest(classes = AiRagServiceApplication.class)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_LLM_INTEGRATION_TESTS", matches = "true")
@EnabledIfEnvironmentVariable(named = "alibaba_key", matches = ".+")
class LlmQueryExpansionServiceIntegrationTest {

    @Autowired
    private QueryExpansionService queryExpansionService;

    @Test
    void shouldExpandQueryThroughConfiguredLlm() {
        String baseQuery = "HashMap put 方法的执行流程是什么，包括哈希计算、数组下标定位、空桶插入、链表或红黑树处理、元素数量判断和扩容 resize 机制？";

        QueryExpansionResult result = queryExpansionService.expand(baseQuery);

        assertThat(result.baseQuery()).isEqualTo(baseQuery);
        assertThat(result.expandedQueries())
                .hasSize(2)
                .allSatisfy(expandedQuery -> {
                    assertThat(expandedQuery).isNotBlank();
                    assertThat(expandedQuery).isNotEqualTo(baseQuery);
                })
                .doesNotHaveDuplicates();
        assertThat(result.retrievalQueries()).containsExactly(baseQuery, result.expandedQueries().get(0), result.expandedQueries().get(1));
    }
}
