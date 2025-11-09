package com.realfds.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * WebSocket Gateway 애플리케이션
 *
 * alert-service의 알림을 WebSocket을 통해 브라우저 클라이언트에게 실시간으로 전송하는 게이트웨이 서비스입니다.
 *
 * 주요 기능:
 * - WebSocket 연결 관리 (/ws/alerts)
 * - alert-service REST API 폴링 (1초마다)
 * - 새 알림을 연결된 모든 클라이언트에 브로드캐스트
 * - 헬스 체크 엔드포인트 제공 (/actuator/health)
 *
 * 어노테이션:
 * - @SpringBootApplication: Spring Boot 애플리케이션 설정
 * - @EnableScheduling: 스케줄링 기능 활성화 (AlertStreamService의 주기적 폴링용)
 */
@SpringBootApplication
@EnableScheduling
public class WebSocketGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebSocketGatewayApplication.class, args);
    }
}
