package com.realfds.alert.model;

import java.time.Instant;

/**
 * UpdateStatusResponse - 알림 상태 변경 응답 DTO
 *
 * PATCH /api/alerts/{alertId}/status 엔드포인트의 응답 바디입니다.
 *
 * 예시:
 * {
 *   "alertId": "uuid",
 *   "status": "IN_PROGRESS",
 *   "processedAt": "2025-11-11T10:30:00Z" (COMPLETED인 경우만)
 * }
 */
public class UpdateStatusResponse {

    private String alertId;
    private AlertStatus status;
    private Instant processedAt;

    /**
     * 기본 생성자 (Jackson 직렬화용)
     */
    public UpdateStatusResponse() {
    }

    /**
     * 전체 필드 생성자
     *
     * @param alertId 알림 ID
     * @param status 변경된 상태
     * @param processedAt 처리 완료 시각 (COMPLETED 상태인 경우만)
     */
    public UpdateStatusResponse(String alertId, AlertStatus status, Instant processedAt) {
        this.alertId = alertId;
        this.status = status;
        this.processedAt = processedAt;
    }

    /**
     * 알림 ID 반환
     *
     * @return 알림 ID
     */
    public String getAlertId() {
        return alertId;
    }

    /**
     * 알림 ID 설정
     *
     * @param alertId 알림 ID
     */
    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    /**
     * 상태 반환
     *
     * @return 변경된 상태
     */
    public AlertStatus getStatus() {
        return status;
    }

    /**
     * 상태 설정
     *
     * @param status 변경된 상태
     */
    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    /**
     * 처리 완료 시각 반환
     *
     * @return 처리 완료 시각 (COMPLETED 상태인 경우만)
     */
    public Instant getProcessedAt() {
        return processedAt;
    }

    /**
     * 처리 완료 시각 설정
     *
     * @param processedAt 처리 완료 시각
     */
    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    @Override
    public String toString() {
        return "UpdateStatusResponse{" +
                "alertId='" + alertId + '\'' +
                ", status=" + status +
                ", processedAt=" + processedAt +
                '}';
    }
}
