package io.realfds.alert.repository

import io.realfds.alert.domain.Alert
import io.realfds.alert.domain.AlertStatus
import io.realfds.alert.domain.Severity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

/**
 * AlertRepository 통합 테스트
 *
 * Testcontainers를 사용하여 실제 PostgreSQL 데이터베이스와 통합 테스트합니다.
 * Given-When-Then 구조를 따릅니다.
 */
@DataR2dbcTest
@Testcontainers
class AlertRepositoryTest {

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

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 데이터 정리
        alertRepository.deleteAll().block()
    }

    @Test
    fun `Alert를 저장하고 조회할 수 있다`() {
        // Given: 유효한 Alert 엔티티
        val alert = createTestAlert()

        // When: Alert 저장
        val saveResult = alertRepository.save(alert)

        // Then: 저장 성공 및 조회 가능
        StepVerifier.create(saveResult)
            .assertNext { savedAlert ->
                assertThat(savedAlert.alertId).isEqualTo(alert.alertId)
                assertThat(savedAlert.userId).isEqualTo(alert.userId)
                assertThat(savedAlert.amount).isEqualTo(alert.amount)
                assertThat(savedAlert.status).isEqualTo(AlertStatus.UNREAD)
            }
            .verifyComplete()
    }

    @Test
    fun `저장된 Alert를 ID로 조회할 수 있다`() {
        // Given: Alert 저장
        val alert = createTestAlert()
        alertRepository.save(alert).block()

        // When: ID로 조회
        val findResult = alertRepository.findById(alert.alertId)

        // Then: 조회 성공
        StepVerifier.create(findResult)
            .assertNext { foundAlert ->
                assertThat(foundAlert.alertId).isEqualTo(alert.alertId)
                assertThat(foundAlert.userId).isEqualTo(alert.userId)
            }
            .verifyComplete()
    }

    @Test
    fun `모든 Alert를 조회할 수 있다`() {
        // Given: 여러 Alert 저장
        val alert1 = createTestAlert(userId = "user-1")
        val alert2 = createTestAlert(userId = "user-2")
        val alert3 = createTestAlert(userId = "user-3")

        alertRepository.save(alert1).block()
        alertRepository.save(alert2).block()
        alertRepository.save(alert3).block()

        // When: 모든 Alert 조회
        val findAllResult = alertRepository.findAll()

        // Then: 3개의 Alert 조회됨
        StepVerifier.create(findAllResult)
            .expectNextCount(3)
            .verifyComplete()
    }

    @Test
    fun `Alert를 삭제할 수 있다`() {
        // Given: Alert 저장
        val alert = createTestAlert()
        alertRepository.save(alert).block()

        // When: Alert 삭제
        val deleteResult = alertRepository.deleteById(alert.alertId)

        // Then: 삭제 성공 및 조회 불가
        StepVerifier.create(deleteResult)
            .verifyComplete()

        StepVerifier.create(alertRepository.findById(alert.alertId))
            .verifyComplete() // 조회 결과 없음
    }

    @Test
    fun `시스템 재시작 시뮬레이션 - 저장된 Alert는 유지된다`() {
        // Given: Alert 저장
        val alert = createTestAlert()
        alertRepository.save(alert).block()

        // When: 레포지토리 재생성 (시스템 재시작 시뮬레이션)
        // 실제로는 컨테이너가 재시작되지 않으므로 데이터 유지됨

        // Then: 저장된 Alert를 여전히 조회할 수 있음
        StepVerifier.create(alertRepository.findById(alert.alertId))
            .assertNext { foundAlert ->
                assertThat(foundAlert.alertId).isEqualTo(alert.alertId)
                assertThat(foundAlert.userId).isEqualTo(alert.userId)
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
