package com.realfds.detector.rules

import com.realfds.detector.models.{Alert, Transaction}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * ForeignCountryRule 테스트 스펙
 * 해외 거래 탐지 규칙 (KR 외 국가 거래)에 대한 단위 테스트
 */
class ForeignCountryRuleSpec extends AnyFlatSpec with Matchers {

  val rule = new ForeignCountryRule()

  // 테스트용 샘플 거래 (한국 거래)
  val krTransaction = Transaction(
    schemaVersion = "1.0",
    transactionId = "550e8400-e29b-41d4-a716-446655440000",
    userId = "user-1",
    amount = 50000,
    currency = "KRW",
    countryCode = "KR",
    timestamp = "2025-11-06T10:30:45.123Z"
  )

  // 테스트용 샘플 거래 (미국 거래)
  val usTransaction = Transaction(
    schemaVersion = "1.0",
    transactionId = "550e8400-e29b-41d4-a716-446655440001",
    userId = "user-2",
    amount = 75000,
    currency = "KRW",
    countryCode = "US",
    timestamp = "2025-11-06T10:31:00.000Z"
  )

  // 테스트용 샘플 거래 (일본 거래)
  val jpTransaction = Transaction(
    schemaVersion = "1.0",
    transactionId = "550e8400-e29b-41d4-a716-446655440002",
    userId = "user-3",
    amount = 100000,
    currency = "KRW",
    countryCode = "JP",
    timestamp = "2025-11-06T10:32:00.000Z"
  )

  // 테스트용 샘플 거래 (중국 거래)
  val cnTransaction = Transaction(
    schemaVersion = "1.0",
    transactionId = "550e8400-e29b-41d4-a716-446655440003",
    userId = "user-4",
    amount = 120000,
    currency = "KRW",
    countryCode = "CN",
    timestamp = "2025-11-06T10:33:00.000Z"
  )

  "ForeignCountryRule" should "한국 외 국가 거래를 필터링해야 함" in {
    // Given: 미국 거래
    // When: filter 메서드 호출
    val result = rule.filter(usTransaction)

    // Then: true 반환 (필터 통과)
    result shouldBe true
  }

  it should "한국(KR) 거래는 필터링하지 않아야 함" in {
    // Given: 한국 거래
    // When: filter 메서드 호출
    val result = rule.filter(krTransaction)

    // Then: false 반환 (필터 미통과)
    result shouldBe false
  }

  it should "미국(US) 거래를 필터링해야 함" in {
    // Given: 미국 거래
    // When: filter 호출
    val result = rule.filter(usTransaction)

    // Then: true 반환
    result shouldBe true
  }

  it should "일본(JP) 거래를 필터링해야 함" in {
    // Given: 일본 거래
    // When: filter 호출
    val result = rule.filter(jpTransaction)

    // Then: true 반환
    result shouldBe true
  }

  it should "중국(CN) 거래를 필터링해야 함" in {
    // Given: 중국 거래
    // When: filter 호출
    val result = rule.filter(cnTransaction)

    // Then: true 반환
    result shouldBe true
  }

  it should "해외 거래를 Alert로 변환해야 함" in {
    // Given: 미국 거래
    // When: toAlert 메서드 호출
    val alert = rule.toAlert(usTransaction)

    // Then: Alert가 생성되어야 함
    alert should not be null
    alert.originalTransaction shouldBe usTransaction
  }

  it should "Alert의 ruleName이 FOREIGN_COUNTRY여야 함" in {
    // Given: 해외 거래
    // When: toAlert 호출
    val alert = rule.toAlert(usTransaction)

    // Then: ruleName이 FOREIGN_COUNTRY
    alert.ruleName shouldBe "FOREIGN_COUNTRY"
  }

  it should "Alert의 ruleType이 SIMPLE_RULE이어야 함" in {
    // Given: 해외 거래
    // When: toAlert 호출
    val alert = rule.toAlert(usTransaction)

    // Then: ruleType이 SIMPLE_RULE
    alert.ruleType shouldBe "SIMPLE_RULE"
  }

  it should "Alert의 severity가 MEDIUM이어야 함" in {
    // Given: 해외 거래
    // When: toAlert 호출
    val alert = rule.toAlert(usTransaction)

    // Then: severity가 MEDIUM
    alert.severity shouldBe "MEDIUM"
  }

