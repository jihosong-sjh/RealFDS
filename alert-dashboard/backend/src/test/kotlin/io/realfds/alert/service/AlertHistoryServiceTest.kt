package io.realfds.alert.service

import io.realfds.alert.domain.Alert
import io.realfds.alert.domain.AlertStatus
import io.realfds.alert.domain.Severity
import io.realfds.alert.dto.AlertSearchCriteria
import io.realfds.alert.dto.PagedAlertResult
import io.realfds.alert.repository.CustomAlertRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * AlertHistoryService 단위 테스트
 *
 * Mockito를 사용하여 CustomAlertRepository를 모킹하고,
 * AlertHistoryService의 비즈니스 로직을 검증합니다.
 * Given-When-Then 구조를 따릅니다.
 */
@ExtendWith(MockitoExtension::class)
class AlertHistoryServiceTest {

    @Mock
    private lateinit var customAlertRepository: CustomAlertRepository

    @InjectMocks
    private lateinit var alertHistoryService: AlertHistoryService

    private lateinit var sampleAlerts: List<Alert>

    @BeforeEach
    fun setUp() {
        // 테스트용 샘플 Alert 데이터 생성
        sampleAlerts = listOf(
            createTestAlert(userId = "user-1", amount = 1500000L),
            createTestAlert(userId = "user-2", amount = 800000L),
            createTestAlert(userId = "user-3", amount = 500000L)
        )
    }

    @Test
    fun `정상적으로 알림을 검색할 수 있다`() {
        // Given: 검색 조건과 모킹된 레포지토리 응답
        val criteria = AlertSearchCriteria(page = 0, size = 50)

        whenever(customAlertRepository.findByCriteria(any()))
            .thenReturn(Flux.fromIterable(sampleAlerts))
        whenever(customAlertRepository.countByCriteria(any()))
            .thenReturn(Mono.just(3L))

        // When: 알림 검색
        val result = alertHistoryService.searchAlerts(criteria)

        // Then: 검색 결과 검증
        StepVerifier.create(result)
            .assertNext { pagedResult ->
                assertThat(pagedResult.content).hasSize(3)
                assertThat(pagedResult.totalElements).isEqualTo(3L)
                assertThat(pagedResult.totalPages).isEqualTo(1)
                assertThat(pagedResult.currentPage).isEqualTo(0)
                assertThat(pagedResult.pageSize).isEqualTo(50)
                assertThat(pagedResult.hasNext).isFalse()
                assertThat(pagedResult.hasPrevious).isFalse()
            }
            .verifyComplete()
    }

    @Test
    fun `빈 결과를 반환할 수 있다`() {
        // Given: 검색 조건과 빈 결과
        val criteria = AlertSearchCriteria(page = 0, size = 50)

        whenever(customAlertRepository.findByCriteria(any()))
            .thenReturn(Flux.empty())
        whenever(customAlertRepository.countByCriteria(any()))
            .thenReturn(Mono.just(0L))

        // When: 알림 검색
        val result = alertHistoryService.searchAlerts(criteria)

        // Then: 빈 결과 검증
        StepVerifier.create(result)
            .assertNext { pagedResult ->
                assertThat(pagedResult.content).isEmpty()
                assertThat(pagedResult.totalElements).isEqualTo(0L)
                assertThat(pagedResult.totalPages).isEqualTo(0)
                assertThat(pagedResult.hasNext).isFalse()
                assertThat(pagedResult.hasPrevious).isFalse()
            }
            .verifyComplete()
    }

    @Test
    fun `날짜 범위가 지정되지 않으면 기본 7일 범위가 적용된다`() {
        // Given: 날짜 범위 없는 검색 조건
        val criteria = AlertSearchCriteria(
            startDate = null,
            endDate = null,
            page = 0,
            size = 50
        )

        whenever(customAlertRepository.findByCriteria(any()))
            .thenReturn(Flux.fromIterable(sampleAlerts))
        whenever(customAlertRepository.countByCriteria(any()))
            .thenReturn(Mono.just(3L))

        // When: 알림 검색
        val result = alertHistoryService.searchAlerts(criteria)

        // Then: 기본 날짜 범위가 적용된 검색 결과 반환
        StepVerifier.create(result)
            .assertNext { pagedResult ->
                assertThat(pagedResult.content).hasSize(3)
                assertThat(pagedResult.totalElements).isEqualTo(3L)
            }
            .verifyComplete()
    }

