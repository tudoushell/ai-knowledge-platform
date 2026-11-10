package com.elliot.ai.rag.service.impl;

import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.common.prompt.PromptTemplateConfig;
import com.elliot.ai.identity.security.CurrentUserProvider;
import com.elliot.ai.rag.config.RagProperties;
import com.elliot.ai.rag.dto.ExpandedSource;
import com.elliot.ai.rag.dto.RagChatDto;
import com.elliot.ai.rag.dto.RagChatResultDto;
import com.elliot.ai.rag.dto.RagSourceChunkDto;
import com.elliot.ai.rag.entity.Conversation;
import com.elliot.ai.rag.entity.KnowledgeBase;
import com.elliot.ai.rag.memory.ConversationHistoryResult;
import com.elliot.ai.rag.memory.ConversationMemoryCacheStatus;
import com.elliot.ai.rag.memory.ConversationMemorySource;
import com.elliot.ai.rag.query.conversation.ConversationQueryRewriteService;
import com.elliot.ai.rag.query.conversation.model.ConversationMessage;
import com.elliot.ai.rag.query.conversation.model.ConversationQueryRewriteResult;
import com.elliot.ai.rag.retrieval.context.ChunkContextExpansionService;
import com.elliot.ai.rag.retrieval.model.ContextCandidate;
import com.elliot.ai.rag.retrieval.model.RerankCandidate;
import com.elliot.ai.rag.retrieval.pipeline.RagRetrievalPipeline;
import com.elliot.ai.rag.retrieval.pipeline.model.RagRetrievalPipelineResult;
import com.elliot.ai.rag.retrieval.pipeline.model.RetrievalTrace;
import com.elliot.ai.rag.router.ChatClientRouter;
import com.elliot.ai.rag.router.ChatIntentRouter;
import com.elliot.ai.rag.service.ConversationHistoryService;
import com.elliot.ai.rag.service.ConversationService;
import com.elliot.ai.rag.service.KbDocumentService;
import com.elliot.ai.rag.service.KnowledgeBaseService;
import com.elliot.ai.rag.service.RagService;
import com.elliot.ai.rag.tool.KnowledgeBaseToolRegistry;
import com.elliot.ai.rag.tool.KnowledgeBaseTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * ragChat 的 Spring 服务集成测试。
 *
 * <p>真实装配 RagService、模型路由、意图路由、ChatClient、提示词与工具回调；
 * 仅替换模型、持久化服务、历史改写及检索边界，不连接数据库、Redis 或外部模型。
 * 工具测试通过模型的 tool call 实际执行 countDocuments，而不是直接模拟最终工具答案。</p>
 */
