package com.elliot.ai.identity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 对应 iam_refresh_token 表，保存刷新令牌哈希及其生命周期信息。 */
@Getter
@Setter
@TableName("iam_refresh_token")
public class RefreshToken {

    /** 刷新令牌记录 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    /** 所属用户 ID。 */
    private UUID userId;

    /** 刷新令牌哈希，不保存原始令牌。 */
    private String tokenHash;

    /** 过期时间。 */
    private OffsetDateTime expiresAt;

    /** 撤销时间，未撤销时为空。 */
    private OffsetDateTime revokedAt;

    /** 轮换后替代此令牌的新记录 ID，未被替代时为空。 */
    private UUID replacedByTokenId;

    /** 创建时间。 */
    private OffsetDateTime createdAt;
}
