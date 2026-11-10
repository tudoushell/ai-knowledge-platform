package com.elliot.ai.identity.security;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.identity.entity.UserStatus;
import com.elliot.ai.identity.security.principal.IdentityUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import java.util.UUID;

class SecurityContextCurrentUserProviderTest {

    private final SecurityContextCurrentUserProvider provider = new SecurityContextCurrentUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsUserIdForAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        var user = new IdentityUserDetails(userId, "alice", null, UserStatus.ENABLED, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));

        assertEquals(userId.toString(), provider.requireUserId());
    }

    @Test
    void rejectsMissingAuthentication() {
        assertUnauthorized();
    }

    @Test
    void rejectsAnonymousAuthenticationEvenWhenAuthenticated() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertUnauthorized();
    }

    @Test
    void rejectsUnauthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("user-123", null));

        assertUnauthorized();
    }

    @Test
    void rejectsUnexpectedPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("alice", null, AuthorityUtils.NO_AUTHORITIES));

        assertUnauthorized();
    }

    @Test
    void rejectsIdentityWithoutUserId() {
        var user = new IdentityUserDetails(null, "alice", null, UserStatus.ENABLED, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));

        assertUnauthorized();
    }

    private void assertUnauthorized() {
        BusinessException exception = assertThrows(BusinessException.class, provider::requireUserId);
        assertEquals(ResultCode.UNAUTHORIZED, exception.getResultCode());
    }
}
