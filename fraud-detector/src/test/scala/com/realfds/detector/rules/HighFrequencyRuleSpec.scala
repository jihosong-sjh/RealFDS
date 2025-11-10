package com.realfds.detector.rules

import com.realfds.detector.models.{Alert, Transaction}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.operators.KeyedProcessOperator
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.time.Instant

/**
 * HighFrequencyRule 테스트 스펙
 * 빈번한 거래 탐지 규칙 (1분 내 5회 초과)에 대한 단위 테스트
 * Flink Testing Harness를 사용한 상태 기반 테스트
 */
class HighFrequencyRuleSpec extends AnyFlatSpec with Matchers {

  /**
   * 테스트용 Transaction 생성 헬퍼 메서드
   *
   * @param userId 사용자 ID
   * @param timestamp 타임스탬프 (Instant)
   * @param transactionId 거래 ID (선택적)
   * @return Transaction 객체
   */
  def createTransaction(
    userId: String,
    timestamp: Instant,
    transactionId: String = java.util.UUID.randomUUID().toString
  ): Transaction = {
    Transaction(
      schemaVersion = "1.0",
      transactionId = transactionId,
      userId = userId,
      amount = 100000,
      currency = "KRW",
      countryCode = "KR",
      timestamp = timestamp.toString
    )
  }

  /**
   * Flink Testing Harness 생성
   *
   * @return KeyedOneInputStreamOperatorTestHarness
   */
  def createTestHarness(): KeyedOneInputStreamOperatorTestHarness[String, Transaction, Alert] = {
    val rule = new HighFrequencyRule()
    val operator = new KeyedProcessOperator[String, Transaction, Alert](rule)

    // TypeInformation for String key
    val keyTypeInfo: TypeInformation[String] = TypeInformation.of(classOf[String])

    new KeyedOneInputStreamOperatorTestHarness[String, Transaction, Alert](
      operator,
      (transaction: Transaction) => transaction.userId, // Key selector
      keyTypeInfo // Key type information
    )
  }

  "HighFrequencyRule" should "1분 내 6번째 거래에서 알림을 생성해야 함" in {
    // Given: Testing Harness 설정
    val testHarness = createTestHarness()
    testHarness.open()

    val baseTime = Instant.parse("2025-11-06T10:00:00.000Z")
    val userId = "user-3"

    try {
      // When: 1분 내에 6번의 거래 발생
      for (i <- 0 until 6) {
        val transaction = createTransaction(
          userId = userId,
          timestamp = baseTime.plusSeconds(i * 5) // 5초 간격
        )
        testHarness.processElement(transaction, baseTime.plusSeconds(i * 5).toEpochMilli)
      }

      // Then: 6번째 거래부터 알림 생성 (임계값 5 초과)
      val output = testHarness.extractOutputValues()
      output.size() should be >= 1

      val alert = output.get(0)
      alert.ruleName shouldBe "HIGH_FREQUENCY"
      alert.severity shouldBe "HIGH"
      alert.ruleType shouldBe "STATEFUL_RULE"
      alert.originalTransaction.userId shouldBe userId
      alert.reason should include("빈번한 거래")
      alert.reason should include("6회")
    } finally {
      testHarness.close()
    }
  }

  it should "5회 이하 거래에서는 알림을 생성하지 않아야 함" in {
    // Given: Testing Harness 설정
    val testHarness = createTestHarness()
    testHarness.open()

    val baseTime = Instant.parse("2025-11-06T10:00:00.000Z")
    val userId = "user-1"

    try {
      // When: 1분 내에 5번의 거래 발생 (임계값과 동일)
      for (i <- 0 until 5) {
        val transaction = createTransaction(
          userId = userId,
          timestamp = baseTime.plusSeconds(i * 5)
        )
        testHarness.processElement(transaction, baseTime.plusSeconds(i * 5).toEpochMilli)
      }

      // Then: 알림이 생성되지 않아야 함 (5회는 임계값이므로 초과 아님)
      val output = testHarness.extractOutputValues()
      output.size() shouldBe 0
    } finally {
      testHarness.close()
    }
  }

  it should "1분 윈도우가 경과하면 카운터가 리셋되어야 함" in {
    // Given: Testing Harness 설정
    val testHarness = createTestHarness()
    testHarness.open()

    val baseTime = Instant.parse("2025-11-06T10:00:00.000Z")
    val userId = "user-2"

    try {
      // When: 첫 번째 윈도우에서 3번 거래
      for (i <- 0 until 3) {
        val transaction = createTransaction(
          userId = userId,
          timestamp = baseTime.plusSeconds(i * 5)
        )
        testHarness.processElement(transaction, baseTime.plusSeconds(i * 5).toEpochMilli)
      }

      // 1분 20초 후 (윈도우 경과)에 다시 3번 거래
      val newWindowTime = baseTime.plusSeconds(80)
      for (i <- 0 until 3) {
        val transaction = createTransaction(
          userId = userId,
          timestamp = newWindowTime.plusSeconds(i * 5)
        )
        testHarness.processElement(transaction, newWindowTime.plusSeconds(i * 5).toEpochMilli)
      }

      // Then: 윈도우가 분리되어 총 6번 거래지만 알림 생성 안 됨
      val output = testHarness.extractOutputValues()
      output.size() shouldBe 0
    } finally {
      testHarness.close()
    }
  }

