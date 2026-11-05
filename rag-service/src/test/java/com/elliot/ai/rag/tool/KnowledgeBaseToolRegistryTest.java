package com.elliot.ai.rag.tool;

import com.elliot.ai.rag.router.ChatIntent;
import com.elliot.ai.rag.service.KbDocumentService;
import com.elliot.ai.rag.service.KnowledgeBaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.ToolCallback;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseToolRegistryTest {

    @Mock
    private KbDocumentService kbDocumentService;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    private KnowledgeBaseToolRegistry registry;

    @BeforeEach
    void setUp() {
        KnowledgeBaseTools knowledgeBaseTools =
                new KnowledgeBaseTools(
                        kbDocumentService,
                        knowledgeBaseService
                );

        registry = new KnowledgeBaseToolRegistry(
                knowledgeBaseTools
        );
    }

    @Test
    void shouldExposeCountDocumentsOnly() {
        ToolCallback[] toolCallbacks = registry.forIntent(ChatIntent.DOCUMENT_COUNT);
        assertThat(toolCallbacks).hasSize(1);
        assertThat(toolCallbacks[0].getToolDefinition().name()).isEqualTo("countDocuments");
    }

    @Test
    void shouldExposeNoToolsForRag() {
        ToolCallback[] toolCallbacks = registry.forIntent(ChatIntent.RAG);
        assertThat(toolCallbacks).hasSize(0);
    }

    @Test
    void shouldRejectNullIntent() {
        assertThatThrownBy(() -> registry.forIntent(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ChatIntent 不能为空");
    }
}