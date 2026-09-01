ALTER TABLE kb_document
    ADD COLUMN vector_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE kb_document
    ADD COLUMN embedding_model VARCHAR(128);

ALTER TABLE kb_document
    ADD COLUMN indexed_at TIMESTAMPTZ;

ALTER TABLE kb_document
    DROP CONSTRAINT ck_document_status;

ALTER TABLE kb_document
    ADD CONSTRAINT ck_document_status
        CHECK (
            status IN (
                       'UPLOADED',
                       'PARSED',
                       'CHUNKED',
                       'INDEXING',
                       'INDEXED',
                       'INDEX_FAILED',
                       'FAILED'
                )
            );