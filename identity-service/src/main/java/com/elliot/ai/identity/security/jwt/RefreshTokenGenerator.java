package com.elliot.ai.identity.security.jwt;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class RefreshTokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成 256 bit 随机 Refresh Token。
     */
    public GeneratedToken generate() {

        byte[] randomBytes = new byte[32];

        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        return new GeneratedToken(
                rawToken,
                hash(rawToken)
        );
    }

    /**
     * 计算 SHA-256 哈希。
     */
    public String hash(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm unavailable", e
            );
        }
    }

    public record GeneratedToken(
            String rawToken,
            String tokenHash
    ) {
    }
}