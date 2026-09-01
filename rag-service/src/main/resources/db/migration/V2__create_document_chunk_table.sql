-- 文档增加 Chunk 数量
ALTER TABLE kb_document
    ADD COLUMN chunk_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE kb_document
    ADD COLUMN chunk_strategy VARCHAR(32);

ALTER TABLE kb_document
    ADD COLUMN chunk_size INTEGER;

ALTER TABLE kb_document
    ADD COLUMN chunk_overlap INTEGER NOT NULL DEFAULT 0;

-- 扩展文档状态
ALTER TABLE kb_document
    DROP CONSTRAINT ck_document_status;

ALTER TABLE kb_document
    ADD CONSTRAINT ck_document_status
        CHECK (
            status IN (
                       'UPLOADED',
                       'PARSED',
                       'CHUNKED',
                       'FAILED'
                )
            );

CREATE TABLE document_chunk
(
    id                UUID PRIMARY KEY,
    knowledge_base_id UUID        NOT NULL,
    document_id       UUID        NOT NULL,

    chunk_index       INTEGER     NOT NULL,
    content           TEXT        NOT NULL,
    char_count        INTEGER     NOT NULL,
    token_count       INTEGER,
    content_hash      CHAR(64)    NOT NULL,

    section_title     VARCHAR(500),
    page_number       INTEGER,

    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chunk_knowledge_base
    ON document_chunk (knowledge_base_id);

CREATE INDEX idx_chunk_document
    ON document_chunk (document_id);

CREATE INDEX idx_chunk_content_hash
    ON document_chunk (content_hash);