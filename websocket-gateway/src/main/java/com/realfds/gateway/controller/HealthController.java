package com.realfds.gateway.controller;

import com.realfds.gateway.handler.AlertWebSocketHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 헬스 체크 컨트롤러
 *
 * 서비스 상태를 확인하기 위한 헬스 체크 엔드포인트를 제공합니다.
 *
 * 엔드포인트:
 * - GET /actuator/health: 서비스 상태 및 WebSocket 연결 통계 반환
 *
 * 헬스 체크 응답 형식:
 * {
 *   "status": "UP",
 *   "websocket": {
 *     "activeConnections": 5
 *   }
 * }
 */
@RestController
public class HealthController {

    private final AlertWebSocketHandler webSocketHandler;

    /**
     * 생성자 기반 의존성 주입
     *
     * @param webSocketHandler WebSocket 핸들러
     */
    public HealthController(AlertWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * 헬스 체크 엔드포인트
     *
     * 서비스가 정상 실행 중임을 확인하고, WebSocket 연결 통계를 포함한 헬스 체크 정보를 반환합니다.
     * Docker 헬스 체크나 로드 밸런서의 상태 확인에 사용할 수 있습니다.
     *
     * 통계 정보:
     * - 활성 WebSocket 연결 수
     *
     * @return Mono<Map<String, Object>> - 헬스 체크 및 WebSocket 통계 정보
     */
    @GetMapping("/actuator/health")
    public Mono<Map<String, Object>> health() {
        // WebSocket 연결 수 조회
        int activeConnections = webSocketHandler.getSessions().size();

        // WebSocket 통계 정보
        Map<String, Object> websocketStats = new HashMap<>();
        websocketStats.put("activeConnections", activeConnections);

        // 헬스 체크 응답
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("websocket", websocketStats);

        return Mono.just(response);
    }
}
