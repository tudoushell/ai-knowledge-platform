package com.elliot.ai.rag.trace.model;

import com.elliot.ai.rag.retrieval.pipeline.model.RetrievalTrace;

public record RagTrace(
        ConversationTrace conversation,
        RetrievalTrace retrieval
) {
}
