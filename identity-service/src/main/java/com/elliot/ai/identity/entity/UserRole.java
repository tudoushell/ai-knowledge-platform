package com.elliot.ai.identity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 对应 iam_user_role 表，userId 和 roleId 共同构成主键。 */
@Getter
@Setter
@TableName("iam_user_role")
public class UserRole {

    private UUID userId;
    private UUID roleId;
    private OffsetDateTime createdAt;
}
