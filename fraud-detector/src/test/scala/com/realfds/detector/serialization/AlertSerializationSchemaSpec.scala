package com.realfds.detector.serialization

import com.realfds.detector.models.{Alert, Transaction}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.nio.charset.StandardCharsets
import scala.collection.JavaConverters._
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema

/**
 * AlertSerializationSchema 테스트 스펙
 * Alert 객체를 Kafka transaction-alerts 토픽의 ProducerRecord로 직렬화하는 기능 검증
 */
class AlertSerializationSchemaSpec extends AnyFlatSpec with Matchers {

  val schema = new AlertSerializationSchema()
  val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  // Mock Kafka Sink Context (null은 테스트에서 사용 가능)
  val mockContext: KafkaRecordSerializationSchema.KafkaSinkContext = null
  val mockTimestamp: java.lang.Long = System.currentTimeMillis()

  // 테스트용 샘플 Transaction
  val sampleTransaction = Transaction(
    schemaVersion = "1.0",
    transactionId = "550e8400-e29b-41d4-a716-446655440000",
    userId = "user-3",
    amount = 1250000,
    currency = "KRW",
    countryCode = "KR",
    timestamp = "2025-11-06T10:30:45.123Z"
  )

  // 테스트용 샘플 Alert
  val sampleAlert = Alert(
    schemaVersion = "1.0",
    alertId = "660e9511-f39c-52e5-b827-557766551111",
    originalTransaction = sampleTransaction,
    ruleType = "SIMPLE_RULE",
    ruleName = "HIGH_VALUE",
    reason = "고액 거래 (100만원 초과): 1,250,000원",
    severity = "HIGH",
    alertTimestamp = "2025-11-06T10:30:47.456Z"
  )

  "AlertSerializationSchema" should "Alert 객체를 Kafka ProducerRecord로 직렬화해야 함" in {
    // When: Alert 객체 직렬화
    val producerRecord = schema.serialize(sampleAlert, mockContext, mockTimestamp)

    // Then: ProducerRecord가 생성되어야 함
    producerRecord should not be null
    producerRecord.topic() shouldBe "transaction-alerts"
  }

  it should "Kafka 메시지 키를 originalTransaction.userId로 설정해야 함" in {
    // When: Alert 객체 직렬화
    val producerRecord = schema.serialize(sampleAlert, mockContext, mockTimestamp)

    // Then: 메시지 키가 userId여야 함
    val key = new String(producerRecord.key(), StandardCharsets.UTF_8)
    key shouldBe "user-3"
  }

  it should "Kafka 메시지 값을 JSON 형식으로 직렬화해야 함" in {
    // When: Alert 객체 직렬화
    val producerRecord = schema.serialize(sampleAlert, mockContext, mockTimestamp)

    // Then: 메시지 값이 유효한 JSON이어야 함
    val value = new String(producerRecord.value(), StandardCharsets.UTF_8)
    val parsedAlert = objectMapper.readValue(value, classOf[Alert])

    parsedAlert.schemaVersion shouldBe "1.0"
    parsedAlert.alertId shouldBe "660e9511-f39c-52e5-b827-557766551111"
    parsedAlert.originalTransaction.userId shouldBe "user-3"
    parsedAlert.ruleType shouldBe "SIMPLE_RULE"
    parsedAlert.ruleName shouldBe "HIGH_VALUE"
    parsedAlert.severity shouldBe "HIGH"
  }

  it should "Kafka 헤더에 schema-version을 포함해야 함" in {
    // When: Alert 객체 직렬화
    val producerRecord = schema.serialize(sampleAlert, mockContext, mockTimestamp)

    // Then: schema-version 헤더가 있어야 함
    val headers = producerRecord.headers().asScala.toList
    val schemaVersionHeader = headers.find(_.key() == "schema-version")

    schemaVersionHeader should not be empty
    new String(schemaVersionHeader.get.value(), StandardCharsets.UTF_8) shouldBe "1.0"
  }

  it should "Kafka 헤더에 source-service를 포함해야 함" in {
    // When: Alert 객체 직렬화
    val producerRecord = schema.serialize(sampleAlert, mockContext, mockTimestamp)

    // Then: source-service 헤더가 있어야 함
    val headers = producerRecord.headers().asScala.toList
    val sourceServiceHeader = headers.find(_.key() == "source-service")

    sourceServiceHeader should not be empty
    new String(sourceServiceHeader.get.value(), StandardCharsets.UTF_8) shouldBe "fraud-detector"
  }