  it should "사용자별로 독립적으로 카운트해야 함" in {
    // Given: Testing Harness 설정
    val testHarness = createTestHarness()
    testHarness.open()

    val baseTime = Instant.parse("2025-11-06T10:00:00.000Z")

    try {
      // When: user-1과 user-2가 각각 3번씩 거래 (같은 시간대)
      for (i <- 0 until 3) {
        val tx1 = createTransaction(
          userId = "user-1",
          timestamp = baseTime.plusSeconds(i * 5)
        )
        val tx2 = createTransaction(
          userId = "user-2",
          timestamp = baseTime.plusSeconds(i * 5)
        )

        testHarness.processElement(tx1, baseTime.plusSeconds(i * 5).toEpochMilli)
        testHarness.processElement(tx2, baseTime.plusSeconds(i * 5).toEpochMilli)
      }

      // Then: 각 사용자당 3번씩이므로 알림 생성 안 됨
      val output = testHarness.extractOutputValues()
      output.size() shouldBe 0
    } finally {
      testHarness.close()
    }
  }

  it should "동일 사용자가 1분 내 7번 거래 시 2개의 알림을 생성해야 함" in {
    // Given: Testing Harness 설정
    val testHarness = createTestHarness()
    testHarness.open()

    val baseTime = Instant.parse("2025-11-06T10:00:00.000Z")
    val userId = "user-5"

    try {
      // When: 1분 내에 7번의 거래 발생
      for (i <- 0 until 7) {
        val transaction = createTransaction(
          userId = userId,
          timestamp = baseTime.plusSeconds(i * 5)
        )
        testHarness.processElement(transaction, baseTime.plusSeconds(i * 5).toEpochMilli)
      }

      // Then: 6번째와 7번째 거래에서 각각 알림 생성
      val output = testHarness.extractOutputValues()
      output.size() should be >= 2

      // 모든 알림이 HIGH_FREQUENCY 규칙이어야 함
      output.forEach { alert =>
        alert.ruleName shouldBe "HIGH_FREQUENCY"
        alert.originalTransaction.userId shouldBe userId
      }
    } finally {
      testHarness.close()
    }
  }

  it should "Alert의 reason에 거래 횟수가 정확히 표시되어야 함" in {
    // Given: Testing Harness 설정
    val testHarness = createTestHarness()
    testHarness.open()

    val baseTime = Instant.parse("2025-11-06T10:00:00.000Z")
    val userId = "user-7"

    try {
      // When: 1분 내에 8번의 거래 발생
      for (i <- 0 until 8) {
        val transaction = createTransaction(
          userId = userId,
          timestamp = baseTime.plusSeconds(i * 5)
        )
        testHarness.processElement(transaction, baseTime.plusSeconds(i * 5).toEpochMilli)
      }

      // Then: 생성된 알림들의 reason 확인
      val output = testHarness.extractOutputValues()
      output.size() should be >= 1

      // 첫 번째 알림은 6회에 발생
      val firstAlert = output.get(0)
      firstAlert.reason should include("6회")

      // 두 번째 알림이 있다면 7회에 발생
      if (output.size() >= 2) {
        val secondAlert = output.get(1)
        secondAlert.reason should include("7회")
      }
    } finally {
      testHarness.close()
    }
  }

  it should "Alert의 기본 필드들이 올바르게 설정되어야 함" in {
    // Given: Testing Harness 설정
    val testHarness = createTestHarness()
    testHarness.open()

    val baseTime = Instant.parse("2025-11-06T10:00:00.000Z")
    val userId = "user-8"

    try {
      // When: 1분 내에 6번의 거래 발생
      for (i <- 0 until 6) {
        val transaction = createTransaction(
          userId = userId,
          timestamp = baseTime.plusSeconds(i * 5)
        )
        testHarness.processElement(transaction, baseTime.plusSeconds(i * 5).toEpochMilli)
      }

      // Then: 생성된 Alert 검증
      val output = testHarness.extractOutputValues()
      output.size() should be >= 1

      val alert = output.get(0)

      // 기본 필드 검증
      alert.schemaVersion shouldBe "1.0"
      alert.alertId should not be empty
      alert.alertTimestamp should not be empty
      alert.ruleName shouldBe "HIGH_FREQUENCY"
      alert.ruleType shouldBe "STATEFUL_RULE"
      alert.severity shouldBe "HIGH"
      alert.originalTransaction.userId shouldBe userId
      alert.isValid shouldBe true
    } finally {
      testHarness.close()
    }
  }

  it should "매우 짧은 시간 내 연속 거래를 정확히 탐지해야 함" in {
    // Given: Testing Harness 설정
    val testHarness = createTestHarness()
    testHarness.open()

    val baseTime = Instant.parse("2025-11-06T10:00:00.000Z")
    val userId = "user-9"

    try {
      // When: 1초 간격으로 6번 거래 (매우 빈번)
      for (i <- 0 until 6) {
        val transaction = createTransaction(
          userId = userId,
          timestamp = baseTime.plusSeconds(i)
        )
        testHarness.processElement(transaction, baseTime.plusSeconds(i).toEpochMilli)
      }

      // Then: 알림 생성 확인
      val output = testHarness.extractOutputValues()
      output.size() should be >= 1

      val alert = output.get(0)
      alert.ruleName shouldBe "HIGH_FREQUENCY"
      alert.severity shouldBe "HIGH"
    } finally {
      testHarness.close()
    }
  }

  it should "단일 거래에서는 알림을 생성하지 않아야 함" in {
    // Given: Testing Harness 설정
    val testHarness = createTestHarness()
    testHarness.open()

    val baseTime = Instant.parse("2025-11-06T10:00:00.000Z")
    val userId = "user-10"

    try {
      // When: 단 1번의 거래만 발생
      val transaction = createTransaction(
        userId = userId,
        timestamp = baseTime
      )
      testHarness.processElement(transaction, baseTime.toEpochMilli)

      // Then: 알림이 생성되지 않아야 함
      val output = testHarness.extractOutputValues()
      output.size() shouldBe 0
    } finally {
      testHarness.close()
    }
  }
}