  it should "Alert의 reason에 국가 코드가 포함되어야 함" in {
    // Given: 미국 거래
    // When: toAlert 호출
    val alert = rule.toAlert(usTransaction)

    // Then: reason에 "해외 거래 탐지"와 국가 코드가 포함됨
    alert.reason should include("해외 거래 탐지")
    alert.reason should include("US")
  }

  it should "Alert의 schemaVersion이 1.0이어야 함" in {
    // Given: 해외 거래
    // When: toAlert 호출
    val alert = rule.toAlert(usTransaction)

    // Then: schemaVersion이 1.0
    alert.schemaVersion shouldBe "1.0"
  }

  it should "Alert의 alertId가 생성되어야 함" in {
    // Given: 해외 거래
    // When: toAlert 호출
    val alert = rule.toAlert(usTransaction)

    // Then: alertId가 비어있지 않음
    alert.alertId should not be empty
  }

  it should "Alert의 alertTimestamp가 생성되어야 함" in {
    // Given: 해외 거래
    // When: toAlert 호출
    val alert = rule.toAlert(usTransaction)

    // Then: alertTimestamp가 비어있지 않음
    alert.alertTimestamp should not be empty
  }

  it should "Alert가 유효한 형식이어야 함" in {
    // Given: 해외 거래
    // When: toAlert 호출
    val alert = rule.toAlert(usTransaction)

    // Then: Alert.isValid가 true
    alert.isValid shouldBe true
  }

  it should "다양한 국가 코드에 대해 정확하게 필터링해야 함" in {
    // Given: 다양한 국가의 거래들
    val countryCodes = List(
      ("KR", false),  // 한국 -> false (필터 미통과)
      ("US", true),   // 미국 -> true (필터 통과)
      ("JP", true),   // 일본 -> true (필터 통과)
      ("CN", true),   // 중국 -> true (필터 통과)
      ("GB", true),   // 영국 -> true (필터 통과)
      ("DE", true)    // 독일 -> true (필터 통과)
    )

    countryCodes.foreach { case (countryCode, expected) =>
      // When: 각 국가 코드로 거래 생성 및 필터 적용
      val transaction = usTransaction.copy(countryCode = countryCode)
      val result = rule.filter(transaction)

      // Then: 예상된 결과와 일치
      withClue(s"CountryCode: $countryCode should be filtered=$expected, but got $result: ") {
        result shouldBe expected
      }
    }
  }

  it should "여러 해외 거래에 대해 독립적으로 Alert를 생성해야 함" in {
    // Given: 두 개의 해외 거래 (미국, 일본)
    val transaction1 = usTransaction
    val transaction2 = jpTransaction

    // When: 각각 Alert로 변환
    val alert1 = rule.toAlert(transaction1)
    val alert2 = rule.toAlert(transaction2)

    // Then: 두 Alert는 독립적이어야 함 (alertId가 다름)
    alert1.alertId should not equal alert2.alertId
    alert1.originalTransaction.transactionId should not equal alert2.originalTransaction.transactionId

    // 각 Alert의 reason에 해당 국가 코드가 포함됨
    alert1.reason should include("US")
    alert2.reason should include("JP")
  }

  it should "대소문자를 구분하여 필터링해야 함" in {
    // Given: 소문자 "kr" 거래 (잘못된 형식)
    val lowercaseKrTransaction = krTransaction.copy(countryCode = "kr")

    // When: filter 호출
    val result = rule.filter(lowercaseKrTransaction)

    // Then: true 반환 (KR과 kr은 다르므로 해외 거래로 간주)
    result shouldBe true
  }

  it should "각 국가별로 적절한 reason 메시지를 생성해야 함" in {
    // Given: 다양한 국가의 거래들
    val transactions = List(usTransaction, jpTransaction, cnTransaction)

    transactions.foreach { transaction =>
      // When: toAlert 호출
      val alert = rule.toAlert(transaction)

      // Then: reason에 해당 국가 코드가 정확히 포함됨
      withClue(s"CountryCode ${transaction.countryCode} should be in reason: ") {
        alert.reason should include(transaction.countryCode)
        alert.reason should include("해외 거래 탐지")
      }
    }
  }
}
