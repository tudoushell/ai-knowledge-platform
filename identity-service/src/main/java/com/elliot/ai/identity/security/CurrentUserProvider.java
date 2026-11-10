package com.elliot.ai.identity.security;


import com.elliot.ai.common.exception.BusinessException;

/**
 * 当前登录用户身份提供接口。
 * <p>
 * 为业务模块提供统一的当前用户身份获取能力，
 * 隔离具体的身份认证实现。
 */
public interface CurrentUserProvider {

    /**
     * 获取当前经过认证的用户 ID。
     *
     * @return 当前用户唯一标识
     * @throws {@link BusinessException} 当前请求不存在有效认证身份时抛出
     */
    String requireUserId();
}