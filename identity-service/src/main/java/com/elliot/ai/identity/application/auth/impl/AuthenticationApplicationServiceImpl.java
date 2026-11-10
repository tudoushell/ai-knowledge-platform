package com.elliot.ai.identity.application.auth.impl;

import com.elliot.ai.identity.application.auth.AuthenticationApplicationService;
import com.elliot.ai.identity.application.auth.LoginResult;
import com.elliot.ai.identity.security.jwt.JwtProperties;
import com.elliot.ai.identity.security.jwt.JwtTokenProvider;
import com.elliot.ai.identity.security.principal.IdentityUserDetails;
import com.elliot.ai.identity.service.AuthenticationService;
import com.elliot.ai.identity.service.RefreshTokenService;
import com.elliot.ai.identity.service.RefreshTokenService.IssuedRefreshToken;
import com.elliot.ai.identity.security.userdetails.IdentityUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 登录流程的应用层实现。 */
@Service
@RequiredArgsConstructor
public class AuthenticationApplicationServiceImpl implements AuthenticationApplicationService {

    private final AuthenticationService authenticationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final IdentityUserDetailsService userDetailsService;

    @Override
    @Transactional
    public LoginResult login(String username, String password) {
        Authentication authentication = authenticationService.authenticate(username, password);
        if (!(authentication.getPrincipal() instanceof IdentityUserDetails userDetails)) {
            throw new IllegalStateException("Unexpected authentication principal");
        }

        IssuedRefreshToken refreshToken = refreshTokenService.issue(userDetails.getUserId());
        return tokenResult(userDetails, refreshToken);
    }

    @Override
    @Transactional
    public LoginResult refresh(String refreshToken) {
        // 轮换与签发处于同一事务；账户检查或 JWT 签发失败时回滚轮换。
        IssuedRefreshToken issued = refreshTokenService.rotate(refreshToken);
        IdentityUserDetails userDetails = userDetailsService.loadUserById(issued.userId());
        try {
            new AccountStatusUserDetailsChecker().check(userDetails);
            return tokenResult(userDetails, issued);
        } finally {
            userDetails.eraseCredentials();
        }
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private LoginResult tokenResult(IdentityUserDetails userDetails, IssuedRefreshToken refreshToken) {
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        return new LoginResult(accessToken, "Bearer", jwtProperties.getAccessTokenTtl().toSeconds(),
                refreshToken.rawToken(), userDetails.getUsername());
    }
}
