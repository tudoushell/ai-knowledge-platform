package com.elliot.ai.rag.service.impl;

import com.elliot.ai.rag.config.RerankProperties;
import com.elliot.ai.rag.retrieval.model.HybridCandidate;
import com.elliot.ai.rag.retrieval.model.RerankCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;
import com.elliot.ai.rag.retrieval.rerank.RerankModelClient;
import com.elliot.ai.rag.retrieval.rerank.exception.RerankException;
import com.elliot.ai.rag.retrieval.rerank.model.RerankDocument;
import com.elliot.ai.rag.retrieval.rerank.model.RerankModelResult;
import com.elliot.ai.rag.service.RerankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RerankServiceImpl implements RerankService {

    private final RerankModelClient rerankModelClient;

    private final RerankProperties rerankProperties;

    @Override
    public List<RerankCandidate> rerank(RetrievalQuery query, List<HybridCandidate> candidates) {
        StopWatch stopWatch = new StopWatch("rerank");
        stopWatch.start();
        try {
            if (candidates == null || candidates.isEmpty()) {
                return List.of();
            }
            if (!rerankProperties.isEnabled()) {
                return fallback(
                        query,
                        candidates
                );
            }

            try {
                return doRerank(query, candidates);
            } catch (RerankException e) {

                if (!rerankProperties.isFallbackEnabled()) {
                    throw e;
                }
                log.warn(
                        "rerank failed, fallback to rrf ranking, knowledgeBaseId={}, candidateCount={}, reason={}",
                        query.knowledgeBaseId(),
                        candidates.size(),
                        e.getMessage()
                );
                return fallback(query, candidates);
            }
        } finally {
            stopWatch.stop();
            log.info(
                    "rerank finished, knowledgeBaseId={}, candidateCount={}, elapsedMillis={}",
                    query == null ? null : query.knowledgeBaseId(),
                    candidates == null ? 0 : candidates.size(),
                    stopWatch.getTotalTimeMillis()
            );
        }
    }

    private List<RerankCandidate> fallback(RetrievalQuery query, List<HybridCandidate> candidates) {
        List<HybridCandidate> fallbackCandidates = candidates.stream()
                .sorted(Comparator.comparing(HybridCandidate::getRrfRank)
                        .thenComparing(Comparator.comparing(HybridCandidate::getChunkId)))
                .limit(query.topK())
                .toList();
        List<RerankCandidate> results = new ArrayList<>(fallbackCandidates.size());
        for (int index = 0; index < fallbackCandidates.size(); index++) {
            HybridCandidate candidate = fallbackCandidates.get(index);
            results.add(new RerankCandidate(
                    index + 1,
                    null,
                    false,
                    candidate.getRrfRank(),
                    candidate.getRrfScore(),
                    candidate.getChunkId(),
                    candidate.getDocumentId(),
                    candidate.getDocumentName(),
                    candidate.getChunkIndex(),
                    candidate.getSectionTitle(),
                    candidate.getPageNumber(),
                    candidate.getContent()
            ));
        }
        return results;
    }

    private List<RerankCandidate> doRerank(RetrievalQuery query, List<HybridCandidate> candidates) {
        //1.构建Document
        List<RerankDocument> documents = buildDocuments(candidates);
        //2.调用Rerank 模型
        List<RerankModelResult> modelResults = rerankModelClient.rerank(query.query(),
                documents);
        //3. 模型没有返回任何结果
        if (modelResults == null
                || modelResults.isEmpty()) {

            throw new RerankException(
                    "Rerank model returned no results"
            );
        }
        //4. index -> hybridCandidate
        List<ScoredCandidate> scoredCandidates = mapModelResults(candidates, modelResults);
        //5. sorted and final TopK
        List<ScoredCandidate> topCandidates = sortByRerankScore(scoredCandidates, query.topK());
        return toRerankCandidates(topCandidates);
    }


    private List<RerankCandidate> toRerankCandidates(List<ScoredCandidate> scoredCandidates) {
        List<RerankCandidate> results = new ArrayList<>(scoredCandidates.size());
        for (int index = 0; index < scoredCandidates.size(); index++) {
            ScoredCandidate scored = scoredCandidates.get(index);
            HybridCandidate candidate = scored.candidate;
            results.add(
                    new RerankCandidate(
                            index + 1,
                            scored.score,
                            true,
                            candidate.getRrfRank(),
                            candidate.getRrfScore(),
                            candidate.getChunkId(),
                            candidate.getDocumentId(),
                            candidate.getDocumentName(),
                            candidate.getChunkIndex(),
                            candidate.getSectionTitle(),
                            candidate.getPageNumber(),
                            candidate.getContent()
                    )
            );
        }
        return results;
    }


    private List<ScoredCandidate> sortByRerankScore(List<ScoredCandidate> candidates, int topK) {
        return candidates.stream()
                .sorted(
                        Comparator.comparingDouble(
                                        ScoredCandidate::score
                                )
                                .reversed()
                                .thenComparing(item -> item.candidate.getChunkId())
                )
                .limit(topK)
                .toList();
    }


    private List<ScoredCandidate> mapModelResults(List<HybridCandidate> candidates,
                                                  List<RerankModelResult> results) {
        Set<Integer> seenIndexes = new HashSet<>();

        List<ScoredCandidate> scoredCandidates = new ArrayList<>(results.size());

        for (RerankModelResult result : results) {
            validateIndex(result, candidates.size());
            if (!seenIndexes.add(result.index())) {
                throw new RerankException(
                        "Duplicate rerank result index: "
                                + result.index()
                );
            }

            HybridCandidate candidate = candidates.get(result.index());
            scoredCandidates.add(
                    new ScoredCandidate(candidate, result.score())
            );
        }
        return scoredCandidates;
    }


    private void validateIndex(RerankModelResult result,
                               int candidateSize) {
        if (result.index() < 0 || result.index() >= candidateSize) {
            throw new RerankException(
                    "Invalid rerank result index: "
                            + result.index()
            );
        }

    }


    private List<RerankDocument> buildDocuments(List<HybridCandidate> candidates) {
        return IntStream.range(0, candidates.size())
                .mapToObj(index -> {
                    HybridCandidate candidate = candidates.get(index);
                    return new RerankDocument(
                            index,
                            candidate.getChunkId(),
                            candidate.getContent()
                    );
                }).toList();
    }


    private record ScoredCandidate(
            HybridCandidate candidate,
            double score
    ) {

    }
}
