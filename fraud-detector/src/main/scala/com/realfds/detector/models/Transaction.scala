package com.realfds.detector.models

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}

/**
 * 금융 거래 엔터티
 * transaction-generator에서 생성되어 Kafka virtual-transactions 토픽으로 발행되는 데이터 모델
 *
 * @param schemaVersion 스키마 버전 (고정값: "1.0")
 * @param transactionId 거래 고유 식별자 (UUID v4 형식)
 * @param userId 거래 발생 사용자 ID ("user-1" ~ "user-10")
 * @param amount 거래 금액 (KRW 정수, 1,000 ~ 1,500,000 범위)
 * @param currency 통화 코드 (고정값: "KRW")
 * @param countryCode 거래 발생 국가 코드 (ISO 3166-1 alpha-2: KR, US, JP, CN)
 * @param timestamp 거래 발생 시각 (ISO 8601 형식, UTC)
 */
case class Transaction(
  @JsonProperty("schemaVersion") schemaVersion: String,
  @JsonProperty("transactionId") transactionId: String,
  @JsonProperty("userId") userId: String,
  @JsonProperty("amount") amount: Long,
  @JsonProperty("currency") currency: String,
  @JsonProperty("countryCode") countryCode: String,
  @JsonProperty("timestamp") timestamp: String
) {

  /**
   * 거래가 유효한지 검증
   * @return 유효하면 true, 그렇지 않으면 false
   */
  @JsonIgnore
  def isValid: Boolean = {
    schemaVersion == "1.0" &&
      transactionId.nonEmpty &&
      userId.matches("^user-(1[0]|[1-9])$") &&
      amount >= 1000 && amount <= 1500000 &&
      currency == "KRW" &&
      countryCode.matches("^[A-Z]{2}$") &&
      timestamp.nonEmpty
  }

  /**
   * 거래 금액을 포맷팅된 문자열로 반환
   * @return 예: "1,250,000원"
   */
  def formattedAmount: String = f"${amount}%,d원"
}

/**
 * Transaction 케이스 클래스를 위한 컴패니언 객체
 * 기본값 및 유틸리티 메서드 제공
 */
object Transaction {

  /**
   * 기본 스키마 버전
   */
  val DEFAULT_SCHEMA_VERSION = "1.0"

  /**
   * 기본 통화 코드
   */
  val DEFAULT_CURRENCY = "KRW"

  /**
   * 최소 거래 금액
   */
  val MIN_AMOUNT = 1000L

  /**
   * 최대 거래 금액
   */
  val MAX_AMOUNT = 1500000L

  /**
   * 지원되는 국가 코드 목록
   */
  val SUPPORTED_COUNTRIES = Set("KR", "US", "JP", "CN")
}
