package io.realfds.alert.dto

import io.realfds.alert.domain.Alert

/**
 * 페이지네이션 알림 검색 결과 DTO
 *
 * 이 클래스는 알림 검색 결과와 페이지네이션 메타데이터를 함께 반환합니다.
 * 프론트엔드에서 페이지네이션 UI를 구현하는 데 필요한 모든 정보를 제공합니다.
 *
 * @property content 현재 페이지의 알림 목록
 * @property totalElements 검색 조건에 맞는 전체 알림 개수
 * @property totalPages 전체 페이지 수
 * @property currentPage 현재 페이지 번호 (0부터 시작)
 * @property pageSize 페이지 크기 (한 페이지에 표시되는 알림 개수)
 * @property hasNext 다음 페이지 존재 여부 (페이지네이션 UI에서 "다음" 버튼 활성화 판단)
 * @property hasPrevious 이전 페이지 존재 여부 (페이지네이션 UI에서 "이전" 버튼 활성화 판단)
 */
data class PagedAlertResult(
    val content: List<Alert>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int
) {
    /**
     * 다음 페이지가 존재하는지 여부
     * 현재 페이지가 마지막 페이지가 아니면 true
     */
    val hasNext: Boolean = currentPage < totalPages - 1

    /**
     * 이전 페이지가 존재하는지 여부
     * 현재 페이지가 첫 페이지가 아니면 true
     */
    val hasPrevious: Boolean = currentPage > 0
}
