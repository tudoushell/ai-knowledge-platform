package com.elliot.ai.identity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elliot.ai.identity.entity.User;
import com.elliot.ai.identity.entity.UserStatus;
import com.elliot.ai.identity.mapper.UserMapper;
import com.elliot.ai.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public Optional<User> findById(UUID userId) {
        return Optional.ofNullable(userMapper.selectById(userId));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                userMapper.selectOne(
                        Wrappers.<User>lambdaQuery()
                                .apply(
                                        "LOWER(username) = LOWER({0})",
                                        username.trim()
                                )
                )
        );
    }

    @Override
    public boolean existsByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }

        Long count = userMapper.selectCount(
                Wrappers.<User>lambdaQuery()
                        .apply(
                                "LOWER(username) = LOWER({0})",
                                username.trim()
                        )
        );

        return count != null && count > 0;
    }

    @Override
    @Transactional
    public User create(User user) {
        Objects.requireNonNull(user, "user");

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }

        if (user.getCreatedAt() == null) {
            user.setCreatedAt(now);
        }

        if (user.getUpdatedAt() == null) {
            user.setUpdatedAt(now);
        }

        // passwordHash 必须是经过 PasswordEncoder 编码的密码。
        userMapper.insert(user);

        return user;
    }

    @Override
    @Transactional
    public boolean updateStatus(UUID userId, UserStatus status) {
        return userMapper.update(
                null,
                Wrappers.<User>lambdaUpdate()
                        .eq(User::getId, userId)
                        .set(User::getStatus, status)
                        .set(User::getUpdatedAt, OffsetDateTime.now(ZoneOffset.UTC))
        ) > 0;
    }

    @Override
    @Transactional
    public boolean updateLastLoginAt(
            UUID userId,
            OffsetDateTime lastLoginAt
    ) {
        return userMapper.update(
                null,
                Wrappers.<User>lambdaUpdate()
                        .eq(User::getId, userId)
                        .set(User::getLastLoginAt, lastLoginAt)
                        .set(User::getUpdatedAt, OffsetDateTime.now(ZoneOffset.UTC))
        ) > 0;
    }
}