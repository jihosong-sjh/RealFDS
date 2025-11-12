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
import java.util.*

/**
 * 사용자 ID 필터링 통합 테스트
 *
 * Given: 여러 사용자의 알림이 데이터베이스에 존재
 * When: 특정 사용자 ID로 검색
 * Then: 해당 사용자의 알림만 반환
 *
 * 테스트 환경: Testcontainers를 사용하여 실제 PostgreSQL 환경에서 테스트
 */
@SpringBootTest
@Testcontainers
class UserIdFilterTest {

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
    fun `Given 여러 사용자의 알림이 존재 When user-5로 검색 Then 해당 사용자의 알림만 반환`() {
        // Given: user-1 ~ user-10까지 각 사용자당 1개씩 알림 생성
        val alerts = (1..10).map { i ->
            createAlert(
                userId = "user-$i",
                amount = 1000000L + (i * 100000L),
                reason = "user-$i의 테스트 알림입니다"
            )
        }

        // 알림 저장
        alertRepository.saveAll(alerts).blockLast()

        // When: userId="user-5"로 검색
        val criteria = AlertSearchCriteria(
            userId = "user-5",
            page = 0,
            size = 50
        )

        // Then: user-5의 알림만 반환 (1개)
        StepVerifier.create(customAlertRepository.findByCriteria(criteria))
            .assertNext { alert ->
                assertThat(alert.userId).isEqualTo("user-5")
                assertThat(alert.amount).isEqualTo(1500000L)
                assertThat(alert.reason).contains("user-5")
            }
            .verifyComplete()

        // Then: 전체 개수도 1개로 확인
        StepVerifier.create(customAlertRepository.countByCriteria(criteria))
            .assertNext { count -> assertThat(count).isEqualTo(1L) }
            .verifyComplete()
    }

    @Test
    fun `Given 여러 사용자의 알림이 존재 When 동일 사용자가 여러 알림을 가진 경우 Then 모든 알림 반환`() {
        // Given: user-3가 3개의 알림을 가짐, 다른 사용자는 각 1개씩
        val user3Alerts = listOf(
            createAlert(userId = "user-3", amount = 1000000L, ruleName = "HIGH_AMOUNT"),
            createAlert(userId = "user-3", amount = 2000000L, ruleName = "HIGH_AMOUNT"),
            createAlert(userId = "user-3", amount = 500000L, ruleName = "RAPID_TRANSACTION")
        )
        val otherAlerts = listOf(
            createAlert(userId = "user-1"),
            createAlert(userId = "user-2")
        )

        alertRepository.saveAll(user3Alerts + otherAlerts).blockLast()

        // When: userId="user-3"로 검색
        val criteria = AlertSearchCriteria(
            userId = "user-3",
            page = 0,
            size = 50
        )

        // Then: user-3의 모든 알림 반환 (3개)
        StepVerifier.create(customAlertRepository.findByCriteria(criteria))
            .assertNext { alert -> assertThat(alert.userId).isEqualTo("user-3") }
            .assertNext { alert -> assertThat(alert.userId).isEqualTo("user-3") }
            .assertNext { alert -> assertThat(alert.userId).isEqualTo("user-3") }
            .verifyComplete()

        // Then: 전체 개수도 3개로 확인
        StepVerifier.create(customAlertRepository.countByCriteria(criteria))
            .assertNext { count -> assertThat(count).isEqualTo(3L) }
            .verifyComplete()
    }

    @Test
    fun `Given 알림이 존재하지 않는 사용자 When 해당 사용자로 검색 Then 빈 결과 반환`() {
        // Given: user-1, user-2의 알림만 존재
        val alerts = listOf(
            createAlert(userId = "user-1"),
            createAlert(userId = "user-2")
        )
        alertRepository.saveAll(alerts).blockLast()

        // When: 존재하지 않는 user-999로 검색
        val criteria = AlertSearchCriteria(
            userId = "user-999",
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
    fun `Given 하이픈이 포함된 사용자 ID When 검색 Then 정상적으로 필터링`() {
        // Given: 다양한 형식의 사용자 ID
        val alerts = listOf(
            createAlert(userId = "user-special-1"),
            createAlert(userId = "user-special-2"),
            createAlert(userId = "normaluser"),
            createAlert(userId = "user123")
        )
        alertRepository.saveAll(alerts).blockLast()

        // When: userId="user-special-1"로 검색
        val criteria = AlertSearchCriteria(
            userId = "user-special-1",
            page = 0,
            size = 50
        )

        // Then: user-special-1의 알림만 반환
        StepVerifier.create(customAlertRepository.findByCriteria(criteria))
            .assertNext { alert ->
                assertThat(alert.userId).isEqualTo("user-special-1")
            }
            .verifyComplete()

        // Then: 전체 개수도 1개로 확인
        StepVerifier.create(customAlertRepository.countByCriteria(criteria))
            .assertNext { count -> assertThat(count).isEqualTo(1L) }
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
        status: AlertStatus = AlertStatus.UNREAD
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
            alertTimestamp = Instant.now(),
            status = status,
            assignedTo = null,
            actionNote = null,
            processedAt = null,
            createdAt = Instant.now()
        )
    }
}
