package com.elliot.ai.rag.retrieval.multiquery;

import com.elliot.ai.rag.retrieval.hybrid.HybridRetrievalService;
import com.elliot.ai.rag.retrieval.model.HybridCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;
import com.elliot.ai.rag.retrieval.multiquery.model.MultiQueryCandidate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MultiQueryRetrievalServiceImpl implements MultiQueryRetrievalService {

    private final HybridRetrievalService hybridRetrievalService;

    private static final int MULTI_QUERY_RRF_K = 60;

    @Override
    public List<MultiQueryCandidate> retrieve(List<RetrievalQuery> queries) {
        Map<UUID, CandidateAccumulator> accumulators = new LinkedHashMap<>();
        for (RetrievalQuery query : queries) {
            List<HybridCandidate> candidates = hybridRetrievalService.retrieve(query);
            for (HybridCandidate candidate : candidates) {
                CandidateAccumulator accumulator =
                        accumulators.computeIfAbsent(candidate.getChunkId(), key -> new CandidateAccumulator());
                accumulator.add(candidate, MULTI_QUERY_RRF_K);
            }
        }
        List<CandidateAccumulator> ranked = accumulators.values()
                .stream()
                .sorted(Comparator.comparingDouble(CandidateAccumulator::getScore).reversed())
                .toList();

        List<MultiQueryCandidate> result = new ArrayList<>(ranked.size());
        for (int index = 0; index < ranked.size(); index++) {
            CandidateAccumulator accumulator = ranked.get(index);
            result.add(
                    new MultiQueryCandidate(
                            accumulator.getCandidate(),
                            accumulator.getScore(),
                            accumulator.getHitCount(),
                            index + 1
                    )
            );
        }
        return List.copyOf(result);
    }


    @Getter
    private static class CandidateAccumulator {
        private HybridCandidate candidate;
        private double score;
        private int hitCount;

        private void add(HybridCandidate current,
                         int rrf) {
            score += 1.0 / (rrf + current.getRrfRank());
            hitCount++;
            if (candidate == null
                    || current.getRrfRank() < candidate.getRrfRank()) {
                candidate = current;
            }

        }
    }
}
