package com.realfds.detector.serialization

import com.realfds.detector.models.Transaction
import org.apache.flink.api.common.serialization.DeserializationSchema
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.scala._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

/**
 * Transaction 역직렬화 스키마
 * Kafka virtual-transactions 토픽에서 JSON 메시지를 Transaction 객체로 변환
 *
 * Flink의 DeserializationSchema를 구현하여:
 * - Kafka 메시지 (byte[]) → Transaction 객체 변환
 * - JSON 파싱 에러 처리
 * - 스키마 버전 검증
 *
 * @see org.apache.flink.api.common.serialization.DeserializationSchema
 */
class TransactionDeserializationSchema extends DeserializationSchema[Transaction] {

  @transient private lazy val logger = LoggerFactory.getLogger(getClass)

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
   * Kafka 메시지를 Transaction 객체로 역직렬화
   *
   * @param message Kafka 메시지 바이트 배열 (UTF-8 인코딩 JSON)
   * @return Transaction 객체
   * @throws IllegalArgumentException message가 null이거나 비어있는 경우
   * @throws com.fasterxml.jackson.core.JsonProcessingException JSON 파싱 실패 시
   */
  override def deserialize(message: Array[Byte]): Transaction = {
    // null 체크
    if (message == null) {
      logger.error("메시지가 null입니다")
      throw new IllegalArgumentException("메시지가 null일 수 없습니다")
    }

    // 빈 메시지 체크
    if (message.isEmpty) {
      logger.error("메시지가 비어 있습니다")
      throw new IllegalArgumentException("메시지가 비어 있을 수 없습니다")
    }

    try {
      // UTF-8 인코딩으로 JSON 문자열 변환
      val jsonString = new String(message, StandardCharsets.UTF_8)

      // JSON → Transaction 객체 변환
      val transaction = objectMapper.readValue(jsonString, classOf[Transaction])

      // 스키마 버전 검증
      if (transaction.schemaVersion != "1.0") {
        logger.warn(s"지원하지 않는 스키마 버전: ${transaction.schemaVersion}")
      }

      // 기본 유효성 검증 (선택적)
      if (!transaction.isValid) {
        logger.warn(s"유효하지 않은 거래 데이터: transactionId=${transaction.transactionId}")
      }

      logger.debug(s"거래 역직렬화 성공: transactionId=${transaction.transactionId}, userId=${transaction.userId}")
      transaction

    } catch {
      case e: com.fasterxml.jackson.core.JsonProcessingException =>
        logger.error(s"JSON 파싱 실패: ${e.getMessage}")
        throw e

      case e: Exception =>
        logger.error(s"역직렬화 중 예상치 못한 오류: ${e.getMessage}", e)
        throw e
    }
  }

  /**
   * 스트림 종료 여부 확인
   * Kafka 스트림은 무한 스트림이므로 항상 false 반환
   *
   * @param nextElement 다음 Transaction 요소
   * @return 항상 false (무한 스트림)
   */
  override def isEndOfStream(nextElement: Transaction): Boolean = false

  /**
   * 생성되는 데이터 타입 정보 반환
   *
   * @return Transaction 타입 정보
   */
  override def getProducedType: TypeInformation[Transaction] = {
    TypeInformation.of(classOf[Transaction])
  }
}

/**
 * TransactionDeserializationSchema 컴패니언 객체
 */
object TransactionDeserializationSchema {

  /**
   * 새 TransactionDeserializationSchema 인스턴스 생성
   *
   * @return TransactionDeserializationSchema 인스턴스
   */
  def apply(): TransactionDeserializationSchema = new TransactionDeserializationSchema()
}
