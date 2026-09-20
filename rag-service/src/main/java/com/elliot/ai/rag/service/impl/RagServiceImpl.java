package com.elliot.ai.rag.service.impl;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.common.prompt.PromptTemplate;
import com.elliot.ai.common.prompt.PromptTemplateConfig;
import com.elliot.ai.rag.config.RagProperties;
import com.elliot.ai.rag.dto.ExpandedSource;
import com.elliot.ai.rag.dto.RagChatDto;
import com.elliot.ai.rag.dto.RagChatResultDto;
import com.elliot.ai.rag.dto.RagSourceChunkDto;
import com.elliot.ai.rag.dto.RagSourceDto;
import com.elliot.ai.rag.dto.RagStreamEvent;
import com.elliot.ai.rag.dto.TokenUsageDto;
import com.elliot.ai.rag.entity.Conversation;
import com.elliot.ai.rag.query.conversation.ConversationQueryRewriteService;
import com.elliot.ai.rag.query.conversation.model.ConversationContext;
import com.elliot.ai.rag.query.conversation.model.ConversationMessage;
import com.elliot.ai.rag.query.conversation.model.ConversationQueryRewriteResult;
import com.elliot.ai.rag.retrieval.context.ChunkContextExpansionService;
import com.elliot.ai.rag.retrieval.model.ContextCandidate;
import com.elliot.ai.rag.retrieval.model.RerankCandidate;
import com.elliot.ai.rag.retrieval.pipeline.RagRetrievalPipeline;
import com.elliot.ai.rag.retrieval.pipeline.model.RagRetrievalPipelineResult;
import com.elliot.ai.rag.router.ChatClientRouter;
import com.elliot.ai.rag.service.ConversationHistoryService;
import com.elliot.ai.rag.service.ConversationService;
import com.elliot.ai.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor
@Service
public class RagServiceImpl implements RagService {

    private final static String UNKNOW = "大模型没有返回有效回答";
    private final static String LLM_UNKNOW = "大模型没有返回";

    private final ChatClientRouter chatClientRouter;
    private final PromptTemplateConfig promptTemplateConfig;
    private final RagProperties ragProperties;
    private final ChunkContextExpansionService chunkContextExpansionService;
    private final RagRetrievalPipeline ragRetrievalPipeline;
    private final ConversationService conversationService;
    private final ConversationHistoryService conversationHistoryService;
    private final ConversationQueryRewriteService conversationQueryRewriteService;


    /**
     * 基于指定知识库执行检索增强生成，并以 SSE 事件流持续返回回答。
     *
     * <p>执行顺序为：先进行阻塞式检索并准备 RAG 上下文，再按 {@code modelCode}
     * 路由到对应的 {@link ChatClient}，最后将模型输出转换为 {conversation} {@code sources}、
     * {@code delta}、{@code done} 等 SSE 事件。</p>
     *
     * @param ragChatDto 知识库 ID、问题、检索参数和模型编码
     * @return 面向前端的 RAG SSE 事件流
     */
    @Override
    public Flux<ServerSentEvent<RagStreamEvent>> ragStreamChat(RagChatDto ragChatDto) {
        /*
         * MyBatis 和向量检索是阻塞调用，不能占用响应式执行线程；
         * 因此将“准备检索上下文”这一单值任务切换到 boundedElastic 线程池。
         */
        return Mono.fromCallable(() -> preparedRagContext(ragChatDto))
                .subscribeOn(Schedulers.boundedElastic())
                /*
                 * Mono 只产生一个 PreparedRagContext，模型回答则会产生多条事件，
                 * 所以通过 flatMapMany 从 Mono 切换为 Flux。
                 */
                .flatMapMany(prepared -> {
                    ConversationContext conversationContext = prepared.conversationContext();
                    Mono<Void> saveUserMessage =
                            Mono.fromRunnable(() ->
                                            conversationHistoryService.saveUserMessage(conversationContext.conversationId(), conversationContext.originalQuery())
                                    ).subscribeOn(Schedulers.boundedElastic())
                                    .then();
                    return saveUserMessage.thenMany(
                            createEventStream(
                                    prepared,
                                    // 根据本次请求选择 qwen、local 等已注册的聊天客户端。
                                    chatClientRouter.get(ragChatDto.modelCode())
                            )
                    );

                })
                // 检索、模型路由或流创建失败时，仍以 SSE 格式通知前端。
                .onErrorResume(exception -> errorEventStream("知识库检索失败"));
    }

