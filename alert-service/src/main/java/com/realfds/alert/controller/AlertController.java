package com.realfds.alert.controller;

import com.realfds.alert.model.Alert;
import com.realfds.alert.model.AlertStatus;
import com.realfds.alert.model.UpdateStatusRequest;
import com.realfds.alert.model.UpdateStatusResponse;
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
     * @param status 필터링할 상태 (선택 사항: UNREAD, IN_PROGRESS, COMPLETED)
     * @return Mono<List<Alert>> 최근 알림 리스트 (최신순)
     */
    @GetMapping("/alerts")
    public Mono<List<Alert>> getAlerts(
            @RequestParam(required = false) AlertStatus status) {

        logger.debug("알림 조회 요청 수신 - limit={}, status={}",
                DEFAULT_ALERT_LIMIT, status);

        List<Alert> alerts;

        if (status != null) {
            // 상태별 필터링
            logger.debug("상태별 필터링 시작 - status={}", status);
            alerts = alertService.filterByStatus(status);
        } else {
            // 전체 알림 조회
            alerts = alertService.getRecentAlerts(DEFAULT_ALERT_LIMIT);
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
}
