package com.elliot.ai.rag.tool;

import com.elliot.ai.rag.security.tool.ToolContextUserResolver;
import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.common.util.JsonUtils;
import com.elliot.ai.rag.entity.KnowledgeBase;
import com.elliot.ai.rag.service.KbDocumentService;
import com.elliot.ai.rag.service.KnowledgeBaseService;
import com.elliot.ai.rag.tool.result.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class KnowledgeBaseToolsTest {

    @InjectMocks
    private KnowledgeBaseTools knowledgeBaseTools;

    @Mock
    private KbDocumentService kbDocumentService;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;


    @BeforeEach
    void setUp() {
        knowledgeBaseTools = new KnowledgeBaseTools(
                kbDocumentService,
                knowledgeBaseService,
                new ToolContextUserResolver()
        );
    }

    @Test
    void shouldSerializeCountDocumentsResult() throws Exception {

        UUID knowledgeBaseId = UUID.randomUUID();
        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId))
                .thenReturn(new KnowledgeBase());
        when(kbDocumentService.count(any()))
                .thenReturn(12L);
        ToolCallback callback = Arrays.stream(
                        ToolCallbacks.from(knowledgeBaseTools)
                )
                .filter(each -> "countDocuments".equals(
                        each.getToolDefinition().name()
                ))
                .findFirst()
                .orElseThrow();
        ToolContext toolContext = new ToolContext(Map.of("knowledgeBaseId", knowledgeBaseId, "userId", UUID.randomUUID().toString()));
        String call = callback.call("{}", toolContext);
        ToolResult toolResult = JsonUtils.fromJson(call, ToolResult.class);
        assertThat(toolResult).isNotNull();
        assertThat(toolResult.success()).isTrue();
        assertThat(toolResult.data()).isEqualTo(12);
    }

    @Test
    void shouldThrowExceptionWhenFailFastEnabled() {

        BusinessException cause = new BusinessException(
                ResultCode.PARAM_ERROR,
                "知识库上下文缺失"
        );

        ToolCallback callback = Arrays.stream(
                        ToolCallbacks.from(knowledgeBaseTools)
                )
                .filter(each -> "countDocuments".equals(
                        each.getToolDefinition().name()
                ))
                .findFirst()
                .orElseThrow();

        ToolExecutionException exception =
                new ToolExecutionException(
                        callback.getToolDefinition(),
                        cause
                );

        DefaultToolExecutionExceptionProcessor processor =
                DefaultToolExecutionExceptionProcessor.builder()
                        .alwaysThrow(true)
                        .build();

        assertThatThrownBy(() -> processor.process(exception))
                .isInstanceOf(ToolExecutionException.class)
                .hasCauseInstanceOf(BusinessException.class);
    }


    @Test
    void shouldReturnErrorMessageByDefault() {

        BusinessException cause = new BusinessException(
                ResultCode.PARAM_ERROR,
                "知识库上下文缺失"
        );
        ToolCallback callback = Arrays.stream(
                        ToolCallbacks.from(knowledgeBaseTools)
                )
                .filter(each -> "countDocuments".equals(
                        each.getToolDefinition().name()
                ))
                .findFirst()
                .orElseThrow();

        ToolExecutionException exception =
                new ToolExecutionException(callback.getToolDefinition(), cause);
        DefaultToolExecutionExceptionProcessor processor = DefaultToolExecutionExceptionProcessor.builder()
                .alwaysThrow(false)
                .build();
        String process = processor.process(exception);
        assertThat(process).isEqualTo("知识库上下文缺失");
    }

    @Test
    void shouldWrapToolBusinessException() {

        ToolCallback callback = Arrays.stream(
                        ToolCallbacks.from(knowledgeBaseTools)
                )
                .filter(each -> "countDocuments".equals(
                        each.getToolDefinition().name()
                ))
                .findFirst()
                .orElseThrow();

        ToolContext context = new ToolContext(
                Map.of("requestId", "test", "userId", UUID.randomUUID().toString())
        );

        assertThatThrownBy(() -> callback.call("{}", context))
                .isInstanceOf(ToolExecutionException.class)
                .hasCauseInstanceOf(BusinessException.class)
                .hasMessage("知识库上下文缺失");
    }

    @Test
    void shouldRequireKeywordOnly() {
        ToolCallback callback = Arrays.stream(
                        ToolCallbacks.from(knowledgeBaseTools)
                )
                .filter(each -> each.getToolDefinition()
                        .name().equals("searchDocuments"))
                .findFirst()
                .orElseThrow();

        JsonNode schema = JsonMapper.builder()
                .build()
                .readTree(callback.getToolDefinition().inputSchema());

        JsonNode required = schema.path("required");

        assertThat(required.isArray()).isTrue();
        assertThat(required.size()).isEqualTo(1);
        assertThat(required.get(0).asString())
                .isEqualTo("keyword");
    }

    @Test
    void shouldGenerateSearchSchema() {
        ToolCallback[] callbacks = ToolCallbacks.from(knowledgeBaseTools);
        ToolCallback searchDocumentCallBack = Arrays.stream(callbacks)
                .filter(each -> each.getToolDefinition().name().equals("searchDocuments"))
                .findFirst().orElseThrow();
        JsonNode schema = JsonMapper.builder()
                .build()
                .readTree(searchDocumentCallBack.getToolDefinition().inputSchema());
        JsonNode properties = schema.path("properties");
        String keywordType = properties
                .path("keyword").path("type").asString();
        assertThat(keywordType).isEqualTo("string");
        String limitType = properties.path("limit").path("type").asString();
        assertThat(limitType).isEqualTo("integer");
    }


    @Test
    void countDocumentsShouldRejectMissingContext() {
        ToolContext context = new ToolContext(Map.of("userId", UUID.randomUUID().toString()));

        assertThatThrownBy(
                () -> knowledgeBaseTools.countDocuments(context)
        )
                .isInstanceOf(BusinessException.class)
                .hasMessage("知识库上下文缺失");

        verifyNoInteractions(
                kbDocumentService,
                knowledgeBaseService
        );
    }

    @Test
    void countDocumentsShouldNotExposeToolContext() {
        ToolCallback callback = Arrays.stream(
                ToolCallbacks.from(knowledgeBaseTools)
        ).filter(tool -> "countDocuments".equals(
                tool.getToolDefinition().name()
        )).findFirst().orElseThrow();

        JsonNode schema = JsonMapper.builder()
                .build()
                .readTree(callback.getToolDefinition().inputSchema());

        assertThat(schema.path("type").asText())
                .isEqualTo("object");

        assertThat(schema.path("properties").size())
                .isZero();

        assertThat(schema.path("required").size())
                .isZero();
    }

    @Test
    void inspectToolSchema() {
        ToolCallback[] callbacks =
                ToolCallbacks.from(knowledgeBaseTools);

        for (ToolCallback callback : callbacks) {
            ToolDefinition definition =
                    callback.getToolDefinition();

            System.out.println("name = " + definition.name());
            System.out.println("description = " + definition.description());
            System.out.println("schema = " + definition.inputSchema());
        }
    }

}
