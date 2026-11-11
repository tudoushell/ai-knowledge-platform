package com.elliot.ai.rag.security.config;

import com.elliot.ai.rag.security.handler.RestAccessDeniedHandler;
import com.elliot.ai.rag.security.handler.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) throws Exception {

        http
                // 1. 暂时关闭 CSRF 校验
                .csrf(AbstractHttpConfigurer::disable)

                // 过滤链中的认证与授权异常统一返回 Result。
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // 2. 接口访问控制
                .authorizeHttpRequests(auth -> auth

                        // 登录、刷新、退出允许匿名访问
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout"
                        ).permitAll()

                        // Swagger 文档
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 管理员接口
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // 其他接口必须认证
                        .anyRequest().authenticated()
                )

                // 3. 无状态认证
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // 4. 禁用表单登录
                .formLogin(AbstractHttpConfigurer::disable)

                // 5. 禁用 HTTP Basic
                .httpBasic(AbstractHttpConfigurer::disable)

                // 6. 禁用请求缓存
                .requestCache(AbstractHttpConfigurer::disable)

                // 7. JWT Resource Server
                .oauth2ResourceServer(oauth2 -> oauth2
                        //未携带token
                        .authenticationEntryPoint(authenticationEntryPoint)
                        //携带token,没有权限访问
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                );

        return http.build();
    }
}