  it should "Kafka 헤더에 rule-name을 포함해야 함" in {
    // When: Alert 객체 직렬화
    val producerRecord = schema.serialize(sampleAlert, mockContext, mockTimestamp)

    // Then: rule-name 헤더가 있어야 함
    val headers = producerRecord.headers().asScala.toList
    val ruleNameHeader = headers.find(_.key() == "rule-name")

    ruleNameHeader should not be empty
    new String(ruleNameHeader.get.value(), StandardCharsets.UTF_8) shouldBe "HIGH_VALUE"
  }

  it should "다양한 rule 타입 (HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY)을 올바르게 직렬화해야 함" in {
    val ruleNames = List("HIGH_VALUE", "FOREIGN_COUNTRY", "HIGH_FREQUENCY")

    ruleNames.foreach { ruleName =>
      // Given: 특정 rule이 포함된 Alert
      val alert = sampleAlert.copy(ruleName = ruleName)

      // When: 직렬화 수행
      val producerRecord = schema.serialize(alert, mockContext, mockTimestamp)

      // Then: rule-name 헤더가 올바르게 설정되어야 함
      val headers = producerRecord.headers().asScala.toList
      val ruleNameHeader = headers.find(_.key() == "rule-name")
      new String(ruleNameHeader.get.value(), StandardCharsets.UTF_8) shouldBe ruleName
    }
  }

  it should "다양한 severity (HIGH, MEDIUM, LOW)를 올바르게 직렬화해야 함" in {
    val severities = List("HIGH", "MEDIUM", "LOW")

    severities.foreach { severity =>
      // Given: 특정 severity가 포함된 Alert
      val alert = sampleAlert.copy(severity = severity)

      // When: 직렬화 수행
      val producerRecord = schema.serialize(alert, mockContext, mockTimestamp)

      // Then: 메시지가 올바르게 직렬화되어야 함
      val value = new String(producerRecord.value(), StandardCharsets.UTF_8)
      val parsedAlert = objectMapper.readValue(value, classOf[Alert])
      parsedAlert.severity shouldBe severity
    }
  }

  it should "STATEFUL_RULE 타입을 올바르게 직렬화해야 함" in {
    // Given: STATEFUL_RULE 타입의 Alert
    val statefulAlert = sampleAlert.copy(
      ruleType = "STATEFUL_RULE",
      ruleName = "HIGH_FREQUENCY"
    )

    // When: 직렬화 수행
    val producerRecord = schema.serialize(statefulAlert, mockContext, mockTimestamp)

    // Then: ruleType이 올바르게 직렬화되어야 함
    val value = new String(producerRecord.value(), StandardCharsets.UTF_8)
    val parsedAlert = objectMapper.readValue(value, classOf[Alert])
    parsedAlert.ruleType shouldBe "STATEFUL_RULE"
  }

  it should "원본 거래 정보를 완전하게 포함해야 함" in {
    // When: Alert 객체 직렬화
    val producerRecord = schema.serialize(sampleAlert, mockContext, mockTimestamp)

    // Then: originalTransaction이 완전하게 포함되어야 함
    val value = new String(producerRecord.value(), StandardCharsets.UTF_8)
    val parsedAlert = objectMapper.readValue(value, classOf[Alert])
    val tx = parsedAlert.originalTransaction

    tx.schemaVersion shouldBe "1.0"
    tx.transactionId shouldBe "550e8400-e29b-41d4-a716-446655440000"
    tx.userId shouldBe "user-3"
    tx.amount shouldBe 1250000
    tx.currency shouldBe "KRW"
    tx.countryCode shouldBe "KR"
    tx.timestamp shouldBe "2025-11-06T10:30:45.123Z"
  }

  it should "타임스탬프가 null이어도 정상 동작해야 함" in {
    // When: 타임스탬프 null로 직렬화
    val producerRecord = schema.serialize(sampleAlert, mockContext, null)

    // Then: 정상적으로 직렬화되어야 함
    producerRecord should not be null
    producerRecord.topic() shouldBe "transaction-alerts"
  }

  it should "한국어 reason 문자열을 올바르게 직렬화해야 함" in {
    // Given: 한국어 reason이 포함된 Alert
    val koreanAlert = sampleAlert.copy(
      reason = "해외 거래 탐지: 국가 코드 US로 거래 발생"
    )

    // When: 직렬화 수행
    val producerRecord = schema.serialize(koreanAlert, mockContext, mockTimestamp)

    // Then: 한국어가 올바르게 직렬화되어야 함
    val value = new String(producerRecord.value(), StandardCharsets.UTF_8)
    val parsedAlert = objectMapper.readValue(value, classOf[Alert])
    parsedAlert.reason shouldBe "해외 거래 탐지: 국가 코드 US로 거래 발생"
  }
}
