package com.realfds.alert.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 담당자 할당 요청 모델
 *
 * PATCH /api/alerts/{alertId}/assign 엔드포인트의 요청 본문
 *
 * 예시:
 * {
 *   "assignedTo": "김보안"
 * }
 */
public class AssignRequest {

    @JsonProperty("assignedTo")
    private String assignedTo;

    // 기본 생성자 (Jackson 역직렬화용)
    public AssignRequest() {
    }

    // 전체 필드 생성자
    public AssignRequest(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    /**
     * 담당자 이름 반환
     *
     * @return 담당자 이름 (최대 100자)
     */
    public String getAssignedTo() {
        return assignedTo;
    }

    /**
     * 담당자 이름 설정
     *
     * @param assignedTo 담당자 이름 (최대 100자)
     */
    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    @Override
    public String toString() {
        return "AssignRequest{" +
                "assignedTo='" + assignedTo + '\'' +
                '}';
    }
}
