package com.elliot.ai.identity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elliot.ai.identity.entity.RefreshToken;
import com.elliot.ai.identity.interfaces.rest.advice.InvalidRefreshTokenException;
import com.elliot.ai.identity.mapper.RefreshTokenMapper;
import com.elliot.ai.identity.security.jwt.JwtProperties;
import com.elliot.ai.identity.security.jwt.RefreshTokenGenerator;
import com.elliot.ai.identity.security.jwt.RefreshTokenGenerator.GeneratedToken;
import com.elliot.ai.identity.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenMapper refreshTokenMapper;

    private final RefreshTokenGenerator tokenGenerator;

    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public IssuedRefreshToken issue(UUID userId) {

        GeneratedToken generated = tokenGenerator.generate();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        RefreshToken token = new RefreshToken();

        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setTokenHash(generated.tokenHash());
        token.setExpiresAt(
                now.plus(jwtProperties.getRefreshTokenTtl())
        );
        token.setCreatedAt(now);

        refreshTokenMapper.insert(token);

        return new IssuedRefreshToken(
                userId,
                generated.rawToken(),
                token.getExpiresAt()
        );
    }

    @Override
    @Transactional
    public IssuedRefreshToken rotate(String rawToken) {

        if (!StringUtils.hasText(rawToken)) {
            throw new InvalidRefreshTokenException();
        }

        String tokenHash = tokenGenerator.hash(rawToken);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // 1. 查询原 Refresh Token。
        RefreshToken oldToken = refreshTokenMapper.selectOne(
                Wrappers.<RefreshToken>lambdaQuery()
                        .eq(RefreshToken::getTokenHash, tokenHash)
        );

        if (oldToken == null
                || oldToken.getRevokedAt() != null
                || !oldToken.getExpiresAt().isAfter(now)) {
            throw new InvalidRefreshTokenException();
        }

        // 2. 生成新的 Refresh Token。
        GeneratedToken generated = tokenGenerator.generate();

        UUID newTokenId = UUID.randomUUID();

        OffsetDateTime newExpiresAt =
                now.plus(jwtProperties.getRefreshTokenTtl());

        // 3. 原子撤销旧 Token。
        // 即使两个请求同时刷新，也只能有一个成功。
        int updated = refreshTokenMapper.update(
                null,
                Wrappers.<RefreshToken>lambdaUpdate()
                        .eq(RefreshToken::getId, oldToken.getId())
                        .isNull(RefreshToken::getRevokedAt)
                        .gt(RefreshToken::getExpiresAt, now)
                        .set(RefreshToken::getRevokedAt, now)
                        .set(
                                RefreshToken::getReplacedByTokenId,
                                newTokenId
                        )
        );

        if (updated != 1) {
            throw new InvalidRefreshTokenException();
        }

        // 4. 保存新的 Token。
        RefreshToken newToken = new RefreshToken();

        newToken.setId(newTokenId);
        newToken.setUserId(oldToken.getUserId());
        newToken.setTokenHash(generated.tokenHash());
        newToken.setExpiresAt(newExpiresAt);
        newToken.setCreatedAt(now);

        refreshTokenMapper.insert(newToken);

        // 5. 只将原始 Token 返回给调用者。
        return new IssuedRefreshToken(
                oldToken.getUserId(),
                generated.rawToken(),
                newExpiresAt
        );
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {

        if (!StringUtils.hasText(rawToken)) {
            return;
        }

        String tokenHash = tokenGenerator.hash(rawToken);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        refreshTokenMapper.update(
                null,
                Wrappers.<RefreshToken>lambdaUpdate()
                        .eq(RefreshToken::getTokenHash, tokenHash)
                        .isNull(RefreshToken::getRevokedAt)
                        .set(RefreshToken::getRevokedAt, now)
        );
    }
}