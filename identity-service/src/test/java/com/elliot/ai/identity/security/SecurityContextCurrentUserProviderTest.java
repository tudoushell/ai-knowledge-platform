package com.elliot.ai.identity.security;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(userId.toString(), null, AuthorityUtils.NO_AUTHORITIES));

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
    void rejectsBlankAuthenticationName() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(" ", null, AuthorityUtils.NO_AUTHORITIES));

        assertUnauthorized();
    }

    @Test
    void rejectsMissingAuthenticationName() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(null, null, AuthorityUtils.NO_AUTHORITIES));

        assertUnauthorized();
    }

    private void assertUnauthorized() {
        BusinessException exception = assertThrows(BusinessException.class, provider::requireUserId);
        assertEquals(ResultCode.UNAUTHORIZED, exception.getResultCode());
    }
}
