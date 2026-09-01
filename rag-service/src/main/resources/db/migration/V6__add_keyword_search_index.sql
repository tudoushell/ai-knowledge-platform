CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_chunk_content_trgm
    on document_chunk
        USING GIN (
                   content gin_trgm_ops
            );

CREATE INDEX IF NOT EXISTS idx_chunk_section_title_trgm
ON document_chunk
USING GIN(
         section_title gin_trgm_ops
        );