package io.realfds.alert.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor

/**
 * WebSocket 설정
 *
 * 실시간 메트릭 데이터 브로드캐스트를 위한 WebSocket 엔드포인트를 등록합니다.
 *
 * ## 엔드포인트
 * - **ws://localhost:8082/ws/metrics**: 실시간 메트릭 데이터 스트림
 *
 * ## 설정 특징
 * - CORS 허용 (로컬 개발용): http://localhost:3000
 * - SockJS fallback 미사용 (순수 WebSocket만 사용)
 * - HTTP 세션 연동 (HttpSessionHandshakeInterceptor)
 *
 * @see io.realfds.alert.websocket.MetricsWebSocketHandler
 */
@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val metricsWebSocketHandler: io.realfds.alert.websocket.MetricsWebSocketHandler
) : WebSocketConfigurer {

    /**
     * WebSocket 핸들러 등록
     *
     * @param registry WebSocket 핸들러 레지스트리
     */
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry
            // 실시간 메트릭 데이터 WebSocket 엔드포인트 등록
            .addHandler(metricsWebSocketHandler, "/ws/metrics")
            // CORS 설정 (로컬 개발용)
            .setAllowedOrigins(
                "http://localhost:3000",      // 로컬 React 개발 서버
                "http://localhost:8080",      // 로컬 프론트엔드 프로덕션
                "http://localhost:8082",      // 백엔드 서버 (동일 origin)
                "http://frontend-dashboard:8080"  // Docker 컨테이너
            )
            // HTTP 세션 정보를 WebSocket 세션에 복사
            .addInterceptors(HttpSessionHandshakeInterceptor())
            // SockJS fallback 미사용 (순수 WebSocket만 사용)
            // .withSockJS()를 호출하지 않음
    }
}
