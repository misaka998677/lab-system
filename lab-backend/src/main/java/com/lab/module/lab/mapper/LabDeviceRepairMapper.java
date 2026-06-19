package com.lab.module.lab.mapper;

import com.lab.module.lab.entity.LabDeviceRepair;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface LabDeviceRepairMapper {
    List<LabDeviceRepair> page(@Param("keyword") String keyword,
                               @Param("status") Integer status,
                               @Param("deviceId") Long deviceId,
                               @Param("reporterId") Long reporterId,
                               @Param("scopeLabIds") List<Long> scopeLabIds);
    LabDeviceRepair findById(@Param("id") Long id);
    int insert(LabDeviceRepair r);
    int update(LabDeviceRepair r);
    int deleteById(@Param("id") Long id);

    List<LabDeviceRepair> findAll(@Param("scopeLabIds") List<Long> scopeLabIds);

    /** 检查指定设备是否有未完成的维修单（status = 0 或 1）。 */
    int countByDeviceIdAndStatus(@Param("deviceId") Long deviceId);
}
