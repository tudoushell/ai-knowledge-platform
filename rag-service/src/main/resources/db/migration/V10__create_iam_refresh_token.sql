-- Refresh Token 管理表
CREATE TABLE iam_refresh_token
(
    id                    UUID PRIMARY KEY,
    user_id               UUID         NOT NULL,
    token_hash            VARCHAR(64)  NOT NULL,
    expires_at            TIMESTAMPTZ  NOT NULL,
    revoked_at            TIMESTAMPTZ,
    replaced_by_token_id  UUID,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Token 哈希唯一
CREATE UNIQUE INDEX uk_iam_refresh_token_hash
    ON iam_refresh_token (token_hash);

-- 根据用户查询 Token
CREATE INDEX idx_iam_refresh_token_user_id
    ON iam_refresh_token (user_id);

-- 方便清理过期 Token
CREATE INDEX idx_iam_refresh_token_expires_at
    ON iam_refresh_token (expires_at);