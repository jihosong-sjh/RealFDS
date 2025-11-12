package io.realfds.alert.controller

import io.realfds.alert.domain.Alert
import io.realfds.alert.domain.AlertStatus
import io.realfds.alert.domain.Severity
import io.realfds.alert.repository.AlertRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * AlertHistoryController 통합 테스트
 *
 * WebTestClient와 Testcontainers를 사용하여 실제 HTTP 요청/응답을 검증합니다.
 * Given-When-Then 구조를 따릅니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class AlertHistoryControllerTest {

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
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var alertRepository: AlertRepository

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 데이터 정리
        alertRepository.deleteAll().block()
    }

    @Test
    fun `GET api_alerts_history - 정상 응답을 반환한다 (200 OK)`() {
        // Given: 알림 데이터 저장
        val alert = createTestAlert()
        alertRepository.save(alert).block()

        // When: GET /api/alerts/history 요청
        // Then: 200 OK 응답 및 알림 데이터 반환
        webTestClient.get()
            .uri("/api/alerts/history?page=0&size=50")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.content").isArray
            .jsonPath("$.content.length()").isEqualTo(1)
            .jsonPath("$.content[0].alertId").isEqualTo(alert.alertId.toString())
            .jsonPath("$.content[0].userId").isEqualTo(alert.userId)
            .jsonPath("$.totalElements").isEqualTo(1)
            .jsonPath("$.totalPages").isEqualTo(1)
            .jsonPath("$.currentPage").isEqualTo(0)
            .jsonPath("$.pageSize").isEqualTo(50)
            .jsonPath("$.hasNext").isEqualTo(false)
            .jsonPath("$.hasPrevious").isEqualTo(false)
    }

    @Test
    fun `GET api_alerts_history - 페이지네이션이 정상 작동한다`() {
        // Given: 10개의 알림 저장
        repeat(10) { index ->
            val alert = createTestAlert(
                userId = "user-$index",
                alertTimestamp = Instant.now().minus(index.toLong(), ChronoUnit.HOURS)
            )
            alertRepository.save(alert).block()
        }

        // When: 첫 번째 페이지 조회 (size=5)
        // Then: 5개의 알림 반환
        webTestClient.get()
            .uri("/api/alerts/history?page=0&size=5")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.content.length()").isEqualTo(5)
            .jsonPath("$.totalElements").isEqualTo(10)
            .jsonPath("$.totalPages").isEqualTo(2)
            .jsonPath("$.currentPage").isEqualTo(0)
            .jsonPath("$.hasNext").isEqualTo(true)
            .jsonPath("$.hasPrevious").isEqualTo(false)

        // When: 두 번째 페이지 조회 (size=5)
        // Then: 나머지 5개의 알림 반환
        webTestClient.get()
            .uri("/api/alerts/history?page=1&size=5")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.content.length()").isEqualTo(5)
            .jsonPath("$.totalElements").isEqualTo(10)
            .jsonPath("$.currentPage").isEqualTo(1)
            .jsonPath("$.hasNext").isEqualTo(false)
            .jsonPath("$.hasPrevious").isEqualTo(true)
    }

    @Test
    fun `GET api_alerts_history - 빈 결과를 반환한다 (content=[], totalElements=0)`() {
        // Given: 알림 데이터 없음

        // When: GET /api/alerts/history 요청
        // Then: 빈 결과 반환
        webTestClient.get()
            .uri("/api/alerts/history?page=0&size=50")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.content").isArray
            .jsonPath("$.content.length()").isEqualTo(0)
            .jsonPath("$.totalElements").isEqualTo(0)
            .jsonPath("$.totalPages").isEqualTo(0)
    }

    @Test
    fun `GET api_alerts_history - 잘못된 페이지 번호는 400 Bad Request를 반환한다`() {
        // Given: 음수 페이지 번호

        // When: GET /api/alerts/history?page=-1
        // Then: 400 Bad Request
        webTestClient.get()
            .uri("/api/alerts/history?page=-1&size=50")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").exists()
    }

    @Test
    fun `GET api_alerts_history - 잘못된 페이지 크기는 400 Bad Request를 반환한다`() {
        // Given: 잘못된 페이지 크기 (0)

        // When: GET /api/alerts/history?size=0
        // Then: 400 Bad Request
        webTestClient.get()
            .uri("/api/alerts/history?page=0&size=0")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").exists()
    }

    @Test
    fun `GET api_alerts_history - 페이지 크기 101은 400 Bad Request를 반환한다`() {
        // Given: 최대 페이지 크기 초과 (101)

        // When: GET /api/alerts/history?size=101
        // Then: 400 Bad Request
        webTestClient.get()
            .uri("/api/alerts/history?page=0&size=101")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").exists()
    }

    @Test
    fun `GET api_alerts_history - 잘못된 날짜 범위는 400 Bad Request를 반환한다`() {
        // Given: 시작 날짜가 종료 날짜보다 늦음
        val now = Instant.now()
        val sevenDaysAgo = now.minus(7, ChronoUnit.DAYS)

        // When: GET /api/alerts/history?startDate=now&endDate=sevenDaysAgo
        // Then: 400 Bad Request
        webTestClient.get()
            .uri("/api/alerts/history?startDate=$now&endDate=$sevenDaysAgo")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").exists()
    }

    @Test
    fun `GET api_alerts_history - 날짜 범위 검색이 정상 작동한다`() {
        // Given: 다양한 날짜의 알림 저장
        val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS)
        val threeDaysAgo = Instant.now().minus(3, ChronoUnit.DAYS)
        val oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS)

        alertRepository.save(createTestAlert(alertTimestamp = sevenDaysAgo)).block()
        val alert2 = alertRepository.save(createTestAlert(alertTimestamp = threeDaysAgo)).block()!!
        val alert3 = alertRepository.save(createTestAlert(alertTimestamp = oneDayAgo)).block()!!

        // When: 3일 전 ~ 오늘 범위로 검색
        val now = Instant.now()
        // Then: 2개의 알림 반환 (alert2, alert3)
        webTestClient.get()
            .uri("/api/alerts/history?startDate=$threeDaysAgo&endDate=$now&page=0&size=50")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.content.length()").isEqualTo(2)
            .jsonPath("$.totalElements").isEqualTo(2)
    }

    @Test
    fun `GET api_alerts_history - 알림이 최신 순으로 정렬된다`() {
        // Given: 다양한 시각의 알림 저장
        val oldAlert = createTestAlert(alertTimestamp = Instant.now().minus(10, ChronoUnit.DAYS))
        val recentAlert = createTestAlert(alertTimestamp = Instant.now().minus(1, ChronoUnit.DAYS))
        val newestAlert = createTestAlert(alertTimestamp = Instant.now())

        alertRepository.save(oldAlert).block()
        alertRepository.save(recentAlert).block()
        alertRepository.save(newestAlert).block()

        // When: 모든 알림 조회
        // Then: 최신 순으로 정렬됨 (newestAlert, recentAlert, oldAlert)
        webTestClient.get()
            .uri("/api/alerts/history?page=0&size=50")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.content.length()").isEqualTo(3)
            .jsonPath("$.content[0].alertId").isEqualTo(newestAlert.alertId.toString()) // 가장 최신
            .jsonPath("$.content[1].alertId").isEqualTo(recentAlert.alertId.toString())
            .jsonPath("$.content[2].alertId").isEqualTo(oldAlert.alertId.toString()) // 가장 오래됨
    }

    @Test
    fun `GET api_alerts_history - 기본 파라미터로 요청 가능하다`() {
        // Given: 알림 데이터 저장
        alertRepository.save(createTestAlert()).block()

        // When: 파라미터 없이 GET /api/alerts/history 요청
        // Then: 기본값(page=0, size=50) 적용되어 정상 응답
        webTestClient.get()
            .uri("/api/alerts/history")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.currentPage").isEqualTo(0)
            .jsonPath("$.pageSize").isEqualTo(50)
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
