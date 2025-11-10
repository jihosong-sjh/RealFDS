package com.realfds.alert.service;

import com.realfds.alert.model.Alert;
import com.realfds.alert.model.AlertStatus;
import com.realfds.alert.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AlertService - 알림 비즈니스 로직 처리
 *
 * 알림 저장 및 조회를 담당하는 서비스 계층입니다.
 * - AlertConsumer에서 Kafka로부터 수신한 알림을 처리
 * - AlertController에서 REST API 요청 시 알림 조회
 * - AlertRepository를 통한 데이터 접근
 * - 알림 상태 변경 시 Kafka 이벤트 발행
 *
 * 비즈니스 로직:
 * - 알림 검증 (null 체크)
 * - 알림 저장 로깅
 * - 최근 알림 조회
 * - 상태 변경 및 이벤트 발행
 */
@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    private static final String ALERT_STATUS_CHANGED_TOPIC = "alert-status-changed";

    private final AlertRepository alertRepository;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    /**
     * 생성자 기반 의존성 주입
     *
     * @param alertRepository 알림 저장소
     * @param kafkaTemplate Kafka 메시지 전송 템플릿
     */
    public AlertService(AlertRepository alertRepository,
                        KafkaTemplate<String, Map<String, Object>> kafkaTemplate) {
        this.alertRepository = alertRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 알림 처리
     *
     * Kafka로부터 수신한 알림을 검증하고 저장합니다.
     *
     * @param alert 처리할 알림
     */
    public void processAlert(Alert alert) {
        if (alert == null) {
            logger.warn("알림 처리 실패: null 알림");
            return;
        }

        logger.info("알림 처리 시작 - alertId={}, ruleName={}, severity={}",
            alert.getAlertId(), alert.getRuleName(), alert.getSeverity());

        // 알림 저장
        alertRepository.addAlert(alert);

        logger.info("알림 처리 완료 - alertId={}, 저장소 크기={}",
            alert.getAlertId(), alertRepository.size());
    }

    /**
     * 최근 알림 조회
     *
     * REST API 요청 시 최근 알림을 조회하여 반환합니다.
     *
     * @param limit 조회할 알림 개수
     * @return 최근 알림 리스트 (최신순)
     */
    public List<Alert> getRecentAlerts(int limit) {
        if (limit <= 0) {
            logger.warn("잘못된 limit 값: {}, 빈 리스트 반환", limit);
            return List.of();
        }

        logger.debug("최근 알림 조회 요청 - limit={}", limit);

        List<Alert> alerts = alertRepository.getRecentAlerts(limit);

        logger.debug("최근 알림 조회 완료 - 반환 개수={}", alerts.size());

        return alerts;
    }

    /**
     * 알림 상태 변경
     *
     * 알림의 처리 상태를 변경합니다.
     * - UNREAD → IN_PROGRESS → COMPLETED (정방향)
     * - 역방향 전이도 허용 (재조사 시나리오)
     * - COMPLETED 상태로 변경 시 processedAt 자동 설정
     * - 상태 변경 성공 시 Kafka 이벤트 발행 (alert-status-changed 토픽)
     *
     * 구조화된 로깅:
     * - INFO: 상태 변경 성공 (alertId, oldStatus, newStatus, processedAt 포함)
     * - ERROR: 상태 변경 실패 (alertId, 오류 원인 포함)
     *
     * @param alertId 변경할 알림 ID
     * @param newStatus 새로운 상태 (UNREAD, IN_PROGRESS, COMPLETED)
     * @return 업데이트된 Alert 객체 (실패 시 null)
     */
    public Alert changeStatus(String alertId, AlertStatus newStatus) {
        // 입력 검증: alertId
        if (alertId == null || alertId.isEmpty()) {
            logger.error("상태 변경 실패: alertId={}, 오류 원인=alertId가 비어있음", alertId);
            return null;
        }

        // 입력 검증: newStatus
        if (newStatus == null) {
            logger.error("상태 변경 실패: alertId={}, 오류 원인=newStatus가 null", alertId);
            return null;
        }

        // 알림 조회
        Alert alert = alertRepository.findByAlertId(alertId);
        if (alert == null) {
            logger.error("상태 변경 실패: alertId={}, 오류 원인=알림을 찾을 수 없음", alertId);
            return null;
        }

        AlertStatus oldStatus = alert.getStatus();

        try {
            // 상태 변경
            alert.setStatus(newStatus);

            // COMPLETED 상태로 변경 시 processedAt 자동 설정
            if (newStatus == AlertStatus.COMPLETED && alert.getProcessedAt() == null) {
                alert.setProcessedAt(Instant.now());

                // 구조화된 로깅: INFO 레벨 (성공 - processedAt 포함)
                logger.info("상태 변경 성공: alertId={}, oldStatus={}, newStatus={}, processedAt={}",
                    alertId, oldStatus, newStatus, alert.getProcessedAt());
            } else {
                // 구조화된 로깅: INFO 레벨 (성공)
                logger.info("상태 변경 성공: alertId={}, oldStatus={}, newStatus={}, processedAt={}",
                    alertId, oldStatus, newStatus, alert.getProcessedAt());
            }

            // Kafka 이벤트 발행
            publishStatusChangedEvent(alert);

            return alert;

        } catch (Exception e) {
            // 구조화된 로깅: ERROR 레벨 (실패)
            logger.error("상태 변경 실패: alertId={}, oldStatus={}, newStatus={}, 오류 원인={}",
                alertId, oldStatus, newStatus, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 상태 변경 이벤트를 Kafka로 발행
     *
     * alert-status-changed 토픽에 이벤트를 발행합니다.
     * 이벤트 스키마:
     * {
     *   "alertId": "uuid",
     *   "status": "IN_PROGRESS",
     *   "processedAt": "2025-11-11T10:30:00Z" (COMPLETED인 경우만)
     * }
     *
     * @param alert 상태가 변경된 알림 객체
     */
    private void publishStatusChangedEvent(Alert alert) {
        try {
            // 이벤트 데이터 생성
            Map<String, Object> event = new HashMap<>();
            event.put("alertId", alert.getAlertId());
            event.put("status", alert.getStatus().toString());

            // COMPLETED 상태인 경우에만 processedAt 포함
            if (alert.getProcessedAt() != null) {
                event.put("processedAt", alert.getProcessedAt().toString());
            }

            // Kafka로 이벤트 전송
            kafkaTemplate.send(ALERT_STATUS_CHANGED_TOPIC, alert.getAlertId(), event);

            logger.info("상태 변경 이벤트 발행 완료 - topic={}, alertId={}, status={}",
                    ALERT_STATUS_CHANGED_TOPIC, alert.getAlertId(), alert.getStatus());

        } catch (Exception e) {
            // 이벤트 발행 실패 시 로깅만 하고 계속 진행 (알림 상태 변경은 이미 성공)
            logger.error("상태 변경 이벤트 발행 실패 - alertId={}, 오류: {}",
                    alert.getAlertId(), e.getMessage(), e);
        }
    }

    /**
     * 알림 ID로 조회
     *
     * @param alertId 조회할 알림 ID
     * @return Alert 객체 (없으면 null)
     */
    public Alert getAlertById(String alertId) {
        if (alertId == null || alertId.isEmpty()) {
            logger.warn("알림 조회 실패: alertId가 비어있음");
            return null;
        }

        Alert alert = alertRepository.findByAlertId(alertId);
        if (alert == null) {
            logger.debug("알림 조회 실패: 알림을 찾을 수 없음 - alertId={}", alertId);
            return null;
        }

        logger.debug("알림 조회 완료 - alertId={}", alertId);
        return alert;
    }

    /**
     * 상태별 알림 필터링
     *
     * 특정 상태의 알림만 필터링하여 반환합니다.
     * 응답 시간 <100ms 목표 (100개 알림 기준)
     *
     * @param status 필터링할 상태 (UNREAD, IN_PROGRESS, COMPLETED)
     * @return 필터링된 알림 리스트 (최신순)
     */
    public List<Alert> filterByStatus(AlertStatus status) {
        if (status == null) {
            logger.warn("상태별 필터링 실패: status가 null");
            return List.of();
        }

        logger.debug("상태별 필터링 시작 - status={}", status);

        long startTime = System.currentTimeMillis();

        // 전체 알림 조회 후 필터링
        List<Alert> allAlerts = alertRepository.getRecentAlerts(100);
        List<Alert> filteredAlerts = allAlerts.stream()
            .filter(alert -> status == alert.getStatus())
            .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        logger.debug("상태별 필터링 완료 - status={}, 결과 개수={}, 소요 시간={}ms",
            status, filteredAlerts.size(), duration);

        return filteredAlerts;
    }
}
