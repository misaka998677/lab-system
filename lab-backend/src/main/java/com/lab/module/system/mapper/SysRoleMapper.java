package com.lab.module.system.mapper;

import com.lab.module.system.entity.SysRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysRoleMapper {
    List<SysRole> page(@Param("keyword") String keyword);
    List<SysRole> all();
    SysRole findById(@Param("id") Long id);
    Long findIdByRoleCode(@Param("roleCode") String roleCode);
    int insert(SysRole r);
    int update(SysRole r);
    int deleteById(@Param("id") Long id);

    List<Long> findMenuIds(@Param("roleId") Long roleId);
    int deleteRoleMenus(@Param("roleId") Long roleId);
    int insertRoleMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);
}
