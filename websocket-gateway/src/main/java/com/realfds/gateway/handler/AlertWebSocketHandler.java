package com.realfds.gateway.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alert WebSocket 핸들러
 *
 * WebSocket 연결을 관리하고 클라이언트에게 알림을 전송하는 핸들러입니다.
 *
 * 주요 기능:
 * - WebSocket 세션 관리 (Thread-safe)
 * - 클라이언트 연결/종료 처리
 * - 연결 상태 로깅
 *
 * Thread Safety:
 * - ConcurrentHashMap을 사용하여 여러 스레드에서 안전하게 세션을 관리합니다.
 */
@Component
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AlertWebSocketHandler.class);

    // Thread-safe 세션 관리를 위한 ConcurrentHashMap
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    /**
     * WebSocket 연결 수립 시 호출됩니다.
     *
     * @param session 새로 연결된 WebSocket 세션
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 세션 저장
        sessions.add(session);
        logger.info("WebSocket 연결 수립: sessionId={}, 총 연결 수={}",
                    session.getId(), sessions.size());
    }

    /**
     * WebSocket 연결 종료 시 호출됩니다.
     *
     * @param session 종료된 WebSocket 세션
     * @param status 종료 상태
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 세션 제거
        sessions.remove(session);
        logger.info("WebSocket 연결 종료: sessionId={}, 상태={}, 총 연결 수={}",
                    session.getId(), status, sessions.size());
    }

    /**
     * WebSocket 오류 발생 시 호출됩니다.
     *
     * @param session WebSocket 세션
     * @param exception 발생한 예외
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket 오류 발생: sessionId={}, 오류={}",
                     session.getId(), exception.getMessage(), exception);

        // 오류 발생 시 세션 제거
        if (sessions.contains(session)) {
            sessions.remove(session);
        }
    }

    /**
     * 현재 활성화된 모든 WebSocket 세션을 반환합니다.
     * (테스트용 접근자)
     *
     * @return 읽기 전용 세션 Set
     */
    public Set<WebSocketSession> getSessions() {
        return Collections.unmodifiableSet(sessions);
    }
}
