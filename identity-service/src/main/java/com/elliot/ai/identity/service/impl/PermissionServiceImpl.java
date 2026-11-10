package com.elliot.ai.identity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elliot.ai.identity.entity.Permission;
import com.elliot.ai.identity.entity.RolePermission;
import com.elliot.ai.identity.entity.UserRole;
import com.elliot.ai.identity.mapper.PermissionMapper;
import com.elliot.ai.identity.mapper.RolePermissionMapper;
import com.elliot.ai.identity.mapper.UserRoleMapper;
import com.elliot.ai.identity.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public Optional<Permission> findByCode(String code) {
        return Optional.ofNullable(
                permissionMapper.selectOne(
                        Wrappers.<Permission>lambdaQuery()
                                .eq(Permission::getCode, code)
                )
        );
    }

    @Override
    public List<Permission> findByUserId(UUID userId) {

        // 1. 查询用户拥有的角色 ID。
        List<UUID> roleIds = userRoleMapper.selectList(
                        Wrappers.<UserRole>lambdaQuery()
                                .select(UserRole::getRoleId)
                                .eq(UserRole::getUserId, userId)
                )
                .stream()
                .map(UserRole::getRoleId)
                .distinct()
                .toList();

        if (roleIds.isEmpty()) {
            return List.of();
        }

        // 2. 查询这些角色对应的权限 ID。
        List<UUID> permissionIds = rolePermissionMapper.selectList(
                        Wrappers.<RolePermission>lambdaQuery()
                                .select(RolePermission::getPermissionId)
                                .in(RolePermission::getRoleId, roleIds)
                )
                .stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .toList();

        if (permissionIds.isEmpty()) {
            return List.of();
        }

        // 3. 查询最终权限列表。
        return permissionMapper.selectList(
                Wrappers.<Permission>lambdaQuery()
                        .in(Permission::getId, permissionIds)
                        .orderByAsc(Permission::getCode)
        );
    }
}