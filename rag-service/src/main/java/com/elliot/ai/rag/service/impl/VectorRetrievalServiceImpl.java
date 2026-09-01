package com.elliot.ai.rag.service.impl;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.config.RagProperties;
import com.elliot.ai.rag.retrieval.model.RetrievalCandidate;
import com.elliot.ai.rag.dto.RetrievalHitDto;
import com.elliot.ai.rag.dto.RetrievalSearchDto;
import com.elliot.ai.rag.dto.RetrievalSearchResultDto;
import com.elliot.ai.rag.entity.KnowledgeBase;
import com.elliot.ai.rag.enums.KnowledgeBaseStatus;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;
import com.elliot.ai.rag.service.KnowledgeBaseService;
import com.elliot.ai.rag.service.VectorRetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 向量检索
 */
@RequiredArgsConstructor
@Service
public class VectorRetrievalServiceImpl implements VectorRetrievalService {

    private final VectorStoreRetriever vectorStoreRetriever;

    private final KnowledgeBaseService knowledgeBaseService;

    private final RagProperties ragProperties;


    @Override
    public RetrievalSearchResultDto search(RetrievalSearchDto searchDto) {
        String query = searchDto.getQuery().trim();
        int topK =
                searchDto.getTopK() == null
                        ? ragProperties.getTopK()
                        : searchDto.getTopK();

        double threshold =
                searchDto.getSimilarityThreshold() == null
                        ? ragProperties.getSimilarityThreshold()
                        : searchDto.getSimilarityThreshold();
        List<RetrievalCandidate> candidates = retrieve(new RetrievalQuery(
                searchDto.getKnowledgeBaseId(),
                query,
                topK,
                threshold
        ));
        List<RetrievalHitDto> hits = candidates.stream().map(this::toRetrievalHit).toList();
        return new RetrievalSearchResultDto(searchDto.getKnowledgeBaseId(),
                query,
                topK,
                threshold,
                hits.size(), hits);
    }

    private RetrievalHitDto toRetrievalHit(RetrievalCandidate retrievalCandidate) {
        return new RetrievalHitDto(
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
        // 仅允许从处于启用状态的知识库中检索。
        validateKnowledgeBase(query.knowledgeBaseId());
        // 向量写入时将 knowledgeBaseId 保存为字符串 metadata，因此过滤值也使用字符串。
        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        Filter.Expression knowledgeBaseFilter = filterBuilder.eq("knowledgeBaseId", query.knowledgeBaseId().toString())
                .build();
        // 查询文本生成向量后，仅在目标知识库的向量范围内按相似度召回 TopK 个结果。
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query.query().trim())
                .topK(query.topK())
                .similarityThreshold(query.similarityThreshold())
                .filterExpression(knowledgeBaseFilter)
                .build();
        List<Document> documents = vectorStoreRetriever.similaritySearch(searchRequest);
        return convertCandidates(documents);
    }


    private List<RetrievalCandidate> convertCandidates(List<Document> documents) {
        List<RetrievalCandidate> results = new ArrayList<>(documents.size());

        for (int index = 0; index < documents.size(); index++) {
            Document document = documents.get(index);
            Map<String, Object> metadata = document.getMetadata();
            UUID chunkId = parseUUid(metadata.get("chunkId"), document.getId());
            UUID documentId = parseUUid(metadata.get("documentId"), null);
            results.add(
                    new RetrievalCandidate(
                            index + 1,
                            document.getScore(),
                            chunkId,
                            documentId,
                            getString(metadata, "documentName"),
                            getInteger(metadata, "chunkIndex"),
                            getString(metadata, "sectionTitle"),
                            getInteger(metadata, "pageNumber"),
                            document.getText()
                    )
            );
        }

        return results;
    }

    private String getString(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null
                ? null
                : value.toString();
    }

    private Integer getInteger(
            Map<String, Object> metadata,
            String key
    ) {
        Object value = metadata.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private UUID parseUUid(Object metadataValue, String fallbackValue) {
        String value = metadataValue == null ? fallbackValue : metadataValue.toString();
        if (value == null) {
            return null;
        }
        return UUID.fromString(value);
    }


    private void validateKnowledgeBase(UUID knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(knowledgeBaseId);
        if (!KnowledgeBaseStatus.ENABLED.equals(knowledgeBase.getStatus())) {
            throw new BusinessException(ResultCode.FAIL, "知识库已被禁用");
        }
    }
}
