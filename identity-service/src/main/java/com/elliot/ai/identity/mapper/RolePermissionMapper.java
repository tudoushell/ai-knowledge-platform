package com.elliot.ai.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elliot.ai.identity.entity.RolePermission;

/** 角色与权限关联的数据访问接口；按联合主键操作时需指定两个字段。 */
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
