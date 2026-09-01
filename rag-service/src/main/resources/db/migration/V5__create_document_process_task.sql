ALTER TABLE kb_document
    DROP CONSTRAINT ck_document_status;

CREATE TABLE document_process_task
(
    id            UUID PRIMARY KEY,
    document_id   UUID        NOT NULL,

    task_type     VARCHAR(32) NOT NULL,
    status        VARCHAR(32) NOT NULL,
    current_step  VARCHAR(32) NOT NULL,
    progress      INTEGER     NOT NULL DEFAULT 0,

    retry_count   INTEGER     NOT NULL DEFAULT 0,
    max_retries   INTEGER     NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMPTZ,

    worker_id     VARCHAR(128),
    locked_at     TIMESTAMPTZ,
    heartbeat_at  TIMESTAMPTZ,

    error_code    VARCHAR(128),
    error_message VARCHAR(2000),

    started_at    TIMESTAMPTZ,
    finished_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_process_task_status_retry
    ON document_process_task (
                              status,
                              next_retry_at,
                              created_at
        );

CREATE INDEX idx_process_task_document
    ON document_process_task (document_id);

-- 一个文档同一时间只能有一个活动任务
CREATE UNIQUE INDEX uk_active_document_process_task
    ON document_process_task (document_id)
    WHERE status IN (
                     'PENDING',
                     'RUNNING',
                     'RETRY_WAIT'
        );