package com.elliot.ai.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.refresh-cookie")
public class RefreshCookieProperties {

    /**
     * 签发和删除 Cookie 必须使用相同路径。
     */
    private String path = "/api/auth";

    /**
     * HTTPS 环境保持 true，本地 HTTP 调试可通过配置关闭。
     */
    private boolean secure = true;

    /**
     * Strict
     * 跨站请求不携带 Cookie
     * Lax
     * 大多数跨站请求不携带；通过普通链接跳转等顶层 GET 导航通常可以携带
     * None
     * 允许跨站携带，但必须同时设置 Secure
     */
    private String sameSite = "Lax";
}
