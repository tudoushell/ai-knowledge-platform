package com.elliot.ai.identity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 对应 iam_user 表。 */
@Getter
@Setter
@TableName("iam_user")
public class User {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;
    private String username;
    /** 密码哈希，不保存明文密码。 */
    private String passwordHash;
    private String nickname;
    private String email;
    /** 用户状态：ENABLED 或 DISABLED。 */
    private UserStatus status;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
