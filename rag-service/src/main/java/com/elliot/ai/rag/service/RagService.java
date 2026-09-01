package com.elliot.ai.rag.service;

import com.elliot.ai.rag.dto.RagChatDto;
import com.elliot.ai.rag.dto.RagChatResultDto;
import com.elliot.ai.rag.dto.RagStreamEvent;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface RagService {

    /**
     * 检索知识库并通过 SSE 流式生成回答。
     * <>p</>
     *                           User Question
     *                               │
     *                               ▼
     *                        RetrievalQuery
     *                               │
     *                               ▼
     *                  HybridRetrievalService
     *                      /                \
     *                     ▼                  ▼
     *                Vector20           Keyword20
     *                     \                 /
     *                      \               /
     *                       ▼             ▼
     *                          RRF Top10
     *                               │
     *                               ▼
     *                        RerankService
     *                               │
     *                               ▼
     *                     RerankModelClient
     *                               │
     *                               ▼
     *                          HTTP Model
     *                               │
     *                               ▼
     *                          Final Top5
     *                               │
     *                               ▼
     *                     RerankCandidate
     *                               │
     *                               ▼
     *                     ContextCandidate
     *                               │
     *                               ▼
     *                  ChunkContextExpansion
     *                               │
     *                               ▼
     *                            Context
     *                               │
     *                               ▼
     *                              LLM
     *
     * @param ragChatDto 知识库 ID、问题和检索参数
     * @return 引用来源、回答增量、完成信息或错误信息组成的事件流
     */
    Flux<ServerSentEvent<RagStreamEvent>> ragStreamChat(RagChatDto ragChatDto);

    /**
     * 检索知识库并一次性生成完整回答。
     *
     * @param ragChatDto 知识库 ID、问题和检索参数
     * @return 完整回答、引用来源及 Token 用量
     */
    RagChatResultDto ragChat(RagChatDto ragChatDto);
}
