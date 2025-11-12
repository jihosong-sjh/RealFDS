package io.realfds.alert.repository

import io.realfds.alert.domain.Alert
import io.realfds.alert.domain.AlertStatus
import io.realfds.alert.domain.Severity
import io.realfds.alert.dto.AlertSearchCriteria
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.test.StepVerifier
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * CustomAlertRepository 통합 테스트
 *
 * 날짜 범위 검색 및 페이지네이션 기능을 테스트합니다.
 * Testcontainers를 사용하여 실제 PostgreSQL과 통합 테스트합니다.
 */
@DataR2dbcTest
@Testcontainers
@Import(CustomAlertRepositoryImpl::class)
class CustomAlertRepositoryTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username", postgres::getUsername)
            registry.add("spring.r2dbc.password", postgres::getPassword)
            registry.add("spring.flyway.url") {
                "jdbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}"
            }
            registry.add("spring.flyway.user", postgres::getUsername)
            registry.add("spring.flyway.password", postgres::getPassword)
        }
    }

    @Autowired
    private lateinit var alertRepository: AlertRepository

    @Autowired
    private lateinit var customAlertRepository: CustomAlertRepository

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 데이터 정리
        alertRepository.deleteAll().block()
    }

    @Test
    fun `날짜 범위로 Alert를 검색할 수 있다`() {
        // Given: 다양한 날짜의 Alert 저장
        val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS)
        val threeDaysAgo = Instant.now().minus(3, ChronoUnit.DAYS)
        val oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS)

        val alert1 = createTestAlert(alertTimestamp = sevenDaysAgo)
        val alert2 = createTestAlert(alertTimestamp = threeDaysAgo)
        val alert3 = createTestAlert(alertTimestamp = oneDayAgo)

        alertRepository.save(alert1).block()
        alertRepository.save(alert2).block()
        alertRepository.save(alert3).block()

        // When: 3일 전 ~ 오늘 범위로 검색
        val criteria = AlertSearchCriteria(
            startDate = threeDaysAgo,
            endDate = Instant.now(),
            page = 0,
            size = 50
        )
        val searchResult = customAlertRepository.findByCriteria(criteria)

        // Then: 2개의 Alert 조회됨 (alert2, alert3)
        StepVerifier.create(searchResult.collectList())
            .assertNext { alerts ->
                assertThat(alerts).hasSize(2)
                assertThat(alerts).extracting<UUID> { it.alertId }
                    .containsExactlyInAnyOrder(alert2.alertId, alert3.alertId)
            }
            .verifyComplete()
    }

    @Test
    fun `페이지네이션이 정상 작동한다`() {
        // Given: 10개의 Alert 저장
        repeat(10) { index ->
            val alert = createTestAlert(
                userId = "user-$index",
                alertTimestamp = Instant.now().minus(index.toLong(), ChronoUnit.HOURS)
            )
            alertRepository.save(alert).block()
        }

        // When: 첫 번째 페이지 조회 (size=5)
        val criteriaPage1 = AlertSearchCriteria(page = 0, size = 5)
        val page1Result = customAlertRepository.findByCriteria(criteriaPage1)

        // Then: 5개의 Alert 조회됨
        StepVerifier.create(page1Result.collectList())
            .assertNext { alerts ->
                assertThat(alerts).hasSize(5)
            }
            .verifyComplete()

        // When: 두 번째 페이지 조회 (size=5)
        val criteriaPage2 = AlertSearchCriteria(page = 1, size = 5)
        val page2Result = customAlertRepository.findByCriteria(criteriaPage2)

        // Then: 나머지 5개의 Alert 조회됨
        StepVerifier.create(page2Result.collectList())
            .assertNext { alerts ->
                assertThat(alerts).hasSize(5)
            }
            .verifyComplete()
    }

    @Test
    fun `Alert가 최신 순으로 정렬된다`() {
        // Given: 다양한 시각의 Alert 저장
        val oldAlert = createTestAlert(alertTimestamp = Instant.now().minus(10, ChronoUnit.DAYS))
        val recentAlert = createTestAlert(alertTimestamp = Instant.now().minus(1, ChronoUnit.DAYS))
        val newestAlert = createTestAlert(alertTimestamp = Instant.now())

        alertRepository.save(oldAlert).block()
        alertRepository.save(recentAlert).block()
        alertRepository.save(newestAlert).block()

        // When: 모든 Alert 조회
        val criteria = AlertSearchCriteria(page = 0, size = 50)
        val searchResult = customAlertRepository.findByCriteria(criteria)

        // Then: 최신 순으로 정렬됨
        StepVerifier.create(searchResult.collectList())
            .assertNext { alerts ->
                assertThat(alerts).hasSize(3)
                assertThat(alerts[0].alertId).isEqualTo(newestAlert.alertId) // 가장 최신
                assertThat(alerts[1].alertId).isEqualTo(recentAlert.alertId)
                assertThat(alerts[2].alertId).isEqualTo(oldAlert.alertId) // 가장 오래됨
            }
            .verifyComplete()
    }

    @Test
    fun `빈 결과를 반환할 수 있다`() {
        // Given: Alert 없음

        // When: 검색
        val criteria = AlertSearchCriteria(page = 0, size = 50)
        val searchResult = customAlertRepository.findByCriteria(criteria)

        // Then: 빈 목록 반환
        StepVerifier.create(searchResult.collectList())
            .assertNext { alerts ->
                assertThat(alerts).isEmpty()
            }
            .verifyComplete()
    }

    @Test
    fun `검색 조건에 맞는 전체 개수를 조회할 수 있다`() {
        // Given: 100개의 Alert 저장
        repeat(100) { index ->
            val alert = createTestAlert(userId = "user-$index")
            alertRepository.save(alert).block()
        }

        // When: 전체 개수 조회
        val criteria = AlertSearchCriteria(page = 0, size = 50)
        val countResult = customAlertRepository.countByCriteria(criteria)

        // Then: 100개 반환
        StepVerifier.create(countResult)
            .assertNext { count ->
                assertThat(count).isEqualTo(100L)
            }
            .verifyComplete()
    }

    /**
     * 테스트용 Alert 엔티티 생성 헬퍼 함수
     */
    private fun createTestAlert(
        alertId: UUID = UUID.randomUUID(),
        userId: String = "user-1",
        amount: Long = 500000L,
        alertTimestamp: Instant = Instant.now()
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
            alertTimestamp = alertTimestamp,
            status = AlertStatus.UNREAD,
            createdAt = Instant.now()
        )
    }
}
