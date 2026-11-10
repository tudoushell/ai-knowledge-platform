package com.elliot.ai.identity.security.principal;

import com.elliot.ai.identity.entity.UserStatus;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class IdentityUserDetails implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;

    private final UUID userId;

    private final String username;

    private String passwordHash;

    private final UserStatus status;

    private final List<GrantedAuthority> authorities;

    public IdentityUserDetails(
            UUID userId,
            String username,
            String passwordHash,
            UserStatus status,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status;
        this.authorities = List.copyOf(authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return UserStatus.ENABLED == status;
    }

    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }
}