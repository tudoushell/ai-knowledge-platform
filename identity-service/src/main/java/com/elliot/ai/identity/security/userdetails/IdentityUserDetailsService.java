package com.elliot.ai.identity.security.userdetails;

import com.elliot.ai.identity.entity.Permission;
import com.elliot.ai.identity.entity.Role;
import com.elliot.ai.identity.entity.User;
import com.elliot.ai.identity.mapper.UserMapper;
import com.elliot.ai.identity.service.PermissionService;
import com.elliot.ai.identity.service.RoleService;
import com.elliot.ai.identity.service.UserService;
import com.elliot.ai.identity.security.principal.IdentityUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityUserDetailsService implements UserDetailsService {

    private final UserService userService;

    private final RoleService roleService;

    private final PermissionService permissionService;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // 1. 根据用户名查询用户。
        User user = userService.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("用户不存在")
                );

        return buildUserDetails(user);
    }

    /** 根据稳定的用户 ID 重新加载账户状态、角色和权限，用于令牌刷新。 */
    public IdentityUserDetails loadUserById(UUID userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        return buildUserDetails(user);
    }

    private IdentityUserDetails buildUserDetails(User user) {
        // 2. 查询用户角色。
        List<Role> roles = roleService.findByUserId(user.getId());

        // 3. 查询用户权限。
        List<Permission> permissions =
                permissionService.findByUserId(user.getId());

        // 4. 将角色和权限转换为 Spring Security Authority。
        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();

        roles.forEach(role ->
                authorities.add(
                        new SimpleGrantedAuthority(role.getCode())
                )
        );

        permissions.forEach(permission ->
                authorities.add(
                        new SimpleGrantedAuthority(permission.getCode())
                )
        );

        // 5. 构建 Spring Security 用户身份对象。
        return new IdentityUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getStatus(),
                List.copyOf(authorities)
        );
    }
}
