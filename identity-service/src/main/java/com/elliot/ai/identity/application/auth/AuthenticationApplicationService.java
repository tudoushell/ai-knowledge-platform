package com.elliot.ai.identity.application.auth;

/** 登录应用服务，协调身份认证与访问令牌签发。 */
public interface AuthenticationApplicationService {

    /**
     * 使用用户名和密码登录。
     *
     * @param username 登录用户名
     * @param password 用户输入的密码
     * @return 包含访问令牌、令牌类型和有效期的登录结果
     * @throws org.springframework.security.core.AuthenticationException 身份认证失败时抛出
     */
    LoginResult login(String username, String password);

    /** 轮换有效的刷新令牌，并为当前仍可用的用户签发两个新令牌。 */
    LoginResult refresh(String refreshToken);

    /** 撤销指定刷新令牌，重复退出不会报错。 */
    void logout(String refreshToken);
}
