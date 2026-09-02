package com.elliot.ai.rag.retrieval.multiquery;

import com.elliot.ai.rag.retrieval.model.HybridCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;

import java.util.List;

public interface MultiQueryRetrievalService {

    List<HybridCandidate> retrieve(List<RetrievalQuery> queries);
}
