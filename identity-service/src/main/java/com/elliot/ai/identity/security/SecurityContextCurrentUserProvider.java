package com.elliot.ai.identity.security;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.identity.security.principal.IdentityUserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public String requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null ||
                (authentication instanceof AnonymousAuthenticationToken) || !(authentication.isAuthenticated())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof IdentityUserDetails userDetails)
                || userDetails.getUserId() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userDetails.getUserId().toString();
    }
}
