package com.elliot.ai.identity.service.impl;

import com.elliot.ai.identity.security.principal.IdentityUserDetails;
import com.elliot.ai.identity.service.AuthenticationService;
import com.elliot.ai.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    @Override
    public Authentication authenticate(String username, String password) {
        // 1. 创建未认证的 Authentication。
        Authentication request =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        username,
                        password
                );
        // 2. 委托 AuthenticationManager 完成认证。
        Authentication authentication =
                authenticationManager.authenticate(request);

        // 3. 认证成功后更新最后登录时间。
        IdentityUserDetails userDetails =
                (IdentityUserDetails) authentication.getPrincipal();
        userService.updateLastLoginAt(
                userDetails.getUserId(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        return authentication;
    }
}
