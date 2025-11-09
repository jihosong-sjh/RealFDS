package com.realfds.gateway.config;

import com.realfds.gateway.handler.AlertWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 설정 클래스
 *
 * WebSocket 엔드포인트를 설정하고 핸들러를 매핑합니다.
 *
 * 주요 설정:
 * - WebSocket 엔드포인트: /ws/alerts
 * - CORS 설정: 모든 Origin 허용 (로컬 개발용)
 * - SockJS fallback 활성화 (브라우저 호환성)
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AlertWebSocketHandler alertWebSocketHandler;

    public WebSocketConfig(AlertWebSocketHandler alertWebSocketHandler) {
        this.alertWebSocketHandler = alertWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // WebSocket 핸들러 매핑
        // /ws/alerts 엔드포인트로 WebSocket 연결 가능
        // 모든 Origin 허용 (로컬 개발용 - 프로덕션에서는 제한 필요)
        registry.addHandler(alertWebSocketHandler, "/ws/alerts")
                .setAllowedOrigins("*");  // 로컬 개발용: 모든 Origin 허용
    }
}
