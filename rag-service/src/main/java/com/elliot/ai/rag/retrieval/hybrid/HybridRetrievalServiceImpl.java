package com.elliot.ai.rag.retrieval.hybrid;

import com.elliot.ai.rag.config.RagProperties;
import com.elliot.ai.rag.dto.HybridSearchDto;
import com.elliot.ai.rag.dto.HybridSearchHitDto;
import com.elliot.ai.rag.dto.HybridSearchRequestDto;
import com.elliot.ai.rag.retrieval.model.RetrievalCandidate;
import com.elliot.ai.rag.retrieval.model.HybridCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;
import com.elliot.ai.rag.retrieval.keyword.KeywordRetrievalService;
import com.elliot.ai.rag.retrieval.vector.VectorRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class HybridRetrievalServiceImpl implements HybridRetrievalService {

    private final VectorRetrievalService vectorRetrievalService;

    private final KeywordRetrievalService keywordRetrievalService;

    private final RagProperties ragProperties;


    @Override
    public HybridSearchDto search(HybridSearchRequestDto request) {
        String query = request.query().trim();

        int topK =
                request.topK() == null
                        ? ragProperties.getTopK()
                        : request.topK();

        double similarityThreshold =
                request.similarityThreshold() == null
                        ? ragProperties.getSimilarityThreshold()
                        : request.similarityThreshold();

        RetrievalQuery retrievalQuery =
                new RetrievalQuery(
                        request.knowledgeBaseId(),
                        query,
                        topK,
                        similarityThreshold
                );

        List<HybridCandidate> candidates =
                retrieve(retrievalQuery);

        List<HybridSearchHitDto> hits =
                candidates.stream()
                        .map(this::toHybridSearchHit)
                        .toList();

        return new HybridSearchDto(
                retrievalQuery.knowledgeBaseId(),
                retrievalQuery.query(),
                retrievalQuery.topK(),
                ragProperties.getRecallTopK(),
                retrievalQuery.similarityThreshold(),
                ragProperties.getRrfK(),
                hits.size(),
                hits
        );
    }

    private HybridSearchHitDto toHybridSearchHit(HybridCandidate candidate) {
        return new HybridSearchHitDto(
                candidate.getHybridRank(),
                candidate.getRrfScore(),
                candidate.getChunkId(),
                candidate.getDocumentId(),
                candidate.getDocumentName(),
                candidate.getChunkIndex(),
                candidate.getSectionTitle(),
                candidate.getPageNumber(),
                candidate.getContent(),
                candidate.getVectorRank(),
                candidate.getVectorScore(),
                candidate.getKeywordRank(),
                candidate.getKeywordScore()
        );
    }

    @Override
    public List<HybridCandidate> retrieve(RetrievalQuery query) {
        RetrievalQuery recallQuery = buildRecallQuery(query);
        //向量查询召回
        List<RetrievalCandidate> vectorCandidates = vectorRetrievalService.retrieve(recallQuery);
        //关键字查询召回
        List<RetrievalCandidate> keywordCandidates = keywordRetrievalService.retrieve(recallQuery);
        log.debug("hybrid retrieval recall completed, vector={}, keyword={}",
                vectorCandidates.size(),
                keywordCandidates.size()
        );
        //根据chunkId 统计 向量和关键字查询 rank和score
        Map<UUID, HybridCandidate> candidateMap = new LinkedHashMap<>();
        mergeVectorCandidates(candidateMap, vectorCandidates);
        mergeKeywordCandidates(candidateMap, keywordCandidates);
        //计算RRF score
        candidateMap.values().forEach(each -> each.setRrfScore(calculateRrfScore(each)));
        //RRF 粗排
        return rankByRrf(candidateMap.values());
    }

    private void assignHybridRank(List<HybridCandidate> candidates) {
        for (int index = 0; index < candidates.size(); index++) {
            candidates.get(index).setHybridRank(index + 1);
        }
    }

    /**
     * 根据rrf score 排序，
     * 如果score相等，则以chunkId排序
     *
     * @param candidates
     * @param topK
     * @return
     */
    private List<HybridCandidate> rankCandidates(Collection<HybridCandidate> candidates,
                                                 int topK) {
        return candidates.stream().sorted(
                Comparator
                        .comparing(HybridCandidate::getRrfScore, Comparator.reverseOrder())
                        .thenComparing(HybridCandidate::getChunkId)

        ).limit(topK).toList();
    }


    private List<HybridCandidate> rankByRrf(Collection<HybridCandidate> candidates) {
        List<HybridCandidate> ranked = candidates.stream().sorted(Comparator
                        .comparing(HybridCandidate::getRrfScore,
                                Comparator.reverseOrder())
                        .thenComparing(HybridCandidate::getChunkId))
                .limit(ragProperties.getRerankCandidateTopK()).toList();
        for (int index = 0; index < ranked.size(); index++) {
            ranked.get(index)
                    .setRrfRank(index + 1);
        }
        return ranked;

    }

    private double calculateRrfScore(HybridCandidate candidate) {
        int rrfK = ragProperties.getRrfK();
        double score = 0.0;
        if (candidate.getVectorRank() != null) {
            score += reciprocalRank(
                    rrfK,
                    candidate.getVectorRank()
            );
        }

        if (candidate.getKeywordRank() != null) {
            score += reciprocalRank(
                    rrfK,
                    candidate.getKeywordRank()
            );
        }
        return score;
    }

    private double reciprocalRank(
            int rrfK,
            int rank
    ) {
        return 1.0 / (rrfK + rank);
    }


    private void mergeVectorCandidates(
            Map<UUID, HybridCandidate> candidateMap,
            List<RetrievalCandidate> candidates
    ) {
        for (RetrievalCandidate candidate : candidates) {
            HybridCandidate hybridCandidate = candidateMap.computeIfAbsent(candidate.chunkId(),
                    chunkId -> createCandidate(candidate));
            hybridCandidate.setVectorRank(candidate.rank());
            hybridCandidate.setVectorScore(candidate.score());
        }
    }

    private void mergeKeywordCandidates(
            Map<UUID, HybridCandidate> candidateMap,
            List<RetrievalCandidate> candidates
    ) {
        for (RetrievalCandidate candidate : candidates) {
            HybridCandidate hybridCandidate = candidateMap.computeIfAbsent(candidate.chunkId(),
                    chunkId -> createCandidate(candidate));
            hybridCandidate.setKeywordRank(candidate.rank());
            hybridCandidate.setKeywordScore(candidate.score());
        }
    }

    private HybridCandidate createCandidate(RetrievalCandidate candidate) {
        return HybridCandidate.builder()
                .chunkId(candidate.chunkId())
                .documentId(candidate.documentId())
                .documentName(candidate.documentName())
                .chunkIndex(candidate.chunkIndex())
                .sectionTitle(candidate.sectionTitle())
                .pageNumber(candidate.pageNumber())
                .content(candidate.content())
                .build();
    }


    private RetrievalQuery buildRecallQuery(RetrievalQuery query) {
        return new RetrievalQuery(
                query.knowledgeBaseId(),
                query.query(),
                ragProperties.getRecallTopK(),
                query.similarityThreshold()
        );
    }
}
