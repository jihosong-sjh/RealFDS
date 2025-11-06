package com.realfds.detector.serialization

import com.realfds.detector.models.Transaction
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.nio.charset.StandardCharsets

/**
 * TransactionDeserializationSchema 테스트 스펙
 * Kafka virtual-transactions 토픽에서 Transaction JSON을 역직렬화하는 기능 검증
 */
class TransactionDeserializationSchemaSpec extends AnyFlatSpec with Matchers {

  val schema = new TransactionDeserializationSchema()

  "TransactionDeserializationSchema" should "유효한 Transaction JSON을 Transaction 객체로 역직렬화해야 함" in {
    // Given: 유효한 Transaction JSON 메시지
    val json =
      """
        |{
        |  "schemaVersion": "1.0",
        |  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
        |  "userId": "user-3",
        |  "amount": 1250000,
        |  "currency": "KRW",
        |  "countryCode": "KR",
        |  "timestamp": "2025-11-06T10:30:45.123Z"
        |}
        |""".stripMargin.trim

    val message = json.getBytes(StandardCharsets.UTF_8)

    // When: 역직렬화 수행
    val transaction = schema.deserialize(message)

    // Then: Transaction 객체가 정확하게 파싱되어야 함
    transaction should not be null
    transaction.schemaVersion shouldBe "1.0"
    transaction.transactionId shouldBe "550e8400-e29b-41d4-a716-446655440000"
    transaction.userId shouldBe "user-3"
    transaction.amount shouldBe 1250000
    transaction.currency shouldBe "KRW"
    transaction.countryCode shouldBe "KR"
    transaction.timestamp shouldBe "2025-11-06T10:30:45.123Z"
  }

  it should "isEndOfStream이 항상 false를 반환해야 함 (무한 스트림)" in {
    // Given: 임의의 Transaction
    val transaction = Transaction(
      schemaVersion = "1.0",
      transactionId = "test-id",
      userId = "user-1",
      amount = 10000,
      currency = "KRW",
      countryCode = "KR",
      timestamp = "2025-11-06T10:00:00.000Z"
    )

    // When: isEndOfStream 호출
    val result = schema.isEndOfStream(transaction)

    // Then: 항상 false (무한 스트림)
    result shouldBe false
  }

  it should "TypeInformation을 올바르게 반환해야 함" in {
    // When: TypeInformation 조회
    val typeInfo = schema.getProducedType

    // Then: Transaction 타입 정보가 반환되어야 함
    typeInfo should not be null
    typeInfo.getTypeClass shouldBe classOf[Transaction]
  }

  it should "잘못된 JSON 형식을 처리할 때 예외를 발생시켜야 함" in {
    // Given: 잘못된 JSON 형식
    val invalidJson = """{"invalid": "json""".getBytes(StandardCharsets.UTF_8)

    // When & Then: 역직렬화 시 예외 발생
    intercept[Exception] {
      schema.deserialize(invalidJson)
    }
  }

  it should "필수 필드가 누락된 JSON을 처리할 때 예외를 발생시켜야 함" in {
    // Given: userId 필드 누락
    val incompleteJson =
      """
        |{
        |  "schemaVersion": "1.0",
        |  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
        |  "amount": 1250000,
        |  "currency": "KRW",
        |  "countryCode": "KR",
        |  "timestamp": "2025-11-06T10:30:45.123Z"
        |}
        |""".stripMargin.trim.getBytes(StandardCharsets.UTF_8)

    // When & Then: 역직렬화 시 예외 발생
    intercept[Exception] {
      schema.deserialize(incompleteJson)
    }
  }

  it should "빈 메시지를 처리할 때 예외를 발생시켜야 함" in {
    // Given: 빈 바이트 배열
    val emptyMessage = Array.empty[Byte]

    // When & Then: 역직렬화 시 예외 발생
    intercept[Exception] {
      schema.deserialize(emptyMessage)
    }
  }

  it should "null 메시지를 처리할 때 예외를 발생시켜야 함" in {
    // Given: null 메시지
    val nullMessage: Array[Byte] = null

    // When & Then: 역직렬화 시 예외 발생
    intercept[Exception] {
      schema.deserialize(nullMessage)
    }
  }

  it should "모든 지원 국가 코드 (KR, US, JP, CN)를 올바르게 역직렬화해야 함" in {
    val countryCodes = List("KR", "US", "JP", "CN")

    countryCodes.foreach { countryCode =>
      // Given: 특정 국가 코드가 포함된 JSON
      val json =
        s"""
          |{
          |  "schemaVersion": "1.0",
          |  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
          |  "userId": "user-1",
          |  "amount": 10000,
          |  "currency": "KRW",
          |  "countryCode": "$countryCode",
          |  "timestamp": "2025-11-06T10:30:45.123Z"
          |}
          |""".stripMargin.trim.getBytes(StandardCharsets.UTF_8)

      // When: 역직렬화 수행
      val transaction = schema.deserialize(json)

      // Then: 국가 코드가 정확하게 파싱되어야 함
      transaction.countryCode shouldBe countryCode
    }
  }

  it should "다양한 거래 금액 범위를 올바르게 역직렬화해야 함" in {
    val amounts = List(1000L, 500000L, 1000000L, 1500000L)

    amounts.foreach { amount =>
      // Given: 특정 금액이 포함된 JSON
      val json =
        s"""
          |{
          |  "schemaVersion": "1.0",
          |  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
          |  "userId": "user-1",
          |  "amount": $amount,
          |  "currency": "KRW",
          |  "countryCode": "KR",
          |  "timestamp": "2025-11-06T10:30:45.123Z"
          |}
          |""".stripMargin.trim.getBytes(StandardCharsets.UTF_8)

      // When: 역직렬화 수행
      val transaction = schema.deserialize(json)

      // Then: 금액이 정확하게 파싱되어야 함
      transaction.amount shouldBe amount
    }
  }

  it should "모든 사용자 ID (user-1 ~ user-10)를 올바르게 역직렬화해야 함" in {
    (1 to 10).foreach { userNum =>
      // Given: 특정 사용자 ID가 포함된 JSON
      val userId = s"user-$userNum"
      val json =
        s"""
          |{
          |  "schemaVersion": "1.0",
          |  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
          |  "userId": "$userId",
          |  "amount": 10000,
          |  "currency": "KRW",
          |  "countryCode": "KR",
          |  "timestamp": "2025-11-06T10:30:45.123Z"
          |}
          |""".stripMargin.trim.getBytes(StandardCharsets.UTF_8)

      // When: 역직렬화 수행
      val transaction = schema.deserialize(json)

      // Then: 사용자 ID가 정확하게 파싱되어야 함
      transaction.userId shouldBe userId
    }
  }
}
