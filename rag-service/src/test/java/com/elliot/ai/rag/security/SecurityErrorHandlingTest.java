package com.elliot.ai.rag.security;

import com.elliot.ai.rag.security.config.JwtAuthenticationConfiguration;
import com.elliot.ai.rag.security.config.SecurityConfiguration;
import com.elliot.ai.rag.security.handler.RestAccessDeniedHandler;
import com.elliot.ai.rag.security.handler.RestAuthenticationEntryPoint;
import com.elliot.ai.rag.security.handler.SecurityErrorResponseWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 通过真实安全过滤链验证 401、403 及正常访问，无需数据库和外部服务。 */
@SpringJUnitConfig(SecurityErrorHandlingTest.TestConfig.class)
@WebAppConfiguration
class SecurityErrorHandlingTest {
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new DelegatingFilterProxy("springSecurityFilterChain", context)).build();
    }

    @Test
    void missingCredentialsReturns401Result() throws Exception {
        mvc.perform(get("/api/admin/check"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer")))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录或登录已失效，请重新登录"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void invalidJwtReturns401ResultAndBearerErrorHeader() throws Exception {
        mvc.perform(get("/api/admin/check").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("invalid_token")))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void userWithoutAdminRoleReturns403Result() throws Exception {
        mvc.perform(get("/api/admin/check").header("Authorization", "Bearer user"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("WWW-Authenticate", containsString("insufficient_scope")))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("没有操作权限"));
    }

    @Test
    void adminCanStillAccessProtectedEndpoint() throws Exception {
        mvc.perform(get("/api/admin/check").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk()).andExpect(content().string("ok"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfiguration.class, JwtAuthenticationConfiguration.class,
            SecurityErrorResponseWriter.class, RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class, ProbeController.class})
    static class TestConfig {
        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }

        @Bean JwtDecoder jwtDecoder() {
            return value -> {
                if (value.equals("invalid")) throw new BadJwtException("invalid test token");
                return Jwt.withTokenValue(value).header("alg", "RS256").subject("test-user")
                        .claim("authorities", List.of(value.equals("admin") ? "ROLE_ADMIN" : "ROLE_USER"))
                        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(900)).build();
            };
        }
    }

    @RestController
    static class ProbeController {
        @GetMapping("/api/admin/check")
        String check() { return "ok"; }
    }
}
