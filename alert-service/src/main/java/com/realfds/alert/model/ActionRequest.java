package com.realfds.alert.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 조치 내용 기록 요청 모델
 *
 * POST /api/alerts/{alertId}/action 엔드포인트의 요청 본문
 *
 * 예시:
 * {
 *   "actionNote": "고객 확인 완료. 정상 거래로 판명됨.",
 *   "status": "COMPLETED"
 * }
 *
 * status 필드는 선택 사항:
 * - "COMPLETED"를 전달하면 알림을 완료 처리
 * - null이거나 생략하면 상태는 변경되지 않음
 */
public class ActionRequest {

    @JsonProperty("actionNote")
    private String actionNote;

    @JsonProperty("status")
    private AlertStatus status;

    // 기본 생성자 (Jackson 역직렬화용)
    public ActionRequest() {
    }

    // 전체 필드 생성자
    public ActionRequest(String actionNote, AlertStatus status) {
        this.actionNote = actionNote;
        this.status = status;
    }

    /**
     * 조치 내용 반환
     *
     * @return 조치 내용 (최대 2000자)
     */
    public String getActionNote() {
        return actionNote;
    }

    /**
     * 조치 내용 설정
     *
     * @param actionNote 조치 내용 (최대 2000자)
     */
    public void setActionNote(String actionNote) {
        this.actionNote = actionNote;
    }

    /**
     * 상태 반환
     *
     * @return 상태 (COMPLETED 또는 null)
     */
    public AlertStatus getStatus() {
        return status;
    }

    /**
     * 상태 설정
     *
     * @param status 상태 (COMPLETED 또는 null)
     */
    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ActionRequest{" +
                "actionNote='" + actionNote + '\'' +
                ", status=" + status +
                '}';
    }
}
