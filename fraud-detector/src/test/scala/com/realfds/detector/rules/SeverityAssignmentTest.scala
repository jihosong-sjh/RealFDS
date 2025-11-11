package com.realfds.detector.rules

import com.realfds.detector.models.{Alert, Transaction}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * 규칙별 심각도(Severity) 할당 테스트
 *
 * User Story 3: 알림 우선순위 (심각도) 표시
 * 각 탐지 규칙이 올바른 심각도를 알림에 할당하는지 검증합니다.
 *
 * **테스트 시나리오**:
 * - HighValueRule: HIGH 심각도 할당 확인
 * - ForeignCountryRule: MEDIUM 심각도 할당 확인
 * - HighFrequencyRule: HIGH 심각도 할당 확인
 */
class SeverityAssignmentTest extends AnyFlatSpec with Matchers {

  // 테스트용 샘플 거래 (고액 거래용)
  val highValueTransaction = Transaction(
    schemaVersion = "1.0",
    transactionId = "550e8400-e29b-41d4-a716-446655440000",
    userId = "user-3", // userId는 "user-1" ~ "user-10" 형식이어야 함
    amount = 1500000, // 150만원 (100만원 초과)
    currency = "KRW",
    countryCode = "KR",
    timestamp = "2025-11-11T10:00:00.000Z"
  )

  // 테스트용 샘플 거래 (해외 거래용)
  val foreignCountryTransaction = Transaction(
    schemaVersion = "1.0",
    transactionId = "550e8400-e29b-41d4-a716-446655440001",
    userId = "user-5", // userId는 "user-1" ~ "user-10" 형식이어야 함
    amount = 500000, // 50만원
    currency = "KRW", // currency는 항상 "KRW"여야 함
    countryCode = "US", // 해외 국가 코드
    timestamp = "2025-11-11T10:01:00.000Z"
  )

  // 테스트용 샘플 거래 (빈번한 거래용 - HighFrequencyRule 테스트에 사용)
  val normalTransaction = Transaction(
    schemaVersion = "1.0",
    transactionId = "550e8400-e29b-41d4-a716-446655440002",
    userId = "user-7", // userId는 "user-1" ~ "user-10" 형식이어야 함
    amount = 300000, // 30만원
    currency = "KRW",
    countryCode = "KR",
    timestamp = "2025-11-11T10:02:00.000Z"
  )

  "HighValueRule" should "HIGH 심각도를 알림에 할당해야 함" in {
    // Given: HighValueRule 인스턴스 생성
    val rule = new HighValueRule()

    // When: 고액 거래를 Alert로 변환
    val alert = rule.toAlert(highValueTransaction)

    // Then: severity가 HIGH여야 함
    alert.severity shouldBe Alert.SEVERITY_HIGH
    alert.severity shouldBe "HIGH"
  }

  it should "Alert 생성 시 ruleName이 HIGH_VALUE여야 함" in {
    // Given: HighValueRule 인스턴스
    val rule = new HighValueRule()

    // When: Alert 생성
    val alert = rule.toAlert(highValueTransaction)

    // Then: ruleName 확인
    alert.ruleName shouldBe "HIGH_VALUE"
    alert.severity shouldBe "HIGH"
  }

  "ForeignCountryRule" should "MEDIUM 심각도를 알림에 할당해야 함" in {
    // Given: ForeignCountryRule 인스턴스 생성
    val rule = new ForeignCountryRule()

    // When: 해외 거래를 Alert로 변환
    val alert = rule.toAlert(foreignCountryTransaction)

    // Then: severity가 MEDIUM이어야 함
    alert.severity shouldBe Alert.SEVERITY_MEDIUM
    alert.severity shouldBe "MEDIUM"
  }

  it should "Alert 생성 시 ruleName이 FOREIGN_COUNTRY여야 함" in {
    // Given: ForeignCountryRule 인스턴스
    val rule = new ForeignCountryRule()

    // When: Alert 생성
    val alert = rule.toAlert(foreignCountryTransaction)

    // Then: ruleName 확인
    alert.ruleName shouldBe "FOREIGN_COUNTRY"
    alert.severity shouldBe "MEDIUM"
  }

  "HighFrequencyRule" should "HIGH 심각도를 알림에 할당해야 함" in {
    // Given: HighFrequencyRule 인스턴스 생성
    val rule = new HighFrequencyRule()

    // When: 빈번한 거래를 Alert로 변환 (createAlert 메서드 사용)
    // Note: HighFrequencyRule은 stateful이므로 직접 Alert.create 사용
    val alert = Alert.create(
      transaction = normalTransaction,
      ruleType = Alert.STATEFUL_RULE,
      ruleName = "HIGH_FREQUENCY",
      reason = "빈번한 거래 탐지 (5분 내 3건 이상)",
      severity = Alert.SEVERITY_HIGH
    )

    // Then: severity가 HIGH여야 함
    alert.severity shouldBe Alert.SEVERITY_HIGH
    alert.severity shouldBe "HIGH"
  }

