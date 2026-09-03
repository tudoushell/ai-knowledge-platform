package com.elliot.ai.rag.retrieval.multiquery;

import com.elliot.ai.rag.retrieval.model.HybridCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;
import com.elliot.ai.rag.retrieval.multiquery.model.MultiQueryCandidate;

import java.util.List;

public interface MultiQueryRetrievalService {

    List<MultiQueryCandidate> retrieve(List<RetrievalQuery> queries);
}
