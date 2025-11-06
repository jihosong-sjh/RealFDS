package com.realfds.detector.serialization

import com.realfds.detector.models.Alert
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

/**
 * Alert 직렬화 스키마
 * Alert 객체를 Kafka transaction-alerts 토픽의 ProducerRecord로 변환
 *
 * Flink Kafka Sink의 KafkaRecordSerializationSchema를 구현하여:
 * - Alert 객체 → Kafka ProducerRecord 변환
 * - Kafka 메시지 키: originalTransaction.userId
 * - Kafka 메시지 값: Alert JSON
 * - Kafka 헤더: schema-version, source-service, rule-name
 *
 * @see org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema
 */
class AlertSerializationSchema extends KafkaRecordSerializationSchema[Alert] {

  @transient private lazy val logger = LoggerFactory.getLogger(getClass)

  /**
   * Kafka 토픽 이름
   */
  private val TOPIC = "transaction-alerts"

  /**
   * Jackson ObjectMapper (Scala 모듈 포함)
   * transient: Flink 직렬화 시 제외
   */
  @transient private lazy val objectMapper: ObjectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper
  }

  /**
   * Alert 객체를 Kafka ProducerRecord로 직렬화
   *
   * @param alert Alert 객체
   * @param context Kafka sink context (타임스탬프 등)
   * @return ProducerRecord[Array[Byte], Array[Byte]]
   */
  override def serialize(
    alert: Alert,
    context: KafkaRecordSerializationSchema.KafkaSinkContext,
    timestamp: java.lang.Long
  ): ProducerRecord[Array[Byte], Array[Byte]] = {

    try {
      // Kafka 메시지 키: originalTransaction.userId (UTF-8 인코딩)
      val key = alert.originalTransaction.userId.getBytes(StandardCharsets.UTF_8)

      // Kafka 메시지 값: Alert JSON (UTF-8 인코딩)
      val value = objectMapper.writeValueAsString(alert).getBytes(StandardCharsets.UTF_8)

      // Kafka 헤더 생성
      val headers: java.lang.Iterable[org.apache.kafka.common.header.Header] = {
        val list = new java.util.ArrayList[org.apache.kafka.common.header.Header]()
        list.add(new RecordHeader("schema-version", "1.0".getBytes(StandardCharsets.UTF_8)))
        list.add(new RecordHeader("source-service", "fraud-detector".getBytes(StandardCharsets.UTF_8)))
        list.add(new RecordHeader("rule-name", alert.ruleName.getBytes(StandardCharsets.UTF_8)))
        list
      }

      logger.debug(
        s"Alert 직렬화 성공: alertId=${alert.alertId}, userId=${alert.originalTransaction.userId}, ruleName=${alert.ruleName}"
      )

      // ProducerRecord 생성 (토픽, 파티션, 키, 값, 헤더)
      new ProducerRecord[Array[Byte], Array[Byte]](
        TOPIC,
        null.asInstanceOf[Integer], // 파티션: Kafka가 키 기반으로 자동 할당
        key,
        value,
        headers
      )

    } catch {
      case e: com.fasterxml.jackson.core.JsonProcessingException =>
        logger.error(s"Alert JSON 직렬화 실패: alertId=${alert.alertId}, ${e.getMessage}")
        throw e

      case e: Exception =>
        logger.error(s"Alert 직렬화 중 예상치 못한 오류: alertId=${alert.alertId}, ${e.getMessage}", e)
        throw e
    }
  }

  /**
   * open() 메서드 - 초기화 로직
   * Flink가 직렬화 스키마를 사용하기 전에 호출
   */
  override def open(
    initializationContext: org.apache.flink.api.common.serialization.SerializationSchema.InitializationContext,
    context: KafkaRecordSerializationSchema.KafkaSinkContext
  ): Unit = {
    logger.info("AlertSerializationSchema 초기화됨")
  }
}

/**
 * AlertSerializationSchema 컴패니언 객체
 */
object AlertSerializationSchema {

  /**
   * 새 AlertSerializationSchema 인스턴스 생성
   *
   * @return AlertSerializationSchema 인스턴스
   */
  def apply(): AlertSerializationSchema = new AlertSerializationSchema()
}
