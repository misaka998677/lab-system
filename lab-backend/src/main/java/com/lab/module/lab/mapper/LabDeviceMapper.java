package com.lab.module.lab.mapper;

import com.lab.module.lab.entity.LabDevice;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface LabDeviceMapper {
    List<LabDevice> page(@Param("keyword") String keyword,
                         @Param("labId") Long labId,
                         @Param("category") String category,
                         @Param("status") Integer status,
                         @Param("scopeLabIds") List<Long> scopeLabIds);
    List<LabDevice> findByLab(@Param("labId") Long labId);
    LabDevice findById(@Param("id") Long id);
    int insert(LabDevice d);
    int update(LabDevice d);
    int deleteById(@Param("id") Long id);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    List<LabDevice> findAll(@Param("scopeLabIds") List<Long> scopeLabIds);
}
