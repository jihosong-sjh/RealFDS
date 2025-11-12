package io.realfds.dashboard.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.realfds.dashboard.model.MetricsDataPoint;
import io.realfds.dashboard.service.MetricsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 메트릭 핸들러
 *
 * WebSocket 연결을 관리하고 메트릭 데이터를 모든 클라이언트에 브로드캐스트합니다.
 * 또한 재연결 시 누락된 데이터를 백필하는 기능을 제공합니다.
 */
@Component
public class MetricsWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(MetricsWebSocketHandler.class);

    /**
     * 연결된 모든 WebSocket 세션을 저장하는 맵
     * 스레드 안전성을 위해 ConcurrentHashMap 사용
     */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 메트릭 데이터를 저장하는 circular buffer
     */
    private final MetricsStore metricsStore;

    /**
     * JSON 직렬화/역직렬화를 위한 ObjectMapper
     */
    private final ObjectMapper objectMapper;

    /**
     * Ping-pong heartbeat를 위한 스케줄러
     */
    private final ScheduledExecutorService heartbeatScheduler;

    public MetricsWebSocketHandler(MetricsStore metricsStore, ObjectMapper objectMapper) {
        this.metricsStore = metricsStore;
        this.objectMapper = objectMapper;
        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

        // 30초마다 ping-pong heartbeat 전송
        startHeartbeat();
    }

    /**
     * WebSocket 연결 수립 시 호출됩니다.
     *
     * @param session 새로 연결된 WebSocket 세션
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        logger.info("WebSocket 연결 수립: sessionId={}, 총 연결 수={}", sessionId, sessions.size());
    }

    /**
     * WebSocket 연결 종료 시 호출됩니다.
     *
     * @param session 종료된 WebSocket 세션
     * @param status  종료 상태
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        logger.info("WebSocket 연결 종료: sessionId={}, status={}, 총 연결 수={}", sessionId, status, sessions.size());
    }

    /**
     * 클라이언트로부터 텍스트 메시지를 수신했을 때 호출됩니다.
     * 주로 BACKFILL_REQUEST 메시지를 처리합니다.
     *
     * @param session WebSocket 세션
     * @param message 수신된 텍스트 메시지
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = session.getId();
        String payload = message.getPayload();

        try {
            // 메시지 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> messageMap = objectMapper.readValue(payload, Map.class);
            String type = (String) messageMap.get("type");

            if ("BACKFILL_REQUEST".equals(type)) {
                handleBackfillRequest(session, messageMap);
            } else {
                logger.warn("알 수 없는 메시지 타입: sessionId={}, type={}", sessionId, type);
                sendErrorMessage(session, "UNKNOWN_MESSAGE_TYPE", "알 수 없는 메시지 타입: " + type);
            }

        } catch (Exception e) {
            logger.error("메시지 파싱 실패: sessionId={}, payload={}", sessionId, payload, e);
            sendErrorMessage(session, "INVALID_MESSAGE", "잘못된 메시지 형식입니다");
        }
    }

    /**
     * BACKFILL_REQUEST 처리
     * 클라이언트가 마지막으로 수신한 timestamp 이후의 모든 데이터를 전송합니다.
     */
    private void handleBackfillRequest(WebSocketSession session, Map<String, Object> messageMap) {
        String sessionId = session.getId();

        try {
            String lastReceivedTimestampStr = (String) messageMap.get("lastReceivedTimestamp");
            Instant lastReceivedTimestamp = Instant.parse(lastReceivedTimestampStr);

            logger.info("BACKFILL_REQUEST 수신: sessionId={}, lastReceivedTimestamp={}", sessionId, lastReceivedTimestamp);

            // MetricsStore에서 누락된 데이터 조회
            List<MetricsDataPoint> missingData = metricsStore.getDataSince(lastReceivedTimestamp);

            // BACKFILL_RESPONSE 전송
            MetricsMessage backfillResponse = new MetricsMessage(missingData);
            String json = objectMapper.writeValueAsString(backfillResponse);
            session.sendMessage(new TextMessage(json));

            logger.info("BACKFILL_RESPONSE 전송 완료: sessionId={}, 데이터 수={}", sessionId, missingData.size());

        } catch (Exception e) {
            logger.error("BACKFILL_REQUEST 처리 실패: sessionId={}", sessionId, e);
            sendErrorMessage(session, "BACKFILL_ERROR", "백필 처리 중 오류가 발생했습니다");
        }
    }

    /**
     * 모든 연결된 클라이언트에게 메트릭 업데이트를 브로드캐스트합니다.
     * 이 메서드는 MetricsScheduler에서 5초마다 호출됩니다.
     *
     * @param dataPoint 전송할 메트릭 데이터
     */
    public void broadcast(MetricsDataPoint dataPoint) {
        if (sessions.isEmpty()) {
            logger.debug("브로드캐스트 건너뜀: 연결된 클라이언트 없음");
            return;
        }

        try {
            // METRICS_UPDATE 메시지 생성
            MetricsMessage message = new MetricsMessage(dataPoint);
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);

            // 모든 세션에 브로드캐스트
            int successCount = 0;
            int failCount = 0;

            for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
                String sessionId = entry.getKey();
                WebSocketSession session = entry.getValue();

                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                        successCount++;
                    } catch (IOException e) {
                        logger.error("메시지 전송 실패: sessionId={}", sessionId, e);
                        failCount++;
                    }
                } else {
                    logger.warn("세션 닫힘: sessionId={}", sessionId);
                    sessions.remove(sessionId);
                    failCount++;
                }
            }

            logger.debug("브로드캐스트 완료: 성공={}, 실패={}, 총 세션={}", successCount, failCount, sessions.size());

        } catch (Exception e) {
            logger.error("브로드캐스트 중 오류 발생", e);
        }
    }

    /**
     * 에러 메시지를 특정 세션에 전송합니다.
     */
    private void sendErrorMessage(WebSocketSession session, String errorCode, String errorMessage) {
        try {
            MetricsMessage message = new MetricsMessage(errorCode, errorMessage);
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            logger.error("에러 메시지 전송 실패: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 30초마다 ping-pong heartbeat를 전송하여 연결 상태를 유지합니다.
     */
    private void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
                String sessionId = entry.getKey();
                WebSocketSession session = entry.getValue();

                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage("ping"));
                        logger.trace("Heartbeat 전송: sessionId={}", sessionId);
                    } catch (IOException e) {
                        logger.warn("Heartbeat 전송 실패: sessionId={}", sessionId, e);
                        sessions.remove(sessionId);
                    }
                } else {
                    logger.warn("세션 닫힘 (heartbeat): sessionId={}", sessionId);
                    sessions.remove(sessionId);
                }
            }
        }, 30, 30, TimeUnit.SECONDS);

        logger.info("Heartbeat 스케줄러 시작: 30초 간격");
    }

    /**
     * 현재 연결된 세션 수를 반환합니다.
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * 핸들러 종료 시 리소스 정리
     */
    public void shutdown() {
        heartbeatScheduler.shutdown();
        logger.info("MetricsWebSocketHandler 종료");
    }
}
