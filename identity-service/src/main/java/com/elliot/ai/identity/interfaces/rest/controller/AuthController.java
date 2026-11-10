package com.elliot.ai.identity.interfaces.rest.controller;

import com.elliot.ai.common.dto.Result;
import com.elliot.ai.identity.interfaces.rest.dto.request.LoginRequest;
import com.elliot.ai.identity.interfaces.rest.advice.InvalidRefreshTokenException;
import com.elliot.ai.identity.interfaces.rest.cookie.RefreshTokenCookieWriter;
import com.elliot.ai.identity.interfaces.rest.dto.response.LoginResponse;
import com.elliot.ai.identity.application.auth.AuthenticationApplicationService;
import com.elliot.ai.identity.application.auth.LoginResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "身份认证", description = "用户登录、令牌刷新和退出登录接口")
public class AuthController {

    private final AuthenticationApplicationService authenticationApplicationService;
    private final RefreshTokenCookieWriter cookieWriter;

    /**
     * 用户登录。
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "校验用户名和密码，返回 Access Token，并通过 Set-Cookie 写入 HttpOnly Refresh Token。"
            + "响应体不包含 Refresh Token；参数校验失败时 Result.code 为 400。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "统一 Result 响应，成功时 data 包含访问令牌信息"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误，或者账号不可用"),
            @ApiResponse(responseCode = "403", description = "缺少或使用了错误的认证请求保护头")
    })
    public Result<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            @Parameter(description = "认证请求保护头，必须为 XMLHttpRequest", required = true,
                    schema = @Schema(allowableValues = {"XMLHttpRequest"}, defaultValue = "XMLHttpRequest"))
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            HttpServletResponse response
    ) {

        requireAjaxRequest(requestedWith);
        LoginResult result = authenticationApplicationService.login(request.username(), request.password());
        cookieWriter.write(response, result.refreshToken());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return Result.buildSuccess(toResponse(result));
    }

    /** 轮换刷新令牌并签发新的访问令牌。 */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "从浏览器自动携带的 refresh_token Cookie 读取刷新令牌，无需请求体。"
            + "轮换成功后返回新 Access Token，并通过 Set-Cookie 替换 Refresh Token；旧刷新令牌失效。"
            + "跨源请求需携带 credentials，且服务端仅放行可信源。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回新的访问令牌，并更新刷新令牌 Cookie"),
            @ApiResponse(responseCode = "401", description = "刷新令牌缺失、无效、过期，或者账号不可用"),
            @ApiResponse(responseCode = "403", description = "缺少或使用了错误的认证请求保护头")
    })
    public Result<LoginResponse> refresh(
            @Parameter(hidden = true)
            @CookieValue(value = RefreshTokenCookieWriter.COOKIE_NAME, required = false) String refreshToken,
            @Parameter(description = "认证请求保护头，必须为 XMLHttpRequest", required = true,
                    schema = @Schema(allowableValues = {"XMLHttpRequest"}, defaultValue = "XMLHttpRequest"))
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            HttpServletResponse response) {
        requireAjaxRequest(requestedWith);
        if (!StringUtils.hasText(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }
        LoginResult result = authenticationApplicationService.refresh(refreshToken);
        cookieWriter.write(response, result.refreshToken());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return Result.buildSuccess(toResponse(result));
    }

    /** 撤销刷新令牌。已签发的访问令牌仍按自身有效期到期。 */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "撤销 refresh_token Cookie 对应的刷新令牌并清除 Cookie，无需请求体。"
            + "Cookie 缺失或令牌已撤销时仍返回成功。已签发的 Access Token 仍按原有效期到期。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "退出成功，返回无数据的 Result 并清除刷新令牌 Cookie"),
            @ApiResponse(responseCode = "403", description = "缺少或使用了错误的认证请求保护头")
    })
    public Result<Void> logout(
            @Parameter(hidden = true)
            @CookieValue(value = RefreshTokenCookieWriter.COOKIE_NAME, required = false) String refreshToken,
            @Parameter(description = "认证请求保护头，必须为 XMLHttpRequest", required = true,
                    schema = @Schema(allowableValues = {"XMLHttpRequest"}, defaultValue = "XMLHttpRequest"))
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            HttpServletResponse response) {
        requireAjaxRequest(requestedWith);
        authenticationApplicationService.logout(refreshToken);
        cookieWriter.clear(response);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return Result.buildSuccess();
    }

    private LoginResponse toResponse(LoginResult result) {
        return new LoginResponse(
                result.accessToken(),
                result.tokenType(),
                result.expiresIn(),
                result.username()
        );
    }

    private void requireAjaxRequest(String requestedWith) {
        // 此非简单请求头不能由跨站 HTML 表单设置；跨域部署仅应放行可信源。
        if (!"XMLHttpRequest".equals(requestedWith)) {
            throw new AccessDeniedException("缺少认证请求保护头");
        }
    }

}
