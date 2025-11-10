package com.realfds.alert.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;

/**
 * Alert 모델 클래스
 *
 * 탐지된 의심스러운 거래 패턴을 나타내는 알림 엔터티입니다.
 * fraud-detector에서 생성되어 Kafka transaction-alerts 토픽으로 발행됩니다.
 *
 * 기존 필드:
 * - schemaVersion: 스키마 버전 (고정값: "1.0")
 * - alertId: 알림 고유 식별자 (UUID v4 형식)
 * - originalTransaction: 탐지된 원본 거래 (Transaction 엔터티 전체 포함)
 * - ruleType: 규칙 유형 ("SIMPLE_RULE" 또는 "STATEFUL_RULE")
 * - ruleName: 탐지 규칙 이름 ("HIGH_VALUE", "FOREIGN_COUNTRY", "HIGH_FREQUENCY")
 * - reason: 한국어 설명 (사용자에게 표시될 사유)
 * - severity: 심각도 ("HIGH", "MEDIUM", "LOW", "CRITICAL")
 * - alertTimestamp: 알림 생성 시각 (ISO 8601 형식)
 *
 * 신규 필드 (002-alert-management):
 * - status: 처리 상태 (UNREAD, IN_PROGRESS, COMPLETED) - 기본값: UNREAD
 * - assignedTo: 담당자 이름 (최대 100자, null 허용)
 * - actionNote: 조치 내용 (최대 2000자, null 허용)
 * - processedAt: 처리 완료 시각 (status=COMPLETED 시 자동 설정, null 허용)
 */
public class Alert {

    @JsonProperty("schemaVersion")
    private String schemaVersion;

    @JsonProperty("alertId")
    private String alertId;

    @JsonProperty("originalTransaction")
    private Transaction originalTransaction;

    @JsonProperty("ruleType")
    private String ruleType;

    @JsonProperty("ruleName")
    private String ruleName;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("alertTimestamp")
    private String alertTimestamp;

    // 신규 필드 (002-alert-management)
    @JsonProperty("status")
    private AlertStatus status = AlertStatus.UNREAD;

    @JsonProperty("assignedTo")
    private String assignedTo;

    @JsonProperty("actionNote")
    private String actionNote;

    @JsonProperty("processedAt")
    private Instant processedAt;

    // 기본 생성자 (Jackson 역직렬화용)
    public Alert() {
        this.status = AlertStatus.UNREAD;
    }

    // 전체 필드 생성자
    public Alert(String schemaVersion, String alertId, Transaction originalTransaction,
                 String ruleType, String ruleName, String reason,
                 String severity, String alertTimestamp) {
        this.schemaVersion = schemaVersion;
        this.alertId = alertId;
        this.originalTransaction = originalTransaction;
        this.ruleType = ruleType;
        this.ruleName = ruleName;
        this.reason = reason;
        this.severity = severity;
        this.alertTimestamp = alertTimestamp;
    }

    // Getters
    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getAlertId() {
        return alertId;
    }

    public Transaction getOriginalTransaction() {
        return originalTransaction;
    }

    public String getRuleType() {
        return ruleType;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getReason() {
        return reason;
    }

    public String getSeverity() {
        return severity;
    }

    public String getAlertTimestamp() {
        return alertTimestamp;
    }

    /**
     * 알림 처리 상태 반환
     *
     * @return 처리 상태 (UNREAD, IN_PROGRESS, COMPLETED)
     */
    public AlertStatus getStatus() {
        return status;
    }

    /**
     * 담당자 이름 반환
     *
     * @return 담당자 이름 (미할당 시 null)
     */
    public String getAssignedTo() {
        return assignedTo;
    }

    /**
     * 조치 내용 반환
     *
     * @return 조치 내용 (없으면 null)
     */
    public String getActionNote() {
        return actionNote;
    }

    /**
     * 처리 완료 시각 반환
     *
     * @return 처리 완료 시각 (status=COMPLETED 시 자동 설정, 미완료 시 null)
     */
    public Instant getProcessedAt() {
        return processedAt;
    }

    // Setters
    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public void setOriginalTransaction(Transaction originalTransaction) {
        this.originalTransaction = originalTransaction;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setAlertTimestamp(String alertTimestamp) {
        this.alertTimestamp = alertTimestamp;
    }

    /**
     * 알림 처리 상태 설정
     *
     * @param status 처리 상태 (UNREAD, IN_PROGRESS, COMPLETED)
     * @throws IllegalArgumentException status가 null인 경우
     */
    public void setStatus(AlertStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status는 null일 수 없습니다");
        }
        this.status = status;

        // COMPLETED 상태로 변경 시 processedAt 자동 설정
        if (status == AlertStatus.COMPLETED && this.processedAt == null) {
            this.processedAt = Instant.now();
        }
    }

    /**
     * 담당자 이름 설정
     *
     * @param assignedTo 담당자 이름 (최대 100자, null 허용)
     * @throws IllegalArgumentException assignedTo가 100자를 초과하는 경우
     */
    public void setAssignedTo(String assignedTo) {
        if (assignedTo != null && assignedTo.length() > 100) {
            throw new IllegalArgumentException(
                "담당자 이름은 100자를 초과할 수 없습니다. 현재 길이: " + assignedTo.length()
            );
        }
        this.assignedTo = assignedTo;
    }

    /**
     * 조치 내용 설정
     *
     * @param actionNote 조치 내용 (최대 2000자, null 허용)
     * @throws IllegalArgumentException actionNote가 2000자를 초과하는 경우
     */
    public void setActionNote(String actionNote) {
        if (actionNote != null && actionNote.length() > 2000) {
            throw new IllegalArgumentException(
                "조치 내용은 2000자를 초과할 수 없습니다. 현재 길이: " + actionNote.length()
            );
        }
        this.actionNote = actionNote;
    }

    /**
     * 처리 완료 시각 설정
     * 일반적으로 자동 설정되므로 직접 호출할 필요 없음
     *
     * @param processedAt 처리 완료 시각
     */
    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Alert alert = (Alert) o;
        return Objects.equals(alertId, alert.alertId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alertId);
    }

    @Override
    public String toString() {
        return "Alert{" +
                "schemaVersion='" + schemaVersion + '\'' +
                ", alertId='" + alertId + '\'' +
                ", ruleType='" + ruleType + '\'' +
                ", ruleName='" + ruleName + '\'' +
                ", reason='" + reason + '\'' +
                ", severity='" + severity + '\'' +
                ", alertTimestamp='" + alertTimestamp + '\'' +
                ", status=" + status +
                ", assignedTo='" + assignedTo + '\'' +
                ", actionNote='" + actionNote + '\'' +
                ", processedAt=" + processedAt +
                ", originalTransaction=" + originalTransaction +
                '}';
    }
}
