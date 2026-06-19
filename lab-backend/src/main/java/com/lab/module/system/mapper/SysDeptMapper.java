package com.lab.module.system.mapper;

import com.lab.module.system.entity.SysDept;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysDeptMapper {
    List<SysDept> all();
    SysDept findById(@Param("id") Long id);
    int insert(SysDept d);
    int update(SysDept d);
    int deleteById(@Param("id") Long id);
}
