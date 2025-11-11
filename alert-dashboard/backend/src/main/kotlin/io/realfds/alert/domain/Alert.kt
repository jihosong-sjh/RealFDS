package io.realfds.alert.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * 알림 엔티티
 *
 * 사기 탐지 시스템에서 발생한 알림 정보를 저장하는 핵심 엔티티입니다.
 * PostgreSQL의 alerts 테이블과 매핑됩니다.
 *
 * @property alertId 알림 고유 식별자 (UUID)
 * @property schemaVersion 스키마 버전 (현재: "1.0")
 * @property transactionId 원본 거래 ID
 * @property userId 사용자 ID (예: user-1, user-10)
 * @property amount 거래 금액 (KRW, 정수)
 * @property currency 통화 코드 (예: KRW, USD)
 * @property countryCode 국가 코드 (예: KR, US, JP, CN)
 * @property ruleName 탐지 규칙명 (예: HIGH_AMOUNT, FOREIGN_COUNTRY, RAPID_TRANSACTION)
 * @property reason 알림 발생 이유 (한국어 설명)
 * @property severity 심각도 (HIGH, MEDIUM, LOW)
 * @property alertTimestamp 알림 발생 시각
 * @property status 알림 처리 상태 (UNREAD, IN_PROGRESS, COMPLETED)
 * @property assignedTo 담당자 ID (선택적)
 * @property actionNote 처리 내용 메모 (선택적)
 * @property processedAt 처리 완료 시각 (선택적)
 * @property createdAt 데이터베이스 생성 시각
 */
@Table("alerts")
data class Alert(
    /**
     * 알림 고유 식별자
     * UUID v4 형식으로 자동 생성됩니다.
     */
    @Id
    @Column("alert_id")
    val alertId: UUID = UUID.randomUUID(),

    /**
     * 스키마 버전
     * 향후 스키마 변경 시 호환성을 위해 사용됩니다.
     * 현재 버전: "1.0"
     */
    @Column("schema_version")
    val schemaVersion: String = "1.0",

    /**
     * 원본 거래 ID
     * 알림을 발생시킨 거래의 고유 식별자입니다.
     */
    @Column("transaction_id")
    val transactionId: UUID,

    /**
     * 사용자 ID
     * 거래를 수행한 사용자의 식별자입니다.
     * 예: user-1, user-2, ..., user-10
     */
    @Column("user_id")
    val userId: String,

    /**
     * 거래 금액 (KRW)
     * 정수로 저장되며, 양수여야 합니다.
     * 예: 1500000 (1,500,000원)
     */
    @Column("amount")
    val amount: Long,

    /**
     * 통화 코드
     * ISO 4217 3자리 코드입니다.
     * 예: KRW, USD, JPY
     */
    @Column("currency")
    val currency: String,

    /**
     * 국가 코드
     * ISO 3166-1 alpha-2 2자리 코드입니다.
     * 예: KR, US, JP, CN
     */
    @Column("country_code")
    val countryCode: String,

    /**
     * 탐지 규칙명
     * 알림을 발생시킨 사기 탐지 규칙의 이름입니다.
     * 예: HIGH_AMOUNT, FOREIGN_COUNTRY, RAPID_TRANSACTION
     */
    @Column("rule_name")
    val ruleName: String,

    /**
     * 알림 발생 이유
     * 왜 이 알림이 발생했는지 한국어로 설명합니다.
     * 최소 10자 이상, 최대 1000자입니다.
     */
    @Column("reason")
    val reason: String,

    /**
     * 심각도
     * 알림의 심각도 수준을 나타냅니다.
     * HIGH: 즉시 대응 필요
     * MEDIUM: 주의 필요
     * LOW: 모니터링 필요
     */
    @Column("severity")
    val severity: Severity,

    /**
     * 알림 발생 시각
     * 알림이 생성된 시점의 타임스탬프입니다.
     * 시간대 정보(Time Zone)를 포함합니다.
     */
    @Column("alert_timestamp")
    val alertTimestamp: Instant,

    /**
     * 알림 처리 상태
     * UNREAD: 미확인 (초기 상태)
     * IN_PROGRESS: 진행 중
     * COMPLETED: 완료 (최종 상태)
     * 기본값: UNREAD
     */
    @Column("status")
    val status: AlertStatus = AlertStatus.UNREAD,

    /**
     * 담당자 ID (선택적)
     * 이 알림을 처리하는 보안 담당자의 식별자입니다.
     * IN_PROGRESS 상태일 때 설정됩니다.
     */
    @Column("assigned_to")
    val assignedTo: String? = null,

    /**
     * 처리 내용 메모 (선택적)
     * 담당자가 작성한 조사 내용이나 조치 내용입니다.
     * 최대 2000자까지 저장 가능합니다.
     */
    @Column("action_note")
    val actionNote: String? = null,

    /**
     * 처리 완료 시각 (선택적)
     * 알림 처리가 완료된 시점의 타임스탬프입니다.
     * status가 COMPLETED일 때 반드시 설정되어야 합니다.
     */
    @Column("processed_at")
    val processedAt: Instant? = null,

    /**
     * 데이터베이스 생성 시각
     * 데이터베이스에 레코드가 삽입된 시점의 타임스탬프입니다.
     * 자동으로 설정됩니다.
     */
    @Column("created_at")
    val createdAt: Instant = Instant.now()
) {
    init {
        // 금액은 양수여야 함
        require(amount > 0) { "거래 금액은 0보다 커야 합니다" }

        // 사유는 최소 10자 이상, 최대 1000자 이하
        require(reason.length in 10..1000) {
            "알림 발생 이유는 10자 이상 1000자 이하여야 합니다"
        }

        // 통화 코드는 3자리 대문자
        require(currency.length == 3 && currency.all { it.isUpperCase() }) {
            "통화 코드는 3자리 대문자여야 합니다 (예: KRW)"
        }

        // 국가 코드는 2자리 대문자
        require(countryCode.length == 2 && countryCode.all { it.isUpperCase() }) {
            "국가 코드는 2자리 대문자여야 합니다 (예: KR)"
        }

        // 알림 시각은 미래일 수 없음
        require(alertTimestamp <= Instant.now()) {
            "알림 발생 시각은 미래일 수 없습니다"
        }

        // COMPLETED 상태일 때는 processedAt이 반드시 설정되어야 함
        if (status == AlertStatus.COMPLETED) {
            requireNotNull(processedAt) {
                "COMPLETED 상태일 때는 처리 완료 시각(processedAt)이 반드시 설정되어야 합니다"
            }
        }

        // actionNote는 최대 2000자까지
        actionNote?.let {
            require(it.length <= 2000) {
                "처리 내용 메모는 최대 2000자까지 입력 가능합니다"
            }
        }
    }
}
