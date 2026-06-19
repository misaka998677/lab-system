package com.lab.module.reserve.mapper;

import com.lab.module.reserve.entity.Reservation;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationMapper {
    List<Reservation> page(@Param("keyword") String keyword,
                           @Param("userId")  Long userId,
                           @Param("labId")   Long labId,
                           @Param("status")  Integer status,
                           @Param("start")   LocalDateTime start,
                           @Param("end")     LocalDateTime end,
                           @Param("scopeLabIds") List<Long> scopeLabIds);

    List<Reservation> checkRecords(@Param("userId") Long userId,
                                   @Param("scopeLabIds") List<Long> scopeLabIds);

    Reservation findById(@Param("id") Long id);

    /** 时段冲突 */
    int countConflict(@Param("labId") Long labId,
                      @Param("start") LocalDateTime start,
                      @Param("end")   LocalDateTime end,
                      @Param("excludeId") Long excludeId);

    int insert(Reservation r);
    int update(Reservation r);
    int deleteById(@Param("id") Long id);
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("auditUserId") Long auditUserId,
                     @Param("auditNote")   String auditNote);
    int checkIn (@Param("id") Long id, @Param("time") LocalDateTime time);
    int checkOut(@Param("id") Long id, @Param("time") LocalDateTime time);

    List<Long> findPendingTimeoutIds(@Param("cutoff") LocalDateTime cutoff);
    List<Long> findNoCheckInIds(@Param("cutoff") LocalDateTime cutoff);
    List<Long> findNoCheckOutIds(@Param("cutoff") LocalDateTime cutoff);
}
