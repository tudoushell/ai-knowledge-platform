package com.elliot.ai.identity.service;

import com.elliot.ai.identity.entity.Permission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 权限查询服务。 */
public interface PermissionService {

    /**
     * 根据权限编码查询权限。
     *
     * @param code 权限编码
     * @return 匹配的权限；不存在时返回空 Optional
     */
    Optional<Permission> findByCode(String code);

    /**
     * 查询用户通过其角色获得的权限，结果按权限编码升序排列。
     *
     * @param userId 用户 ID
     * @return 用户拥有的权限列表；没有角色或权限时返回空列表
     */
    List<Permission> findByUserId(UUID userId);
}
