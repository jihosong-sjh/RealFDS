package io.realfds.alert.service

import io.realfds.alert.domain.Alert
import io.realfds.alert.domain.AlertStatus
import io.realfds.alert.domain.Severity
import io.realfds.alert.repository.AlertRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Alert 서비스
 *
 * 알림 생성 및 영속화를 담당합니다.
 * Kafka에서 AlertEvent를 수신하면 PostgreSQL에 저장하고,
 * 실시간 WebSocket 전송도 처리합니다.
 */
@Service
class AlertService(
    private val alertRepository: AlertRepository
) {
    private val logger = LoggerFactory.getLogger(AlertService::class.java)

    /**
     * AlertEvent를 수신하여 PostgreSQL에 저장합니다.
     *
     * 저장 실패 시 최대 3회 재시도하며, 재시도 간격은 1초씩 증가합니다.
     * 실시간 WebSocket 전송은 별도로 처리되며, 데이터베이스 저장 실패 시에도
     * WebSocket 전송은 정상적으로 동작합니다.
     *
     * @param alertEvent Kafka에서 수신한 AlertEvent
     * @return 저장된 Alert (Mono)
     */
    fun processAndSaveAlert(alertEvent: AlertEvent): Mono<Alert> {
        logger.info("알림 저장 시작: alertId={}", alertEvent.alertId)

        // AlertEvent를 Alert 엔티티로 변환
        val alert = alertEvent.toAlert()

        // PostgreSQL에 저장 (재시도 로직 포함)
        return alertRepository.save(alert)
            .retryWhen(
                Retry.backoff(3, Duration.ofSeconds(1))
                    .doBeforeRetry { retrySignal ->
                        logger.warn(
                            "알림 저장 재시도 {}/3: alertId={}",
                            retrySignal.totalRetries() + 1,
                            alertEvent.alertId
                        )
                    }
            )
            .doOnSuccess {
                logger.info("알림 저장 성공: alertId={}", it.alertId)
            }
            .doOnError { error ->
                logger.error("알림 저장 실패: alertId={}", alertEvent.alertId, error)
            }
    }
}

/**
 * AlertEvent DTO (Kafka 메시지)
 *
 * Kafka Topic (transaction-alerts)에서 수신하는 알림 이벤트입니다.
 * transaction-processor에서 발행한 메시지를 수신합니다.
 *
 * @property alertId 알림 고유 식별자
 * @property schemaVersion 스키마 버전
 * @property transactionId 원본 거래 ID
 * @property userId 사용자 ID
 * @property amount 거래 금액
 * @property currency 통화 코드
 * @property countryCode 국가 코드
 * @property ruleName 탐지 규칙명
 * @property reason 알림 발생 이유
 * @property severity 심각도
 * @property alertTimestamp 알림 발생 시각
 */
data class AlertEvent(
    val alertId: UUID,
    val schemaVersion: String,
    val transactionId: UUID,
    val userId: String,
    val amount: Long,
    val currency: String,
    val countryCode: String,
    val ruleName: String,
    val reason: String,
    val severity: String, // HIGH, MEDIUM, LOW
    val alertTimestamp: Instant
) {
    /**
     * AlertEvent를 Alert 엔티티로 변환합니다.
     *
     * Severity 문자열을 Enum으로 변환하며,
     * 기본 상태는 UNREAD로 설정됩니다.
     */
    fun toAlert(): Alert {
        return Alert(
            alertId = alertId,
            schemaVersion = schemaVersion,
            transactionId = transactionId,
            userId = userId,
            amount = amount,
            currency = currency,
            countryCode = countryCode,
            ruleName = ruleName,
            reason = reason,
            severity = Severity.valueOf(severity),
            alertTimestamp = alertTimestamp,
            status = AlertStatus.UNREAD,
            createdAt = Instant.now()
        )
    }
}
