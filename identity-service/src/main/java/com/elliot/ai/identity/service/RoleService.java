package com.elliot.ai.identity.service;

import com.elliot.ai.identity.entity.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 角色查询服务。 */
public interface RoleService {

    /**
     * 根据角色编码查询角色。
     *
     * @param code 角色编码
     * @return 匹配的角色；不存在时返回空 Optional
     */
    Optional<Role> findByCode(String code);

    /**
     * 查询用户拥有的角色，结果按角色编码升序排列。
     *
     * @param userId 用户 ID
     * @return 用户拥有的角色列表；没有角色时返回空列表
     */
    List<Role> findByUserId(UUID userId);
}