@Tag("integration")
@SpringBootTest(classes = RagServiceImplIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RagServiceImplIntegrationTest {

    private static final String MODEL = "integration-model";

    @Configuration(proxyBeanMethods = false)
    @Import({RagServiceImpl.class, ChatClientRouter.class, ChatIntentRouter.class,
            KnowledgeBaseToolRegistry.class, KnowledgeBaseTools.class,
            RagProperties.class, PromptTemplateConfig.class})
    static class TestConfig {
        @Bean(MODEL)
        ChatClient chatClient(ChatModel model) {
            return ChatClient.builder(model).build();
        }
    }

    @Autowired
    private RagService ragService;
    @MockitoBean
    private ChatModel chatModel;
    @MockitoBean
    private ConversationService conversationService;
    @MockitoBean
    private ConversationHistoryService historyService;
    @MockitoBean
    private ConversationQueryRewriteService rewriteService;
    @MockitoBean
    private RagRetrievalPipeline retrievalPipeline;
    @MockitoBean
    private ChunkContextExpansionService expansionService;
    @MockitoBean
    private KbDocumentService documentService;
    @MockitoBean
    private KnowledgeBaseService knowledgeBaseService;
    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private UUID knowledgeBaseId;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        knowledgeBaseId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        when(chatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
    }

    @Test
    void shouldExecuteCountToolAndSaveBothMessagesWithoutRetrieval() {
        String question = "当前知识库有多少份文档？";
        prepareConversation(question, question);
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(knowledgeBaseId);
        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId)).thenReturn(knowledgeBase);
        when(documentService.count(any())).thenReturn(3L);
        AssistantMessage toolCall = AssistantMessage.builder().content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("count-1", "function", "countDocuments", "{}")))
                .build();
        when(chatModel.call(any(Prompt.class))).thenReturn(
                response("DOCUMENT_COUNT"), new ChatResponse(List.of(new Generation(toolCall))),
                response("当前知识库共有 3 份文档。"));

        RagChatResultDto result = ragService.ragChat(request(question, null, null, null));

        assertThat(result.answerModel()).isEqualTo(RagChatResultDto.AnswerMode.TOOL);
        assertThat(result.answer()).isEqualTo("当前知识库共有 3 份文档。");
        assertThat(result.knowledgeBaseId()).isEqualTo(knowledgeBaseId);
        assertThat(result.conversationId()).isEqualTo(conversationId);
        assertThat(result.knowledgeFound()).isFalse();
        assertThat(result.sources()).isEmpty();
        assertThat(result.tokenUsage()).isNull();
        assertThat(result.trace().retrieval()).isNull();
        verify(knowledgeBaseService).getKnowledgeBase(knowledgeBaseId);
        verify(documentService).count(any());
        verifyNoInteractions(retrievalPipeline, expansionService);
        verifySavedMessages(question, result.answer());

        ArgumentCaptor<Prompt> prompts = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(3)).call(prompts.capture());
        ToolCallingChatOptions options = (ToolCallingChatOptions) prompts.getAllValues().get(1).getOptions();
        assertThat(options.getToolCallbacks()).extracting(tool -> tool.getToolDefinition().name())
                .containsExactly("countDocuments");
        assertThat(prompts.getAllValues().get(2).getInstructions())
                .anySatisfy(message -> {
                    assertThat(message).isInstanceOf(ToolResponseMessage.class);
                    String toolResult = ((ToolResponseMessage) message).getResponses().get(0).responseData();
                    assertThat(com.jayway.jsonpath.JsonPath.<Boolean>read(toolResult, "$.success")).isTrue();
                    assertThat(com.jayway.jsonpath.JsonPath.<Integer>read(toolResult, "$.data")).isEqualTo(3);
                });
    }

    @Test
    void shouldReturnRagAnswerWithExpandedSourceAndUsage() {
        String question = "HashMap 是什么？";
        prepareConversation(question, question);
        UUID chunkId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        String content = "HashMap 是基于哈希表的 Map 实现。";
        RerankCandidate candidate = new RerankCandidate(1, 0.95, true, 1, 0.03,
                chunkId, documentId, "Java.md", 0, null, null, content);
        RetrievalTrace trace = trace(question, 1);
        when(retrievalPipeline.retrieve(conversationId, knowledgeBaseId, question, 2, 0.7))
                .thenReturn(new RagRetrievalPipelineResult(List.of(candidate), trace));
        when(expansionService.expand(any(ContextCandidate.class))).thenReturn(new ExpandedSource(
                1, 0.95, chunkId, 0, documentId, "Java.md", 0, 0,
                List.of(new RagSourceChunkDto(chunkId, 0, null, null, content, true))));
        when(chatModel.call(any(Prompt.class))).thenReturn(response("RAG"), response("HashMap 基于哈希表。[S1]"));

        RagChatResultDto result = ragService.ragChat(request("  " + question + "  ", null, 2, 0.7));

        assertThat(result.answerModel()).isEqualTo(RagChatResultDto.AnswerMode.RAG);
        assertThat(result.question()).isEqualTo(question);
        assertThat(result.conversationId()).isEqualTo(conversationId);
        assertThat(result.knowledgeFound()).isTrue();
        assertThat(result.answer()).isEqualTo("HashMap 基于哈希表。[S1]");
        assertThat(result.sources()).singleElement().satisfies(source -> {
            assertThat(source.referenceId()).isEqualTo("S1");
            assertThat(source.documentId()).isEqualTo(documentId);
        });
        assertThat(result.tokenUsage().totalTokens()).isEqualTo(15);
        assertThat(result.trace().retrieval()).isEqualTo(trace);
        verifySavedMessages(question, result.answer());

        ArgumentCaptor<Prompt> prompts = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(2)).call(prompts.capture());
        assertThat(prompts.getAllValues().get(1).getUserMessage().getText())
                .contains(question, content, "S1");
        verifyNoInteractions(documentService, knowledgeBaseService);
    }

    @Test
    void shouldReuseConversationAndReturnFallbackWhenNoKnowledgeIsFound() {
        String question = "它有什么特点？";
        String standalone = "HashMap 有什么特点？";
        prepareConversation(question, standalone);
        when(conversationService.getConversation(conversationId, knowledgeBaseId)).thenReturn(conversation());
        when(chatModel.call(any(Prompt.class))).thenReturn(response("RAG"));
        when(retrievalPipeline.retrieve(conversationId, knowledgeBaseId, standalone, 5, 0.5))
                .thenReturn(new RagRetrievalPipelineResult(List.of(), trace(standalone, 0)));

        RagChatResultDto result = ragService.ragChat(request(question, conversationId, null, null));

        assertThat(result.conversationId()).isEqualTo(conversationId);
        assertThat(result.answerModel()).isEqualTo(RagChatResultDto.AnswerMode.RAG);
        assertThat(result.knowledgeFound()).isFalse();
        assertThat(result.answer()).isEqualTo("大模型没有返回有效回答");
        assertThat(result.sources()).isEmpty();
        assertThat(result.tokenUsage()).isNull();
        assertThat(result.trace().conversation().standaloneQuery()).isEqualTo(standalone);
        assertThat(result.trace().conversation().rewritten()).isTrue();
        verify(conversationService, never()).createConversation(any(), any());
        // 唯一的模型调用是意图分类，没有调用最终回答模型。
        verify(chatModel).call(any(Prompt.class));
        verifyNoInteractions(expansionService, documentService);
        verifySavedMessages(question, result.answer());
    }

    @Test
    void shouldRejectConversationNotFoundInKnowledgeBaseBeforeCallingModel() {
        UUID unknownConversation = UUID.randomUUID();
        assertThatThrownBy(() -> ragService.ragChat(request("问题", unknownConversation, null, null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("not found");
        verify(conversationService).getConversation(unknownConversation, knowledgeBaseId);
        verify(chatModel, never()).call(any(Prompt.class));
        verifyNoInteractions(historyService, rewriteService, retrievalPipeline, documentService);
    }

    private void prepareConversation(String question, String standalone) {
        boolean rewritten = !question.equals(standalone);
        List<ConversationMessage> history = rewritten ? List.of(
                new ConversationMessage(ConversationMessage.Role.USER, "HashMap 是什么？"),
                new ConversationMessage(ConversationMessage.Role.ASSISTANT, "HashMap 是基于哈希表的 Map 实现。")
        ) : List.of();
        when(conversationService.createConversation(knowledgeBaseId, question)).thenReturn(conversation());
        when(historyService.getHistory(conversationId)).thenReturn(new ConversationHistoryResult(
                history, ConversationMemorySource.DATABASE, ConversationMemoryCacheStatus.MISS,
                history.size(), history.size(), history.stream().mapToInt(message -> message.content().length()).sum()));
        when(rewriteService.rewrite(history, question)).thenReturn(new ConversationQueryRewriteResult(
                question, standalone, rewritten, rewritten));
    }

    private Conversation conversation() {
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setKnowledgeBaseId(knowledgeBaseId);
        return conversation;
    }

    private RagChatDto request(String question, UUID id, Integer topK, Double threshold) {
        return new RagChatDto(knowledgeBaseId, id, question, topK, threshold, MODEL);
    }

    private RetrievalTrace trace(String query, int count) {
        return new RetrievalTrace(query, query, false, List.of(query), count, count, count);
    }

    private ChatResponse response(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))),
                ChatResponseMetadata.builder().usage(new DefaultUsage(10, 5, 15, null)).build());
    }

    private void verifySavedMessages(String question, String answer) {
        var order = inOrder(historyService, rewriteService);
        order.verify(historyService).getHistory(conversationId);
        order.verify(rewriteService).rewrite(anyList(), eq(question));
        order.verify(historyService).saveUserMessage(conversationId, question);
        order.verify(historyService).saveAssistantMessage(conversationId, answer);
    }
}
