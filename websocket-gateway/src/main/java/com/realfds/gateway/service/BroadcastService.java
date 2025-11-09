package com.realfds.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realfds.gateway.handler.AlertWebSocketHandler;
import com.realfds.gateway.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * 브로드캐스트 서비스
 *
 * 연결된 모든 WebSocket 클라이언트에게 알림을 브로드캐스트합니다.
 *
 * 주요 기능:
 * - Alert를 JSON으로 직렬화
 * - 모든 활성 세션에 메시지 전송
 * - 전송 실패 시 세션 제거
 */
@Service
public class BroadcastService {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastService.class);

    private final AlertWebSocketHandler alertWebSocketHandler;
    private final ObjectMapper objectMapper;

    public BroadcastService(AlertWebSocketHandler alertWebSocketHandler, ObjectMapper objectMapper) {
        this.alertWebSocketHandler = alertWebSocketHandler;
        this.objectMapper = objectMapper;
    }

    /**
     * 모든 WebSocket 세션에 알림을 브로드캐스트합니다.
     *
     * @param alert 브로드캐스트할 알림
     */
    public void broadcast(Alert alert) {
        try {
            // Alert를 JSON으로 직렬화
            String json = objectMapper.writeValueAsString(alert);
            TextMessage message = new TextMessage(json);

            // 모든 활성 세션에 메시지 전송
            for (WebSocketSession session : alertWebSocketHandler.getSessions()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                        logger.debug("알림 전송 성공: sessionId={}, alertId={}",
                                    session.getId(), alert.getAlertId());
                    } catch (IOException e) {
                        logger.error("알림 전송 실패: sessionId={}, 오류={}",
                                    session.getId(), e.getMessage());
                        // 전송 실패 시 세션 닫기
                        closeSession(session);
                    }
                }
            }

            logger.info("알림 브로드캐스트 완료: alertId={}, 전송 세션 수={}",
                       alert.getAlertId(), alertWebSocketHandler.getSessions().size());

        } catch (Exception e) {
            logger.error("알림 브로드캐스트 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * WebSocket 세션을 안전하게 닫습니다.
     *
     * @param session 닫을 세션
     */
    private void closeSession(WebSocketSession session) {
        try {
            session.close();
            logger.info("세션 닫기 완료: sessionId={}", session.getId());
        } catch (IOException e) {
            logger.error("세션 닫기 실패: sessionId={}, 오류={}",
                        session.getId(), e.getMessage());
        }
    }
}
