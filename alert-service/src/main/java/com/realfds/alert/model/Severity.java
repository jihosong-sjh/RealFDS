package com.realfds.alert.model;

/**
 * 알림 심각도를 나타내는 열거형
 *
 * 탐지 규칙(DetectionRule)에서 설정된 심각도가 Alert에 자동으로 복사됩니다.
 * 심각도는 우선순위 정렬 및 색상 코딩에 사용됩니다.
 *
 * <p>규칙별 Severity 매핑:</p>
 * <ul>
 *   <li>HIGH_VALUE (고액 거래): HIGH - 100만원 이상 고액 거래, 사기 가능성 높음</li>
 *   <li>FOREIGN_COUNTRY (해외 거래): MEDIUM - 해외 거래, 위험도 중간</li>
 *   <li>HIGH_FREQUENCY (빈번한 거래): HIGH - 1분 내 5회 이상 거래, 비정상 패턴</li>
 * </ul>
 *
 * <p>UI 색상 코딩:</p>
 * <ul>
 *   <li>LOW: 파란색 (#blue-600), 배경 연한 파란색 (#blue-50)</li>
 *   <li>MEDIUM: 노란색 (#yellow-600), 배경 연한 노란색 (#yellow-50)</li>
 *   <li>HIGH: 주황색 (#orange-600), 배경 연한 주황색 (#orange-50)</li>
 *   <li>CRITICAL: 빨간색 (#red-600), 배경 연한 빨간색 (#red-50)</li>
 * </ul>
 *
 * <p>정렬 순서:</p>
 * CRITICAL (4) → HIGH (3) → MEDIUM (2) → LOW (1)
 */
public enum Severity {
    /**
     * 낮음: 낮은 위험도
     */
    LOW("낮음", 1),

    /**
     * 보통: 중간 위험도
     */
    MEDIUM("보통", 2),

    /**
     * 높음: 높은 위험도
     */
    HIGH("높음", 3),

    /**
     * 긴급: 긴급 대응 필요
     */
    CRITICAL("긴급", 4);

    private final String displayName;
    private final int priority;

    /**
     * Severity 생성자
     *
     * @param displayName 한국어 표시명
     * @param priority 우선순위 (1=낮음, 2=보통, 3=높음, 4=긴급)
     */
    Severity(String displayName, int priority) {
        this.displayName = displayName;
        this.priority = priority;
    }

    /**
     * 한국어 표시명 반환
     *
     * @return 한국어 표시명 (예: "낮음", "보통", "높음", "긴급")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 우선순위 반환
     * 정렬 시 사용됩니다 (높은 숫자 = 높은 우선순위).
     *
     * @return 우선순위 (1-4)
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 문자열로부터 Severity 변환
     *
     * @param value 심각도 문자열 (예: "LOW", "MEDIUM", "HIGH", "CRITICAL")
     * @return 해당하는 Severity
     * @throws IllegalArgumentException 유효하지 않은 값인 경우
     */
    public static Severity fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Severity 값이 null입니다");
        }

        try {
            return Severity.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "유효하지 않은 Severity 값입니다: " + value +
                ". 허용되는 값: LOW, MEDIUM, HIGH, CRITICAL"
            );
        }
    }

    /**
     * 우선순위 기준 비교
     * 정렬 시 사용됩니다.
     *
     * @param other 비교할 다른 Severity
     * @return 이 Severity가 더 높으면 양수, 같으면 0, 낮으면 음수
     */
    public int compareTo(Severity other) {
        return Integer.compare(this.priority, other.priority);
    }
}
