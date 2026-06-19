package com.lab.module.reserve.service;

import com.lab.module.reserve.mapper.ReservationMapper;
import com.lab.websocket.StatPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationTimeoutService {

    private static final Logger log = LoggerFactory.getLogger(ReservationTimeoutService.class);

    @Autowired private ReservationMapper reservationMapper;
    @Autowired private StatPushService pushService;

    /** 每 5 分钟处理超时预约 */
    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void processTimeouts() {
        LocalDateTime now = LocalDateTime.now();

        // 1) 提交后 24 小时未审核 → 自动拒绝
        List<Long> pendingIds = reservationMapper.findPendingTimeoutIds(now.minusHours(24));
        if (!pendingIds.isEmpty()) {
            for (Long id : pendingIds) {
                reservationMapper.updateStatus(id, 2, null, "系统自动拒绝：超时未审核");
            }
            log.info("[Timeout] 自动拒绝 {} 条超时未审核预约", pendingIds.size());
        }

        // 2) 已通过但超过预约开始时间 30 分钟仍未签到 → 取消
        List<Long> noCheckInIds = reservationMapper.findNoCheckInIds(now.minusMinutes(30));
        if (!noCheckInIds.isEmpty()) {
            for (Long id : noCheckInIds) {
                reservationMapper.updateStatus(id, 2, null, "系统自动取消：超时未签到");
            }
            log.info("[Timeout] 自动取消 {} 条超时未签到预约", noCheckInIds.size());
        }

        // 3) 已签到但超过结束时间 30 分钟仍未签退 → 自动签退
        List<Long> noCheckOutIds = reservationMapper.findNoCheckOutIds(now.minusMinutes(30));
        if (!noCheckOutIds.isEmpty()) {
            for (Long id : noCheckOutIds) {
                reservationMapper.checkOut(id, now);
            }
            log.info("[Timeout] 自动签退 {} 条超时未签退预约", noCheckOutIds.size());
        }

        if (!pendingIds.isEmpty() || !noCheckInIds.isEmpty() || !noCheckOutIds.isEmpty()) {
            pushService.pushOverviewUpdate();
        }
    }
}
