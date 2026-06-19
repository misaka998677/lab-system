package com.lab.module.system.mapper;

import com.lab.module.system.entity.SysMenu;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysMenuMapper {
    List<SysMenu> all();
    List<SysMenu> findByUserId(@Param("userId") Long userId);
    SysMenu findById(@Param("id") Long id);
    int insert(SysMenu m);
    int update(SysMenu m);
    int deleteById(@Param("id") Long id);
}
