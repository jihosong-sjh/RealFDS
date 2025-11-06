package com.realfds.detector.rules

import com.realfds.detector.models.{Alert, Transaction}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * HighValueRule 테스트 스펙
 * 고액 거래 탐지 규칙 (100만원 초과 거래)에 대한 단위 테스트
 */
class HighValueRuleSpec extends AnyFlatSpec with Matchers {

  val rule = new HighValueRule()

  // 테스트용 샘플 거래 (100만원 초과)
  val highValueTransaction = Transaction(
    schemaVersion = "1.0",
    transactionId = "550e8400-e29b-41d4-a716-446655440000",
    userId = "user-3",
    amount = 1250000, // 125만원
    currency = "KRW",
    countryCode = "KR",
    timestamp = "2025-11-06T10:30:45.123Z"
  )

  // 테스트용 샘플 거래 (100만원 이하)
  val normalTransaction = Transaction(
    schemaVersion = "1.0",
    transactionId = "550e8400-e29b-41d4-a716-446655440001",
    userId = "user-1",
    amount = 500000, // 50만원
    currency = "KRW",
    countryCode = "KR",
    timestamp = "2025-11-06T10:31:00.000Z"
  )

  // 경계값 테스트용 (정확히 100만원)
  val exactlyOneMillionTransaction = Transaction(
    schemaVersion = "1.0",
    transactionId = "550e8400-e29b-41d4-a716-446655440002",
    userId = "user-2",
    amount = 1000000, // 정확히 100만원
    currency = "KRW",
    countryCode = "KR",
    timestamp = "2025-11-06T10:32:00.000Z"
  )

  "HighValueRule" should "100만원을 초과하는 거래를 필터링해야 함" in {
    // Given: 125만원 거래
    // When: filter 메서드 호출
    val result = rule.filter(highValueTransaction)

    // Then: true 반환 (필터 통과)
    result shouldBe true
  }

  it should "100만원 이하 거래는 필터링하지 않아야 함" in {
    // Given: 50만원 거래
    // When: filter 메서드 호출
    val result = rule.filter(normalTransaction)

    // Then: false 반환 (필터 미통과)
    result shouldBe false
  }

  it should "정확히 100만원 거래는 필터링하지 않아야 함 (초과가 아니므로)" in {
    // Given: 정확히 100만원 거래
    // When: filter 메서드 호출
    val result = rule.filter(exactlyOneMillionTransaction)

    // Then: false 반환 (초과가 아니므로 필터 미통과)
    result shouldBe false
  }

  it should "고액 거래를 Alert로 변환해야 함" in {
    // Given: 125만원 거래
    // When: toAlert 메서드 호출
    val alert = rule.toAlert(highValueTransaction)

    // Then: Alert가 생성되어야 함
    alert should not be null
    alert.originalTransaction shouldBe highValueTransaction
  }

  it should "Alert의 ruleName이 HIGH_VALUE여야 함" in {
    // Given: 고액 거래
    // When: toAlert 호출
    val alert = rule.toAlert(highValueTransaction)

    // Then: ruleName이 HIGH_VALUE
    alert.ruleName shouldBe "HIGH_VALUE"
  }

  it should "Alert의 ruleType이 SIMPLE_RULE이어야 함" in {
    // Given: 고액 거래
    // When: toAlert 호출
    val alert = rule.toAlert(highValueTransaction)

    // Then: ruleType이 SIMPLE_RULE
    alert.ruleType shouldBe "SIMPLE_RULE"
  }

  it should "Alert의 severity가 HIGH여야 함" in {
    // Given: 고액 거래
    // When: toAlert 호출
    val alert = rule.toAlert(highValueTransaction)

    // Then: severity가 HIGH
    alert.severity shouldBe "HIGH"
  }

  it should "Alert의 reason에 거래 금액이 포함되어야 함" in {
    // Given: 125만원 거래
    // When: toAlert 호출
    val alert = rule.toAlert(highValueTransaction)

    // Then: reason에 "고액 거래 (100만원 초과)"와 금액이 포함됨
    alert.reason should include("고액 거래")
    alert.reason should include("100만원 초과")
    alert.reason should include("1,250,000")
  }

  it should "Alert의 schemaVersion이 1.0이어야 함" in {
    // Given: 고액 거래
    // When: toAlert 호출
    val alert = rule.toAlert(highValueTransaction)

    // Then: schemaVersion이 1.0
    alert.schemaVersion shouldBe "1.0"
  }

  it should "Alert의 alertId가 생성되어야 함" in {
    // Given: 고액 거래
    // When: toAlert 호출
    val alert = rule.toAlert(highValueTransaction)

    // Then: alertId가 비어있지 않음
    alert.alertId should not be empty
  }

  it should "Alert의 alertTimestamp가 생성되어야 함" in {
    // Given: 고액 거래
    // When: toAlert 호출
    val alert = rule.toAlert(highValueTransaction)

    // Then: alertTimestamp가 비어있지 않음
    alert.alertTimestamp should not be empty
  }

  it should "Alert가 유효한 형식이어야 함" in {
    // Given: 고액 거래
    // When: toAlert 호출
    val alert = rule.toAlert(highValueTransaction)

    // Then: Alert.isValid가 true
    alert.isValid shouldBe true
  }

  it should "다양한 금액에 대해 정확하게 필터링해야 함" in {
    // Given: 다양한 금액의 거래들
    val amounts = List(
      (999999, false),    // 999,999원 -> false (초과 아님)
      (1000000, false),   // 1,000,000원 -> false (초과 아님)
      (1000001, true),    // 1,000,001원 -> true (초과)
      (1500000, true),    // 1,500,000원 -> true (초과)
      (100000, false)     // 100,000원 -> false (초과 아님)
    )

    amounts.foreach { case (amount, expected) =>
      // When: 각 금액으로 거래 생성 및 필터 적용
      val transaction = highValueTransaction.copy(amount = amount)
      val result = rule.filter(transaction)

      // Then: 예상된 결과와 일치
      withClue(s"Amount: $amount should be filtered=$expected, but got $result: ") {
        result shouldBe expected
      }
    }
  }

  it should "여러 거래에 대해 독립적으로 Alert를 생성해야 함" in {
    // Given: 두 개의 고액 거래
    val transaction1 = highValueTransaction
    val transaction2 = highValueTransaction.copy(
      transactionId = "different-id",
      userId = "user-5",
      amount = 1500000
    )

    // When: 각각 Alert로 변환
    val alert1 = rule.toAlert(transaction1)
    val alert2 = rule.toAlert(transaction2)

    // Then: 두 Alert는 독립적이어야 함 (alertId가 다름)
    alert1.alertId should not equal alert2.alertId
    alert1.originalTransaction.transactionId should not equal alert2.originalTransaction.transactionId
  }
}
