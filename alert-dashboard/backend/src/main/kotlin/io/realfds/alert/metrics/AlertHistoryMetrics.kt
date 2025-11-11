package io.realfds.alert.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Alert History 메트릭 수집기
 *
 * Micrometer를 사용하여 알림 이력 조회 및 영속화 관련 메트릭을 수집합니다.
 * 수집된 메트릭은 Prometheus 형식으로 /actuator/prometheus 엔드포인트를 통해 노출됩니다.
 *
 * Constitution V: 관찰 가능성(Observability) 필수
 *
 * @property meterRegistry Micrometer 메트릭 레지스트리
 */
@Component
class AlertHistoryMetrics(
    private val meterRegistry: MeterRegistry
) {

    // 알림 이력 검색 카운터
    private val searchCounter: Counter = Counter.builder("alert.history.search.count")
        .description("알림 이력 검색 요청 횟수")
        .tag("service", "alert-dashboard")
        .register(meterRegistry)

    // 알림 이력 검색 성공 카운터
    private val searchSuccessCounter: Counter = Counter.builder("alert.history.search.success.count")
        .description("알림 이력 검색 성공 횟수")
        .tag("service", "alert-dashboard")
        .register(meterRegistry)

    // 알림 이력 검색 실패 카운터
    private val searchFailureCounter: Counter = Counter.builder("alert.history.search.failure.count")
        .description("알림 이력 검색 실패 횟수")
        .tag("service", "alert-dashboard")
        .register(meterRegistry)

    // 알림 이력 검색 응답 시간 타이머
    private val searchTimer: Timer = Timer.builder("alert.history.search.duration")
        .description("알림 이력 검색 응답 시간 (밀리초)")
        .tag("service", "alert-dashboard")
        .register(meterRegistry)

    // 알림 영속화 성공 카운터
    private val persistenceSuccessCounter: Counter = Counter.builder("alert.persistence.success.count")
        .description("알림 저장 성공 횟수")
        .tag("service", "alert-dashboard")
        .register(meterRegistry)

    // 알림 영속화 실패 카운터
    private val persistenceFailureCounter: Counter = Counter.builder("alert.persistence.failure.count")
        .description("알림 저장 실패 횟수")
        .tag("service", "alert-dashboard")
        .register(meterRegistry)

    // 알림 영속화 재시도 카운터
    private val persistenceRetryCounter: Counter = Counter.builder("alert.persistence.retry.count")
        .description("알림 저장 재시도 횟수")
        .tag("service", "alert-dashboard")
        .register(meterRegistry)

    /**
     * 알림 이력 검색 요청을 기록합니다.
     *
     * 검색 요청이 시작될 때 호출되어 검색 횟수를 증가시킵니다.
     */
    fun recordSearchRequest() {
        searchCounter.increment()
    }

    /**
     * 알림 이력 검색 성공을 기록합니다.
     *
     * @param duration 검색 소요 시간
     * @param resultCount 검색 결과 개수
     */
    fun recordSearchSuccess(duration: Duration, resultCount: Int) {
        searchSuccessCounter.increment()
        searchTimer.record(duration)

        // 결과 개수별 메트릭 추가
        Counter.builder("alert.history.search.results")
            .description("검색된 알림 개수")
            .tag("service", "alert-dashboard")
            .register(meterRegistry)
            .increment(resultCount.toDouble())
    }

    /**
     * 알림 이력 검색 실패를 기록합니다.
     *
     * @param errorType 에러 타입 (예: DatabaseConnectionException, IllegalArgumentException)
     */
    fun recordSearchFailure(errorType: String) {
        Counter.builder("alert.history.search.failure.count")
            .description("알림 이력 검색 실패 횟수")
            .tag("service", "alert-dashboard")
            .tag("error_type", errorType)
            .register(meterRegistry)
            .increment()

        searchFailureCounter.increment()
    }

    /**
     * 알림 영속화 성공을 기록합니다.
     *
     * PostgreSQL에 알림이 성공적으로 저장되었을 때 호출됩니다.
     */
    fun recordPersistenceSuccess() {
        persistenceSuccessCounter.increment()
    }

    /**
     * 알림 영속화 실패를 기록합니다.
     *
     * PostgreSQL 저장이 실패했을 때 호출됩니다.
     *
     * @param errorType 에러 타입 (예: R2dbcException, TimeoutException)
     */
    fun recordPersistenceFailure(errorType: String) {
        Counter.builder("alert.persistence.failure.count")
            .description("알림 저장 실패 횟수")
            .tag("service", "alert-dashboard")
            .tag("error_type", errorType)
            .register(meterRegistry)
            .increment()

        persistenceFailureCounter.increment()
    }

    /**
     * 알림 영속화 재시도를 기록합니다.
     *
     * 저장 실패 후 재시도가 발생할 때 호출됩니다.
     *
     * @param attemptNumber 재시도 횟수 (1, 2, 3)
     */
    fun recordPersistenceRetry(attemptNumber: Int) {
        Counter.builder("alert.persistence.retry.count")
            .description("알림 저장 재시도 횟수")
            .tag("service", "alert-dashboard")
            .tag("attempt", attemptNumber.toString())
            .register(meterRegistry)
            .increment()

        persistenceRetryCounter.increment()
    }

    /**
     * 비즈니스 메트릭: 빈 검색 결과를 기록합니다.
     *
     * 검색 조건에 맞는 알림이 없을 때 호출되어 오탐 분석에 활용됩니다.
     */
    fun recordEmptySearchResult() {
        Counter.builder("alert.history.search.empty.count")
            .description("검색 결과가 없는 경우 횟수")
            .tag("service", "alert-dashboard")
            .register(meterRegistry)
            .increment()
    }

    /**
     * 타이머를 사용하여 작업 시간을 측정하고 메트릭을 기록합니다.
     *
     * 사용 예시:
     * ```
     * alertHistoryMetrics.recordSearchDuration {
     *     // 검색 로직
     * }
     * ```
     *
     * @param block 측정할 작업
     * @return 작업 결과
     */
    fun <T> recordSearchDuration(block: () -> T): T {
        val startTime = System.nanoTime()
        return try {
            block()
        } finally {
            val duration = Duration.ofNanos(System.nanoTime() - startTime)
            searchTimer.record(duration)
        }
    }
}
