package com.realfds.gateway.consumer;

import com.realfds.gateway.service.BroadcastService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * StatusChangeConsumer - 알림 상태 변경 이벤트 수신
 *
 * alert-service의 alert-status-changed 토픽을 수신하여
 * WebSocket 클라이언트에게 상태 변경 이벤트를 브로드캐스트합니다.
 *
 * 주요 기능:
 * - Kafka alert-status-changed 토픽 수신
 * - 상태 변경 이벤트를 WebSocket으로 브로드캐스트
 * - 에러 처리 및 로깅
 *
 * 이벤트 스키마 (alert-service에서 발행):
 * {
 *   "alertId": "uuid",
 *   "status": "IN_PROGRESS",
 *   "processedAt": "2025-11-11T10:30:00Z" (COMPLETED인 경우만),
 *   "assignedTo": "김보안" (할당된 경우만),
 *   "actionNote": "고객 확인 완료" (기록된 경우만)
 * }
 *
 * Micrometer 메트릭:
 * - status_change_events_received_total: 상태 변경 이벤트 수신 횟수 (counter)
 */
@Component
public class StatusChangeConsumer {

    private static final Logger logger = LoggerFactory.getLogger(StatusChangeConsumer.class);
    private static final String ALERT_STATUS_CHANGED_TOPIC = "alert-status-changed";

    private final BroadcastService broadcastService;
    private final MeterRegistry meterRegistry;

    // Micrometer 메트릭
    private final Counter statusChangeEventsCounter;

    /**
     * 생성자 기반 의존성 주입
     *
     * @param broadcastService WebSocket 브로드캐스트 서비스
     * @param meterRegistry Micrometer 메트릭 레지스트리
     */
    public StatusChangeConsumer(BroadcastService broadcastService,
                               MeterRegistry meterRegistry) {
        this.broadcastService = broadcastService;
        this.meterRegistry = meterRegistry;

        // Micrometer 메트릭 초기화
        this.statusChangeEventsCounter = Counter.builder("status_change_events_received_total")
                .description("상태 변경 이벤트 수신 총 횟수")
                .register(meterRegistry);
    }

    /**
     * alert-status-changed 토픽 메시지 수신 처리
     *
     * Kafka 메시지를 수신하여 WebSocket 클라이언트에게 브로드캐스트합니다.
     * - 메시지 포맷: Map<String, Object>
     * - 필수 필드: alertId, status
     * - 선택 필드: processedAt, assignedTo, actionNote
     *
     * 구조화된 로깅:
     * - INFO: 이벤트 수신 성공 (alertId, status 포함)
     * - ERROR: 이벤트 수신 실패 (오류 원인 포함)
     *
     * Micrometer 메트릭:
     * - status_change_events_received_total: 이벤트 수신 성공 시 카운터 증가
     *
     * @param event 상태 변경 이벤트 (Map)
     */
    @KafkaListener(topics = ALERT_STATUS_CHANGED_TOPIC, groupId = "websocket-gateway-group")
    public void consumeStatusChangeEvent(Map<String, Object> event) {
        try {
            // 필수 필드 검증
            if (event == null || !event.containsKey("alertId") || !event.containsKey("status")) {
                logger.error("상태 변경 이벤트 수신 실패: 필수 필드 누락 - event={}", event);
                return;
            }

            String alertId = (String) event.get("alertId");
            String status = (String) event.get("status");
            String assignedTo = (String) event.get("assignedTo");
            String actionNote = (String) event.get("actionNote");
            String processedAt = (String) event.get("processedAt");

            // 구조화된 로깅: INFO 레벨 (이벤트 수신)
            logger.info("상태 변경 이벤트 수신: alertId={}, status={}, assignedTo={}, actionNote 존재={}, processedAt={}",
                    alertId, status, assignedTo, actionNote != null, processedAt);

            // Micrometer 메트릭: 이벤트 수신 카운터 증가
            statusChangeEventsCounter.increment();

            // WebSocket으로 브로드캐스트
            broadcastService.broadcastStatusChange(event);

        } catch (Exception e) {
            // 구조화된 로깅: ERROR 레벨 (이벤트 수신 실패)
            logger.error("상태 변경 이벤트 처리 실패: 오류 원인={}, event={}",
                    e.getMessage(), event, e);
        }
    }
}
