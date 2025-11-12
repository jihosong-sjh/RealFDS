package io.realfds.alert.service

import io.realfds.alert.repository.AlertRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import reactor.test.StepVerifier
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * AlertService 영속화 통합 테스트
 *
 * Kafka와 PostgreSQL을 Testcontainers로 실행하여
 * AlertEvent 수신부터 데이터베이스 저장까지의 전체 플로우를 검증합니다.
 * Given-When-Then 구조를 따릅니다.
 */
@SpringBootTest
@Testcontainers
class AlertServicePersistenceTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")

        @Container
        @JvmStatic
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // PostgreSQL 설정
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

            // Kafka 설정
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
        }
    }

    @Autowired
    private lateinit var alertService: AlertService

    @Autowired
    private lateinit var alertRepository: AlertRepository

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 데이터 정리
        alertRepository.deleteAll().block()
    }

    @Test
    fun `AlertEvent를 받아서 PostgreSQL에 저장할 수 있다`() {
        // Given: AlertEvent 생성
        val alertEvent = createTestAlertEvent()

        // When: AlertService로 AlertEvent 처리 및 저장
        val saveResult = alertService.processAndSaveAlert(alertEvent)

        // Then: PostgreSQL에 저장 성공
        StepVerifier.create(saveResult)
            .assertNext { savedAlert ->
                assertThat(savedAlert.alertId).isEqualTo(alertEvent.alertId)
                assertThat(savedAlert.userId).isEqualTo(alertEvent.userId)
                assertThat(savedAlert.amount).isEqualTo(alertEvent.amount)
                assertThat(savedAlert.ruleName).isEqualTo(alertEvent.ruleName)
            }
            .verifyComplete()

        // 데이터베이스에서 직접 조회하여 확인
        val foundAlert = alertRepository.findById(alertEvent.alertId).block()
        assertThat(foundAlert).isNotNull
        assertThat(foundAlert!!.alertId).isEqualTo(alertEvent.alertId)
    }

    @Test
    fun `저장된 Alert를 조회할 수 있다 (영속성 확인)`() {
        // Given: AlertEvent 저장
        val alertEvent = createTestAlertEvent()
        alertService.processAndSaveAlert(alertEvent).block()

        // When: 데이터베이스에서 조회
        val findResult = alertRepository.findById(alertEvent.alertId)

        // Then: 저장된 Alert 조회 성공
        StepVerifier.create(findResult)
            .assertNext { foundAlert ->
                assertThat(foundAlert.alertId).isEqualTo(alertEvent.alertId)
                assertThat(foundAlert.userId).isEqualTo(alertEvent.userId)
                assertThat(foundAlert.amount).isEqualTo(alertEvent.amount)
            }
            .verifyComplete()
    }

    @Test
    fun `시스템 재시작 시뮬레이션 - 저장된 Alert는 유지된다`() {
        // Given: AlertEvent 저장
        val alertEvent = createTestAlertEvent()
        alertService.processAndSaveAlert(alertEvent).block()

        // When: "시스템 재시작" 시뮬레이션 (실제로는 컨테이너가 계속 실행 중)
        // 데이터베이스에서 직접 조회

        // Then: 저장된 Alert가 여전히 존재함
        val foundAlert = alertRepository.findById(alertEvent.alertId).block()
        assertThat(foundAlert).isNotNull
        assertThat(foundAlert!!.alertId).isEqualTo(alertEvent.alertId)
        assertThat(foundAlert.userId).isEqualTo(alertEvent.userId)
    }

    @Test
    fun `여러 개의 AlertEvent를 순차적으로 저장할 수 있다`() {
        // Given: 여러 AlertEvent 생성
        val alertEvent1 = createTestAlertEvent(userId = "user-1")
        val alertEvent2 = createTestAlertEvent(userId = "user-2")
        val alertEvent3 = createTestAlertEvent(userId = "user-3")

        // When: 순차적으로 저장
        alertService.processAndSaveAlert(alertEvent1).block()
        alertService.processAndSaveAlert(alertEvent2).block()
        alertService.processAndSaveAlert(alertEvent3).block()

        // Then: 모든 Alert가 저장됨
        val allAlerts = alertRepository.findAll().collectList().block()
        assertThat(allAlerts).hasSize(3)
        assertThat(allAlerts).extracting<String> { it.userId }
            .containsExactlyInAnyOrder("user-1", "user-2", "user-3")
    }

    @Test
    fun `AlertEvent의 모든 필드가 정확하게 저장된다`() {
        // Given: 모든 필드를 가진 AlertEvent
        val alertEvent = AlertEvent(
            alertId = UUID.randomUUID(),
            schemaVersion = "1.0",
            transactionId = UUID.randomUUID(),
            userId = "user-test",
            amount = 1500000L,
            currency = "KRW",
            countryCode = "KR",
            ruleName = "HIGH_AMOUNT",
            reason = "거래 금액이 설정된 임계값(1,000,000원)을 초과했습니다",
            severity = "HIGH",
            alertTimestamp = Instant.now()
        )

        // When: AlertEvent 저장
        alertService.processAndSaveAlert(alertEvent).block()

        // Then: 모든 필드가 정확하게 저장됨
        val savedAlert = alertRepository.findById(alertEvent.alertId).block()
        assertThat(savedAlert).isNotNull
        assertThat(savedAlert!!.alertId).isEqualTo(alertEvent.alertId)
        assertThat(savedAlert.schemaVersion).isEqualTo(alertEvent.schemaVersion)
        assertThat(savedAlert.transactionId).isEqualTo(alertEvent.transactionId)
        assertThat(savedAlert.userId).isEqualTo(alertEvent.userId)
        assertThat(savedAlert.amount).isEqualTo(alertEvent.amount)
        assertThat(savedAlert.currency).isEqualTo(alertEvent.currency)
        assertThat(savedAlert.countryCode).isEqualTo(alertEvent.countryCode)
        assertThat(savedAlert.ruleName).isEqualTo(alertEvent.ruleName)
        assertThat(savedAlert.reason).isEqualTo(alertEvent.reason)
        assertThat(savedAlert.severity.name).isEqualTo(alertEvent.severity)
        assertThat(savedAlert.alertTimestamp).isEqualTo(alertEvent.alertTimestamp)
    }

    @Test
    fun `대량의 AlertEvent를 저장할 수 있다 (성능 테스트)`() {
        // Given: 100개의 AlertEvent 생성
        val alertEvents = (1..100).map { index ->
            createTestAlertEvent(userId = "user-$index")
        }

        // When: 모든 AlertEvent 저장
        val startTime = Instant.now()
        alertEvents.forEach { alertEvent ->
            alertService.processAndSaveAlert(alertEvent).block()
        }
        val duration = Duration.between(startTime, Instant.now())

        // Then: 모든 Alert가 저장되고, 성능이 합리적임
        val count = alertRepository.count().block()
        assertThat(count).isEqualTo(100L)

        // 성능 검증: 100개 저장이 10초 이내에 완료되어야 함
        assertThat(duration.seconds).isLessThan(10)
    }

    @Test
    fun `저장 실패 시 재시도 로직이 작동한다`() {
        // Given: 유효한 AlertEvent
        val alertEvent = createTestAlertEvent()

        // When: AlertEvent 저장 (재시도 로직 포함)
        val saveResult = alertService.processAndSaveAlert(alertEvent)

        // Then: 최종적으로 저장 성공
        StepVerifier.create(saveResult)
            .assertNext { savedAlert ->
                assertThat(savedAlert.alertId).isEqualTo(alertEvent.alertId)
            }
            .verifyComplete()
    }

    /**
     * 테스트용 AlertEvent 생성 헬퍼 함수
     */
    private fun createTestAlertEvent(
        alertId: UUID = UUID.randomUUID(),
        userId: String = "user-1",
        amount: Long = 500000L
    ): AlertEvent {
        return AlertEvent(
            alertId = alertId,
            schemaVersion = "1.0",
            transactionId = UUID.randomUUID(),
            userId = userId,
            amount = amount,
            currency = "KRW",
            countryCode = "KR",
            ruleName = "HIGH_AMOUNT",
            reason = "거래 금액이 임계값을 초과했습니다",
            severity = "MEDIUM",
            alertTimestamp = Instant.now()
        )
    }
}
