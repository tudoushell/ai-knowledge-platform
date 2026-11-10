package com.elliot.ai.identity.application.auth.impl;

import com.elliot.ai.identity.application.auth.LoginResult;
import com.elliot.ai.identity.entity.UserStatus;
import com.elliot.ai.identity.security.jwt.JwtProperties;
import com.elliot.ai.identity.security.jwt.JwtTokenProvider;
import com.elliot.ai.identity.security.principal.IdentityUserDetails;
import com.elliot.ai.identity.service.AuthenticationService;
import com.elliot.ai.identity.service.RefreshTokenService;
import com.elliot.ai.identity.service.RefreshTokenService.IssuedRefreshToken;
import com.elliot.ai.identity.security.userdetails.IdentityUserDetailsService;
import com.elliot.ai.identity.interfaces.rest.advice.InvalidRefreshTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationApplicationServiceImplTest {

    private final AuthenticationService authenticationService = mock(AuthenticationService.class);
    private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
    private final JwtProperties properties = new JwtProperties();
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final IdentityUserDetailsService userDetailsService = mock(IdentityUserDetailsService.class);
    private final AuthenticationApplicationServiceImpl service =
            new AuthenticationApplicationServiceImpl(authenticationService, tokenProvider, properties,
                    refreshTokenService, userDetailsService);

    @Test
    void authenticatesBeforeIssuingTokenAndReturnsConfiguredExpiry() {
        var user = new IdentityUserDetails(UUID.randomUUID(), "alice", null, UserStatus.ENABLED, List.of());
        when(authenticationService.authenticate("alice", "password"))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        when(tokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.issue(user.getUserId())).thenReturn(
                new IssuedRefreshToken(user.getUserId(), "refresh-token", OffsetDateTime.now().plusDays(7)));
        properties.setAccessTokenTtl(Duration.ofMinutes(20));

        assertEquals(new LoginResult("access-token", "Bearer", 1200, "refresh-token", "alice"), service.login("alice", "password"));

        var order = inOrder(authenticationService, tokenProvider);
        order.verify(authenticationService).authenticate("alice", "password");
        order.verify(tokenProvider).generateAccessToken(user);
    }

    @Test
    void propagatesAuthenticationFailureWithoutIssuingToken() {
        var failure = new BadCredentialsException("bad credentials");
        when(authenticationService.authenticate("alice", "wrong")).thenThrow(failure);

        assertSame(failure, assertThrows(BadCredentialsException.class, () -> service.login("alice", "wrong")));
        verifyNoInteractions(tokenProvider, refreshTokenService);
    }

    @Test
    void rejectsUnexpectedPrincipalWithoutIssuingToken() {
        when(authenticationService.authenticate("alice", "password"))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated("alice", null, List.of()));

        assertThrows(IllegalStateException.class, () -> service.login("alice", "password"));
        verifyNoInteractions(tokenProvider, refreshTokenService);
    }

    @Test
    void refreshLoadsCurrentIdentityAndReturnsRotatedTokens() {
        var user = new IdentityUserDetails(UUID.randomUUID(), "alice", "hash", UserStatus.ENABLED, List.of());
        when(refreshTokenService.rotate("old-token")).thenReturn(
                new IssuedRefreshToken(user.getUserId(), "new-refresh", OffsetDateTime.now().plusDays(7)));
        when(userDetailsService.loadUserById(user.getUserId())).thenReturn(user);
        when(tokenProvider.generateAccessToken(user)).thenReturn("new-access");

        assertEquals(new LoginResult("new-access", "Bearer", 900, "new-refresh", "alice"), service.refresh("old-token"));
        assertNull(user.getPassword());
        verifyNoInteractions(authenticationService);
    }

    @Test
    void rejectsInvalidRefreshWithoutLoadingUserOrSigningToken() {
        when(refreshTokenService.rotate("invalid")).thenThrow(new InvalidRefreshTokenException());

        assertThrows(InvalidRefreshTokenException.class, () -> service.refresh("invalid"));
        verifyNoInteractions(userDetailsService, tokenProvider);
    }

    @Test
    void rejectsDisabledUserOnRefresh() {
        var user = new IdentityUserDetails(UUID.randomUUID(), "alice", "hash", UserStatus.DISABLED, List.of());
        when(refreshTokenService.rotate("old-token")).thenReturn(
                new IssuedRefreshToken(user.getUserId(), "new-refresh", OffsetDateTime.now().plusDays(7)));
        when(userDetailsService.loadUserById(user.getUserId())).thenReturn(user);

        assertThrows(DisabledException.class, () -> service.refresh("old-token"));
        assertNull(user.getPassword());
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void logoutRevokesRefreshTokenWithoutIssuingTokens() {
        service.logout("refresh-token");

        verify(refreshTokenService).revoke("refresh-token");
        verifyNoInteractions(tokenProvider, authenticationService, userDetailsService);
    }
}
