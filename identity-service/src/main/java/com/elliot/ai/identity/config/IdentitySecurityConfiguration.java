package com.elliot.ai.identity.config;

import com.elliot.ai.identity.security.jwt.JwtProperties;
import com.elliot.ai.identity.security.userdetails.IdentityUserDetailsService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = {"com.elliot.ai.identity.security",
        "com.elliot.ai.identity.service",
        "com.elliot.ai.identity.application",
        "com.elliot.ai.identity.interfaces.rest"}
)
@MapperScan("com.elliot.ai.identity.mapper")
@EnableConfigurationProperties({JwtProperties.class, RefreshCookieProperties.class})
public class IdentitySecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            IdentityUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            DaoAuthenticationProvider provider
    ) {
        return new ProviderManager(provider);
    }
}
