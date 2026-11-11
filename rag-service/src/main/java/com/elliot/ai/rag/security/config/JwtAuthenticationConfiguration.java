package com.elliot.ai.rag.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration(proxyBeanMethods = false)
public class JwtAuthenticationConfiguration {

    /**
     * 配置已验证 JWT 到 Spring Security 认证对象的转换规则。
     * JWT 的解析、签名和有效期校验由 JwtDecoder 完成，本转换器负责提取身份和权限。
     *
     * <p>项目签发的 JWT Claims 示例（时间为 Unix 秒，具体值仅作示意）：</p>
     * <pre>{@code
     * {
     *   "iss": "ai-knowledge-platform",
     *   "sub": "00000000-0000-0000-0003-000000000001",
     *   "iat": 1800000000,
     *   "exp": 1800000900,
     *   "jti": "00000000-0000-0000-0000-000000000002",
     *   "authorities": ["ROLE_ADMIN", "rag:chat:use"]
     * }
     * }</pre>
     *
     * <p>当前 Spring Security 7.1.1 转换后的主要数据：</p>
     * <pre>{@code
     * Authentication 类型：JwtAuthenticationToken
     * authentication.getName()：sub 中的用户 UUID，而非登录用户名
     * authentication.getPrincipal()：默认的 JwtAuthenticatedPrincipal，而非 IdentityUserDetails
     * jwtAuthentication.getToken().getSubject()：sub 中的用户 UUID
     * authentication.getAuthorities()：ROLE_ADMIN、rag:chat:use、FACTOR_BEARER
     * }</pre>
     *
     * <p>authorities 中的字符串原样转换为权限，不添加 SCOPE_ 前缀；
     * ROLE_ADMIN 可用于 hasRole("ADMIN")，rag:chat:use 可用于 hasAuthority("rag:chat:use")。
     * FACTOR_BEARER 是框架附加的 Bearer 认证因子权限，权限集合不保证顺序。
     * 转换过程不会查询数据库，也不会生成 IdentityUserDetails。</p>
     *
     * @return JWT 身份和权限转换器，供 Resource Server 验证 JWT 后使用
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        // 1. 创建权限转换器。
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        // 2. 指定从 authorities Claim 中读取权限。
        authoritiesConverter.setAuthoritiesClaimName("authorities");

        // 3. 不添加默认的 SCOPE_ 前缀。
        authoritiesConverter.setAuthorityPrefix("");

        // 4. 创建 JWT Authentication 转换器。
        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        // 5. 设置权限转换器。
        converter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        // 6. 使用 sub 作为 Authentication 的 principal name。
        converter.setPrincipalClaimName("sub");

        return converter;
    }
}
