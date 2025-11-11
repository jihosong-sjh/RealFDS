package io.realfds.alert.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * AlertHistoryMetrics 단위 테스트
 *
 * 메트릭 수집 로직이 올바르게 동작하는지 검증합니다.
 */
class AlertHistoryMetricsTest {

    private lateinit var meterRegistry: SimpleMeterRegistry
    private lateinit var alertHistoryMetrics: AlertHistoryMetrics

    @BeforeEach
    fun setUp() {
        // SimpleMeterRegistry: 테스트용 간단한 메트릭 레지스트리
        meterRegistry = SimpleMeterRegistry()
        alertHistoryMetrics = AlertHistoryMetrics(meterRegistry)
    }

    /**
     * Given: 검색 요청이 발생할 때
     * When: recordSearchRequest()를 호출하면
     * Then: 검색 요청 카운터가 증가해야 합니다
     */
    @Test
    fun `should increment search request counter`() {
        // Given: 초기 카운터 값은 0
        val initialCount = getCounterValue("alert.history.search.count")
        assertThat(initialCount).isEqualTo(0.0)

        // When: 검색 요청 기록
        alertHistoryMetrics.recordSearchRequest()

        // Then: 카운터가 1 증가
        val updatedCount = getCounterValue("alert.history.search.count")
        assertThat(updatedCount).isEqualTo(1.0)
    }

    /**
     * Given: 검색이 성공했을 때
     * When: recordSearchSuccess()를 호출하면
     * Then: 검색 성공 카운터와 타이머가 기록되어야 합니다
     */
    @Test
    fun `should record search success with duration`() {
        // Given: 초기 상태
        val initialSuccessCount = getCounterValue("alert.history.search.success.count")
        assertThat(initialSuccessCount).isEqualTo(0.0)

        // When: 검색 성공 기록 (100ms, 결과 5개)
        val duration = Duration.ofMillis(100)
        alertHistoryMetrics.recordSearchSuccess(duration, 5)

        // Then: 성공 카운터가 1 증가
        val updatedSuccessCount = getCounterValue("alert.history.search.success.count")
        assertThat(updatedSuccessCount).isEqualTo(1.0)

        // Then: 타이머가 기록됨
        val timer = meterRegistry.find("alert.history.search.duration").timer()
        assertThat(timer).isNotNull
        assertThat(timer!!.count()).isEqualTo(1)
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(100.0)
    }

    /**
     * Given: 검색이 실패했을 때
     * When: recordSearchFailure()를 호출하면
     * Then: 검색 실패 카운터가 증가해야 합니다
     */
    @Test
    fun `should increment search failure counter with error type`() {
        // Given: 초기 상태
        val initialFailureCount = getCounterValue("alert.history.search.failure.count")
        assertThat(initialFailureCount).isEqualTo(0.0)

        // When: 검색 실패 기록 (DatabaseConnectionException)
        alertHistoryMetrics.recordSearchFailure("DatabaseConnectionException")

        // Then: 실패 카운터가 1 증가
        val updatedFailureCount = getCounterValue("alert.history.search.failure.count")
        assertThat(updatedFailureCount).isGreaterThanOrEqualTo(1.0)
    }

    /**
     * Given: 검색 결과가 비어있을 때
     * When: recordEmptySearchResult()를 호출하면
     * Then: 빈 결과 카운터가 증가해야 합니다
     */
    @Test
    fun `should increment empty search result counter`() {
        // When: 빈 결과 기록
        alertHistoryMetrics.recordEmptySearchResult()

        // Then: 빈 결과 카운터가 1 증가
        val emptyCount = getCounterValue("alert.history.search.empty.count")
        assertThat(emptyCount).isEqualTo(1.0)
    }

    /**
     * Given: 알림 저장이 성공했을 때
     * When: recordPersistenceSuccess()를 호출하면
     * Then: 저장 성공 카운터가 증가해야 합니다
     */
    @Test
    fun `should increment persistence success counter`() {
        // Given: 초기 상태
        val initialCount = getCounterValue("alert.persistence.success.count")
        assertThat(initialCount).isEqualTo(0.0)

        // When: 저장 성공 기록
        alertHistoryMetrics.recordPersistenceSuccess()

        // Then: 카운터가 1 증가
        val updatedCount = getCounterValue("alert.persistence.success.count")
        assertThat(updatedCount).isEqualTo(1.0)
    }

    /**
     * Given: 알림 저장이 실패했을 때
     * When: recordPersistenceFailure()를 호출하면
     * Then: 저장 실패 카운터가 증가해야 합니다
     */
    @Test
    fun `should increment persistence failure counter with error type`() {
        // Given: 초기 상태
        val initialCount = getCounterValue("alert.persistence.failure.count")
        assertThat(initialCount).isEqualTo(0.0)

        // When: 저장 실패 기록 (R2dbcException)
        alertHistoryMetrics.recordPersistenceFailure("R2dbcException")

        // Then: 카운터가 1 이상 증가
        val updatedCount = getCounterValue("alert.persistence.failure.count")
        assertThat(updatedCount).isGreaterThanOrEqualTo(1.0)
    }

    /**
     * Given: 알림 저장 재시도가 발생했을 때
     * When: recordPersistenceRetry()를 호출하면
     * Then: 재시도 카운터가 증가해야 합니다
     */
    @Test
    fun `should increment persistence retry counter`() {
        // Given: 초기 상태
        val initialCount = getCounterValue("alert.persistence.retry.count")
        assertThat(initialCount).isEqualTo(0.0)

        // When: 재시도 기록 (1차 재시도)
        alertHistoryMetrics.recordPersistenceRetry(1)

        // Then: 카운터가 1 이상 증가
        val updatedCount = getCounterValue("alert.persistence.retry.count")
        assertThat(updatedCount).isGreaterThanOrEqualTo(1.0)
    }

    /**
     * 카운터 값을 조회하는 헬퍼 메서드
     *
     * @param counterName 카운터 이름
     * @return 카운터 값 (없으면 0.0)
     */
    private fun getCounterValue(counterName: String): Double {
        return meterRegistry.find(counterName).counter()?.count() ?: 0.0
    }
}
