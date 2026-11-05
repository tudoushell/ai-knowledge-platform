package com.elliot.ai.rag.tool;

import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.service.KbDocumentService;
import com.elliot.ai.rag.service.KnowledgeBaseService;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class KnowledgeBaseToolsTest {

    @InjectMocks
    private KnowledgeBaseTools knowledgeBaseTools;

    @Mock
    private KbDocumentService  kbDocumentService;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;


    @BeforeEach
    void setUp() {
        knowledgeBaseTools = new KnowledgeBaseTools(
                kbDocumentService,
                knowledgeBaseService
        );
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
        ToolContext context = new ToolContext(Map.of());

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