    @Override
    public RagChatResultDto ragChat(RagChatDto ragChatDto) {
        ChatClient selectedChatClient = chatClientRouter.get(ragChatDto.modelCode());
        PreparedRagContext preparedRagContext = preparedRagContext(ragChatDto);
        ConversationContext conversationContext = preparedRagContext.conversationContext();
        if (!preparedRagContext.knowledgeFound) {
            conversationHistoryService.saveUserMessage(conversationContext.conversationId(),
                    conversationContext.originalQuery());
            conversationHistoryService.saveAssistantMessage(conversationContext.conversationId(),
                    UNKNOW);
            return new RagChatResultDto(
                    ragChatDto.knowledgeBaseId(),
                    conversationContext.conversationId(),
                    conversationContext.originalQuery(),
                    UNKNOW,
                    false,
                    preparedRagContext.sources(),
                    null
            );
        }
        conversationHistoryService.saveUserMessage(conversationContext.conversationId(), conversationContext.originalQuery());
        PromptTemplate ragTemplate = promptTemplateConfig.getTemplate("rag");
        ChatResponse chatResponse = selectedChatClient.prompt()
                .system(ragTemplate.systemPrompt())
                .user(user -> user.text(ragTemplate.userTemplate())
                        .param("context", preparedRagContext.content())
                        .param("question", conversationContext.standaloneQuery()))
                .call()
                .chatResponse();
        String answer = extractAnswer(chatResponse);
        conversationHistoryService.saveAssistantMessage(conversationContext.conversationId(), answer);
        return new RagChatResultDto(
                ragChatDto.knowledgeBaseId(),
                conversationContext.conversationId(),
                conversationContext.standaloneQuery(),
                answer,
                true,
                preparedRagContext.sources(),
                extractUsage(chatResponse)
        );
    }

    /**
     * 将已准备好的 RAG 上下文转换为前端可消费的 SSE 事件流。
     *
     * <p>正常事件顺序固定为 {@code sources -> delta... -> done}：先发送召回来源，
     * 再逐段发送模型生成文本，最后发送 Token 用量和结束标记。</p>
     *
     * @param prepared           已完成检索和上下文拼接的 RAG 数据
     * @param selectedChatClient 本次请求按模型编码路由得到的聊天客户端
     * @return SSE 事件流；模型调用异常时返回 {@code error -> done}
     */
    private Flux<ServerSentEvent<RagStreamEvent>> createEventStream(
            PreparedRagContext prepared,
            ChatClient selectedChatClient
    ) {
        //构建会话ID事件
        ServerSentEvent<RagStreamEvent> conversationEvent = event("conversation",
                RagStreamEvent.conversation(prepared.conversationContext().conversationId()));

        // 无论是否命中资料，都先让前端拿到引用来源，便于尽早渲染引用区域。
        ServerSentEvent<RagStreamEvent> sourcesEvent = event("sources",
                RagStreamEvent.sources(prepared.sources));
        if (!prepared.knowledgeFound) {
            // 没有召回资料时不调用大模型，直接发送提示并结束流。
            Mono<Void> saveAssistantMessage = Mono.fromRunnable(() -> {
                conversationHistoryService.saveAssistantMessage(prepared.conversationContext().conversationId()
                        , LLM_UNKNOW);
            }).subscribeOn(Schedulers.boundedElastic()).then();
            return Flux.concat(
                    Flux.just(conversationEvent, sourcesEvent, event("delta", RagStreamEvent.delta(UNKNOW))),
                    saveAssistantMessage.thenMany(Flux.just(event("done", RagStreamEvent.done(null))))
            );
        }

        /*
         * 部分模型会在最后一个响应片段中返回 Token 用量；
         * 用 AtomicReference 暂存它，供后面的 done 事件读取。
         */
        AtomicReference<TokenUsageDto> tokenUsageRef = new AtomicReference<>();
        StringBuilder answer = new StringBuilder();
        PromptTemplate ragTemplate = promptTemplateConfig.getTemplate("rag");

        // 将模型的每一个 ChatResponse 转换为一条 delta SSE 事件。
        Flux<ServerSentEvent<RagStreamEvent>> answerFlux = selectedChatClient.prompt().system(ragTemplate.systemPrompt())
                .user(user -> user.text(ragTemplate.userTemplate())
                        .param("context", prepared.content())
                        .param("question", prepared.conversationContext.standaloneQuery()))
                .stream()
                .chatResponse()
                .<ServerSentEvent<RagStreamEvent>>handle((response, sink) -> {
                    // 即使当前片段没有文本，也可能携带最终的 Token 用量。
                    TokenUsageDto tokenUsageDto = extractUsage(response);
                    if (tokenUsageDto != null) {
                        tokenUsageRef.set(tokenUsageDto);
                    }

                    // 仅发送有实际文本的片段；保留原始空格，避免单词在片段边界粘连。
                    String content = extractStreamingAnswer(response);
                    if (StringUtils.hasText(content)) {
                        answer.append(content);
                        sink.next(event("delta", RagStreamEvent.delta(content)));
                    }
                })
                // 模型没有产生任何文本时，仍返回一个可理解的占位回答。
                .switchIfEmpty(Flux.just(event("delta", RagStreamEvent.delta(LLM_UNKNOW))));
        //保存llm result
        Mono<Void> saveAssistantMessage = Mono.fromRunnable(() -> {
                    String result = StringUtils.hasText(answer.toString()) ? answer.toString() : LLM_UNKNOW;
                    conversationHistoryService.saveAssistantMessage(prepared.conversationContext().conversationId(), result);
                }).subscribeOn(Schedulers.boundedElastic()).then()
                .onErrorResume(e -> {
                    log.warn("Save assistant message failed, conversationId = {}", prepared.conversationContext().conversationId(), e);
                    return Mono.empty();
                });
        // defer 确保 answerFlux 完成后再读取最终收集到的 Token 用量。
        Flux<ServerSentEvent<RagStreamEvent>> doneFlux = saveAssistantMessage.thenMany(Flux.defer(() ->
                Flux.just(event("done",
                        RagStreamEvent.done(tokenUsageRef.get())))));

        // concat 保证 sources、回答增量、done 三类事件不会乱序。
        return Flux.concat(Flux.just(conversationEvent, sourcesEvent), answerFlux, doneFlux)
                // 流式模型调用失败后，仍以 SSE 事件让前端结束加载状态。
                .onErrorResume(exception -> errorEventStream(LLM_UNKNOW));
    }

