package io.realfds.alert.dto

import io.realfds.alert.domain.AlertStatus
import java.time.Instant

/**
 * 알림 검색 조건 DTO
 *
 * 이 클래스는 알림 이력 검색 시 사용되는 필터 조건을 정의합니다.
 * User Story 1에서는 날짜 범위와 페이지네이션만 지원하며,
 * User Story 3에서 규칙명, 사용자ID, 상태 필터가 추가될 예정입니다.
 *
 * @property startDate 검색 시작 날짜 (포함), null이면 기본값: 7일 전
 * @property endDate 검색 종료 날짜 (포함), null이면 기본값: 현재
 * @property page 페이지 번호 (0부터 시작), 기본값: 0
 * @property size 페이지 크기 (1~100), 기본값: 50
 */
data class AlertSearchCriteria(
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    val page: Int = 0,
    val size: Int = 50
) {
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
    }
}
