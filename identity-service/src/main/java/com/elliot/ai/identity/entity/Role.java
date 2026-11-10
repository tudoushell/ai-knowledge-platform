package com.elliot.ai.identity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 对应 iam_role 表。 */
@Getter
@Setter
@TableName("iam_role")
public class Role {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;
    private String code;
    private String name;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
