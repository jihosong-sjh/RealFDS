package io.realfds.alert.repository

import io.realfds.alert.domain.Alert
import io.realfds.alert.domain.AlertStatus
import io.realfds.alert.domain.Severity
import io.realfds.alert.dto.AlertSearchCriteria
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.test.StepVerifier
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * 다중 필터 조합 통합 테스트
 *
 * Given: 다양한 조합의 알림이 데이터베이스에 존재
 * When: 여러 필터를 조합하여 검색
 * Then: 모든 조건을 만족하는 알림만 반환
 *
 * 테스트 환경: Testcontainers를 사용하여 실제 PostgreSQL 환경에서 테스트
 */
@SpringBootTest
@Testcontainers
class CombinedFiltersTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("realfds_test")
            withUsername("test_user")
            withPassword("test_password")
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.firstMappedPort}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            registry.add("spring.flyway.url") { postgres.jdbcUrl }
            registry.add("spring.flyway.user") { postgres.username }
            registry.add("spring.flyway.password") { postgres.password }
        }
    }

    @Autowired
    private lateinit var alertRepository: AlertRepository

    @Autowired
    private lateinit var customAlertRepository: CustomAlertRepository

    @BeforeEach
    fun setUp() {
        // 테스트 전 데이터 초기화
        alertRepository.deleteAll().block()
    }

    @Test
    fun `Given 다양한 알림 When ruleName과 status 조합 검색 Then 두 조건 모두 만족하는 알림만 반환`() {
        // Given: 다양한 규칙과 상태의 알림 생성
        val highAmountUnread = createAlert(
            userId = "user-1",
            ruleName = "HIGH_AMOUNT",
            status = AlertStatus.UNREAD
        )
        val highAmountCompleted = createAlert(
            userId = "user-2",
            ruleName = "HIGH_AMOUNT",
            status = AlertStatus.COMPLETED,
            processedAt = Instant.now()
        )
        val foreignCountryUnread = createAlert(
            userId = "user-3",
            ruleName = "FOREIGN_COUNTRY",
            status = AlertStatus.UNREAD
        )
        val rapidTransactionUnread = createAlert(
            userId = "user-4",
            ruleName = "RAPID_TRANSACTION",
            status = AlertStatus.UNREAD
        )

        alertRepository.saveAll(
            listOf(highAmountUnread, highAmountCompleted, foreignCountryUnread, rapidTransactionUnread)
        ).blockLast()

        // When: ruleName="HIGH_AMOUNT" AND status=UNREAD 검색
        val criteria = AlertSearchCriteria(
            ruleName = "HIGH_AMOUNT",
            status = AlertStatus.UNREAD,
            page = 0,
            size = 50
        )

        // Then: HIGH_AMOUNT이면서 UNREAD인 알림만 반환 (1개)
        StepVerifier.create(customAlertRepository.findByCriteria(criteria))
            .assertNext { alert ->
                assertThat(alert.ruleName).isEqualTo("HIGH_AMOUNT")
                assertThat(alert.status).isEqualTo(AlertStatus.UNREAD)
                assertThat(alert.userId).isEqualTo("user-1")
            }
            .verifyComplete()

        // Then: 전체 개수도 1개로 확인
        StepVerifier.create(customAlertRepository.countByCriteria(criteria))
            .assertNext { count -> assertThat(count).isEqualTo(1L) }
            .verifyComplete()
    }

    @Test
    fun `Given 다양한 알림 When userId와 ruleName 조합 검색 Then 두 조건 모두 만족하는 알림만 반환`() {
        // Given: 다양한 사용자와 규칙의 알림 생성
        val user5HighAmount = createAlert(
            userId = "user-5",
            ruleName = "HIGH_AMOUNT"
        )
        val user5ForeignCountry = createAlert(
            userId = "user-5",
            ruleName = "FOREIGN_COUNTRY"
        )
        val user3HighAmount = createAlert(
            userId = "user-3",
            ruleName = "HIGH_AMOUNT"
        )
        val user7RapidTransaction = createAlert(
            userId = "user-7",
            ruleName = "RAPID_TRANSACTION"
        )

        alertRepository.saveAll(
            listOf(user5HighAmount, user5ForeignCountry, user3HighAmount, user7RapidTransaction)
        ).blockLast()

        // When: userId="user-5" AND ruleName="HIGH_AMOUNT" 검색
        val criteria = AlertSearchCriteria(
            userId = "user-5",
            ruleName = "HIGH_AMOUNT",
            page = 0,
            size = 50
        )

        // Then: user-5이면서 HIGH_AMOUNT인 알림만 반환 (1개)
        StepVerifier.create(customAlertRepository.findByCriteria(criteria))
            .assertNext { alert ->
                assertThat(alert.userId).isEqualTo("user-5")
                assertThat(alert.ruleName).isEqualTo("HIGH_AMOUNT")
            }
            .verifyComplete()

        // Then: 전체 개수도 1개로 확인
        StepVerifier.create(customAlertRepository.countByCriteria(criteria))
            .assertNext { count -> assertThat(count).isEqualTo(1L) }
            .verifyComplete()
    }

    @Test
    fun `Given 다양한 알림 When 3가지 필터 조합 검색 Then 모든 조건 만족하는 알림만 반환`() {
        // Given: 다양한 조합의 알림 생성
        val targetAlert = createAlert(
            userId = "user-10",
            ruleName = "HIGH_AMOUNT",
            status = AlertStatus.UNREAD
        )
        val wrongUser = createAlert(
            userId = "user-5",
            ruleName = "HIGH_AMOUNT",
            status = AlertStatus.UNREAD
        )
        val wrongRuleName = createAlert(
            userId = "user-10",
            ruleName = "FOREIGN_COUNTRY",
            status = AlertStatus.UNREAD
        )
        val wrongStatus = createAlert(
            userId = "user-10",
            ruleName = "HIGH_AMOUNT",
            status = AlertStatus.COMPLETED,
            processedAt = Instant.now()
        )

        alertRepository.saveAll(
            listOf(targetAlert, wrongUser, wrongRuleName, wrongStatus)
        ).blockLast()

        // When: userId="user-10" AND ruleName="HIGH_AMOUNT" AND status=UNREAD 검색
        val criteria = AlertSearchCriteria(
            userId = "user-10",
            ruleName = "HIGH_AMOUNT",
            status = AlertStatus.UNREAD,
            page = 0,
            size = 50
        )

        // Then: 세 조건 모두 만족하는 알림만 반환 (1개)
        StepVerifier.create(customAlertRepository.findByCriteria(criteria))
            .assertNext { alert ->
                assertThat(alert.userId).isEqualTo("user-10")
                assertThat(alert.ruleName).isEqualTo("HIGH_AMOUNT")
                assertThat(alert.status).isEqualTo(AlertStatus.UNREAD)
            }
            .verifyComplete()

        // Then: 전체 개수도 1개로 확인
        StepVerifier.create(customAlertRepository.countByCriteria(criteria))
            .assertNext { count -> assertThat(count).isEqualTo(1L) }
            .verifyComplete()
    }

    @Test
    fun `Given 다양한 알림 When 날짜 범위와 필터 조합 검색 Then 모든 조건 만족하는 알림만 반환`() {
        // Given: 다양한 날짜와 필터 조합의 알림 생성
        val now = Instant.now()
        val threeDaysAgo = now.minus(3, ChronoUnit.DAYS)
        val fiveDaysAgo = now.minus(5, ChronoUnit.DAYS)

        val recentHighAmountUnread = createAlert(
            userId = "user-1",
            ruleName = "HIGH_AMOUNT",
            status = AlertStatus.UNREAD,
            alertTimestamp = threeDaysAgo
        )
        val oldHighAmountUnread = createAlert(
            userId = "user-2",
            ruleName = "HIGH_AMOUNT",
            status = AlertStatus.UNREAD,
            alertTimestamp = fiveDaysAgo
        )
        val recentForeignCountry = createAlert(
            userId = "user-3",
            ruleName = "FOREIGN_COUNTRY",
            status = AlertStatus.UNREAD,
            alertTimestamp = threeDaysAgo
        )

        alertRepository.saveAll(
            listOf(recentHighAmountUnread, oldHighAmountUnread, recentForeignCountry)
        ).blockLast()

        // When: 날짜 범위(4일 전 ~ 현재) + ruleName + status 검색
        val fourDaysAgo = now.minus(4, ChronoUnit.DAYS)
        val criteria = AlertSearchCriteria(
            startDate = fourDaysAgo,
            endDate = now,
            ruleName = "HIGH_AMOUNT",
            status = AlertStatus.UNREAD,
            page = 0,
            size = 50
        )

        // Then: 모든 조건을 만족하는 알림만 반환 (1개: recentHighAmountUnread)
        StepVerifier.create(customAlertRepository.findByCriteria(criteria))
            .assertNext { alert ->
                assertThat(alert.ruleName).isEqualTo("HIGH_AMOUNT")
                assertThat(alert.status).isEqualTo(AlertStatus.UNREAD)
                assertThat(alert.alertTimestamp).isAfterOrEqualTo(fourDaysAgo)
                assertThat(alert.alertTimestamp).isBeforeOrEqualTo(now)
            }
            .verifyComplete()

        // Then: 전체 개수도 1개로 확인
        StepVerifier.create(customAlertRepository.countByCriteria(criteria))
            .assertNext { count -> assertThat(count).isEqualTo(1L) }
            .verifyComplete()
    }

    @Test
    fun `Given 다양한 알림 When 모든 필터 조건 불일치 Then 빈 결과 반환`() {
        // Given: 다양한 알림 생성 (모두 조건과 다름)
        val alerts = listOf(
            createAlert(userId = "user-1", ruleName = "HIGH_AMOUNT", status = AlertStatus.UNREAD),
            createAlert(userId = "user-2", ruleName = "FOREIGN_COUNTRY", status = AlertStatus.IN_PROGRESS),
            createAlert(userId = "user-3", ruleName = "RAPID_TRANSACTION", status = AlertStatus.COMPLETED, processedAt = Instant.now())
        )
        alertRepository.saveAll(alerts).blockLast()

        // When: 존재하지 않는 조건 조합으로 검색
        val criteria = AlertSearchCriteria(
            userId = "user-999",
            ruleName = "HIGH_AMOUNT",
            status = AlertStatus.COMPLETED,
            page = 0,
            size = 50
        )

        // Then: 빈 결과 반환
        StepVerifier.create(customAlertRepository.findByCriteria(criteria))
            .expectNextCount(0)
            .verifyComplete()

        // Then: 전체 개수도 0으로 확인
        StepVerifier.create(customAlertRepository.countByCriteria(criteria))
            .assertNext { count -> assertThat(count).isEqualTo(0L) }
            .verifyComplete()
    }

    @Test
    fun `Given 다양한 알림 When 모든 필터 조합으로 여러 결과 검색 Then 모든 조건 만족하는 알림들 반환`() {
        // Given: user-5가 HIGH_AMOUNT + UNREAD 알림 3개 보유
        val alerts = listOf(
            createAlert(userId = "user-5", ruleName = "HIGH_AMOUNT", status = AlertStatus.UNREAD, amount = 1500000L),
            createAlert(userId = "user-5", ruleName = "HIGH_AMOUNT", status = AlertStatus.UNREAD, amount = 2000000L),
            createAlert(userId = "user-5", ruleName = "HIGH_AMOUNT", status = AlertStatus.UNREAD, amount = 2500000L),
            createAlert(userId = "user-5", ruleName = "FOREIGN_COUNTRY", status = AlertStatus.UNREAD),
            createAlert(userId = "user-5", ruleName = "HIGH_AMOUNT", status = AlertStatus.COMPLETED, processedAt = Instant.now()),
            createAlert(userId = "user-3", ruleName = "HIGH_AMOUNT", status = AlertStatus.UNREAD)
        )
        alertRepository.saveAll(alerts).blockLast()

        // When: userId="user-5" AND ruleName="HIGH_AMOUNT" AND status=UNREAD 검색
        val criteria = AlertSearchCriteria(
            userId = "user-5",
            ruleName = "HIGH_AMOUNT",
            status = AlertStatus.UNREAD,
            page = 0,
            size = 50
        )

        // Then: 모든 조건을 만족하는 알림 3개 반환
        StepVerifier.create(customAlertRepository.findByCriteria(criteria))
            .assertNext { alert ->
                assertThat(alert.userId).isEqualTo("user-5")
                assertThat(alert.ruleName).isEqualTo("HIGH_AMOUNT")
                assertThat(alert.status).isEqualTo(AlertStatus.UNREAD)
            }
            .assertNext { alert ->
                assertThat(alert.userId).isEqualTo("user-5")
                assertThat(alert.ruleName).isEqualTo("HIGH_AMOUNT")
                assertThat(alert.status).isEqualTo(AlertStatus.UNREAD)
            }
            .assertNext { alert ->
                assertThat(alert.userId).isEqualTo("user-5")
                assertThat(alert.ruleName).isEqualTo("HIGH_AMOUNT")
                assertThat(alert.status).isEqualTo(AlertStatus.UNREAD)
            }
            .verifyComplete()

        // Then: 전체 개수도 3개로 확인
        StepVerifier.create(customAlertRepository.countByCriteria(criteria))
            .assertNext { count -> assertThat(count).isEqualTo(3L) }
            .verifyComplete()
    }

    /**
     * 테스트용 Alert 객체 생성 헬퍼 메서드
     */
    private fun createAlert(
        userId: String = "user-1",
        amount: Long = 1000000L,
        ruleName: String = "HIGH_AMOUNT",
        reason: String = "테스트 알림입니다",
        severity: Severity = Severity.HIGH,
        countryCode: String = "KR",
        status: AlertStatus = AlertStatus.UNREAD,
        assignedTo: String? = null,
        actionNote: String? = null,
        processedAt: Instant? = null,
        alertTimestamp: Instant = Instant.now()
    ): Alert {
        return Alert(
            alertId = UUID.randomUUID(),
            schemaVersion = "1.0",
            transactionId = UUID.randomUUID(),
            userId = userId,
            amount = amount,
            currency = "KRW",
            countryCode = countryCode,
            ruleName = ruleName,
            reason = reason,
            severity = severity,
            alertTimestamp = alertTimestamp,
            status = status,
            assignedTo = assignedTo,
            actionNote = actionNote,
            processedAt = processedAt,
            createdAt = Instant.now()
        )
    }
}
