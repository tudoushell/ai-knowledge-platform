package com.elliot.ai.identity.interfaces.rest.advice;

import org.springframework.security.core.AuthenticationException;

public class InvalidRefreshTokenException
        extends AuthenticationException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}