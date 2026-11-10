package com.elliot.ai.identity.application.auth;

/**
 * 登录结果，不依赖 REST 层的响应类型。
 *
 * @param accessToken 访问令牌
 * @param tokenType 令牌类型
 * @param expiresIn 令牌有效期，单位为秒
 * @param refreshToken 原始刷新令牌
 * @param username 当前用户的登录用户名
 */
public record LoginResult(String accessToken, String tokenType, long expiresIn, String refreshToken, String username) {
}
