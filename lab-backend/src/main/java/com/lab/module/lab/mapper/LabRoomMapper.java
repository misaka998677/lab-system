package com.lab.module.lab.mapper;

import com.lab.module.lab.entity.LabRoom;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface LabRoomMapper {
    List<LabRoom> page(@Param("keyword") String keyword,
                       @Param("deptId") Long deptId,
                       @Param("status") Integer status,
                       @Param("scopeLabIds") List<Long> scopeLabIds);
    List<LabRoom> all(@Param("scopeLabIds") List<Long> scopeLabIds);
    LabRoom findById(@Param("id") Long id);
    int insert(LabRoom r);
    int update(LabRoom r);
    int deleteById(@Param("id") Long id);
}
