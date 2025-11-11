package com.realfds.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realfds.gateway.handler.AlertWebSocketHandler;
import com.realfds.gateway.model.Alert;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
 *
 * Micrometer 메트릭:
 * - websocket_broadcast_total: 브로드캐스트 총 횟수 (counter)
 * - websocket_broadcast_latency: 브로드캐스트 지연 시간 (histogram)
 */
@Service
public class BroadcastService {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastService.class);

    private final AlertWebSocketHandler alertWebSocketHandler;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    // Micrometer 메트릭
    private final Counter broadcastCounter;
    private final Timer broadcastLatencyTimer;

    public BroadcastService(AlertWebSocketHandler alertWebSocketHandler,
                          ObjectMapper objectMapper,
                          MeterRegistry meterRegistry) {
        this.alertWebSocketHandler = alertWebSocketHandler;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;

        // Micrometer 메트릭 초기화
        this.broadcastCounter = Counter.builder("websocket_broadcast_total")
                .description("WebSocket 브로드캐스트 총 횟수")
                .register(meterRegistry);

        this.broadcastLatencyTimer = Timer.builder("websocket_broadcast_latency")
                .description("WebSocket 브로드캐스트 지연 시간")
                .register(meterRegistry);
    }

    /**
     * 모든 WebSocket 세션에 알림을 브로드캐스트합니다.
     *
     * 구조화된 로깅:
     * - INFO: 브로드캐스트 성공 (eventType, alertId, 클라이언트 수 포함)
     * - ERROR: 브로드캐스트 실패 (오류 원인 포함)
     *
     * Micrometer 메트릭:
     * - websocket_broadcast_total: 브로드캐스트 성공 시 카운터 증가
     * - websocket_broadcast_latency: 브로드캐스트 소요 시간 기록
     *
     * @param alert 브로드캐스트할 알림
     */
    public void broadcast(Alert alert) {
        // 브로드캐스트 지연 시간 측정
        broadcastLatencyTimer.record(() -> {
            long startTime = System.currentTimeMillis();
            int successCount = 0;
            int failureCount = 0;

            try {
                // Alert를 JSON으로 직렬화
                String json = objectMapper.writeValueAsString(alert);
                TextMessage message = new TextMessage(json);

                int totalSessions = alertWebSocketHandler.getSessions().size();

                // 모든 활성 세션에 메시지 전송
                for (WebSocketSession session : alertWebSocketHandler.getSessions()) {
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(message);
                            successCount++;
                            logger.debug("알림 전송 성공: sessionId={}, alertId={}",
                                        session.getId(), alert.getAlertId());
                        } catch (IOException e) {
                            failureCount++;
                            logger.error("알림 전송 실패: sessionId={}, alertId={}, 오류={}",
                                        session.getId(), alert.getAlertId(), e.getMessage());
                            // 전송 실패 시 세션 닫기
                            closeSession(session);
                        }
                    }
                }

                long duration = System.currentTimeMillis() - startTime;

                // Micrometer 메트릭: 브로드캐스트 성공 카운터 증가
                broadcastCounter.increment();

                // 구조화된 로깅: INFO 레벨 (브로드캐스트 성공)
                logger.info("브로드캐스트 성공: eventType=ALERT_STATUS_CHANGED, alertId={}, " +
                           "클라이언트 수={}, 성공={}, 실패={}, 소요 시간={}ms",
                           alert.getAlertId(), totalSessions, successCount, failureCount, duration);

            } catch (Exception e) {
                // 구조화된 로깅: ERROR 레벨 (브로드캐스트 실패)
                logger.error("브로드캐스트 실패: eventType=ALERT_STATUS_CHANGED, alertId={}, 오류 원인={}",
                           alert.getAlertId(), e.getMessage(), e);
            }
        });
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
