package io.realfds.alert.service

import io.realfds.alert.dto.AlertSearchCriteria
import io.realfds.alert.dto.PagedAlertResult
import io.realfds.alert.repository.CustomAlertRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Alert History 서비스
 *
 * 알림 이력 조회 비즈니스 로직을 담당합니다.
 * 검색 조건에 따라 알림을 조회하고, 페이지네이션 결과를 반환합니다.
 */
@Service
class AlertHistoryService(
    private val customAlertRepository: CustomAlertRepository
) {
    private val logger = LoggerFactory.getLogger(AlertHistoryService::class.java)

    /**
     * 검색 조건에 따라 알림 이력을 조회합니다.
     *
     * 날짜 범위가 지정되지 않으면 기본적으로 최근 7일간의 알림을 조회합니다.
     * 결과는 알림 발생 시각 기준 내림차순(최신 순)으로 정렬됩니다.
     *
     * @param criteria 검색 조건 (날짜 범위, 페이지 번호, 페이지 크기)
     * @return 페이지네이션된 알림 검색 결과
     */
    fun searchAlerts(criteria: AlertSearchCriteria): Mono<PagedAlertResult> {
        val startTime = Instant.now()

        // 기본 날짜 범위 설정: 날짜가 지정되지 않으면 최근 7일
        val effectiveCriteria = applyDefaultDateRange(criteria)

        logger.info("알림 이력 검색 시작: criteria={}", effectiveCriteria)

        // 알림 목록과 전체 개수를 병렬로 조회
        return Mono.zip(
            customAlertRepository.findByCriteria(effectiveCriteria).collectList(),
            customAlertRepository.countByCriteria(effectiveCriteria)
        )
            .map { tuple ->
                val content = tuple.t1
                val totalElements = tuple.t2

                // 페이지네이션 메타데이터 계산
                val totalPages = if (totalElements == 0L) 0
                else ((totalElements + effectiveCriteria.size - 1) / effectiveCriteria.size).toInt()

                PagedAlertResult(
                    content = content,
                    totalElements = totalElements,
                    totalPages = totalPages,
                    currentPage = effectiveCriteria.page,
                    pageSize = effectiveCriteria.size
                )
            }
            .doOnSuccess { result ->
                val duration = Duration.between(startTime, Instant.now()).toMillis()
                logger.info("알림 이력 검색 완료: {}개의 알림을 {}ms에 조회했습니다", result.content.size, duration)
            }
            .doOnError { error ->
                logger.error("알림 이력 검색 실패: criteria={}", effectiveCriteria, error)
            }
            .onErrorResume { error ->
                logger.error("알림 검색 중 오류 발생", error)
                Mono.error(DatabaseConnectionException("알림 검색에 실패했습니다", error))
            }
    }

    /**
     * 기본 날짜 범위를 적용합니다.
     *
     * startDate와 endDate가 모두 null이면 최근 7일을 기본 날짜 범위로 설정합니다.
     * 이는 사용자가 날짜 범위를 지정하지 않았을 때 너무 많은 데이터를 조회하는 것을 방지합니다.
     */
    private fun applyDefaultDateRange(criteria: AlertSearchCriteria): AlertSearchCriteria {
        return if (criteria.startDate == null && criteria.endDate == null) {
            val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS)
            val now = Instant.now()
            criteria.copy(startDate = sevenDaysAgo, endDate = now)
        } else {
            criteria
        }
    }
}

/**
 * 데이터베이스 연결 예외
 *
 * 데이터베이스 조회 중 발생한 오류를 래핑하는 예외입니다.
 */
class DatabaseConnectionException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