    private Flux<ServerSentEvent<RagStreamEvent>> errorEventStream(String message) {
        return Flux.just(
                event("error", RagStreamEvent.error(message)),
                event("done", RagStreamEvent.done(null))
        );
    }

    private ServerSentEvent<RagStreamEvent> event(String eventName, RagStreamEvent data) {
        return ServerSentEvent
                .builder(data)
                .event(eventName)
                .build();
    }

    private PreparedRagContext preparedRagContext(RagChatDto ragChatDto) {
        String originalQuestion = ragChatDto.question().trim();
        Conversation conversation = resolveConversation(ragChatDto, originalQuestion);
        UUID conversationId = conversation.getId();
        ConversationContext conversationContext = preparedConversationContext(conversationId, originalQuestion);
        String retrievalQuestion = conversationContext.standaloneQuery();

        int topK = ragChatDto.topK() == null ? ragProperties.getTopK() : ragChatDto.topK();
        double threshold = ragChatDto.similarityThreshold() == null ? ragProperties.getSimilarityThreshold() : ragChatDto.similarityThreshold();
        RagRetrievalPipelineResult retrieveResult = ragRetrievalPipeline.retrieve(
                conversationId,
                ragChatDto.knowledgeBaseId(),
                retrievalQuestion,
                topK,
                threshold);
        List<RerankCandidate> rerankCandidates = retrieveResult.candidates();
        if (rerankCandidates.isEmpty()) {
            return noKnowledgeContext(
                    ragChatDto.knowledgeBaseId(),
                    conversationContext);
        }
        //使用 Rerank 后的最终排序构建Context,chunk相邻信息
        ContextResult contextResult = buildContext(rerankCandidates);
        return new PreparedRagContext(
                ragChatDto.knowledgeBaseId(),
                conversationContext,
                true,
                contextResult.content,
                contextResult.ragSources
        );
    }


    private ConversationContext preparedConversationContext(UUID conversationId, String originalQuestion) {
        List<ConversationMessage> history = conversationHistoryService.getHistory(conversationId);
        ConversationQueryRewriteResult rewritten = conversationQueryRewriteService.rewrite(history, originalQuestion);
        return new ConversationContext(
                conversationId,
                history,
                rewritten.originQuery(),
                rewritten.standaloneQuery(),
                rewritten.rewritten(),
                rewritten.historyUsed()
        );
    }

    private Conversation resolveConversation(RagChatDto ragChatDto, String originalQuestion) {
        if (ragChatDto.conversationId() == null) {
            return conversationService.createConversation(ragChatDto.knowledgeBaseId(), buildConversationTitle(originalQuestion));
        }
        Conversation conversation = conversationService.getConversation(ragChatDto.conversationId(), ragChatDto.knowledgeBaseId());
        if (conversation == null) {
            throw new BusinessException(
                    ResultCode.FAIL,
                    "Conversation with id " + ragChatDto.conversationId() + " not found");
        }
        return conversation;
    }

    private String buildConversationTitle(String originalQuestion) {
        int maxLength = 100;
        if (originalQuestion.length() > maxLength) {
            return originalQuestion.substring(0, maxLength);
        }
        return originalQuestion;
    }

