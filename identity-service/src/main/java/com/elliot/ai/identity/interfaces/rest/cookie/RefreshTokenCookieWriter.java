package com.elliot.ai.identity.interfaces.rest.cookie;

import com.elliot.ai.identity.config.RefreshCookieProperties;
import com.elliot.ai.identity.security.jwt.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 管理刷新令牌 Cookie，原始令牌仅通过 Set-Cookie 交给浏览器。 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookieWriter {

    public static final String COOKIE_NAME = "refresh_token";

    private final RefreshCookieProperties properties;
    private final JwtProperties jwtProperties;

    public void write(HttpServletResponse response, String token) {
        setCookie(response, token, jwtProperties.getRefreshTokenTtl());
    }

    public void clear(HttpServletResponse response) {
        setCookie(response, "", Duration.ZERO);
    }

    private void setCookie(HttpServletResponse response, String value, Duration maxAge) {
        if ("None".equalsIgnoreCase(properties.getSameSite()) && !properties.isSecure()) {
            throw new IllegalStateException("SameSite=None 必须启用 Secure");
        }
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .path(properties.getPath())
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
