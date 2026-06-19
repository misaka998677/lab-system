package com.lab.websocket;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.stereotype.Component;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;

@Component
public class WebSocketSessionManager {

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    public void addSession(WebSocketSession session) {
        sessions.add(session);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }

    public Set<WebSocketSession> getAllSessions() {
        return sessions;
    }

    public int getSessionCount() {
        return sessions.size();
    }
}