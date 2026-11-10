package com.elliot.ai.identity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 对应 iam_role_permission 表，roleId 和 permissionId 共同构成主键。 */
@Getter
@Setter
@TableName("iam_role_permission")
public class RolePermission {

    private UUID roleId;
    private UUID permissionId;
    private OffsetDateTime createdAt;
}
