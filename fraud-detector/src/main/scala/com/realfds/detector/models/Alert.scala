package com.realfds.detector.models

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}
import java.util.UUID
import java.time.Instant

/**
 * 탐지된 알림 엔터티
 * fraud-detector에서 생성되어 Kafka transaction-alerts 토픽으로 발행되는 데이터 모델
 *
 * @param schemaVersion 스키마 버전 (고정값: "1.0")
 * @param alertId 알림 고유 식별자 (UUID v4 형식)
 * @param originalTransaction 탐지된 원본 거래 (Transaction 엔터티 전체 포함)
 * @param ruleType 규칙 유형 ("SIMPLE_RULE" 또는 "STATEFUL_RULE")
 * @param ruleName 탐지 규칙 이름 ("HIGH_VALUE", "FOREIGN_COUNTRY", "HIGH_FREQUENCY")
 * @param reason 한국어 설명 (사용자에게 표시될 사유)
 * @param severity 심각도 ("HIGH", "MEDIUM", "LOW")
 * @param alertTimestamp 알림 생성 시각 (ISO 8601 형식, UTC)
 */
case class Alert(
  @JsonProperty("schemaVersion") schemaVersion: String,
  @JsonProperty("alertId") alertId: String,
  @JsonProperty("originalTransaction") originalTransaction: Transaction,
  @JsonProperty("ruleType") ruleType: String,
  @JsonProperty("ruleName") ruleName: String,
  @JsonProperty("reason") reason: String,
  @JsonProperty("severity") severity: String,
  @JsonProperty("alertTimestamp") alertTimestamp: String
) {

  /**
   * 알림이 유효한지 검증
   * @return 유효하면 true, 그렇지 않으면 false
   */
  @JsonIgnore
  def isValid: Boolean = {
    schemaVersion == "1.0" &&
      alertId.nonEmpty &&
      originalTransaction.isValid &&
      Set("SIMPLE_RULE", "STATEFUL_RULE").contains(ruleType) &&
      Alert.SUPPORTED_RULES.contains(ruleName) &&
      reason.nonEmpty && reason.length <= 200 &&
      Alert.SUPPORTED_SEVERITIES.contains(severity) &&
      alertTimestamp.nonEmpty
  }

  /**
   * 알림 생성 시각이 원본 거래 시각 이후인지 확인 (논리적 순서)
   * @return 논리적으로 유효하면 true
   */
  @JsonIgnore
  def isChronologicallyValid: Boolean = {
    try {
      val alertTime = Instant.parse(alertTimestamp)
      val txTime = Instant.parse(originalTransaction.timestamp)
      alertTime.isAfter(txTime) || alertTime.equals(txTime)
    } catch {
      case _: Exception => false
    }
  }

  /**
   * 알림 요약 정보를 한국어로 반환
   * @return 예: "[HIGH] user-3: 고액 거래 (100만원 초과)"
   */
  def summary: String = s"[$severity] ${originalTransaction.userId}: $reason"
}

/**
 * Alert 케이스 클래스를 위한 컴패니언 객체
 * 기본값 및 유틸리티 메서드 제공
 */
object Alert {

  /**
   * 기본 스키마 버전
   */
  val DEFAULT_SCHEMA_VERSION = "1.0"

  /**
   * 지원되는 탐지 규칙 목록
   */
  val SUPPORTED_RULES = Set("HIGH_VALUE", "FOREIGN_COUNTRY", "HIGH_FREQUENCY")

  /**
   * 지원되는 심각도 목록
   */
  val SUPPORTED_SEVERITIES = Set("HIGH", "MEDIUM", "LOW")

  /**
   * 규칙 유형: 단순 규칙 (상태 없음)
   */
  val SIMPLE_RULE = "SIMPLE_RULE"

  /**
   * 규칙 유형: 상태 저장 규칙 (윈도우 기반)
   */
  val STATEFUL_RULE = "STATEFUL_RULE"

  /**
   * 심각도: 높음
   */
  val SEVERITY_HIGH = "HIGH"

  /**
   * 심각도: 중간
   */
  val SEVERITY_MEDIUM = "MEDIUM"

  /**
   * 심각도: 낮음
   */
  val SEVERITY_LOW = "LOW"

  /**
   * 새 Alert 인스턴스를 생성하는 팩토리 메서드
   * @param transaction 원본 거래
   * @param ruleType 규칙 유형
   * @param ruleName 규칙 이름
   * @param reason 한국어 설명
   * @param severity 심각도
   * @return 생성된 Alert 인스턴스
   */
  def create(
    transaction: Transaction,
    ruleType: String,
    ruleName: String,
    reason: String,
    severity: String
  ): Alert = {
    Alert(
      schemaVersion = DEFAULT_SCHEMA_VERSION,
      alertId = UUID.randomUUID().toString,
      originalTransaction = transaction,
      ruleType = ruleType,
      ruleName = ruleName,
      reason = reason,
      severity = severity,
      alertTimestamp = Instant.now().toString
    )
  }
}
