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
     * 이벤트 스키마 (User Story 2 확장):
     * {
     *   "alertId": "uuid",
     *   "status": "IN_PROGRESS",
     *   "processedAt": "2025-11-11T10:30:00Z" (COMPLETED인 경우만),
     *   "assignedTo": "김보안" (할당된 경우만),
     *   "actionNote": "고객 확인 완료" (기록된 경우만)
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

            // 담당자가 할당된 경우에만 assignedTo 포함 (User Story 2)
            if (alert.getAssignedTo() != null) {
                event.put("assignedTo", alert.getAssignedTo());
            }

            // 조치 내용이 기록된 경우에만 actionNote 포함 (User Story 2)
            if (alert.getActionNote() != null) {
                event.put("actionNote", alert.getActionNote());
            }

            // Kafka로 이벤트 전송
            kafkaTemplate.send(ALERT_STATUS_CHANGED_TOPIC, alert.getAlertId(), event);

            logger.info("상태 변경 이벤트 발행 완료 - topic={}, alertId={}, status={}, assignedTo={}, actionNote 존재={}",
                    ALERT_STATUS_CHANGED_TOPIC, alert.getAlertId(), alert.getStatus(),
                    alert.getAssignedTo(), alert.getActionNote() != null);

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

    /**
     * 담당자 할당
     *
     * 알림에 담당자를 할당합니다.
     * - 유효성 검증 (최대 100자)
     * - 할당 성공 시 구조화된 로깅 (INFO 레벨)
     * - 할당 실패 시 구조화된 로깅 (ERROR 레벨)
     *
     * 구조화된 로깅:
     * - INFO: 담당자 할당 성공 (alertId, assignedTo 포함)
     * - ERROR: 할당 실패 (alertId, 오류 원인 포함)
     *
     * @param alertId 할당할 알림 ID
     * @param assignedTo 담당자 이름 (최대 100자)
     * @return 업데이트된 Alert 객체 (실패 시 null)
     */
    public Alert assignAlert(String alertId, String assignedTo) {
        // 입력 검증: alertId
        if (alertId == null || alertId.isEmpty()) {
            logger.error("담당자 할당 실패: alertId={}, 오류 원인=alertId가 비어있음", alertId);
            return null;
        }

        // 입력 검증: assignedTo (최대 100자)
        if (assignedTo != null && assignedTo.length() > 100) {
            logger.error("담당자 할당 실패: alertId={}, 오류 원인=담당자 이름이 100자를 초과함 (길이={})",
                alertId, assignedTo.length());
            return null;
        }

        try {
            // 담당자 할당
            boolean success = alertRepository.assignTo(alertId, assignedTo);

            if (!success) {
                logger.error("담당자 할당 실패: alertId={}, 오류 원인=알림을 찾을 수 없음", alertId);
                return null;
            }

            // 업데이트된 알림 조회
            Alert updatedAlert = alertRepository.findByAlertId(alertId);

            // 구조화된 로깅: INFO 레벨 (성공)
            logger.info("담당자 할당 성공: alertId={}, assignedTo={}", alertId, assignedTo);

            // Kafka 이벤트 발행
            publishStatusChangedEvent(updatedAlert);

            return updatedAlert;

        } catch (Exception e) {
            // 구조화된 로깅: ERROR 레벨 (실패)
            logger.error("담당자 할당 실패: alertId={}, 오류 원인={}",
                alertId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 조치 내용 기록
     *
     * 알림에 조치 내용을 기록하고, 선택적으로 완료 처리합니다.
     * - 유효성 검증 (최대 2000자)
     * - complete=true 시 status=COMPLETED 및 processedAt 자동 설정
     * - 기록 성공 시 구조화된 로깅 (INFO 레벨)
     * - 기록 실패 시 구조화된 로깅 (ERROR 레벨)
     *
     * 비즈니스 규칙:
     * - actionNote는 필수 (null 불가)
     * - complete=true인 경우 상태가 COMPLETED로 변경됨
     * - complete=false인 경우 상태는 변경되지 않음
     *
     * 구조화된 로깅:
     * - INFO: 조치 기록 성공 (alertId, actionNote 길이, status 포함)
     * - ERROR: 기록 실패 (alertId, 오류 원인 포함)
     *
     * @param alertId 기록할 알림 ID
     * @param actionNote 조치 내용 (최대 2000자)
     * @param complete 완료 처리 여부 (true: COMPLETED, false: 상태 유지)
     * @return 업데이트된 Alert 객체 (실패 시 null)
     */
    public Alert recordAction(String alertId, String actionNote, boolean complete) {
        // 입력 검증: alertId
        if (alertId == null || alertId.isEmpty()) {
            logger.error("조치 기록 실패: alertId={}, 오류 원인=alertId가 비어있음", alertId);
            return null;
        }

        // 입력 검증: actionNote (필수)
        if (actionNote == null || actionNote.isEmpty()) {
            logger.error("조치 기록 실패: alertId={}, 오류 원인=actionNote가 비어있음", alertId);
            return null;
        }

        // 입력 검증: actionNote (최대 2000자)
        if (actionNote.length() > 2000) {
            logger.error("조치 기록 실패: alertId={}, 오류 원인=조치 내용이 2000자를 초과함 (길이={})",
                alertId, actionNote.length());
            return null;
        }

        try {
            // 조치 내용 기록
            boolean success = alertRepository.updateActionNote(alertId, actionNote);

            if (!success) {
                logger.error("조치 기록 실패: alertId={}, 오류 원인=알림을 찾을 수 없음", alertId);
                return null;
            }

            // complete=true인 경우 상태를 COMPLETED로 변경
            if (complete) {
                Alert alert = alertRepository.findByAlertId(alertId);
                if (alert != null) {
                    alert.setStatus(AlertStatus.COMPLETED);

                    // COMPLETED 상태로 변경 시 processedAt 자동 설정
                    if (alert.getProcessedAt() == null) {
                        alert.setProcessedAt(Instant.now());
                    }
                }
            }

            // 업데이트된 알림 조회
            Alert updatedAlert = alertRepository.findByAlertId(alertId);

            // 구조화된 로깅: INFO 레벨 (성공)
            logger.info("조치 기록 성공: alertId={}, actionNote 길이={}, status={}",
                alertId, actionNote.length(), updatedAlert.getStatus());

            // Kafka 이벤트 발행
            publishStatusChangedEvent(updatedAlert);

            return updatedAlert;

        } catch (Exception e) {
            // 구조화된 로깅: ERROR 레벨 (실패)
            logger.error("조치 기록 실패: alertId={}, 오류 원인={}",
                alertId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 담당자별 알림 필터링
     *
     * 특정 담당자에게 할당된 알림만 필터링하여 반환합니다.
     * 응답 시간 <100ms 목표 (100개 알림 기준)
     *
     * @param assignedTo 필터링할 담당자 이름
     * @return 필터링된 알림 리스트 (최신순)
     */
    public List<Alert> filterByAssignee(String assignedTo) {
        if (assignedTo == null || assignedTo.isEmpty()) {
            logger.warn("담당자별 필터링 실패: assignedTo가 비어있음");
            return List.of();
        }

        logger.debug("담당자별 필터링 시작 - assignedTo={}", assignedTo);

        long startTime = System.currentTimeMillis();

        // 전체 알림 조회 후 필터링
        List<Alert> allAlerts = alertRepository.getRecentAlerts(100);
        List<Alert> filteredAlerts = allAlerts.stream()
            .filter(alert -> assignedTo.equals(alert.getAssignedTo()))
            .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        logger.debug("담당자별 필터링 완료 - assignedTo={}, 결과 개수={}, 소요 시간={}ms",
            assignedTo, filteredAlerts.size(), duration);

        return filteredAlerts;
    }

    /**
     * 심각도별 알림 필터링
     *
     * 특정 심각도의 알림만 필터링하여 반환합니다.
     * 응답 시간 <100ms 목표 (100개 알림 기준)
     *
     * @param severity 필터링할 심각도 (LOW, MEDIUM, HIGH, CRITICAL)
     * @return 필터링된 알림 리스트 (최신순)
     */
    public List<Alert> filterBySeverity(com.realfds.alert.model.Severity severity) {
        if (severity == null) {
            logger.warn("심각도별 필터링 실패: severity가 null");
            return List.of();
        }

        logger.debug("심각도별 필터링 시작 - severity={}", severity);

        long startTime = System.currentTimeMillis();

        // Severity enum을 문자열로 변환 (Alert 모델의 severity 필드는 String)
        String severityStr = severity.name();

        // 전체 알림 조회 후 필터링
        List<Alert> allAlerts = alertRepository.getRecentAlerts(100);
        List<Alert> filteredAlerts = allAlerts.stream()
            .filter(alert -> severityStr.equals(alert.getSeverity()))
            .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        logger.debug("심각도별 필터링 완료 - severity={}, 결과 개수={}, 소요 시간={}ms",
            severity, filteredAlerts.size(), duration);

        return filteredAlerts;
    }

    /**
     * 심각도별 알림 정렬
     *
     * 알림을 심각도 순서로 정렬하여 반환합니다.
     * 정렬 순서: CRITICAL → HIGH → MEDIUM → LOW (우선순위 내림차순)
     *
     * 비즈니스 규칙:
     * - 심각도가 높은 알림이 먼저 표시됨
     * - Severity enum의 priority 필드 사용 (4 → 3 → 2 → 1)
     * - Comparator 구현으로 효율적인 정렬
     *
     * @param alerts 정렬할 알림 리스트
     * @return 심각도순으로 정렬된 알림 리스트
     */
    public List<Alert> sortBySeverity(List<Alert> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            logger.debug("심각도별 정렬 실패: alerts가 비어있음");
            return List.of();
        }

        logger.debug("심각도별 정렬 시작 - 알림 개수={}", alerts.size());

        long startTime = System.currentTimeMillis();

        // 심각도 우선순위 기준 정렬 (높은 우선순위 → 낮은 우선순위)
        List<Alert> sortedAlerts = alerts.stream()
            .sorted((a1, a2) -> {
                // Alert의 severity는 String이므로 Severity enum으로 변환하여 priority 비교
                int priority1 = getSeverityPriority(a1.getSeverity());
                int priority2 = getSeverityPriority(a2.getSeverity());
                return Integer.compare(priority2, priority1); // 역순 정렬 (높은 우선순위 먼저)
            })
            .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        logger.debug("심각도별 정렬 완료 - 알림 개수={}, 소요 시간={}ms",
            sortedAlerts.size(), duration);

        return sortedAlerts;
    }

    /**
     * 심각도 문자열을 우선순위로 변환
     *
     * Alert 모델의 severity 필드는 String이므로, Severity enum으로 변환하여 priority를 반환합니다.
     *
     * @param severityStr 심각도 문자열 (예: "HIGH", "MEDIUM", "LOW", "CRITICAL")
     * @return 우선순위 (1-4, 유효하지 않은 경우 0)
     */
    private int getSeverityPriority(String severityStr) {
        if (severityStr == null || severityStr.isEmpty()) {
            return 0;
        }

        try {
            com.realfds.alert.model.Severity severity = com.realfds.alert.model.Severity.valueOf(severityStr.toUpperCase());
            return severity.getPriority();
        } catch (IllegalArgumentException e) {
            logger.warn("유효하지 않은 severity 값: {}", severityStr);
            return 0;
        }
    }
}
