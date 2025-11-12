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
 * 규칙명 필터링 통합 테스트
 *
 * Given: 다양한 규칙의 알림이 데이터베이스에 존재
 * When: 특정 규칙명으로 검색
 * Then: 해당 규칙의 알림만 반환
 *
 * 테스트 환경: Testcontainers를 사용하여 실제 PostgreSQL 환경에서 테스트
 */
@SpringBootTest
@Testcontainers
class RuleNameFilterTest {

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
    fun `Given 다양한 규칙의 알림이 존재 When HIGH_AMOUNT로 검색 Then 해당 규칙의 알림만 반환`() {
        // Given: HIGH_AMOUNT, FOREIGN_COUNTRY, RAPID_TRANSACTION 규칙의 알림 생성
        val highAmountAlert1 = createAlert(
            userId = "user-1",
            amount = 1500000L,
            ruleName = "HIGH_AMOUNT",
            reason = "거래 금액이 설정된 임계값(1,000,000원)을 초과했습니다",
            severity = Severity.HIGH
        )
        val highAmountAlert2 = createAlert(
            userId = "user-2",
            amount = 2000000L,
            ruleName = "HIGH_AMOUNT",
            reason = "거래 금액이 설정된 임계값(1,000,000원)을 초과했습니다",
            severity = Severity.HIGH
        )
        val foreignCountryAlert = createAlert(
            userId = "user-3",
            amount = 850000L,
            ruleName = "FOREIGN_COUNTRY",
            reason = "해외(CN) 국가에서 거래가 발생했습니다",
            severity = Severity.MEDIUM,
            countryCode = "CN"
        )
        val rapidTransactionAlert = createAlert(
            userId = "user-4",
            amount = 200000L,
            ruleName = "RAPID_TRANSACTION",
            reason = "1분 동안 5건 이상의 거래가 발생했습니다",
            severity = Severity.LOW
        )

        // 알림 저장
        alertRepository.saveAll(
            listOf(highAmountAlert1, highAmountAlert2, foreignCountryAlert, rapidTransactionAlert)
        ).blockLast()

        // When: ruleName="HIGH_AMOUNT"로 검색
        val criteria = AlertSearchCriteria(
            ruleName = "HIGH_AMOUNT",
            page = 0,
            size = 50
        )

        // Then: HIGH_AMOUNT 규칙의 알림만 반환 (2개)
        StepVerifier.create(customAlertRepository.findByCriteria(criteria))
            .assertNext { alert ->
                assertThat(alert.ruleName).isEqualTo("HIGH_AMOUNT")
                assertThat(alert.severity).isEqualTo(Severity.HIGH)
            }
            .assertNext { alert ->
                assertThat(alert.ruleName).isEqualTo("HIGH_AMOUNT")
                assertThat(alert.severity).isEqualTo(Severity.HIGH)
            }
            .verifyComplete()

        // Then: 전체 개수도 2개로 확인
        StepVerifier.create(customAlertRepository.countByCriteria(criteria))
            .assertNext { count -> assertThat(count).isEqualTo(2L) }
            .verifyComplete()
    }

    @Test
    fun `Given 다양한 규칙의 알림이 존재 When FOREIGN_COUNTRY로 검색 Then 해당 규칙의 알림만 반환`() {
        // Given: 다양한 규칙의 알림 생성
        val highAmountAlert = createAlert(ruleName = "HIGH_AMOUNT")
        val foreignCountryAlert1 = createAlert(ruleName = "FOREIGN_COUNTRY", countryCode = "US")
        val foreignCountryAlert2 = createAlert(ruleName = "FOREIGN_COUNTRY", countryCode = "JP")
        val rapidTransactionAlert = createAlert(ruleName = "RAPID_TRANSACTION")

        alertRepository.saveAll(
            listOf(highAmountAlert, foreignCountryAlert1, foreignCountryAlert2, rapidTransactionAlert)
        ).blockLast()

        // When: ruleName="FOREIGN_COUNTRY"로 검색
        val criteria = AlertSearchCriteria(
            ruleName = "FOREIGN_COUNTRY",
            page = 0,
            size = 50
        )

        // Then: FOREIGN_COUNTRY 규칙의 알림만 반환 (2개)
        StepVerifier.create(customAlertRepository.findByCriteria(criteria))
            .expectNextCount(2)
            .verifyComplete()

        // Then: 전체 개수도 2개로 확인
        StepVerifier.create(customAlertRepository.countByCriteria(criteria))
            .assertNext { count -> assertThat(count).isEqualTo(2L) }
            .verifyComplete()
    }

    @Test
    fun `Given 알림이 존재하지 않는 규칙 When 해당 규칙으로 검색 Then 빈 결과 반환`() {
        // Given: HIGH_AMOUNT 규칙의 알림만 존재
        val highAmountAlert = createAlert(ruleName = "HIGH_AMOUNT")
        alertRepository.save(highAmountAlert).block()

        // When: 존재하지 않는 RAPID_TRANSACTION 규칙으로 검색
        val criteria = AlertSearchCriteria(
            ruleName = "RAPID_TRANSACTION",
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
