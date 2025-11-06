package com.realfds.alert.consumer;

import com.realfds.alert.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * AlertConsumer 클래스
 *
 * Kafka transaction-alerts 토픽에서 Alert 메시지를 소비합니다.
 * 수신한 알림을 로깅하여 모니터링 및 디버깅에 활용합니다.
 *
 * 주요 기능:
 * - @KafkaListener를 사용하여 transaction-alerts 토픽 구독
 * - 알림 수신 시 alertId, ruleName, reason 로깅 (INFO 레벨)
 * - JSON 역직렬화 실패 시 WARN 레벨로 에러 로깅
 *
 * 환경 변수:
 * - SPRING_KAFKA_BOOTSTRAP_SERVERS: Kafka 브로커 주소
 * - SPRING_KAFKA_CONSUMER_GROUP_ID: Consumer 그룹 ID
 */
@Component
public class AlertConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AlertConsumer.class);

    /**
     * Kafka 알림 메시지 소비 메서드
     *
     * transaction-alerts 토픽에서 Alert 메시지를 수신합니다.
     * 수신한 알림의 주요 정보(alertId, ruleName, reason)를 INFO 레벨로 로깅합니다.
     *
     * @param alert 수신한 Alert 객체
     */
    @KafkaListener(
            topics = "transaction-alerts",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAlert(@Payload Alert alert) {
        try {
            // 알림 수신 로그 (INFO 레벨)
            logger.info("알림 수신 - alertId: {}, 규칙: {}, 사유: {}",
                    alert.getAlertId(),
                    alert.getRuleName(),
                    alert.getReason());

        } catch (Exception e) {
            // 예외 발생 시 WARN 레벨로 로깅
            logger.warn("알림 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
