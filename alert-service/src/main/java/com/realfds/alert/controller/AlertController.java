package com.realfds.alert.controller;

import com.realfds.alert.model.Alert;
import com.realfds.alert.model.AlertStatus;
import com.realfds.alert.model.Severity;
import com.realfds.alert.model.UpdateStatusRequest;
import com.realfds.alert.model.UpdateStatusResponse;
import com.realfds.alert.model.AssignRequest;
import com.realfds.alert.model.AssignResponse;
import com.realfds.alert.model.ActionRequest;
import com.realfds.alert.model.ActionResponse;
import com.realfds.alert.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * AlertController - 알림 REST API
 *
 * 알림 조회를 위한 REST API 엔드포인트를 제공합니다.
 * - GET /api/alerts: 최근 100개 알림 조회
 * - Reactive 타입(Mono) 사용으로 비동기 처리
 * - websocket-gateway에서 폴링하여 실시간 알림 전달
 *
 * 엔드포인트:
 * - GET /api/alerts
 *   - 설명: 최근 100개 알림을 최신순으로 반환
 *   - 응답: Mono<List<Alert>>
 *   - Content-Type: application/json
 *
 * 환경:
 * - Spring WebFlux 기반
 * - 비동기 논블로킹 처리
 */
@RestController
@RequestMapping("/api")
public class AlertController {

    private static final Logger logger = LoggerFactory.getLogger(AlertController.class);
    private static final int DEFAULT_ALERT_LIMIT = 100;

    private final AlertService alertService;

