-- ============================================================
-- 1. 知识库
-- ============================================================

CREATE TABLE knowledge_base
(
    id          UUID         PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(500),
    status      VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_knowledge_base_status
        CHECK (status IN ('ENABLED', 'DISABLED'))
);


-- 知识库名称忽略大小写后不能重复
CREATE UNIQUE INDEX uk_knowledge_base_name_lower
    ON knowledge_base (LOWER(name));


-- ============================================================
-- 2. 知识库文档
-- ============================================================

CREATE TABLE kb_document
(
    id                  UUID          PRIMARY KEY,
    knowledge_base_id   UUID          NOT NULL,

    -- 原始文件信息
    original_name       VARCHAR(255)  NOT NULL,
    stored_name         VARCHAR(255)  NOT NULL,
    storage_path        VARCHAR(1000) NOT NULL,
    content_type        VARCHAR(128),
    file_extension      VARCHAR(32)   NOT NULL,
    size_bytes          BIGINT        NOT NULL,
    sha256              CHAR(64)      NOT NULL,

    -- 文档处理信息
    status              VARCHAR(32)   NOT NULL DEFAULT 'UPLOADED',
    parsed_storage_path VARCHAR(1000),
    parsed_preview      VARCHAR(1000),
    parsed_char_count   BIGINT,
    error_message       VARCHAR(2000),

    created_at          TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_document_kb_sha256
        UNIQUE (knowledge_base_id, sha256),

    CONSTRAINT ck_document_status
        CHECK (status IN ('UPLOADED', 'PARSED', 'FAILED')),

    CONSTRAINT ck_document_size
        CHECK (size_bytes >= 0),

    CONSTRAINT ck_document_parsed_char_count
        CHECK (
            parsed_char_count IS NULL
                OR parsed_char_count >= 0
            )
);


-- ============================================================
-- 3. 文档查询索引
-- ============================================================

CREATE INDEX idx_document_knowledge_base
    ON kb_document (knowledge_base_id);

CREATE INDEX idx_document_status
    ON kb_document (status);

CREATE INDEX idx_document_created_at
    ON kb_document (created_at DESC);