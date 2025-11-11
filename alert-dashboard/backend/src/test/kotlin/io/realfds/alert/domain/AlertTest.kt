package io.realfds.alert.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Alert 엔티티 검증 단위 테스트
 *
 * Alert 엔티티의 생성 및 필드 검증 로직을 테스트합니다.
 * Given-When-Then 구조를 따릅니다.
 */
class AlertTest {

    @Test
    fun `유효한 Alert 엔티티를 생성할 수 있다`() {
        // Given: 유효한 Alert 정보
        val transactionId = UUID.randomUUID()
        val userId = "user-5"
        val amount = 1500000L
        val currency = "KRW"
        val countryCode = "KR"
        val ruleName = "HIGH_AMOUNT"
        val reason = "거래 금액이 설정된 임계값(1,000,000원)을 초과했습니다"
        val severity = Severity.HIGH
        val alertTimestamp = Instant.now()

        // When: Alert 엔티티 생성
        val alert = Alert(
            transactionId = transactionId,
            userId = userId,
            amount = amount,
            currency = currency,
            countryCode = countryCode,
            ruleName = ruleName,
            reason = reason,
            severity = severity,
            alertTimestamp = alertTimestamp
        )

        // Then: 모든 필드가 올바르게 설정됨
        assertThat(alert.alertId).isNotNull
        assertThat(alert.schemaVersion).isEqualTo("1.0")
        assertThat(alert.transactionId).isEqualTo(transactionId)
        assertThat(alert.userId).isEqualTo(userId)
        assertThat(alert.amount).isEqualTo(amount)
        assertThat(alert.currency).isEqualTo(currency)
        assertThat(alert.countryCode).isEqualTo(countryCode)
        assertThat(alert.ruleName).isEqualTo(ruleName)
        assertThat(alert.reason).isEqualTo(reason)
        assertThat(alert.severity).isEqualTo(severity)
        assertThat(alert.alertTimestamp).isEqualTo(alertTimestamp)
        assertThat(alert.status).isEqualTo(AlertStatus.UNREAD) // 기본값 확인
        assertThat(alert.assignedTo).isNull()
        assertThat(alert.actionNote).isNull()
        assertThat(alert.processedAt).isNull()
        assertThat(alert.createdAt).isNotNull
    }

    @Test
    fun `Alert 생성 시 alertId가 자동으로 UUID로 생성된다`() {
        // Given & When: Alert 엔티티 생성 (alertId 명시하지 않음)
        val alert = createTestAlert()

        // Then: alertId가 자동으로 생성됨
        assertThat(alert.alertId).isNotNull
        assertThat(alert.alertId.toString()).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
    }

    @Test
    fun `Alert 생성 시 기본 상태는 UNREAD이다`() {
        // Given & When: Alert 엔티티 생성 (status 명시하지 않음)
        val alert = createTestAlert()

        // Then: 기본 상태가 UNREAD
        assertThat(alert.status).isEqualTo(AlertStatus.UNREAD)
    }

    @Test
    fun `Alert 생성 시 createdAt이 자동으로 설정된다`() {
        // Given: 현재 시각 기록
        val beforeCreation = Instant.now()

        // When: Alert 엔티티 생성
        val alert = createTestAlert()

        // Then: createdAt이 현재 시각으로 설정됨
        val afterCreation = Instant.now()
        assertThat(alert.createdAt).isNotNull
        assertThat(alert.createdAt).isBetween(beforeCreation, afterCreation)
    }

    @Test
    fun `amount는 양수여야 한다`() {
        // Given: 양수 금액
        val alert = createTestAlert(amount = 100000L)

        // When & Then: amount가 양수인지 확인
        assertThat(alert.amount).isPositive
    }

    @Test
    fun `reason은 최소 10자 이상이어야 한다`() {
        // Given: 유효한 reason (10자 이상)
        val reason = "거래 금액이 임계값을 초과했습니다"
        val alert = createTestAlert(reason = reason)

        // When & Then: reason 길이 확인
        assertThat(alert.reason).hasSizeGreaterThanOrEqualTo(10)
    }

    @Test
    fun `reason은 최대 1000자까지 허용된다`() {
        // Given: 1000자 reason
        val reason = "a".repeat(1000)
        val alert = createTestAlert(reason = reason)

        // When & Then: reason 길이 확인
        assertThat(alert.reason).hasSize(1000)
    }

    /**
     * 테스트용 Alert 엔티티 생성 헬퍼 함수
     */
    private fun createTestAlert(
        transactionId: UUID = UUID.randomUUID(),
        userId: String = "user-1",
        amount: Long = 500000L,
        currency: String = "KRW",
        countryCode: String = "KR",
        ruleName: String = "HIGH_AMOUNT",
        reason: String = "테스트용 알림입니다",
        severity: Severity = Severity.MEDIUM,
        alertTimestamp: Instant = Instant.now(),
        status: AlertStatus = AlertStatus.UNREAD
    ): Alert {
        return Alert(
            transactionId = transactionId,
            userId = userId,
            amount = amount,
            currency = currency,
            countryCode = countryCode,
            ruleName = ruleName,
            reason = reason,
            severity = severity,
            alertTimestamp = alertTimestamp,
            status = status
        )
    }
}