    private PreparedRagContext noKnowledgeContext(
            UUID knowledgeBaseId,
            ConversationContext conversationContext
    ) {

        return new PreparedRagContext(
                knowledgeBaseId,
                conversationContext,
                false,
                null,
                List.of()
        );
    }

    private TokenUsageDto extractUsage(ChatResponse chatResponse) {
        if (chatResponse == null
                || chatResponse.getMetadata() == null) {
            return null;
        }
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage == null) {
            return null;
        }
        return new TokenUsageDto(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }

    private String extractAnswer(ChatResponse chatResponse) {
        if (chatResponse == null
                || chatResponse.getResult() == null
                || chatResponse
                .getResult()
                .getOutput() == null) {
            return LLM_UNKNOW;
        }

        String answer = chatResponse
                .getResult()
                .getOutput()
                .getText();

        if (!StringUtils.hasText(answer)) {
            return LLM_UNKNOW;
        }
        return answer.trim();
    }

    /**
     * 提取流式响应中的原始文本片段。
     *
     * <p>不能对每个片段调用 {@code trim()}，否则分布在片段边界上的空格会丢失，
     * 最终回答中的英文单词可能粘连。</p>
     */
    private String extractStreamingAnswer(ChatResponse chatResponse) {
        if (chatResponse == null
                || chatResponse.getResult() == null
                || chatResponse.getResult().getOutput() == null) {
            return null;
        }
        return chatResponse.getResult().getOutput().getText();
    }

    private ContextResult buildContext(List<RerankCandidate> candidates) {
        StringBuilder contextBuilder = new StringBuilder();
        List<RagSourceDto> sources = new ArrayList<>();
        int maxChars = ragProperties.getMaxContextChars();
        for (RerankCandidate candidate : candidates) {
            ContextCandidate contextCandidate = toContextCandidate(candidate);
            ExpandedSource expand = chunkContextExpansionService.expand(contextCandidate);
            String referenceId = "S" + (sources.size() + 1);
            String contextItem = buildContextItem(referenceId, expand);
            /*
             * 至少保留第一条资料。
             * 后面的资料超过 Context 限制则停止。
             */
            if (!contextBuilder.isEmpty()
                    && contextBuilder.length()
                    + contextItem.length()
                    > maxChars) {
                break;
            }
            contextBuilder.append(contextItem);
            sources.add(new RagSourceDto(
                    referenceId,
                    expand.rank(),
                    expand.score(),
                    expand.matchedChunkId(),
                    expand.matchedChunkIndex(),
                    expand.documentId(),
                    expand.documentName(),
                    expand.contextStartIndex(),
                    expand.contextEndIndex(),
                    expand.chunks()
            ));
        }
        return new ContextResult(contextBuilder.toString(), sources);
    }

    private ContextCandidate toContextCandidate(RerankCandidate candidate) {
        return new ContextCandidate(
                candidate.rerankRank(),
                candidate.effectiveScore(),
                candidate.chunkId(),
                candidate.documentId(),
                candidate.documentName(),
                candidate.chunkIndex(),
                candidate.sectionTitle(),
                candidate.pageNumber(),
                candidate.content()
        );
    }

    private String buildContextItem(String referenceId, ExpandedSource source) {
        StringBuilder builder = new StringBuilder();
        builder.append("[")
                .append(referenceId)
                .append("]\n");

        builder.append("文档：")
                .append(
                        StringUtils.hasText(source.documentName()) ? source.documentName() : "未知文档"
                )
                .append("\n");
        builder.append("Chunk 范围：")
                .append(source.contextStartIndex())
                .append("-")
                .append(source.contextEndIndex())
                .append("\n");
        builder.append("参考内容：\n");
        for (RagSourceChunkDto chunk : source.chunks()) {
            builder.append("[Chunk ")
                    .append(chunk.chunkIndex());
            if (chunk.matched()) {
                builder.append(", 核心命中");
            }
            builder.append("]\n");

            if (StringUtils.hasText(chunk.selectTitle())) {
                builder.append("章节：")
                        .append(chunk.selectTitle())
                        .append("\n");
            }

            if (chunk.pageNumber() != null) {
                builder.append("页码：")
                        .append(chunk.pageNumber())
                        .append("\n");
            }

            builder.append("内容：\n")
                    .append(chunk.content())
                    .append("\n\n");

        }
        return builder.toString();
    }

    public record ContextResult(
            String content,
            List<RagSourceDto> ragSources
    ) {

    }

    public record PreparedRagContext(
            UUID knowledgeBaseId,
            ConversationContext conversationContext,
            boolean knowledgeFound,
            String content,
            List<RagSourceDto> sources
    ) {

    }
}
