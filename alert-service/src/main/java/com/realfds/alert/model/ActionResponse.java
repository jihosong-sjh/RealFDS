package com.realfds.alert.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * 조치 내용 기록 응답 모델
 *
 * POST /api/alerts/{alertId}/action 엔드포인트의 응답
 *
 * 예시:
 * {
 *   "alertId": "uuid",
 *   "actionNote": "고객 확인 완료. 정상 거래로 판명됨.",
 *   "status": "COMPLETED",
 *   "processedAt": "2025-11-11T10:30:00Z"
 * }
 */
public class ActionResponse {

    @JsonProperty("alertId")
    private String alertId;

    @JsonProperty("actionNote")
    private String actionNote;

    @JsonProperty("status")
    private AlertStatus status;

    @JsonProperty("processedAt")
    private Instant processedAt;

    // 기본 생성자 (Jackson 직렬화용)
    public ActionResponse() {
    }

    // 전체 필드 생성자
    public ActionResponse(String alertId, String actionNote, AlertStatus status, Instant processedAt) {
        this.alertId = alertId;
        this.actionNote = actionNote;
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
     * 조치 내용 반환
     *
     * @return 조치 내용
     */
    public String getActionNote() {
        return actionNote;
    }

    /**
     * 조치 내용 설정
     *
     * @param actionNote 조치 내용
     */
    public void setActionNote(String actionNote) {
        this.actionNote = actionNote;
    }

    /**
     * 상태 반환
     *
     * @return 상태
     */
    public AlertStatus getStatus() {
        return status;
    }

    /**
     * 상태 설정
     *
     * @param status 상태
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
        return "ActionResponse{" +
                "alertId='" + alertId + '\'' +
                ", actionNote='" + actionNote + '\'' +
                ", status=" + status +
                ", processedAt=" + processedAt +
                '}';
    }
}