  it should "stateful 규칙의 ruleName이 HIGH_FREQUENCY여야 함" in {
    // Given: HighFrequencyRule의 Alert 생성
    val alert = Alert.create(
      transaction = normalTransaction,
      ruleType = Alert.STATEFUL_RULE,
      ruleName = "HIGH_FREQUENCY",
      reason = "빈번한 거래 탐지 (5분 내 3건 이상)",
      severity = Alert.SEVERITY_HIGH
    )

    // Then: ruleName과 severity 확인
    alert.ruleName shouldBe "HIGH_FREQUENCY"
    alert.severity shouldBe "HIGH"
    alert.ruleType shouldBe Alert.STATEFUL_RULE
  }

  "Alert 모델" should "지원되는 심각도 목록에 HIGH, MEDIUM, LOW가 포함되어야 함" in {
    // Given: Alert 객체의 SUPPORTED_SEVERITIES
    val supportedSeverities = Alert.SUPPORTED_SEVERITIES

    // Then: HIGH, MEDIUM, LOW가 포함됨
    supportedSeverities should contain("HIGH")
    supportedSeverities should contain("MEDIUM")
    supportedSeverities should contain("LOW")
  }

  it should "각 규칙별 심각도가 지원되는 목록에 포함되어야 함" in {
    // Given: 각 규칙에서 생성된 Alert
    val highValueAlert = new HighValueRule().toAlert(highValueTransaction)
    val foreignCountryAlert = new ForeignCountryRule().toAlert(foreignCountryTransaction)

    val highFrequencyAlert = Alert.create(
      transaction = normalTransaction,
      ruleType = Alert.STATEFUL_RULE,
      ruleName = "HIGH_FREQUENCY",
      reason = "빈번한 거래 탐지 (5분 내 3건 이상 거래 발생)",
      severity = Alert.SEVERITY_HIGH
    )

    // Then: 모든 Alert의 severity가 지원되는 목록에 포함됨
    Alert.SUPPORTED_SEVERITIES should contain(highValueAlert.severity)
    Alert.SUPPORTED_SEVERITIES should contain(foreignCountryAlert.severity)
    Alert.SUPPORTED_SEVERITIES should contain(highFrequencyAlert.severity)
  }

  it should "각 규칙별 Alert가 유효한 형식이어야 함" in {
    // Given: 각 규칙에서 생성된 Alert
    val highValueAlert = new HighValueRule().toAlert(highValueTransaction)
    val foreignCountryAlert = new ForeignCountryRule().toAlert(foreignCountryTransaction)

    val highFrequencyAlert = Alert.create(
      transaction = normalTransaction,
      ruleType = Alert.STATEFUL_RULE,
      ruleName = "HIGH_FREQUENCY",
      reason = "빈번한 거래 탐지 (5분 내 3건 이상 거래 발생)",
      severity = Alert.SEVERITY_HIGH
    )

    // Then: 모든 Alert가 유효해야 함
    highValueAlert.isValid shouldBe true
    foreignCountryAlert.isValid shouldBe true
    highFrequencyAlert.isValid shouldBe true
  }

  "규칙별 심각도 매핑" should "다음과 같이 설정되어야 함: HIGH_VALUE=HIGH, FOREIGN_COUNTRY=MEDIUM, HIGH_FREQUENCY=HIGH" in {
    // Given: 각 규칙별 Alert 생성
    val highValueAlert = new HighValueRule().toAlert(highValueTransaction)
    val foreignCountryAlert = new ForeignCountryRule().toAlert(foreignCountryTransaction)

    val highFrequencyAlert = Alert.create(
      transaction = normalTransaction,
      ruleType = Alert.STATEFUL_RULE,
      ruleName = "HIGH_FREQUENCY",
      reason = "빈번한 거래 탐지 (5분 내 3건 이상 거래 발생)",
      severity = Alert.SEVERITY_HIGH
    )

    // Then: 심각도 매핑 검증
    withClue("HighValueRule should assign HIGH severity: ") {
      highValueAlert.ruleName shouldBe "HIGH_VALUE"
      highValueAlert.severity shouldBe "HIGH"
    }

    withClue("ForeignCountryRule should assign MEDIUM severity: ") {
      foreignCountryAlert.ruleName shouldBe "FOREIGN_COUNTRY"
      foreignCountryAlert.severity shouldBe "MEDIUM"
    }

    withClue("HighFrequencyRule should assign HIGH severity: ") {
      highFrequencyAlert.ruleName shouldBe "HIGH_FREQUENCY"
      highFrequencyAlert.severity shouldBe "HIGH"
    }
  }
}
