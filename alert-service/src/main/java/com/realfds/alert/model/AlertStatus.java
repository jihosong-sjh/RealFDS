package com.realfds.alert.model;

/**
 * 알림 처리 상태를 나타내는 열거형
 *
 * 알림의 라이프사이클에 따라 미확인 → 확인중 → 완료 상태로 전이됩니다.
 * 역방향 전이도 가능합니다 (예: 완료 → 확인중).
 *
 * <p>상태 전이 규칙:</p>
 * <ul>
 *   <li>UNREAD → IN_PROGRESS: 담당자가 알림을 확인하고 조사 시작</li>
 *   <li>UNREAD → COMPLETED: 직접 완료 처리 (조사 불필요)</li>
 *   <li>IN_PROGRESS → COMPLETED: 조사 완료 후 처리</li>
 *   <li>COMPLETED → IN_PROGRESS: 재조사 필요 시 역방향 가능</li>
 *   <li>IN_PROGRESS → UNREAD: 취소 시 역방향 가능</li>
 * </ul>
 *
 * <p>UI 표시:</p>
 * <ul>
 *   <li>UNREAD: "미확인" (회색 배지)</li>
 *   <li>IN_PROGRESS: "확인중" (파란색 배지)</li>
 *   <li>COMPLETED: "완료" (초록색 배지)</li>
 * </ul>
 */
public enum AlertStatus {
    /**
     * 미확인: 신규 알림, 아직 확인하지 않음
     */
    UNREAD("미확인"),

    /**
     * 확인중: 담당자가 확인하고 조사 중
     */
    IN_PROGRESS("확인중"),

    /**
     * 완료: 조치 완료, processedAt 자동 설정
     */
    COMPLETED("완료");

    private final String displayName;

    /**
     * AlertStatus 생성자
     *
     * @param displayName 한국어 표시명
     */
    AlertStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 한국어 표시명 반환
     *
     * @return 한국어 표시명 (예: "미확인", "확인중", "완료")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 문자열로부터 AlertStatus 변환
     *
     * @param value 상태 문자열 (예: "UNREAD", "IN_PROGRESS", "COMPLETED")
     * @return 해당하는 AlertStatus
     * @throws IllegalArgumentException 유효하지 않은 값인 경우
     */
    public static AlertStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("AlertStatus 값이 null입니다");
        }

        try {
            return AlertStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "유효하지 않은 AlertStatus 값입니다: " + value +
                ". 허용되는 값: UNREAD, IN_PROGRESS, COMPLETED"
            );
        }
    }
}
