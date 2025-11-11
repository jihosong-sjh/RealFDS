package com.realfds.alert.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 담당자 할당 응답 모델
 *
 * PATCH /api/alerts/{alertId}/assign 엔드포인트의 응답
 *
 * 예시:
 * {
 *   "alertId": "uuid",
 *   "assignedTo": "김보안"
 * }
 */
public class AssignResponse {

    @JsonProperty("alertId")
    private String alertId;

    @JsonProperty("assignedTo")
    private String assignedTo;

    // 기본 생성자 (Jackson 직렬화용)
    public AssignResponse() {
    }

    // 전체 필드 생성자
    public AssignResponse(String alertId, String assignedTo) {
        this.alertId = alertId;
        this.assignedTo = assignedTo;
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
     * 담당자 이름 반환
     *
     * @return 담당자 이름
     */
    public String getAssignedTo() {
        return assignedTo;
    }

    /**
     * 담당자 이름 설정
     *
     * @param assignedTo 담당자 이름
     */
    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    @Override
    public String toString() {
        return "AssignResponse{" +
                "alertId='" + alertId + '\'' +
                ", assignedTo='" + assignedTo + '\'' +
                '}';
    }
}
