package com.realfds.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;

/**
 * Alert 모델 클래스
 *
 * 탐지된 의심스러운 거래 패턴을 나타내는 알림 엔터티입니다.
 * fraud-detector에서 생성되어 Kafka transaction-alerts 토픽으로 발행됩니다.
 *
 * 필드:
 * - schemaVersion: 스키마 버전 (고정값: "1.0")
 * - alertId: 알림 고유 식별자 (UUID v4 형식)
 * - originalTransaction: 탐지된 원본 거래 (Transaction 엔터티 전체 포함)
 * - ruleType: 규칙 유형 ("SIMPLE_RULE" 또는 "STATEFUL_RULE")
 * - ruleName: 탐지 규칙 이름 ("HIGH_VALUE", "FOREIGN_COUNTRY", "HIGH_FREQUENCY")
 * - reason: 한국어 설명 (사용자에게 표시될 사유)
 * - severity: 심각도 ("HIGH", "MEDIUM", "LOW")
 * - alertTimestamp: 알림 생성 시각 (ISO 8601 형식)
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

    // 기본 생성자 (Jackson 역직렬화용)
    public Alert() {
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
                ", originalTransaction=" + originalTransaction +
                '}';
    }
}
