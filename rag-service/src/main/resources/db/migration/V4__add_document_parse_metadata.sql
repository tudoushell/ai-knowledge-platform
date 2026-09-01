ALTER TABLE kb_document
    ADD COLUMN parsed_format VARCHAR(32);

ALTER TABLE kb_document
    ADD COLUMN page_count INTEGER;

ALTER TABLE kb_document
    ADD COLUMN parsed_block_count INTEGER NOT NULL DEFAULT 0;