package com.realfds.alert.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Transaction 모델 클래스
 *
 * 금융 거래를 나타내는 핵심 엔터티입니다.
 * transaction-generator에서 생성되어 Kafka virtual-transactions 토픽으로 발행됩니다.
 *
 * 필드:
 * - schemaVersion: 스키마 버전 (고정값: "1.0")
 * - transactionId: 거래 고유 식별자 (UUID v4 형식)
 * - userId: 거래 발생 사용자 ID ("user-1" ~ "user-10")
 * - amount: 거래 금액 (정수, 1,000 ~ 1,500,000 KRW 범위)
 * - currency: 통화 코드 (고정값: "KRW")
 * - countryCode: 거래 발생 국가 코드 (ISO 3166-1 alpha-2)
 * - timestamp: 거래 발생 시각 (ISO 8601 형식)
 */
public class Transaction {

    @JsonProperty("schemaVersion")
    private String schemaVersion;

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("countryCode")
    private String countryCode;

    @JsonProperty("timestamp")
    private String timestamp;

    // 기본 생성자 (Jackson 역직렬화용)
    public Transaction() {
    }

    // 전체 필드 생성자
    public Transaction(String schemaVersion, String transactionId, String userId,
                       Long amount, String currency, String countryCode, String timestamp) {
        this.schemaVersion = schemaVersion;
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.countryCode = countryCode;
        this.timestamp = timestamp;
    }

    // Getters
    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public Long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "schemaVersion='" + schemaVersion + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", userId='" + userId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
