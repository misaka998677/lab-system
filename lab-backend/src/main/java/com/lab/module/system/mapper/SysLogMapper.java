package com.lab.module.system.mapper;

import com.lab.module.system.entity.SysLog;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SysLogMapper {
    int insert(SysLog l);
    List<SysLog> page(@Param("username") String username,
                      @Param("module") String module,
                      @Param("status") Integer status,
                      @Param("start") LocalDateTime start,
                      @Param("end")   LocalDateTime end);
    List<SysLog> selectAll();
    int clear();
}
