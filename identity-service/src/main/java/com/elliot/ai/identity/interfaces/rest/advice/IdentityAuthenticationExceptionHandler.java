package com.elliot.ai.identity.interfaces.rest.advice;

import com.elliot.ai.common.dto.Result;
import com.elliot.ai.common.enums.ResultCode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 身份模块 REST 认证异常处理，优先于全局系统异常兜底处理。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.elliot.ai.identity.interfaces.rest")
public class IdentityAuthenticationExceptionHandler {

    /** 拒绝缺少跨站请求防护头的认证请求。 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new Result<>(403, "请求被拒绝", null));
    }

    /** 刷新令牌无效时提示重新登录。 */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Result<Void>> handleInvalidRefreshToken(InvalidRefreshTokenException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.buildFail(ResultCode.UNAUTHORIZED, "刷新令牌无效或已过期，请重新登录"));
    }

    /** 将认证失败转换为 HTTP 401 和统一失败响应，不暴露内部认证细节。 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthenticationException(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.buildFail(ResultCode.UNAUTHORIZED, "用户名或密码错误，或者账号不可用"));
    }
}
