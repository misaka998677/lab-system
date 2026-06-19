package com.lab.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.config.StatCacheInvalidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WebSocket 推送服务。推送前会先清除对应缓存，以保证前端重新拉取时得到最新数据。
 *
 * 消息协议（统一 JSON 格式）：
 *   { "type": "refresh-overview", "ts": 1234567890 }
 *   { "type": "refresh-usage",    "ts": 1234567890 }
 *
 * 前端根据 type 决定拉取哪部分数据。
 */
@Service
public class StatPushService {

    private static final Logger log = LoggerFactory.getLogger(StatPushService.class);

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    @Lazy
    private StatCacheInvalidator cacheInvalidator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 广播「仪表盘概览有新数据，请刷新」信号。推送前先清除概览缓存。 */
    public void pushOverviewUpdate() {
        log.info("[STAT-PUSH] 推送 refresh-overview 信号 -> 清除缓存并广播");
        cacheInvalidator.evictOverview();
        broadcast("refresh-overview");
    }

    /** 广播「实验室使用率/统计数据有新数据，请刷新」信号。推送前先清除使用率缓存。 */
    public void pushUsageUpdate() {
        log.info("[STAT-PUSH] 推送 refresh-usage 信号 -> 清除缓存并广播");
        cacheInvalidator.evictUsage();
        broadcast("refresh-usage");
    }

    /** 广播「设备故障/维修数据有新数据，请刷新」信号。推送前先清除故障统计缓存。 */
    public void pushFaultUpdate() {
        log.info("[STAT-PUSH] 推送 refresh-fault 信号 -> 清除缓存并广播");
        cacheInvalidator.evictFault();
        broadcast("refresh-fault");
    }

    /** 广播「库存预警数据有新数据，请刷新」信号。推送前先清除库存缓存。 */
    public void pushStockUpdate() {
        log.info("[STAT-PUSH] 推送 refresh-stock 信号 -> 清除缓存并广播");
        cacheInvalidator.evictStock();
        broadcast("refresh-stock");
    }

    /** 广播指定模块信号，一般用于业务中触发：导入成功后 pushModuleUpdate("stock") 等。 */
    public void pushModuleUpdate(String module) {
        log.info("[STAT-PUSH] 推送 refresh-{} 信号 -> 清除全部缓存并广播", module);
        cacheInvalidator.evictAll();
        broadcast("refresh-" + module);
    }

    private void broadcast(String type) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", type);
            payload.put("ts", System.currentTimeMillis());
            String text = objectMapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(text);
            Collection<WebSocketSession> sessions = sessionManager.getAllSessions();
            int sent = 0;
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try { session.sendMessage(message); sent++; }
                    catch (IOException e) { sessionManager.removeSession(session); }
                }
            }
            log.info("[STAT-PUSH] 广播 {} 消息完成：当前 session 总数 = {}, 成功发送 = {}", type, sessions.size(), sent);
        } catch (Exception e) {
            log.error("[STAT-PUSH] 广播 {} 消息失败：{}", type, e.getMessage(), e);
        }
    }
}