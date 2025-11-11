package io.realfds.alert.dto

import io.realfds.alert.domain.AlertStatus
import java.time.Instant

/**
 * 알림 검색 조건 DTO
 *
 * 이 클래스는 알림 이력 검색 시 사용되는 필터 조건을 정의합니다.
 * User Story 1: 날짜 범위와 페이지네이션
 * User Story 3: 규칙명, 사용자ID, 상태 필터 지원
 *
 * @property startDate 검색 시작 날짜 (포함), null이면 기본값: 7일 전
 * @property endDate 검색 종료 날짜 (포함), null이면 기본값: 현재
 * @property ruleName 탐지 규칙명 필터 (HIGH_AMOUNT, FOREIGN_COUNTRY, RAPID_TRANSACTION), nullable
 * @property userId 사용자 ID 필터 (예: user-1), nullable
 * @property status 알림 상태 필터 (UNREAD, IN_PROGRESS, COMPLETED), nullable
 * @property page 페이지 번호 (0부터 시작), 기본값: 0
 * @property size 페이지 크기 (1~100), 기본값: 50
 */
data class AlertSearchCriteria(
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    val ruleName: String? = null,
    val userId: String? = null,
    val status: AlertStatus? = null,
    val page: Int = 0,
    val size: Int = 50
) {
    // 허용되는 규칙명 목록
    companion object {
        private val VALID_RULE_NAMES = setOf("HIGH_AMOUNT", "FOREIGN_COUNTRY", "RAPID_TRANSACTION")
        private val USER_ID_REGEX = Regex("^[a-zA-Z0-9-]+$")
    }

    init {
        // 페이지 번호 검증: 0 이상이어야 함
        require(page >= 0) { "페이지 번호는 0 이상이어야 합니다" }

        // 페이지 크기 검증: 1~100 범위
        require(size in 1..100) { "페이지 크기는 1~100 사이여야 합니다" }

        // 날짜 범위 검증: startDate가 endDate보다 늦을 수 없음
        if (startDate != null && endDate != null) {
            require(startDate <= endDate) { "시작 날짜는 종료 날짜보다 늦을 수 없습니다" }
        }

        // 미래 날짜 검증: startDate와 endDate는 현재 시각 이전이어야 함
        val now = Instant.now()
        if (startDate != null) {
            require(startDate <= now) { "시작 날짜는 미래일 수 없습니다" }
        }
        if (endDate != null) {
            require(endDate <= now) { "종료 날짜는 미래일 수 없습니다" }
        }

        // 규칙명 검증: 허용된 Enum 값만 허용
        if (ruleName != null) {
            require(ruleName in VALID_RULE_NAMES) {
                "규칙명은 ${VALID_RULE_NAMES.joinToString(", ")} 중 하나여야 합니다"
            }
        }

        // 사용자 ID 검증: 영문자, 숫자, 하이픈만 허용
        if (userId != null) {
            require(USER_ID_REGEX.matches(userId)) {
                "사용자 ID는 영문자, 숫자, 하이픈만 포함할 수 있습니다"
            }
        }

        // 상태 검증: AlertStatus Enum을 사용하므로 타입 체크로 자동 검증됨
    }
}
