package io.realfds.dashboard.health;

import io.realfds.dashboard.service.MetricsStore;
import io.realfds.dashboard.websocket.MetricsWebSocketHandler;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * WebSocket 연결 상태 헬스 체크 인디케이터
 *
 * /actuator/health 엔드포인트에 WebSocket 연결 수와 브로드캐스트 메트릭을 포함합니다.
 * Constitution V 요구사항: 모든 서비스에 헬스 체크 엔드포인트 필수
 */
@Component
public class WebSocketHealthIndicator implements HealthIndicator {

    private final MetricsWebSocketHandler webSocketHandler;
    private final MetricsStore metricsStore;

    public WebSocketHealthIndicator(MetricsWebSocketHandler webSocketHandler, MetricsStore metricsStore) {
        this.webSocketHandler = webSocketHandler;
        this.metricsStore = metricsStore;
    }

    /**
     * WebSocket 헬스 체크 수행
     *
     * @return Health 상태 (UP/DOWN) 및 상세 정보
     */
    @Override
    public Health health() {
        try {
            // WebSocket 연결 수 조회
            int activeConnections = webSocketHandler.getActiveSessionCount();

            // 메트릭 데이터 포인트 수 조회
            int dataPointCount = metricsStore.size();

            // 상태 결정: WebSocket 핸들러가 정상 작동하면 UP
            return Health.up()
                    .withDetail("activeConnections", activeConnections)
                    .withDetail("maxConnections", 5)  // 최대 동시 사용자 수
                    .withDetail("metricsDataPoints", dataPointCount)
                    .withDetail("maxDataPoints", 720)  // 1시간 최대 데이터 포인트
                    .withDetail("status", "WebSocket 브로드캐스트 정상 작동")
                    .build();

        } catch (Exception e) {
            // 예외 발생 시 DOWN 상태 반환
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "WebSocket 브로드캐스트 오류")
                    .build();
        }
    }
}
