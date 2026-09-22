package com.elliot.ai.rag.service;

import com.elliot.ai.rag.dto.RagChatDto;
import com.elliot.ai.rag.dto.RagChatResultDto;
import com.elliot.ai.rag.dto.RagStreamEvent;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface RagService {

    /**
     * 基于知识库和会话历史执行 RAG 检索，并通过 SSE 流式生成回答。
     *
     * <pre>
     *                         RagServiceImpl
     *                               │
     *                               ▼
     *                  Resolve/Create Conversation
     *                               │
     *                               ▼
     *                    Conversation History
     *                               │
     *                               ▼
     *                 Conversation Query Rewrite
     *                               │
     *                               ▼
     *                       Standalone Query
     *                               │
     *                               ▼
     *                     RagRetrievalPipeline
     *                               │
     *                               ▼
     *                         Query Rewrite
     *                               │
     *                               ▼
     *                        Query Expansion
     *                               │
     *                 ┌─────────────┴─────────────┐
     *                 ▼                           ▼
     *              Query 1        ...          Query N
     *                 │                           │
     *           ┌─────┴─────┐               ┌─────┴─────┐
     *           ▼           ▼               ▼           ▼
     *      Vector Recall Keyword Recall Vector Recall Keyword Recall
     *           └─────┬─────┘               └─────┬─────┘
     *                 ▼                           ▼
     *              RRF #1                      RRF #1
     *                 └─────────────┬─────────────┘
     *                               ▼
     *                   Group by Chunk ID + RRF #2
     *                               │
     *                               ▼
     *                  Rerank Candidate Top N
     *                               │
     *                               ▼
     *                            Rerank
     *                               │
     *                               ▼
     *                  Final Top K (Request topK)
     *                               │
     *                               ▼
     *                      RerankCandidate
     *                               │
     *                               ▼
     *                    Adjacent Chunk Expansion
     *                               │
     *                               ▼
     *                         Context Build
     *                               │
     *                    Standalone Query + Context
     *                               │
     *                               ▼
     *                         LLM Streaming
     *                               │
     *                               ▼
     *        conversation → trace -> sources → delta... → done
     * </pre>
     *
     * @param ragChatDto 知识库 ID、可选会话 ID、用户问题、检索参数及模型编码
     * @return 包含会话 ID、引用来源、回答增量、完成信息或错误信息的 SSE 事件流
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
