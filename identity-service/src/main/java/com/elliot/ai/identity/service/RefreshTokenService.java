package com.elliot.ai.identity.service;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 刷新令牌管理服务，负责签发、轮换和撤销；数据库仅保存令牌哈希。 */
public interface RefreshTokenService {

    /**
     * 为指定用户签发刷新令牌，有效期由 JWT 配置决定。
     *
     * @param userId 令牌所属用户 ID
     * @return 包含原始令牌及过期时间的签发结果
     */
    IssuedRefreshToken issue(UUID userId);

    /**
     * 使用有效的刷新令牌换取新令牌，并在同一事务中撤销旧令牌。
     *
     * <p>旧令牌只允许成功轮换一次；多个请求并发轮换同一令牌时，最多一个成功。
     * 新令牌的有效期从本次轮换时间重新计算。</p>
     *
     * @param rawToken 客户端提交的原始刷新令牌
     * @return 属于同一用户的新令牌及过期时间
     * @throws com.elliot.ai.identity.interfaces.rest.advice.InvalidRefreshTokenException
     *         令牌为空、不存在、已撤销、已过期或并发轮换未成功时抛出
     */
    IssuedRefreshToken rotate(String rawToken);

    /**
     * 撤销指定刷新令牌；重复调用不会重复修改撤销时间。
     *
     * @param rawToken 原始刷新令牌；为空、不存在或已撤销时不执行更新
     */
    void revoke(String rawToken);

    /**
     * 刷新令牌签发结果。
     *
     * @param userId 令牌所属用户 ID
     * @param rawToken 返回给客户端的原始令牌，不应记录到日志或直接存入数据库
     * @param expiresAt 令牌过期时间
     */
    record IssuedRefreshToken(
            UUID userId,
            String rawToken,
            OffsetDateTime expiresAt
    ) {
    }
}
