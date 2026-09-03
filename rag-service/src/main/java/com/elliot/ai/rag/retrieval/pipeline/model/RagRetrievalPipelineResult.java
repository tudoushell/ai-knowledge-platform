package com.elliot.ai.rag.retrieval.pipeline.model;

import com.elliot.ai.rag.retrieval.model.RerankCandidate;

import java.util.List;

public record RagRetrievalPipelineResult(
        List<RerankCandidate> candidates,
        RetrievalTrace trace
) {
}
