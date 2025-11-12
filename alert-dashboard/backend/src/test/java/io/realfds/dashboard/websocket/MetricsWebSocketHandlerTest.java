package io.realfds.dashboard.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.realfds.alert.model.AlertMetrics;
import io.realfds.alert.model.MetricsDataPoint;
import io.realfds.alert.model.ServiceHealth;
import io.realfds.alert.model.TransactionMetrics;
import io.realfds.alert.service.MetricsStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MetricsWebSocketHandler 통합 테스트
 *
 * WebSocket 연결, 메시지 처리, 브로드캐스트 기능을 검증합니다.
 */
@DisplayName("MetricsWebSocketHandler 통합 테스트")
class MetricsWebSocketHandlerTest {

    private MetricsWebSocketHandler handler;
    private MetricsStore metricsStore;
    private ObjectMapper objectMapper;
    private WebSocketSession mockSession1;
    private WebSocketSession mockSession2;

    @BeforeEach
    void setUp() {
        metricsStore = new MetricsStore();
        objectMapper = new ObjectMapper();
        handler = new MetricsWebSocketHandler(metricsStore, objectMapper);

        // Mock WebSocket 세션 생성
        mockSession1 = mock(WebSocketSession.class);
        mockSession2 = mock(WebSocketSession.class);

        when(mockSession1.isOpen()).thenReturn(true);
        when(mockSession2.isOpen()).thenReturn(true);
        when(mockSession1.getId()).thenReturn("session-1");
        when(mockSession2.getId()).thenReturn("session-2");
    }

    @Test
    @DisplayName("Given WebSocket 연결, When 5초마다 METRICS_UPDATE 전송, Then 모든 세션에 브로드캐스트")
    void testBroadcastMetricsUpdate() throws Exception {
        // Given: 두 개의 WebSocket 세션이 연결됨
        handler.afterConnectionEstablished(mockSession1);
        handler.afterConnectionEstablished(mockSession2);

        // MetricsStore에 테스트 데이터 추가
        MetricsDataPoint testData = createTestMetricsDataPoint();
        metricsStore.addDataPoint(testData);

        // When: 브로드캐스트 메서드 호출
        handler.broadcast(testData);

        // Then: 모든 세션에 METRICS_UPDATE 메시지 전송
        verify(mockSession1, times(1)).sendMessage(any(TextMessage.class));
        verify(mockSession2, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Given BACKFILL_REQUEST 수신, When lastReceivedTimestamp 제공, Then BACKFILL_RESPONSE 전송")
    void testHandleBackfillRequest() throws Exception {
        // Given: WebSocket 세션이 연결됨
        handler.afterConnectionEstablished(mockSession1);

        // MetricsStore에 여러 데이터 포인트 추가
        Instant now = Instant.now();
        for (int i = 0; i < 5; i++) {
            MetricsDataPoint dataPoint = createTestMetricsDataPoint();
            metricsStore.addDataPoint(dataPoint);
            Thread.sleep(10); // 시간 차이를 만들기 위해
        }

        // BACKFILL_REQUEST 메시지 생성
        Map<String, Object> backfillRequest = new HashMap<>();
        backfillRequest.put("type", "BACKFILL_REQUEST");
        backfillRequest.put("lastReceivedTimestamp", now.minusSeconds(10).toString());
        String requestJson = objectMapper.writeValueAsString(backfillRequest);

        // When: BACKFILL_REQUEST 메시지 수신
        handler.handleTextMessage(mockSession1, new TextMessage(requestJson));

        // Then: BACKFILL_RESPONSE 메시지 전송
        verify(mockSession1, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Given 잘못된 메시지 형식, When 수신, Then ERROR 메시지 전송")
    void testHandleInvalidMessage() throws Exception {
        // Given: WebSocket 세션이 연결됨
        handler.afterConnectionEstablished(mockSession1);

        // When: 잘못된 형식의 메시지 수신
        String invalidJson = "{ invalid json }";
        handler.handleTextMessage(mockSession1, new TextMessage(invalidJson));

        // Then: ERROR 메시지 전송
        verify(mockSession1, times(1)).sendMessage(argThat(message -> {
            String payload = ((TextMessage) message).getPayload();
            return payload.contains("ERROR");
        }));
    }

    @Test
    @DisplayName("Given 여러 세션 연결, When 한 세션 연결 해제, Then 브로드캐스트 시 해당 세션 제외")
    void testSessionRemovalOnDisconnect() throws Exception {
        // Given: 두 개의 WebSocket 세션이 연결됨
        handler.afterConnectionEstablished(mockSession1);
        handler.afterConnectionEstablished(mockSession2);

        // When: 한 세션이 연결 해제됨
        handler.afterConnectionClosed(mockSession1, CloseStatus.NORMAL);

        // 브로드캐스트 실행
        MetricsDataPoint testData = createTestMetricsDataPoint();
        handler.broadcast(testData);

        // Then: 연결 해제된 세션에는 메시지 전송 안 됨
        verify(mockSession1, never()).sendMessage(any(TextMessage.class));
        verify(mockSession2, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Given WebSocket 세션 닫힘, When 브로드캐스트, Then 예외 발생 안 함")
    void testBroadcastWithClosedSession() throws Exception {
        // Given: WebSocket 세션이 연결됨
        handler.afterConnectionEstablished(mockSession1);

        // 세션이 닫힘
        when(mockSession1.isOpen()).thenReturn(false);

        // When: 브로드캐스트 실행
        MetricsDataPoint testData = createTestMetricsDataPoint();

        // Then: 예외 발생 안 함
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> handler.broadcast(testData));
    }

    /**
     * 테스트용 MetricsDataPoint 생성
     */
    private MetricsDataPoint createTestMetricsDataPoint() {
        Instant now = Instant.now();

        // ServiceHealth 리스트 생성
        List<ServiceHealth> services = new ArrayList<>();
        services.add(new ServiceHealth(
            "transaction-generator",
            ServiceHealth.ServiceStatus.UP,
            now,
            100L,
            512L,
            null,
            null
        ));
        services.add(new ServiceHealth(
            "fraud-detector",
            ServiceHealth.ServiceStatus.UP,
            now,
            150L,
            1024L,
            null,
            null
        ));
        services.add(new ServiceHealth(
            "alert-service",
            ServiceHealth.ServiceStatus.UP,
            now,
            120L,
            768L,
            null,
            null
        ));
        services.add(new ServiceHealth(
            "websocket-gateway",
            ServiceHealth.ServiceStatus.UP,
            now,
            80L,
            256L,
            null,
            null
        ));
        services.add(new ServiceHealth(
            "frontend-dashboard",
            ServiceHealth.ServiceStatus.UP,
            now,
            50L,
            128L,
            null,
            null
        ));

        // TransactionMetrics 생성
        TransactionMetrics transactionMetrics = new TransactionMetrics(
            now,
            50L,
            10000L
        );

        // AlertMetrics 생성
        Map<String, Long> byRule = new HashMap<>();
        byRule.put("HIGH_VALUE", 10L);
        byRule.put("FOREIGN_COUNTRY", 5L);
        byRule.put("HIGH_FREQUENCY", 3L);

        AlertMetrics alertMetrics = new AlertMetrics(
            now,
            18L,
            byRule
        );

        return new MetricsDataPoint(now, services, transactionMetrics, alertMetrics);
    }
}
