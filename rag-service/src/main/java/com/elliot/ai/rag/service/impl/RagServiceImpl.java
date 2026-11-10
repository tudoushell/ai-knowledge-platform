package com.elliot.ai.rag.service.impl;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.common.prompt.PromptTemplate;
import com.elliot.ai.common.prompt.PromptTemplateConfig;
import com.elliot.ai.identity.security.CurrentUserProvider;
import com.elliot.ai.rag.config.RagProperties;
import com.elliot.ai.rag.dto.ExpandedSource;
import com.elliot.ai.rag.dto.RagChatDto;
import com.elliot.ai.rag.dto.RagChatResultDto;
import com.elliot.ai.rag.dto.RagSourceChunkDto;
import com.elliot.ai.rag.dto.RagSourceDto;
import com.elliot.ai.rag.dto.RagStreamEvent;
import com.elliot.ai.rag.dto.TokenUsageDto;
import com.elliot.ai.rag.entity.Conversation;
import com.elliot.ai.rag.memory.ConversationHistoryResult;
import com.elliot.ai.rag.query.conversation.ConversationQueryRewriteService;
import com.elliot.ai.rag.query.conversation.model.ConversationContext;
import com.elliot.ai.rag.query.conversation.model.ConversationQueryRewriteResult;
import com.elliot.ai.rag.retrieval.context.ChunkContextExpansionService;
import com.elliot.ai.rag.retrieval.model.ContextCandidate;
import com.elliot.ai.rag.retrieval.model.RerankCandidate;
import com.elliot.ai.rag.retrieval.pipeline.RagRetrievalPipeline;
import com.elliot.ai.rag.retrieval.pipeline.model.RagRetrievalPipelineResult;
import com.elliot.ai.rag.router.ChatClientRouter;
import com.elliot.ai.rag.router.ChatIntent;
import com.elliot.ai.rag.router.ChatIntentRouter;
import com.elliot.ai.rag.service.ConversationHistoryService;
import com.elliot.ai.rag.service.ConversationService;
import com.elliot.ai.rag.service.RagService;
import com.elliot.ai.rag.tool.KnowledgeBaseToolRegistry;
import com.elliot.ai.rag.trace.model.ConversationTrace;
import com.elliot.ai.rag.trace.model.RagTrace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallLimitExceededException;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final ChatIntentRouter chatIntentRouter;
    private final KnowledgeBaseToolRegistry knowledgeBaseToolRegistry;
    private final CurrentUserProvider currentUserProvider;


    /**
     * 基于指定知识库执行检索增强生成，并以 SSE 事件流持续返回回答。
     *
     * <p>订阅后创建或校验会话，加载历史消息并改写当前问题，再执行检索和上下文
     * 构建，同时收集会话及检索追踪信息。上述阻塞操作在
     * {@code boundedElastic} 线程池执行；本次用户消息在历史读取之后保存，
     * 保存成功后才开始发送事件。</p>
     *
     * <p>正常事件顺序为 {@code conversation -> trace -> sources -> delta... -> done}。
     * 命中资料时，按 {@code modelCode} 选择 {@link ChatClient} 并流式生成回答，
     * 在生成完成后尝试保存助手消息，再通过 {@code done} 返回可用的 Token 用量。
     * 未命中资料时不调用模型，发送兜底提示并尝试保存助手消息后结束。</p>
     *
     * <p>准备上下文、用户消息保存或模型路由等异常由外层转换为
     * {@code error -> done} 事件；模型流异常由内部事件流处理。</p>
     *
     * @param ragChatDto 知识库 ID、可选会话 ID、当前问题、检索参数及模型编码
     * @return 包含会话 ID、追踪信息、引用来源、回答增量和结束标记的 SSE 事件流
     */
    @Override
    public Flux<ServerSentEvent<RagStreamEvent>> ragStreamChat(RagChatDto ragChatDto) {
        /*
         * 会话查询、历史缓存读取、问题改写和检索包含阻塞调用，
         * 将整个上下文准备任务调度到 boundedElastic 线程池。
         */
        return Mono.fromCallable(() -> {
                    ConversationContext context = prepareConversation(ragChatDto);
                    ChatIntent intent = chatIntentRouter.route(ragChatDto.modelCode(), context.standaloneQuery());
                    PreparedRagContext ragContext = intent == ChatIntent.RAG ?
                            retrieveRagContext(ragChatDto, context) : null;
                    return new StreamPreparation(context, intent, ragContext);
                })
                .subscribeOn(Schedulers.boundedElastic())
                /*
                 * Mono 只产生一个 PreparedRagContext，模型回答则会产生多条事件，
                 * 所以通过 flatMapMany 从 Mono 切换为 Flux。
                 */
                .flatMapMany(prepared -> {
                    ConversationContext conversationContext = prepared.conversationContext();
                    // 历史读取和检索已完成；保存本次原始问题后，再订阅回答事件流。
                    Mono<Void> saveUserMessage =
                            Mono.fromRunnable(() ->
                                    conversationHistoryService.saveUserMessage(conversationContext.conversationId(), conversationContext.originalQuery())
                            ).subscribeOn(Schedulers.boundedElastic()).then();
                    return saveUserMessage.thenMany(Flux.defer(() -> {
                        ChatClient client = chatClientRouter.get(ragChatDto.modelCode());
                        if (prepared.intent() == ChatIntent.DOCUMENT_COUNT) {
                            return createToolEventStream(ragChatDto, prepared.conversationContext(), client, prepared.intent);
                        }
                        return createEventStream(prepared.preparedRagContext(), client);
                    }));
                })
                // 将准备上下文、保存用户消息等未被内部处理的异常转换为 error、done 事件。
                .onErrorResume(exception -> errorEventStream("知识库检索失败"));
    }

    @Override
    public RagChatResultDto ragChat(RagChatDto ragChatDto) {
        ChatClient selectedChatClient = chatClientRouter.get(ragChatDto.modelCode());
        ConversationContext conversationContext = prepareConversation(ragChatDto);
        ChatIntent intent = chatIntentRouter.route(ragChatDto.modelCode(), conversationContext.standaloneQuery());
        if (intent == ChatIntent.DOCUMENT_COUNT) {
            conversationHistoryService.saveUserMessage(conversationContext.conversationId(),
                    conversationContext.originalQuery());
            ChatResponse chatResponse = selectedChatClient.prompt()
                    .system("你是知识库助手。回答文档数量问题时必须调用 countDocuments 工具，不允许猜测数量。")
                    .user(conversationContext.standaloneQuery())
                    .tools(knowledgeBaseToolRegistry.forIntent(intent))
                    .toolContext(Map.of("knowledgeBaseId", ragChatDto.knowledgeBaseId()))
                    .call()
                    .chatResponse();
            checkToolCallLimit(chatResponse);
            String content = extractAnswer(chatResponse);
            conversationHistoryService.saveAssistantMessage(conversationContext.conversationId(), content);
            return new RagChatResultDto(ragChatDto.knowledgeBaseId(),
                    conversationContext.conversationId(),
                    conversationContext.originalQuery(),
                    content,
                    false,
                    List.of(),
                    null,
                    new RagTrace(ConversationTrace.from(conversationContext), null),
                    RagChatResultDto.AnswerMode.TOOL
            );
        }
        PreparedRagContext preparedRagContext = retrieveRagContext(ragChatDto, conversationContext);
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
                    null,
                    preparedRagContext.trace(),
                    RagChatResultDto.AnswerMode.RAG
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
                conversationContext.originalQuery(),
                answer,
                true,
                preparedRagContext.sources(),
                extractUsage(chatResponse),
                preparedRagContext.trace(),
                RagChatResultDto.AnswerMode.RAG
        );
    }

    /**
     * 检查模型响应是否包含工具调用次数超限的结束标记。
     *
     * <p>响应或首个结果为空时跳过检查；任一结果包含超限标记时抛出业务异常。
     * 此方法只检查响应标记，工具调用次数的限制由工具调用管理器负责。</p>
     *
     * @param response 模型响应，允许为空
     * @throws BusinessException 响应中存在工具调用次数超限标记时抛出
     */
    private void checkToolCallLimit(ChatResponse response) {
        if (response == null || response.getResult() == null) {
            return;
        }
        List<Generation> results = response.getResults();
        boolean exceeded = results.stream()
                .anyMatch(each ->
                        ToolCallLimitExceededException.FINISH_REASON.equals(each.getMetadata().getFinishReason()));
        if (exceeded) {
            throw new BusinessException(
                    ResultCode.FAIL,
                    "工具调用次数超过限制"
            );
        }
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

        ServerSentEvent<RagStreamEvent> traceEvent = event("trace",
                RagStreamEvent.trace(prepared.trace()));

        ServerSentEvent<RagStreamEvent> modeEvent =
                event("mode", RagStreamEvent.mode("RAG"));

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
                    Flux.just(conversationEvent,
                            traceEvent,
                            sourcesEvent,
                            event("delta", RagStreamEvent.delta(UNKNOW))),
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
                    if (content != null && !content.isEmpty()) {
                        answer.append(content);
                        sink.next(event("delta", RagStreamEvent.delta(content)));
                    }
                })
                .concatWith(Flux.defer(() -> {
                    if (StringUtils.hasText(answer.toString())) {
                        return Flux.empty();
                    }
                    answer.append(LLM_UNKNOW);
                    return Flux.just(event("delta", RagStreamEvent.delta(LLM_UNKNOW)));
                }));
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
        return Flux.concat(Flux.just(conversationEvent, modeEvent, traceEvent, sourcesEvent), answerFlux, doneFlux)
                // 流式模型调用失败后，仍以 SSE 事件让前端结束加载状态。
                .onErrorResume(exception -> errorEventStream(LLM_UNKNOW));
    }


    private Flux<ServerSentEvent<RagStreamEvent>> createToolEventStream(
            RagChatDto ragChatDto,
            ConversationContext context,
            ChatClient selectedChatClient,
            ChatIntent intent) {
        ServerSentEvent<RagStreamEvent> conversationEvent = event("conversation", RagStreamEvent.conversation(context.conversationId()));
        ServerSentEvent<RagStreamEvent> modelEvent = event("mode", RagStreamEvent.mode("TOOL"));
        ServerSentEvent<RagStreamEvent> sourceEvent = event("sources", RagStreamEvent.sources(List.of()));
        RagTrace trace = new RagTrace(ConversationTrace.from(context), null);
        ServerSentEvent<RagStreamEvent> traceEvent = event("trace",
                RagStreamEvent.trace(trace));
        AtomicReference<TokenUsageDto> tokenUsageRef = new AtomicReference<>();
        StringBuilder answer = new StringBuilder();
        Flux<ServerSentEvent<RagStreamEvent>> answerFlux = selectedChatClient.prompt()
                .system("你是知识库助手。回答文档数量问题时必须调用 countDocuments 工具，不允许猜测数量。")
                .user(context.standaloneQuery())
                .tools(knowledgeBaseToolRegistry.forIntent(intent))
                .toolContext(Map.of("knowledgeBaseId", ragChatDto.knowledgeBaseId()))
                .stream()
                .chatResponse()
                .<ServerSentEvent<RagStreamEvent>>handle((response, sink) -> {
                    //处理调用工具是否达上限
                    checkToolCallLimit(response);
                    TokenUsageDto tokenUsageDto = extractUsage(response);
                    if (tokenUsageDto != null) {
                        tokenUsageRef.set(tokenUsageDto);
                    }
                    String content = extractStreamingAnswer(response);
                    // 保留独立的空格和换行片段，避免单词粘连或丢失排版。
                    if (content != null && !content.isEmpty()) {
                        answer.append(content);
                        sink.next(event("delta", RagStreamEvent.delta(content)));
                    }
                })
                // 所有片段接收完成后，再判断整体回答是否包含有效文本。
                .concatWith(Flux.defer(() -> {
                    if (StringUtils.hasText(answer.toString())) {
                        return Flux.empty();
                    }
                    answer.append(LLM_UNKNOW);
                    return Flux.just(event("delta", RagStreamEvent.delta(LLM_UNKNOW)));
                }));
        Mono<Void> saveAssistantMessage = Mono.fromRunnable(() -> {
                    String result = StringUtils.hasText(answer.toString()) ? answer.toString() : LLM_UNKNOW;
                    conversationHistoryService.saveAssistantMessage(context.conversationId(), result);
                }).subscribeOn(Schedulers.boundedElastic()).then()
                .onErrorResume(e -> {
                    log.warn("Save assistant message failed, conversationId = {}", context.conversationId(), e);
                    return Mono.empty();
                });
        Flux<ServerSentEvent<RagStreamEvent>> doneEvent = saveAssistantMessage
                .thenMany(Flux.defer(() -> Flux.just(event("done", RagStreamEvent.done(tokenUsageRef.get())))));
        return Flux.concat(Flux.just(conversationEvent, modelEvent, traceEvent, sourceEvent), answerFlux, doneEvent)
                .onErrorResume(e -> {
                    log.error("Tool Calling stream Failed", e);
                    return errorEventStream(LLM_UNKNOW);
                });
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

    /**
     * 准备本轮问答的会话上下文，将依赖历史的追问改写为可独立理解的问题。
     *
     * <p>未提供会话 ID 时，根据当前问题创建会话；提供会话 ID 时，查询并校验
     * 其所属知识库。随后读取历史消息并执行会话问题改写，供后续检索和回答生成使用。
     * 本轮用户消息由调用方在历史读取之后保存，避免当前问题重复进入改写上下文。</p>
     *
     * @param ragChatDto 包含知识库 ID、可选会话 ID 和当前问题的问答请求
     * @return 包含会话 ID、历史读取结果、原始问题、独立问题及改写标记的会话上下文
     * @throws BusinessException 指定会话不存在或不属于当前知识库时抛出
     */
    private ConversationContext prepareConversation(RagChatDto ragChatDto) {
        // 去除问题首尾空白，作为本轮原始问题，并用于生成新会话的标题。
        String originalQuestion = ragChatDto.question().trim();
        // 创建新会话，或查询并校验请求指定的已有会话。
        Conversation conversation = resolveConversation(ragChatDto, originalQuestion);
        // 读取该会话的历史消息，补全追问中的指代和省略信息，封装为会话上下文。
        return preparedConversationContext(conversation.getId(), originalQuestion);
    }

    private PreparedRagContext retrieveRagContext(RagChatDto ragChatDto,
                                                  ConversationContext conversationContext) {
        UUID conversationId = conversationContext.conversationId();
        String retrievalQuestion = conversationContext.standaloneQuery();
        int topK = ragChatDto.topK() == null ? ragProperties.getTopK() : ragChatDto.topK();
        double threshold = ragChatDto.similarityThreshold() == null ? ragProperties.getSimilarityThreshold() : ragChatDto.similarityThreshold();
        RagRetrievalPipelineResult retrieveResult = ragRetrievalPipeline.retrieve(
                conversationId,
                ragChatDto.knowledgeBaseId(),
                retrievalQuestion,
                topK,
                threshold);
        RagTrace trace = new RagTrace(ConversationTrace.from(conversationContext), retrieveResult.trace());
        List<RerankCandidate> rerankCandidates = retrieveResult.candidates();
        if (rerankCandidates.isEmpty()) {
            return noKnowledgeContext(
                    ragChatDto.knowledgeBaseId(),
                    conversationContext,
                    trace);
        }
        //使用 Rerank 后的最终排序构建Context,chunk相邻信息
        ContextResult contextResult = buildContext(rerankCandidates);
        return new PreparedRagContext(
                ragChatDto.knowledgeBaseId(),
                conversationContext,
                true,
                contextResult.content,
                contextResult.ragSources,
                trace
        );

    }

    /**
     * 按顺序完成会话准备和知识库检索，构建回答生成所需的完整 RAG 上下文。
     *
     * <p>先创建或校验会话、读取历史并改写追问，再使用独立问题执行检索流水线。
     * 命中资料时扩展相邻 Chunk 并拼接带引用编号的上下文，同时汇总会话及检索追踪信息；
     * 未命中时返回 {@code knowledgeFound=false}、资料文本为 null、引用列表为空的上下文。</p>
     *
     * <p>本方法包含数据库、缓存及模型等阻塞调用。流式调用方应将其调度到适合阻塞任务的线程池。
     * 本轮用户消息保存、最终回答生成及助手消息保存由后续流程负责。</p>
     *
     * @param ragChatDto 包含知识库 ID、可选会话 ID、当前问题及检索参数的问答请求
     * @return 汇总会话上下文、资料命中状态、资料文本、引用来源及追踪信息的准备结果
     */
    private PreparedRagContext preparedRagContext(RagChatDto ragChatDto) {
        // 1. 确定会话并结合历史理解当前问题，得到可独立检索的问题。
        ConversationContext conversationContext = prepareConversation(ragChatDto);
        ChatIntent intent = chatIntentRouter.route(ragChatDto.modelCode(), conversationContext.standaloneQuery());
        // 2. 执行检索和资料上下文构建，将检索结果与会话信息合并为回答生成的输入。
        return retrieveRagContext(ragChatDto, conversationContext);
    }

    private record StreamPreparation(
            ConversationContext conversationContext,
            ChatIntent intent,
            PreparedRagContext preparedRagContext
    ) {
    }


    private ConversationContext preparedConversationContext(UUID conversationId, String originalQuestion) {
        ConversationHistoryResult historyResult = conversationHistoryService.getHistory(conversationId);
        ConversationQueryRewriteResult rewritten = conversationQueryRewriteService.rewrite(historyResult.messages(), originalQuestion);
        return new ConversationContext(
                conversationId,
                historyResult,
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
            ConversationContext conversationContext,
            RagTrace trace
    ) {

        return new PreparedRagContext(
                knowledgeBaseId,
                conversationContext,
                false,
                null,
                List.of(),
                trace
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

    /**
     * RAG 回答生成前准备好的上下文，汇总会话问题、检索资料和追踪信息，
     * 供同步问答与 SSE 流式问答共用。
     *
     * <p>由 {@link #preparedRagContext(RagChatDto)} 构建；此时已完成会话解析、
     * 历史读取、问题改写、检索及资料上下文构建，尚未保存本轮用户消息或生成回答。</p>
     *
     * @param knowledgeBaseId     本次检索所属的知识库 ID
     * @param conversationContext 会话上下文，包含会话 ID、历史读取结果、原始问题及改写后的独立问题
     * @param knowledgeFound      是否检索到候选资料；为 false 时返回兜底提示，不调用最终回答模型
     * @param content             拼接后的资料文本，包含引用编号和扩展后的 Chunk 内容，用于填充回答提示词；未命中时为 null
     * @param sources             与资料文本对应的结构化引用来源，供前端展示文档及 Chunk 上下文；未命中时为空列表
     * @param trace               会话与检索追踪信息，记录历史来源、问题改写结果及各阶段候选数量
     */
    public record PreparedRagContext(
            UUID knowledgeBaseId,
            ConversationContext conversationContext,
            boolean knowledgeFound,
            String content,
            List<RagSourceDto> sources,
            RagTrace trace
    ) {

    }
}
