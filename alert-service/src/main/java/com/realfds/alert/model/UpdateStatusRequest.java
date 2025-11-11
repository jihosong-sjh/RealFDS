package com.realfds.alert.model;

/**
 * UpdateStatusRequest - 알림 상태 변경 요청 DTO
 *
 * PATCH /api/alerts/{alertId}/status 엔드포인트의 요청 바디입니다.
 *
 * 예시:
 * {
 *   "status": "IN_PROGRESS"
 * }
 */
public class UpdateStatusRequest {

    private AlertStatus status;

    /**
     * 기본 생성자 (Jackson 역직렬화용)
     */
    public UpdateStatusRequest() {
    }

    /**
     * 상태 설정 생성자
     *
     * @param status 변경할 상태 (UNREAD, IN_PROGRESS, COMPLETED)
     */
    public UpdateStatusRequest(AlertStatus status) {
        this.status = status;
    }

    /**
     * 상태 반환
     *
     * @return 변경할 상태
     */
    public AlertStatus getStatus() {
        return status;
    }

    /**
     * 상태 설정
     *
     * @param status 변경할 상태
     */
    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "UpdateStatusRequest{" +
                "status=" + status +
                '}';
    }
}
