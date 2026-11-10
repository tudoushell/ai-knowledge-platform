package com.elliot.ai.identity.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    /**
     * JWT 签发者。
     */
    private String issuer = "ai-knowledge-platform";

    /**
     * Access Token 有效期。
     */
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    /**
     * Refresh Token 有效期。
     */
    private Duration refreshTokenTtl = Duration.ofDays(7);

    /**
     * RSA 密钥 ID，用于 JWT Header 中的 kid。
     */
    private String keyId = "ai-knowledge-platform-rs256-1";

    /**
     * RSA 私钥文件位置。
     */
    private String privateKeyLocation =
            "file:.local/keys/jwt-private.pem";

    /**
     * RSA 公钥文件位置。
     */
    private String publicKeyLocation =
            "file:.local/keys/jwt-public.pem";
}