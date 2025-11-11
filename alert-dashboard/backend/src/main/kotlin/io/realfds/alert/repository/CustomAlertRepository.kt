package io.realfds.alert.repository

import io.realfds.alert.domain.Alert
import io.realfds.alert.dto.AlertSearchCriteria
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * 커스텀 Alert Repository 인터페이스
 *
 * 동적 쿼리 생성 및 복잡한 검색 조건을 처리하는 메서드를 정의합니다.
 * 이 인터페이스는 CustomAlertRepositoryImpl에서 구현됩니다.
 *
 * Spring Data R2DBC의 기본 Repository 메서드로는 처리하기 어려운
 * 다중 필터, 동적 WHERE 절, 페이지네이션 등을 지원합니다.
 */
interface CustomAlertRepository {

    /**
     * 검색 조건에 따라 알림 목록을 조회합니다.
     *
     * AlertSearchCriteria의 필터 조건 (날짜 범위, 페이지네이션 등)을 기반으로
     * 동적 쿼리를 생성하여 알림을 검색합니다.
     * 결과는 alertTimestamp 내림차순(최신 순)으로 정렬됩니다.
     *
     * @param criteria 검색 조건 (날짜 범위, 페이지 번호, 페이지 크기)
     * @return 검색 조건에 맞는 알림 목록 (Flux)
     */
    fun findByCriteria(criteria: AlertSearchCriteria): Flux<Alert>

    /**
     * 검색 조건에 맞는 알림의 전체 개수를 조회합니다.
     *
     * 페이지네이션을 위해 전체 알림 개수를 계산합니다.
     * findByCriteria와 동일한 검색 조건을 사용하되,
     * LIMIT/OFFSET 없이 전체 개수만 카운트합니다.
     *
     * @param criteria 검색 조건 (날짜 범위)
     * @return 검색 조건에 맞는 전체 알림 개수 (Mono)
     */
    fun countByCriteria(criteria: AlertSearchCriteria): Mono<Long>
}
