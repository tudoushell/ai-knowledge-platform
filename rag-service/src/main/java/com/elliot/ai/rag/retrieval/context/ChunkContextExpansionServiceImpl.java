package com.elliot.ai.rag.retrieval.context;

import com.elliot.ai.rag.config.RagProperties;
import com.elliot.ai.rag.dto.ExpandedSource;
import com.elliot.ai.rag.dto.RagSourceChunkDto;
import com.elliot.ai.rag.entity.DocumentChunk;
import com.elliot.ai.rag.retrieval.model.ContextCandidate;
import com.elliot.ai.rag.service.DocumentChunkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ChunkContextExpansionServiceImpl implements ChunkContextExpansionService {

    private final DocumentChunkService documentChunkService;
    private final RagProperties ragProperties;

    /**
     * 将一个命中的 Chunk 扩展为带相邻上下文的引用来源。
     *
     * <p>以命中 Chunk 为中心，按 {@code adjacentChunkRadius} 查询前后相邻 Chunk；
     * 返回结果保留哪一个 Chunk 是命中，供 Prompt 构建和前端引用详情展示。
     * 如果没有查到相邻 Chunk，或扩展结果不包含原始命中 Chunk，则回退到原始检索结果。</p>
     *
     * @param candidate 检索命中的 Chunk
     * @return 包含命中 Chunk 及其相邻上下文的引用来源
     */
    @Override
    public ExpandedSource expand(ContextCandidate candidate) {
        // 例如 radius=1 时，尝试读取命中 Chunk 前后各一个 Chunk。
        int radius = ragProperties.getAdjacentChunkRadius();
        Integer matchedIndex = candidate.chunkIndex();
        int startIndex = Math.max(0, matchedIndex - radius);
        int endIndex = matchedIndex + radius;

        // 按 Chunk 序号读取同一文档中连续的上下文片段。
        List<DocumentChunk> chunks = documentChunkService.selectNeighborChunks(candidate.documentId(), startIndex, endIndex);
        if (chunks.isEmpty()) {
            // 文档 Chunk 已删除、索引不一致等情况下，至少保留向量检索返回的原始片段。
            return fallback(candidate);
        }

        int totalChars = 0;
        List<RagSourceChunkDto> sourceChunks = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            // 优先按 Chunk ID 判断；旧数据没有 ID 时再按 Chunk 序号兜底。
            boolean matched = isMatched(candidate, chunk);

            /*
             * 当前策略仅在“命中 Chunk”自身超过单条来源字符上限时跳过；
             * totalChars 用于记录已收集内容的总长度，供后续长度控制策略扩展。
             */
            if (matched && totalChars + chunk.getContent().length() > ragProperties.getMaxSourceChars()) {
                continue;
            }

            // 将实体转换为 DTO，并显式标记它是否为向量命中的核心片段。
            sourceChunks.add(new RagSourceChunkDto(
                    chunk.getId(),
                    chunk.getChunkIndex(),
                    chunk.getSectionTitle(),
                    chunk.getPageNumber(),
                    chunk.getContent(),
                    matched
            ));
            totalChars += chunk.getContent().length();
        }

        // 扩展结果必须保留真正命中的 Chunk，否则相邻内容不能作为可靠引用。
        boolean containsMatchedChunk = sourceChunks.stream().anyMatch(RagSourceChunkDto::matched);
        if (!containsMatchedChunk) {
            return fallback(candidate);
        }

        // 实际范围以最终保留下来的首尾 Chunk 为准，而不是初始计算范围。
        Integer actualStart = sourceChunks.get(0).chunkIndex();
        Integer actualEnd = sourceChunks.get(sourceChunks.size() - 1).chunkIndex();
        return new ExpandedSource(
                candidate.rank(),
                candidate.score(),
                candidate.chunkId(),
                candidate.chunkIndex(),
                candidate.documentId(),
                candidate.documentName(),
                actualStart,
                actualEnd,
                List.copyOf(sourceChunks));
    }

    private boolean isMatched(ContextCandidate candidate, DocumentChunk chunk) {
        if (candidate.chunkId() != null) {
            return candidate.chunkId().equals(chunk.getId());
        }
        return candidate.chunkIndex() != null && candidate.chunkIndex().equals(chunk.getChunkIndex());
    }

    private ExpandedSource fallback(ContextCandidate candidate) {
        RagSourceChunkDto chunk = new RagSourceChunkDto(
                candidate.chunkId(),
                candidate.chunkIndex(),
                candidate.sectionTitle(),
                candidate.pageNumber(),
                candidate.content(),
                true
        );
        return new ExpandedSource(
                candidate.rank(),
                candidate.score(),
                candidate.chunkId(),
                candidate.chunkIndex(),
                candidate.documentId(),
                candidate.documentName(),
                candidate.chunkIndex(),
                candidate.chunkIndex(),
                List.of(chunk)
        );
    }
}
