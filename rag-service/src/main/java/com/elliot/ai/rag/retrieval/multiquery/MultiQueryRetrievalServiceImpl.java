package com.elliot.ai.rag.retrieval.multiquery;

import com.elliot.ai.rag.retrieval.hybrid.HybridRetrievalService;
import com.elliot.ai.rag.retrieval.model.HybridCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MultiQueryRetrievalServiceImpl implements MultiQueryRetrievalService {

    private final HybridRetrievalService hybridRetrievalService;

    @Override
    public List<HybridCandidate> retrieve(List<RetrievalQuery> queries) {
        Map<UUID, HybridCandidate> candidatesByChunkId = new LinkedHashMap<>();
        for (RetrievalQuery query : queries) {
            List<HybridCandidate> candidates = hybridRetrievalService.retrieve(query);
            for (HybridCandidate candidate : candidates) {
                candidatesByChunkId.putIfAbsent(candidate.getChunkId(), candidate);
            }
        }
        return List.copyOf(candidatesByChunkId.values());
    }
}