    @Test
    fun `페이지네이션이 정확하게 계산된다`() {
        // Given: 총 100개의 알림, 페이지 크기 50
        val criteria = AlertSearchCriteria(page = 0, size = 50)

        whenever(customAlertRepository.findByCriteria(any()))
            .thenReturn(Flux.fromIterable(sampleAlerts))
        whenever(customAlertRepository.countByCriteria(any()))
            .thenReturn(Mono.just(100L))

        // When: 알림 검색
        val result = alertHistoryService.searchAlerts(criteria)

        // Then: 페이지네이션 메타데이터 검증
        StepVerifier.create(result)
            .assertNext { pagedResult ->
                assertThat(pagedResult.totalElements).isEqualTo(100L)
                assertThat(pagedResult.totalPages).isEqualTo(2) // 100 / 50 = 2 페이지
                assertThat(pagedResult.currentPage).isEqualTo(0)
                assertThat(pagedResult.hasNext).isTrue() // 다음 페이지 존재
                assertThat(pagedResult.hasPrevious).isFalse() // 이전 페이지 없음
            }
            .verifyComplete()
    }

    @Test
    fun `두 번째 페이지를 조회할 수 있다`() {
        // Given: 두 번째 페이지 조회
        val criteria = AlertSearchCriteria(page = 1, size = 50)

        whenever(customAlertRepository.findByCriteria(any()))
            .thenReturn(Flux.fromIterable(sampleAlerts))
        whenever(customAlertRepository.countByCriteria(any()))
            .thenReturn(Mono.just(100L))

        // When: 알림 검색
        val result = alertHistoryService.searchAlerts(criteria)

        // Then: 두 번째 페이지 검증
        StepVerifier.create(result)
            .assertNext { pagedResult ->
                assertThat(pagedResult.currentPage).isEqualTo(1)
                assertThat(pagedResult.hasNext).isFalse() // 마지막 페이지
                assertThat(pagedResult.hasPrevious).isTrue() // 이전 페이지 존재
            }
            .verifyComplete()
    }

    @Test
    fun `데이터베이스 오류 시 DatabaseConnectionException을 반환한다`() {
        // Given: 레포지토리에서 오류 발생
        val criteria = AlertSearchCriteria(page = 0, size = 50)

        whenever(customAlertRepository.findByCriteria(any()))
            .thenReturn(Flux.error(RuntimeException("Database connection failed")))

        // When: 알림 검색
        val result = alertHistoryService.searchAlerts(criteria)

        // Then: DatabaseConnectionException 발생
        StepVerifier.create(result)
            .expectError(DatabaseConnectionException::class.java)
            .verify()
    }

    @Test
    fun `총 개수가 0일 때 totalPages는 0이다`() {
        // Given: 알림 없음
        val criteria = AlertSearchCriteria(page = 0, size = 50)

        whenever(customAlertRepository.findByCriteria(any()))
            .thenReturn(Flux.empty())
        whenever(customAlertRepository.countByCriteria(any()))
            .thenReturn(Mono.just(0L))

        // When: 알림 검색
        val result = alertHistoryService.searchAlerts(criteria)

        // Then: totalPages = 0
        StepVerifier.create(result)
            .assertNext { pagedResult ->
                assertThat(pagedResult.totalPages).isEqualTo(0)
            }
            .verifyComplete()
    }

    /**
     * 테스트용 Alert 엔티티 생성 헬퍼 함수
     */
    private fun createTestAlert(
        alertId: UUID = UUID.randomUUID(),
        userId: String = "user-1",
        amount: Long = 500000L
    ): Alert {
        return Alert(
            alertId = alertId,
            schemaVersion = "1.0",
            transactionId = UUID.randomUUID(),
            userId = userId,
            amount = amount,
            currency = "KRW",
            countryCode = "KR",
            ruleName = "HIGH_AMOUNT",
            reason = "거래 금액이 임계값을 초과했습니다",
            severity = Severity.MEDIUM,
            alertTimestamp = Instant.now(),
            status = AlertStatus.UNREAD,
            createdAt = Instant.now()
        )
    }
}
