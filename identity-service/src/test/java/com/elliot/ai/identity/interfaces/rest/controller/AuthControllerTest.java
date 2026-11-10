package com.elliot.ai.identity.interfaces.rest.controller;

import com.elliot.ai.common.exception.GlobalExceptionHandler;
import com.elliot.ai.identity.application.auth.AuthenticationApplicationService;
import com.elliot.ai.identity.application.auth.LoginResult;
import com.elliot.ai.identity.interfaces.rest.advice.IdentityAuthenticationExceptionHandler;
import com.elliot.ai.identity.interfaces.rest.advice.InvalidRefreshTokenException;
import com.elliot.ai.identity.config.RefreshCookieProperties;
import com.elliot.ai.identity.security.jwt.JwtProperties;
import com.elliot.ai.identity.interfaces.rest.cookie.RefreshTokenCookieWriter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private final AuthenticationApplicationService service = mock(AuthenticationApplicationService.class);
    private final RefreshCookieProperties cookieProperties = new RefreshCookieProperties();
    private final JwtProperties jwtProperties = new JwtProperties();
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(service,
                    new RefreshTokenCookieWriter(cookieProperties, jwtProperties)))
            .setControllerAdvice(new GlobalExceptionHandler(), new IdentityAuthenticationExceptionHandler()).build();

    @Test
    void wrapsLoginResponseInResult() throws Exception {
        when(service.login("alice", "password")).thenReturn(new LoginResult("access-token", "Bearer", 900, "refresh-token", "alice"));

        mvc.perform(authPost("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("成功"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Set-Cookie", allOf(containsString("refresh_token=refresh-token"),
                        containsString("HttpOnly"), containsString("Secure"), containsString("SameSite=Lax"),
                        containsString("Path=/api/auth"), containsString("Max-Age=604800"))));
    }

    @Test
    void preservesUnauthorizedResponseForAuthenticationFailure() throws Exception {
        when(service.login("alice", "wrong")).thenThrow(new BadCredentialsException("internal detail"));

        mvc.perform(authPost("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误，或者账号不可用"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void returnsResultForInvalidRequestWithoutCallingLogin() throws Exception {
        mvc.perform(authPost("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":"password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名不能为空"))
                .andExpect(jsonPath("$.data").isEmpty());

        verifyNoInteractions(service);
    }

    @Test
    void refreshReadsCookieAndReplacesItWithoutExposingTokenInJson() throws Exception {
        when(service.refresh("old-token")).thenReturn(new LoginResult("new-access", "Bearer", 900, "new-refresh", "alice"));

        mvc.perform(authPost("/api/auth/refresh").cookie(new Cookie("refresh_token", "old-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=new-refresh")));
    }

    @Test
    void refreshReturnsUnauthorizedForInvalidToken() throws Exception {
        when(service.refresh("invalid")).thenThrow(new InvalidRefreshTokenException());

        mvc.perform(authPost("/api/auth/refresh").cookie(new Cookie("refresh_token", "invalid")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("刷新令牌无效或已过期，请重新登录"));
    }

    @Test
    void logoutReturnsSuccessForRepeatedRequests() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(authPost("/api/auth/logout").cookie(new Cookie("refresh_token", "refresh-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(header().string("Set-Cookie", allOf(containsString("Max-Age=0"),
                            containsString("Path=/api/auth"), containsString("HttpOnly"), containsString("Secure"))));
        }
        verify(service, times(2)).logout("refresh-token");
    }

    @Test
    void refreshRejectsMissingCookieEvenWhenTokenIsInJson() throws Exception {
        mvc.perform(authPost("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"body-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        verifyNoInteractions(service);
    }

    @Test
    void logoutWithoutCookieStillClearsCookie() throws Exception {
        mvc.perform(authPost("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
        verify(service).logout(null);
    }

    @Test
    void rejectsRequestsWithoutProtectionHeader() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password\"}"))
                .andExpect(status().isForbidden());
        for (String path : new String[]{"/api/auth/refresh", "/api/auth/logout"}) {
            mvc.perform(post(path).cookie(new Cookie("refresh_token", "token")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403));
        }
        verifyNoInteractions(service);
    }

    @Test
    void cookieAttributesRespectConfiguration() throws Exception {
        cookieProperties.setSecure(false);
        cookieProperties.setPath("/api/auth/");
        cookieProperties.setSameSite("Strict");
        jwtProperties.setRefreshTokenTtl(java.time.Duration.ofDays(1));
        when(service.login("alice", "password")).thenReturn(new LoginResult("access", "Bearer", 900, "refresh", "alice"));

        mvc.perform(authPost("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password\"}"))
                .andExpect(header().string("Set-Cookie", allOf(containsString("SameSite=Strict"),
                        containsString("Path=/api/auth/"), containsString("Max-Age=86400"),
                        not(containsString("Secure")))));
    }

    private MockHttpServletRequestBuilder authPost(String path) {
        return post(path).header("X-Requested-With", "XMLHttpRequest");
    }
}
