package com.elliot.ai.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elliot.ai.rag.dto.KeywordSearchResultDto;
import com.elliot.ai.rag.entity.DocumentChunk;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.UUID;

/**
 * 文档文本片段数据访问接口。
 */
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    /**
     * 正文出现 + 0.6， 标题 0.3
     *
     * @param knowledgeBaseId
     * @param query
     * @param topK
     * @return
     */
    @Select("""
             SELECT
                    c.id AS chunk_id,
                    c.document_id,
                    d.original_name AS document_name,
                    c.chunk_index,
                    c.section_title,
                    c.page_number,
                    c.content,
            
                    (
                        CASE
                            WHEN c.content ILIKE
                                 '%' || #{query} || '%'
                            THEN 0.6
                            ELSE 0
                        END
            
                        +
            
                        CASE
                            WHEN c.section_title IS NOT NULL
                                 AND c.section_title ILIKE
                                 '%' || #{query} || '%'
                            THEN 0.3
                            ELSE 0
                        END
            
                        +
            
                        0.1 * GREATEST(
                            word_similarity(
                                #{query},
                                c.content
                            ),
                            CASE
                                WHEN c.section_title IS NULL
                                THEN 0
                                ELSE word_similarity(
                                    #{query},
                                    c.section_title
                                )
                            END
                        )
                    )::double precision AS score
            
                FROM document_chunk c
            
                JOIN kb_document d
                  ON d.id = c.document_id
            
                WHERE c.knowledge_base_id
                      = #{knowledgeBaseId}
            
                  AND d.status = 'INDEXED'
            
                  AND (
                      c.content ILIKE
                          '%' || #{query} || '%'
            
                      OR
            
                      c.section_title ILIKE
                          '%' || #{query} || '%'
                  )
                ORDER BY
                    score DESC,
                    c.chunk_index ASC
                LIMIT #{topK}
            """)
    List<KeywordSearchResultDto> keywordSearch(@Param("knowledgeBaseId")
                                               UUID knowledgeBaseId,
                                               @Param("query")
                                               String query,
                                               @Param("topK")
                                               int topK);

    /**
     * 按 Chunk 顺序分页查询指定文档的文本片段。
     */
    @Select("""
            select *
            from document_chunk
            where document_id = #{documentId}
            order by chunk_index asc
            limit #{size} offset #{offset}
            """)
    List<DocumentChunk> selectPageByDocumentId(
            @Param("documentId") UUID documentId,
            @Param("offset") long offset,
            @Param("size") int size
    );

    /**
     * 统计指定文档的 Chunk 总数。
     */
    @Select("""
            select count(*)
            from document_chunk
            where document_id = #{documentId}
            """)
    long countByDocumentId(@Param("documentId") UUID documentId);
}
