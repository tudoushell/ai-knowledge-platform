package com.elliot.ai.rag.retrieval.keyword;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.config.RagProperties;
import com.elliot.ai.rag.dto.KeywordSearchDto;
import com.elliot.ai.rag.dto.KeywordSearchHitDto;
import com.elliot.ai.rag.dto.KeywordSearchRequestDto;
import com.elliot.ai.rag.dto.KeywordSearchResultDto;
import com.elliot.ai.rag.retrieval.model.RetrievalCandidate;
import com.elliot.ai.rag.entity.KnowledgeBase;
import com.elliot.ai.rag.enums.KnowledgeBaseStatus;
import com.elliot.ai.rag.mapper.DocumentChunkMapper;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;
import com.elliot.ai.rag.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KeywordRetrievalServiceImpl implements KeywordRetrievalService {

    private final DocumentChunkMapper documentChunkMapper;

    private final KnowledgeBaseService knowledgeBaseService;

    private final RagProperties ragProperties;


    @Override
    public KeywordSearchDto search(KeywordSearchRequestDto request) {
        if (!StringUtils.hasText(request.query())) {
            throw new BusinessException(
                    ResultCode.FAIL,
                    "搜索关键词不能为空"
            );
        }

        String query = request.query().trim();

        if (query.length() < 2) {
            throw new BusinessException(
                    ResultCode.FAIL,
                    "搜索关键词至少需要 2 个字符"
            );
        }

        int topK = request.topK() == null ? ragProperties.getTopK() : request.topK();


        List<RetrievalCandidate> candidates = retrieve(new RetrievalQuery(
                request.knowledgeBaseId(),
                query,
                topK,
                ragProperties.getSimilarityThreshold()
        ));
        List<KeywordSearchHitDto> hits =
                candidates.stream().map(this::toKeywordHit).toList();

        return new KeywordSearchDto(
                request.knowledgeBaseId(),
                query,
                topK,
                hits.size(),
                hits
        );
    }

    private KeywordSearchHitDto toKeywordHit(RetrievalCandidate retrievalCandidate) {
        return new KeywordSearchHitDto(
                retrievalCandidate.rank(),
                retrievalCandidate.score(),
                retrievalCandidate.chunkId(),
                retrievalCandidate.documentId(),
                retrievalCandidate.documentName(),
                retrievalCandidate.chunkIndex(),
                retrievalCandidate.sectionTitle(),
                retrievalCandidate.pageNumber(),
                retrievalCandidate.content()
        );
    }

    @Override
    public List<RetrievalCandidate> retrieve(RetrievalQuery query) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(query.knowledgeBaseId());
        if (!KnowledgeBaseStatus.ENABLED.equals(knowledgeBase.getStatus())) {
            throw new BusinessException(ResultCode.FAIL, "当前知识库不可用");
        }
        List<KeywordSearchResultDto> results =
                documentChunkMapper.keywordSearch(knowledgeBase.getId(), query.query(), query.topK());

        List<RetrievalCandidate> candidates = new ArrayList<>(results.size());

        for (int index = 0; index < results.size(); index++) {
            KeywordSearchResultDto result = results.get(index);
            candidates.add(new RetrievalCandidate(
                    index + 1,
                    result.getScore(),
                    result.getChunkId(),
                    result.getDocumentId(),
                    result.getDocumentName(),
                    result.getChunkIndex(),
                    result.getSectionTitle(),
                    result.getPageNumber(),
                    result.getContent()
            ));
        }
        return candidates;
    }
}
