package com.elliot.ai.rag.controller;

import com.elliot.ai.common.dto.Result;
import com.elliot.ai.rag.dto.RagChatDto;
import com.elliot.ai.rag.dto.RagChatResultDto;
import com.elliot.ai.rag.dto.RagStreamEvent;
import com.elliot.ai.rag.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 基于知识库上下文的 RAG 对话接口。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG 对话", description = "检索知识库片段并基于其生成回答")
public class RagChatController {

    private final RagService ragService;

    /**
     * 在指定知识库中检索相关片段，并基于检索结果生成回答。
     *
     * @param ragChatDto 知识库 ID、问题和可选的检索参数
     * @return 回答内容、引用来源及模型 Token 用量
     */
    @PostMapping("/chat")
    @Operation(summary = "知识库问答", description = "从指定知识库召回相关片段，并以其为上下文生成回答。")
    public Result<RagChatResultDto> ragChat(
            @Valid @RequestBody RagChatDto ragChatDto
    ) {
        return Result.buildSuccess(ragService.ragChat(ragChatDto));
    }

    /**
     * 在指定知识库中检索相关片段，并通过 SSE 实时返回模型生成结果。
     *
     * <p>事件依次包括引用来源（sources）、回答增量（delta）和完成信息（done）；
     * 生成失败时会返回 error 事件。</p>
     *
     * @param ragChatDto 知识库 ID、问题和可选的检索参数
     * @return 持续输出的 RAG SSE 事件流
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式知识库问答", description = "从指定知识库召回相关片段，并通过 SSE 实时返回回答。")
    public Flux<ServerSentEvent<RagStreamEvent>> ragStreamChat(
            @Valid @RequestBody RagChatDto ragChatDto
    ) {
        return ragService.ragStreamChat(ragChatDto);
    }
}
