package com.elliot.ai.identity.service;

import com.elliot.ai.identity.entity.User;
import com.elliot.ai.identity.entity.UserStatus;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** 用户管理服务。 */
public interface UserService {

    /**
     * 根据用户 ID 查询用户。
     *
     * @param userId 用户 ID
     * @return 匹配的用户；不存在时返回空 Optional
     */
    Optional<User> findById(UUID userId);

    /**
     * 根据用户名查询用户，忽略大小写及输入两端的空白。
     *
     * @param username 登录用户名
     * @return 匹配的用户；用户名为空或用户不存在时返回空 Optional
     */
    Optional<User> findByUsername(String username);

    /**
     * 判断用户名是否已存在，忽略大小写及输入两端的空白。
     *
     * @param username 登录用户名
     * @return 已存在时返回 true；用户名为空或不存在时返回 false
     */
    boolean existsByUsername(String username);

    /**
     * 创建用户；缺少 ID 或创建、更新时间时自动填充。
     *
     * <p>调用方须事先使用 PasswordEncoder 编码密码并设置 passwordHash。</p>
     *
     * @param user 待创建的用户，不能为 null
     * @return 已创建的用户
     */
    User create(User user);

    /**
     * 更新用户状态及更新时间。
     *
     * @param userId 用户 ID
     * @param status 新的用户状态
     * @return 更新成功时返回 true；用户不存在时返回 false
     */
    boolean updateStatus(UUID userId, UserStatus status);

    /**
     * 更新用户最近登录时间及更新时间。
     *
     * @param userId 用户 ID
     * @param lastLoginAt 最近登录时间
     * @return 更新成功时返回 true；用户不存在时返回 false
     */
    boolean updateLastLoginAt(UUID userId, OffsetDateTime lastLoginAt);
}
