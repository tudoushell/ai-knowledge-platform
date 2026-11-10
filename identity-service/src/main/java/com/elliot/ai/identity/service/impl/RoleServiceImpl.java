package com.elliot.ai.identity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elliot.ai.identity.entity.Role;
import com.elliot.ai.identity.entity.UserRole;
import com.elliot.ai.identity.mapper.RoleMapper;
import com.elliot.ai.identity.mapper.UserRoleMapper;
import com.elliot.ai.identity.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public Optional<Role> findByCode(String code) {
        return Optional.ofNullable(
                roleMapper.selectOne(
                        Wrappers.<Role>lambdaQuery()
                                .eq(Role::getCode, code)
                )
        );
    }

    @Override
    public List<Role> findByUserId(UUID userId) {

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

        return roleMapper.selectList(
                Wrappers.<Role>lambdaQuery()
                        .in(Role::getId, roleIds)
                        .orderByAsc(Role::getCode)
        );
    }
}