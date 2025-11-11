package io.realfds.alert.repository

import io.realfds.alert.domain.Alert
import io.realfds.alert.domain.AlertStatus
import io.realfds.alert.domain.Severity
import io.realfds.alert.dto.AlertSearchCriteria
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.temporal.ChronoUnit

/**
 * CustomAlertRepository 구현체
 *
 * R2dbcEntityTemplate을 사용하여 동적 쿼리를 생성하고 실행합니다.
 * 검색 조건에 따라 WHERE 절을 동적으로 구성하며,
 * 페이지네이션과 정렬을 지원합니다.
 */
@Repository
class CustomAlertRepositoryImpl(
    private val template: R2dbcEntityTemplate
) : CustomAlertRepository {

    /**
     * 검색 조건에 따라 알림 목록을 조회합니다.
     *
     * 동적 쿼리 생성 로직:
     * 1. 기본 Criteria 객체 생성 (1=1)
     * 2. startDate가 있으면 alert_timestamp >= startDate 조건 추가
     * 3. endDate가 있으면 alert_timestamp <= endDate 조건 추가
     * 4. 페이지네이션 (LIMIT, OFFSET) 적용
     * 5. 정렬 (alert_timestamp DESC) 적용
     */
    override fun findByCriteria(criteria: AlertSearchCriteria): Flux<Alert> {
        // 동적 WHERE 절 생성
        val queryCriteria = buildCriteria(criteria)

        // Query 객체 생성 (정렬 + 페이지네이션)
        val query = Query.query(queryCriteria)
            .sort(org.springframework.data.domain.Sort.by("alertTimestamp").descending())
            .limit(criteria.size)
            .offset((criteria.page * criteria.size).toLong())

        // R2dbcEntityTemplate으로 쿼리 실행
        return template.select(query, Alert::class.java)
    }

    /**
     * 검색 조건에 맞는 알림의 전체 개수를 조회합니다.
     *
     * findByCriteria와 동일한 WHERE 절을 사용하되,
     * LIMIT/OFFSET 없이 전체 개수만 카운트합니다.
     */
    override fun countByCriteria(criteria: AlertSearchCriteria): Mono<Long> {
        // 동적 WHERE 절 생성 (findByCriteria와 동일)
        val queryCriteria = buildCriteria(criteria)

        // Query 객체 생성 (LIMIT/OFFSET 제외)
        val query = Query.query(queryCriteria)

        // R2dbcEntityTemplate으로 카운트 쿼리 실행
        return template.count(query, Alert::class.java)
    }

    /**
     * 검색 조건을 기반으로 Criteria 객체를 생성합니다.
     *
     * AlertSearchCriteria의 필드를 검사하여 값이 있으면
     * 해당 조건을 Criteria에 추가합니다.
     *
     * User Story 1: 날짜 범위 필터링
     * User Story 3: 규칙명, 사용자ID, 상태 필터 추가
     *
     * 동적 쿼리 생성:
     * - 선택된 필터만 WHERE 절에 포함
     * - 날짜 범위가 없으면 기본값으로 최근 7일 적용
     */
    private fun buildCriteria(criteria: AlertSearchCriteria): Criteria {
        var queryCriteria = Criteria.empty()

        // 시작 날짜 필터: alert_timestamp >= startDate
        criteria.startDate?.let { startDate ->
            queryCriteria = queryCriteria.and("alertTimestamp").greaterThanOrEquals(startDate)
        }

        // 종료 날짜 필터: alert_timestamp <= endDate
        criteria.endDate?.let { endDate ->
            queryCriteria = queryCriteria.and("alertTimestamp").lessThanOrEquals(endDate)
        }

        // 기본 날짜 범위 설정: 날짜 범위가 지정되지 않으면 최근 7일
        if (criteria.startDate == null && criteria.endDate == null) {
            val sevenDaysAgo = java.time.Instant.now().minus(7, ChronoUnit.DAYS)
            queryCriteria = queryCriteria.and("alertTimestamp").greaterThanOrEquals(sevenDaysAgo)
        }

        // 규칙명 필터: rule_name = :ruleName
        criteria.ruleName?.let { ruleName ->
            queryCriteria = queryCriteria.and("ruleName").`is`(ruleName)
        }

        // 사용자 ID 필터: user_id = :userId
        criteria.userId?.let { userId ->
            queryCriteria = queryCriteria.and("userId").`is`(userId)
        }

        // 상태 필터: status = :status
        criteria.status?.let { status ->
            queryCriteria = queryCriteria.and("status").`is`(status)
        }

        return queryCriteria
    }
}
