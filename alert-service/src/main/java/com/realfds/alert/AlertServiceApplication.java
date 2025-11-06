package com.realfds.alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Alert Service - Spring Boot 메인 애플리케이션 클래스
 *
 * 실시간 금융 거래 탐지 시스템의 알림 서비스입니다.
 * Kafka의 transaction-alerts 토픽에서 알림 메시지를 수신하여 처리합니다.
 *
 * 주요 기능:
 * - Kafka 메시지 소비 (@EnableKafka)
 * - Spring WebFlux 기반 Reactive 웹 서비스
 * - Health Check 엔드포인트 제공 (/actuator/health)
 * - 구조화된 JSON 로깅
 *
 * 실행 방법:
 * - 로컬: ./gradlew bootRun
 * - Docker: docker-compose up alert-service
 *
 * 환경 변수:
 * - SERVER_PORT: 서버 포트 (기본값: 8081)
 * - KAFKA_BOOTSTRAP_SERVERS: Kafka 브로커 주소 (기본값: localhost:9092)
 * - KAFKA_CONSUMER_GROUP_ID: Consumer 그룹 ID (기본값: alert-service-group)
 *
 * @author RealFDS Team
 * @version 1.0.0
 * @since 2025-01-06
 */
@SpringBootApplication
@EnableKafka  // Kafka 리스너 활성화
public class AlertServiceApplication {

    /**
     * Spring Boot 애플리케이션 진입점
     *
     * @param args 커맨드 라인 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(AlertServiceApplication.class, args);
    }
}
