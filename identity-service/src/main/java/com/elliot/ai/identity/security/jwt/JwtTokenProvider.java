package com.elliot.ai.identity.security.jwt;

import com.elliot.ai.identity.security.principal.IdentityUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;

    private final JwtProperties jwtProperties;

    /**
     * 生成 Access Token。
     */
    public String generateAccessToken(
            IdentityUserDetails userDetails
    ) {
        Objects.requireNonNull(userDetails, "userDetails");

        if (!userDetails.isEnabled()) {
            throw new IllegalStateException("用户已被禁用，不能签发 Token");
        }

        // 1. 获取签发时间。
        Instant issuedAt = Instant.now();

        // 2. 计算过期时间。
        Duration ttl = jwtProperties.getAccessTokenTtl();

        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalStateException("JWT 有效期配置不合法");
        }

        Instant expiresAt = issuedAt.plus(ttl);

        // 3. 获取用户角色和权限。
        List<String> authorities = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .distinct()
                .sorted()
                .toList();

        // 4. 构建 JWT Claims。
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(userDetails.getUserId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("authorities", authorities)
                .build();

        // 5. 指定 RS256 签名算法和密钥 ID。
        JwsHeader jwsHeader = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .keyId(jwtProperties.getKeyId())
                .build();

        // 6. 使用 JwtEncoder 签发 Token。
        return jwtEncoder.encode(
                JwtEncoderParameters.from(jwsHeader, claims)
        ).getTokenValue();
    }
}