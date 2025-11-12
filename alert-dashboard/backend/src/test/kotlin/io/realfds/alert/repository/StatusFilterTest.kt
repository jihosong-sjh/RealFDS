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
 * 상태 필터링 통합 테스트
 *
 * Given: 다양한 상태의 알림이 데이터베이스에 존재
 * When: 특정 상태로 검색
 * Then: 해당 상태의 알림만 반환
 *
 * 테스트 환경: Testcontainers를 사용하여 실제 PostgreSQL 환경에서 테스트
 */
@SpringBootTest
@Testcontainers
class StatusFilterTest {

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
    fun `Given 다양한 상태의 알림이 존재 When UNREAD로 검색 Then 미확인 알림만 반환`() {
        // Given: UNREAD, IN_PROGRESS, COMPLETED 상태의 알림 생성
        val unreadAlert1 = createAlert(
            userId = "user-1",
            status = AlertStatus.UNREAD,
            assignedTo = null,
            processedAt = null
        )
        val unreadAlert2 = createAlert(
            userId = "user-2",
            status = AlertStatus.UNREAD,
            assignedTo = null,
            processedAt = null
        )
        val inProgressAlert = createAlert(
            userId = "user-3",
            status = AlertStatus.IN_PROGRESS,
            assignedTo = "admin-1",
            processedAt = null
        )
        val completedAlert = createAlert(
            userId = "user-4",
            status = AlertStatus.COMPLETED,
            assignedTo = "admin-1",
            processedAt = Instant.now()
        )

        // 알림 저장
        alertRepository.saveAll(
            listOf(unreadAlert1, unreadAlert2, inProgressAlert, completedAlert)
        ).blockLast()

        // When: status=UNREAD로 검색
        val criteria = AlertSearchCriteria(
            status = AlertStatus.UNREAD,
            page = 0,
            size = 50
        )

        // Then: UNREAD 상태의 알림만 반환 (2개)
        StepVerifier.create(customAlertRepository.findByCriteria(criteria))
            .assertNext { alert ->
                assertThat(alert.status).isEqualTo(AlertStatus.UNREAD)
                assertThat(alert.assignedTo).isNull()
                assertThat(alert.processedAt).isNull()
            }
            .assertNext { alert ->
                assertThat(alert.status).isEqualTo(AlertStatus.UNREAD)
                assertThat(alert.assignedTo).isNull()
                assertThat(alert.processedAt).isNull()
            }
            .verifyComplete()

        // Then: 전체 개수도 2개로 확인
        StepVerifier.create(customAlertRepository.countByCriteria(criteria))
            .assertNext { count -> assertThat(count).isEqualTo(2L) }
            .verifyComplete()
    }

    @Test
    fun `Given 다양한 상태의 알림이 존재 When IN_PROGRESS로 검색 Then 처리 중인 알림만 반환`() {
        // Given: 다양한 상태의 알림 생성
        val unreadAlert = createAlert(status = AlertStatus.UNREAD)
        val inProgressAlert1 = createAlert(
            userId = "user-1",
            status = AlertStatus.IN_PROGRESS,
            assignedTo = "admin-1"
        )
        val inProgressAlert2 = createAlert(
            userId = "user-2",
            status = AlertStatus.IN_PROGRESS,
            assignedTo = "admin-2"
        )
        val completedAlert = createAlert(
            status = AlertStatus.COMPLETED,
            processedAt = Instant.now()
        )

        alertRepository.saveAll(
            listOf(unreadAlert, inProgressAlert1, inProgressAlert2, completedAlert)
        ).blockLast()

        // When: status=IN_PROGRESS로 검색
        val criteria = AlertSearchCriteria(
            status = AlertStatus.IN_PROGRESS,
            page = 0,
            size = 50
        )

        // Then: IN_PROGRESS 상태의 알림만 반환 (2개)
        StepVerifier.create(customAlertRepository.findByCriteria(criteria))
            .assertNext { alert ->
                assertThat(alert.status).isEqualTo(AlertStatus.IN_PROGRESS)
                assertThat(alert.assignedTo).isNotNull()
            }
            .assertNext { alert ->
                assertThat(alert.status).isEqualTo(AlertStatus.IN_PROGRESS)
                assertThat(alert.assignedTo).isNotNull()
            }
            .verifyComplete()

        // Then: 전체 개수도 2개로 확인
        StepVerifier.create(customAlertRepository.countByCriteria(criteria))
            .assertNext { count -> assertThat(count).isEqualTo(2L) }
            .verifyComplete()
    }

    @Test
    fun `Given 다양한 상태의 알림이 존재 When COMPLETED로 검색 Then 완료된 알림만 반환`() {
        // Given: 다양한 상태의 알림 생성
        val unreadAlert = createAlert(status = AlertStatus.UNREAD)
        val inProgressAlert = createAlert(status = AlertStatus.IN_PROGRESS, assignedTo = "admin-1")
        val completedAlert1 = createAlert(
            userId = "user-1",
            status = AlertStatus.COMPLETED,
            assignedTo = "admin-1",
            processedAt = Instant.now(),
            actionNote = "오탐 확인"
        )
        val completedAlert2 = createAlert(
            userId = "user-2",
            status = AlertStatus.COMPLETED,
            assignedTo = "admin-2",
            processedAt = Instant.now(),
            actionNote = "정상 처리"
        )

        alertRepository.saveAll(
            listOf(unreadAlert, inProgressAlert, completedAlert1, completedAlert2)
        ).blockLast()

        // When: status=COMPLETED로 검색
        val criteria = AlertSearchCriteria(
            status = AlertStatus.COMPLETED,
            page = 0,
            size = 50
        )

        // Then: COMPLETED 상태의 알림만 반환 (2개)
        StepVerifier.create(customAlertRepository.findByCriteria(criteria))
            .assertNext { alert ->
                assertThat(alert.status).isEqualTo(AlertStatus.COMPLETED)
                assertThat(alert.processedAt).isNotNull()
                assertThat(alert.actionNote).isNotNull()
            }
            .assertNext { alert ->
                assertThat(alert.status).isEqualTo(AlertStatus.COMPLETED)
                assertThat(alert.processedAt).isNotNull()
                assertThat(alert.actionNote).isNotNull()
            }
            .verifyComplete()

        // Then: 전체 개수도 2개로 확인
        StepVerifier.create(customAlertRepository.countByCriteria(criteria))
            .assertNext { count -> assertThat(count).isEqualTo(2L) }
            .verifyComplete()
    }

    @Test
    fun `Given 모든 알림이 COMPLETED 상태 When UNREAD로 검색 Then 빈 결과 반환`() {
        // Given: COMPLETED 상태의 알림만 존재
        val completedAlerts = listOf(
            createAlert(status = AlertStatus.COMPLETED, processedAt = Instant.now()),
            createAlert(status = AlertStatus.COMPLETED, processedAt = Instant.now())
        )
        alertRepository.saveAll(completedAlerts).blockLast()

        // When: 존재하지 않는 UNREAD 상태로 검색
        val criteria = AlertSearchCriteria(
            status = AlertStatus.UNREAD,
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
        status: AlertStatus = AlertStatus.UNREAD,
        assignedTo: String? = null,
        actionNote: String? = null,
        processedAt: Instant? = null
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
            assignedTo = assignedTo,
            actionNote = actionNote,
            processedAt = processedAt,
            createdAt = Instant.now()
        )
    }
}
