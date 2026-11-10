package com.elliot.ai.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elliot.ai.identity.entity.UserRole;

/** 用户与角色关联的数据访问接口；按联合主键操作时需指定两个字段。 */
public interface UserRoleMapper extends BaseMapper<UserRole> {
}