    /**
     * 생성자 기반 의존성 주입
     *
     * @param alertService 알림 처리 서비스
     */
    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * 최근 알림 조회 API
     *
     * GET /api/alerts
     * - 최근 100개 알림을 최신순으로 반환
     * - websocket-gateway가 1초마다 폴링하여 새로운 알림 확인
     *
     * GET /api/alerts?status=UNREAD
     * - 특정 상태의 알림만 필터링하여 반환
     *
     * GET /api/alerts?assignedTo=김보안
     * - 특정 담당자에게 할당된 알림만 필터링하여 반환
     *
     * GET /api/alerts?severity=HIGH
     * - 특정 심각도의 알림만 필터링하여 반환
     *
     * GET /api/alerts?sortBy=severity
     * - 알림을 심각도순으로 정렬하여 반환 (CRITICAL → HIGH → MEDIUM → LOW)
     *
     * @param status 필터링할 상태 (선택 사항: UNREAD, IN_PROGRESS, COMPLETED)
     * @param assignedTo 필터링할 담당자 이름 (선택 사항)
     * @param severity 필터링할 심각도 (선택 사항: LOW, MEDIUM, HIGH, CRITICAL)
     * @param sortBy 정렬 기준 (선택 사항: "severity")
     * @return Mono<List<Alert>> 최근 알림 리스트 (최신순 또는 정렬 기준 적용)
     */
    @GetMapping("/alerts")
    public Mono<List<Alert>> getAlerts(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) String sortBy) {

        logger.debug("알림 조회 요청 수신 - limit={}, status={}, assignedTo={}, severity={}, sortBy={}",
                DEFAULT_ALERT_LIMIT, status, assignedTo, severity, sortBy);

        List<Alert> alerts;

        // 필터링 적용 (우선순위: status → assignedTo → severity)
        if (status != null) {
            // 상태별 필터링
            logger.debug("상태별 필터링 시작 - status={}", status);
            alerts = alertService.filterByStatus(status);
        } else if (assignedTo != null && !assignedTo.isEmpty()) {
            // 담당자별 필터링
            logger.debug("담당자별 필터링 시작 - assignedTo={}", assignedTo);
            alerts = alertService.filterByAssignee(assignedTo);
        } else if (severity != null) {
            // 심각도별 필터링
            logger.debug("심각도별 필터링 시작 - severity={}", severity);
            alerts = alertService.filterBySeverity(severity);
        } else {
            // 전체 알림 조회
            alerts = alertService.getRecentAlerts(DEFAULT_ALERT_LIMIT);
        }

        // 정렬 적용 (sortBy 파라미터)
        if ("severity".equalsIgnoreCase(sortBy)) {
            logger.debug("심각도별 정렬 시작 - 알림 개수={}", alerts.size());
            alerts = alertService.sortBySeverity(alerts);
        }

        logger.debug("알림 조회 완료 - 반환 개수={}", alerts.size());

        // Reactive 타입으로 반환 (WebFlux)
        return Mono.just(alerts);
    }

    /**
     * 알림 상태 변경 API
     *
     * PATCH /api/alerts/{alertId}/status
     * - 알림의 처리 상태를 변경합니다 (UNREAD → IN_PROGRESS → COMPLETED)
     * - COMPLETED 상태로 변경 시 processedAt 자동 설정
     *
     * 요청 예시:
     * {
     *   "status": "IN_PROGRESS"
     * }
     *
     * 응답 예시:
     * {
     *   "alertId": "uuid",
     *   "status": "IN_PROGRESS",
     *   "processedAt": "2025-11-11T10:30:00Z" (COMPLETED인 경우만)
     * }
     *
     * @param alertId 변경할 알림 ID
     * @param request 상태 변경 요청 (status 필드 포함)
     * @return ResponseEntity<UpdateStatusResponse> 상태 변경 결과
     *         - 200 OK: 상태 변경 성공
     *         - 400 Bad Request: 잘못된 요청 (alertId 또는 status가 null)
     *         - 404 Not Found: 알림을 찾을 수 없음
     */
    @PatchMapping("/alerts/{alertId}/status")
    public ResponseEntity<UpdateStatusResponse> updateAlertStatus(
            @PathVariable String alertId,
            @RequestBody UpdateStatusRequest request) {

        logger.info("알림 상태 변경 요청 수신 - alertId={}, status={}",
                alertId, request.getStatus());

        // 요청 검증
        if (alertId == null || alertId.isEmpty()) {
            logger.warn("알림 상태 변경 실패: alertId가 비어있음");
            return ResponseEntity.badRequest().build();
        }

        if (request.getStatus() == null) {
            logger.warn("알림 상태 변경 실패: status가 null - alertId={}", alertId);
            return ResponseEntity.badRequest().build();
        }

        // 상태 변경
        Alert updatedAlert = alertService.changeStatus(alertId, request.getStatus());

        if (updatedAlert == null) {
            logger.warn("알림 상태 변경 실패: 알림을 찾을 수 없음 - alertId={}", alertId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // 응답 생성
        UpdateStatusResponse response = new UpdateStatusResponse(
                updatedAlert.getAlertId(),
                updatedAlert.getStatus(),
                updatedAlert.getProcessedAt()
        );

        logger.info("알림 상태 변경 완료 - alertId={}, status={}, processedAt={}",
                response.getAlertId(), response.getStatus(), response.getProcessedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * 담당자 할당 API
     *
     * PATCH /api/alerts/{alertId}/assign
     * - 알림에 담당자를 할당합니다
     * - 담당자 이름은 최대 100자까지 허용
     *
     * 요청 예시:
     * {
     *   "assignedTo": "김보안"
     * }
     *
     * 응답 예시:
     * {
     *   "alertId": "uuid",
     *   "assignedTo": "김보안"
     * }
     *
     * @param alertId 할당할 알림 ID
     * @param request 담당자 할당 요청 (assignedTo 필드 포함)
     * @return ResponseEntity<AssignResponse> 담당자 할당 결과
     *         - 200 OK: 담당자 할당 성공
     *         - 400 Bad Request: 잘못된 요청 (alertId 또는 assignedTo가 null, 또는 100자 초과)
     *         - 404 Not Found: 알림을 찾을 수 없음
     */
    @PatchMapping("/alerts/{alertId}/assign")
    public ResponseEntity<AssignResponse> assignAlert(
            @PathVariable String alertId,
            @RequestBody AssignRequest request) {

        logger.info("담당자 할당 요청 수신 - alertId={}, assignedTo={}",
                alertId, request.getAssignedTo());

        // 요청 검증
        if (alertId == null || alertId.isEmpty()) {
            logger.warn("담당자 할당 실패: alertId가 비어있음");
            return ResponseEntity.badRequest().build();
        }

        if (request.getAssignedTo() == null || request.getAssignedTo().isEmpty()) {
            logger.warn("담당자 할당 실패: assignedTo가 비어있음 - alertId={}", alertId);
            return ResponseEntity.badRequest().build();
        }

        // 담당자 이름 길이 검증 (최대 100자)
        if (request.getAssignedTo().length() > 100) {
            logger.warn("담당자 할당 실패: assignedTo가 100자를 초과함 - alertId={}, 길이={}",
                    alertId, request.getAssignedTo().length());
            return ResponseEntity.badRequest().build();
        }

        // 담당자 할당
        Alert updatedAlert = alertService.assignAlert(alertId, request.getAssignedTo());

        if (updatedAlert == null) {
            logger.warn("담당자 할당 실패: 알림을 찾을 수 없음 - alertId={}", alertId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // 응답 생성
        AssignResponse response = new AssignResponse(
                updatedAlert.getAlertId(),
                updatedAlert.getAssignedTo()
        );

        logger.info("담당자 할당 완료 - alertId={}, assignedTo={}",
                response.getAlertId(), response.getAssignedTo());

        return ResponseEntity.ok(response);
    }

    /**
     * 조치 내용 기록 API
     *
     * POST /api/alerts/{alertId}/action
     * - 알림에 조치 내용을 기록하고, 선택적으로 완료 처리합니다
     * - 조치 내용은 최대 2000자까지 허용
     * - status="COMPLETED"를 전달하면 알림을 완료 처리
     *
     * 요청 예시 (완료 처리):
     * {
     *   "actionNote": "고객 확인 완료. 정상 거래로 판명됨.",
     *   "status": "COMPLETED"
     * }
     *
     * 요청 예시 (조치 내용만 기록):
     * {
     *   "actionNote": "고객에게 연락 시도 중"
     * }
     *
     * 응답 예시:
     * {
     *   "alertId": "uuid",
     *   "actionNote": "고객 확인 완료. 정상 거래로 판명됨.",
     *   "status": "COMPLETED",
     *   "processedAt": "2025-11-11T10:30:00Z"
     * }
     *
     * @param alertId 기록할 알림 ID
     * @param request 조치 기록 요청 (actionNote, status 필드 포함)
     * @return ResponseEntity<ActionResponse> 조치 기록 결과
     *         - 200 OK: 조치 기록 성공
     *         - 400 Bad Request: 잘못된 요청 (alertId 또는 actionNote가 null, 또는 2000자 초과)
     *         - 404 Not Found: 알림을 찾을 수 없음
     */
    @PostMapping("/alerts/{alertId}/action")
    public ResponseEntity<ActionResponse> recordAction(
            @PathVariable String alertId,
            @RequestBody ActionRequest request) {

        logger.info("조치 기록 요청 수신 - alertId={}, actionNote 길이={}, status={}",
                alertId,
                request.getActionNote() != null ? request.getActionNote().length() : 0,
                request.getStatus());

        // 요청 검증
        if (alertId == null || alertId.isEmpty()) {
            logger.warn("조치 기록 실패: alertId가 비어있음");
            return ResponseEntity.badRequest().build();
        }

        if (request.getActionNote() == null || request.getActionNote().isEmpty()) {
            logger.warn("조치 기록 실패: actionNote가 비어있음 - alertId={}", alertId);
            return ResponseEntity.badRequest().build();
        }

        // 조치 내용 길이 검증 (최대 2000자)
        if (request.getActionNote().length() > 2000) {
            logger.warn("조치 기록 실패: actionNote가 2000자를 초과함 - alertId={}, 길이={}",
                    alertId, request.getActionNote().length());
            return ResponseEntity.badRequest().build();
        }

        // 완료 처리 여부 판단
        boolean complete = (request.getStatus() == AlertStatus.COMPLETED);

        // 조치 내용 기록
        Alert updatedAlert = alertService.recordAction(alertId, request.getActionNote(), complete);

        if (updatedAlert == null) {
            logger.warn("조치 기록 실패: 알림을 찾을 수 없음 - alertId={}", alertId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // 응답 생성
        ActionResponse response = new ActionResponse(
                updatedAlert.getAlertId(),
                updatedAlert.getActionNote(),
                updatedAlert.getStatus(),
                updatedAlert.getProcessedAt()
        );

        logger.info("조치 기록 완료 - alertId={}, actionNote 길이={}, status={}, processedAt={}",
                response.getAlertId(),
                response.getActionNote() != null ? response.getActionNote().length() : 0,
                response.getStatus(),
                response.getProcessedAt());

        return ResponseEntity.ok(response);
    }
}
