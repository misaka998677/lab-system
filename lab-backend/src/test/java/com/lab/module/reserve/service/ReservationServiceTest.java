package com.lab.module.reserve.service;

import com.lab.common.BizException;
import com.lab.module.reserve.entity.Reservation;
import com.lab.module.reserve.mapper.ReservationMapper;
import com.lab.module.system.entity.SysUser;
import com.lab.security.LoginUser;
import com.lab.websocket.StatPushService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {

    private FakeReservationMapper mapper;
    private ReservationService service;

    @BeforeEach
    void setUp() {
        mapper = new FakeReservationMapper();
        service = new ReservationService(mapper, new NoopStatPushService());
        loginAs(99L, "ROLE_ADMIN", "reserve:audit");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cancelRejectsNonOwnerEvenWhenCurrentUserCanAudit() {
        mapper.save(reservation(1L, 10L, 1));

        BizException ex = assertThrows(BizException.class, () -> service.cancel(1L));

        assertEquals(403, ex.getCode());
        assertEquals("只能操作自己的预约", ex.getMessage());
        assertNull(mapper.lastStatus, "非申请人取消不应更新状态");
    }

    @Test
    void cancelAllowsPendingReservationForOwner() {
        loginAs(10L, "ROLE_STUDENT", "reserve:apply");
        mapper.save(reservation(4L, 10L, 0));

        service.cancel(4L);

        assertEquals(4L, mapper.lastStatusId);
        assertEquals(5, mapper.lastStatus);
        assertEquals(10L, mapper.lastAuditUserId);
        assertEquals("用户取消", mapper.lastAuditNote);
    }

    @Test
    void cancelAllowsApprovedReservationForOwner() {
        loginAs(10L, "ROLE_STUDENT", "reserve:apply");
        mapper.save(reservation(5L, 10L, 1));

        service.cancel(5L);

        assertEquals(5L, mapper.lastStatusId);
        assertEquals(5, mapper.lastStatus);
        assertEquals(10L, mapper.lastAuditUserId);
        assertEquals("用户取消", mapper.lastAuditNote);
    }

    @Test
    void cancelRejectsRejectedReservationEvenForOwner() {
        loginAs(10L, "ROLE_STUDENT", "reserve:apply");
        mapper.save(reservation(6L, 10L, 2));

        BizException ex = assertThrows(BizException.class, () -> service.cancel(6L));

        assertEquals(400, ex.getCode());
        assertEquals("仅待审核/已通过的预约可取消", ex.getMessage());
        assertNull(mapper.lastStatus, "已拒绝预约不应被取消");
    }

    @Test
    void checkInRejectsNonOwnerBeforeStatusValidation() {
        mapper.save(reservation(7L, 10L, 0));

        BizException ex = assertThrows(BizException.class, () -> service.checkIn(7L));

        assertEquals(403, ex.getCode());
        assertEquals("只能操作自己的预约", ex.getMessage());
        assertNull(mapper.lastCheckInId, "非申请人签到不应写入签到时间");
    }

    @Test
    void checkInRejectsNonOwnerEvenWhenReservationApproved() {
        mapper.save(reservation(2L, 10L, 1));

        BizException ex = assertThrows(BizException.class, () -> service.checkIn(2L));

        assertEquals(403, ex.getCode());
        assertEquals("只能操作自己的预约", ex.getMessage());
        assertNull(mapper.lastCheckInId, "非申请人签到不应写入签到时间");
    }

    @Test
    void checkOutRejectsNonOwnerEvenWhenReservationCheckedIn() {
        mapper.save(reservation(3L, 10L, 3));

        BizException ex = assertThrows(BizException.class, () -> service.checkOut(3L));

        assertEquals(403, ex.getCode());
        assertEquals("只能操作自己的预约", ex.getMessage());
        assertNull(mapper.lastCheckOutId, "非申请人签退不应写入签退时间");
    }

    @Test
    void checkInRejectsInvalidStatusForOwner() {
        loginAs(10L, "ROLE_STUDENT", "reserve:apply");
        mapper.save(reservation(8L, 10L, 0));

        BizException ex = assertThrows(BizException.class, () -> service.checkIn(8L));

        assertEquals("仅已通过的预约可签到", ex.getMessage());
        assertNull(mapper.lastCheckInId, "非已通过预约不应写入签到时间");
    }

    @Test
    void checkOutRejectsNonOwnerBeforeStatusValidation() {
        mapper.save(reservation(9L, 10L, 1));

        BizException ex = assertThrows(BizException.class, () -> service.checkOut(9L));

        assertEquals(403, ex.getCode());
        assertEquals("只能操作自己的预约", ex.getMessage());
        assertNull(mapper.lastCheckOutId, "非申请人签退不应写入签退时间");
    }

    @Test
    void checkOutRejectsInvalidStatusForOwner() {
        loginAs(10L, "ROLE_STUDENT", "reserve:apply");
        mapper.save(reservation(10L, 10L, 1));

        BizException ex = assertThrows(BizException.class, () -> service.checkOut(10L));

        assertEquals("仅已签到的预约可签退", ex.getMessage());
        assertNull(mapper.lastCheckOutId, "非已签到预约不应写入签退时间");
    }

    private static Reservation reservation(Long id, Long userId, Integer status) {
        Reservation r = new Reservation();
        r.setId(id);
        r.setUserId(userId);
        r.setLabId(1L);
        r.setStatus(status);
        r.setStartTime(LocalDateTime.now().plusHours(1));
        r.setEndTime(LocalDateTime.now().plusHours(2));
        return r;
    }

    private static void loginAs(Long userId, String role, String perm) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername("u" + userId);
        user.setPassword("pwd");
        user.setStatus(1);
        LoginUser loginUser = new LoginUser(user, Set.of(role), Set.of(perm));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    static class FakeReservationMapper implements ReservationMapper {
        final Map<Long, Reservation> store = new HashMap<>();
        Long lastStatusId;
        Integer lastStatus;
        Long lastAuditUserId;
        String lastAuditNote;
        Long lastCheckInId;
        LocalDateTime lastCheckInTime;
        Long lastCheckOutId;
        LocalDateTime lastCheckOutTime;

        void save(Reservation r) {
            store.put(r.getId(), r);
        }

        @Override public List<Reservation> page(String keyword, Long userId, Long labId, Integer status, LocalDateTime start, LocalDateTime end) {
            return new ArrayList<>(store.values());
        }

        @Override public List<Reservation> checkRecords(Long userId) {
            return new ArrayList<>(store.values());
        }

        @Override public Reservation findById(Long id) {
            return store.get(id);
        }

        @Override public int countConflict(Long labId, LocalDateTime start, LocalDateTime end, Long excludeId) {
            return 0;
        }

        @Override public int insert(Reservation r) {
            store.put(r.getId(), r);
            return 1;
        }

        @Override public int update(Reservation r) {
            store.put(r.getId(), r);
            return 1;
        }

        @Override public int deleteById(Long id) {
            return store.remove(id) == null ? 0 : 1;
        }

        @Override public int updateStatus(Long id, Integer status, Long auditUserId, String auditNote) {
            lastStatusId = id;
            lastStatus = status;
            lastAuditUserId = auditUserId;
            lastAuditNote = auditNote;
            Reservation r = store.get(id);
            if (r != null) r.setStatus(status);
            return 1;
        }

        @Override public int checkIn(Long id, LocalDateTime time) {
            lastCheckInId = id;
            lastCheckInTime = time;
            Reservation r = store.get(id);
            if (r != null) r.setCheckInTime(time);
            return 1;
        }

        @Override public int checkOut(Long id, LocalDateTime time) {
            lastCheckOutId = id;
            lastCheckOutTime = time;
            Reservation r = store.get(id);
            if (r != null) r.setCheckOutTime(time);
            return 1;
        }
    }

    static class NoopStatPushService extends StatPushService {
        @Override public void pushOverviewUpdate() {}
        @Override public void pushModuleUpdate(String module) {}
    }
}
