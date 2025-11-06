package com.realfds.detector

import com.realfds.detector.models.{Alert, Transaction}
import com.realfds.detector.rules.HighValueRule
import com.realfds.detector.serialization.{AlertSerializationSchema, TransactionDeserializationSchema}
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.connector.kafka.sink.{KafkaRecordSerializationSchema, KafkaSink}
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.CheckpointingMode
import org.slf4j.LoggerFactory
import java.time.{Duration, Instant}

/**
 * Fraud Detection Job (실시간 금융 거래 탐지 Flink Job)
 *
 * **목적**:
 * - Kafka virtual-transactions 토픽에서 거래 데이터를 소비
 * - HighValueRule을 적용하여 고액 거래 탐지
 * - 탐지된 알림을 Kafka transaction-alerts 토픽으로 발행
 *
 * **처리 흐름**:
 * 1. Kafka Source: virtual-transactions 토픽 구독
 * 2. Watermark 설정: 5초 지연 허용 (out-of-order 이벤트 처리)
 * 3. HighValueRule 필터 적용
 * 4. Transaction → Alert 변환
 * 5. Kafka Sink: transaction-alerts 토픽으로 발행
 *
 * **실행 방법**:
 * ```bash
 * ./gradlew shadowJar
 * flink run -c com.realfds.detector.FraudDetectionJob \
 *   build/libs/fraud-detector-1.0.0.jar
 * ```
 *
 * **환경 변수**:
 * - KAFKA_BOOTSTRAP_SERVERS: Kafka 브로커 주소 (기본값: kafka:9092)
 * - HIGH_VALUE_THRESHOLD: 고액 거래 임계값 (기본값: 1000000)
 * - FLINK_CHECKPOINT_INTERVAL: 체크포인트 간격 ms (기본값: 60000)
 */
object FraudDetectionJob {

  private val logger = LoggerFactory.getLogger(getClass)

  /**
   * Flink Job 진입점
   *
   * @param args 커맨드 라인 인자 (사용하지 않음)
   */
  def main(args: Array[String]): Unit = {
    logger.info("=== Fraud Detection Job 시작 ===")

    // 환경 변수 읽기
    val kafkaBootstrapServers = sys.env.getOrElse("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
    val checkpointInterval = sys.env.getOrElse("FLINK_CHECKPOINT_INTERVAL", "60000").toLong

    logger.info(s"Kafka Bootstrap Servers: $kafkaBootstrapServers")
    logger.info(s"Checkpoint Interval: ${checkpointInterval}ms")

    // 1. StreamExecutionEnvironment 초기화
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    // 2. 체크포인트 설정 (60초마다, Exactly-Once 보장)
    env.enableCheckpointing(checkpointInterval, CheckpointingMode.EXACTLY_ONCE)
    env.getCheckpointConfig.setMinPauseBetweenCheckpoints(30000) // 체크포인트 간 최소 30초 간격
    env.getCheckpointConfig.setCheckpointTimeout(120000) // 체크포인트 타임아웃 2분
    env.getCheckpointConfig.setMaxConcurrentCheckpoints(1) // 동시 체크포인트 1개만 허용
    logger.info("체크포인트 설정 완료: 60초마다, Exactly-Once 모드")

    // 3. 재시작 전략 설정 (장애 시 자동 재시작)
    env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
      3, // 최대 3회 재시작 시도
      org.apache.flink.api.common.time.Time.seconds(10) // 10초 간격
    ))
    logger.info("재시작 전략 설정 완료: 최대 3회, 10초 간격")

    // 4. Kafka Source 생성 (virtual-transactions 토픽)
    val kafkaSource = KafkaSource.builder[Transaction]()
      .setBootstrapServers(kafkaBootstrapServers)
      .setTopics("virtual-transactions")
      .setGroupId("fraud-detector-group")
      .setStartingOffsets(OffsetsInitializer.earliest()) // 처음부터 소비
      .setValueOnlyDeserializer(new TransactionDeserializationSchema())
      .build()

    logger.info("Kafka Source 생성 완료: virtual-transactions 토픽")

    // 5. Watermark 전략 (5초 지연 허용)
    val watermarkStrategy = WatermarkStrategy
      .forBoundedOutOfOrderness[Transaction](Duration.ofSeconds(5))
      .withTimestampAssigner(new SerializableTimestampAssigner[Transaction] {
        override def extractTimestamp(transaction: Transaction, recordTimestamp: Long): Long = {
          try {
            Instant.parse(transaction.timestamp).toEpochMilli
          } catch {
            case _: Exception =>
              logger.warn(s"타임스탬프 파싱 실패: ${transaction.timestamp}, 현재 시간 사용")
              System.currentTimeMillis()
          }
        }
      })

    logger.info("Watermark 전략 설정 완료: 5초 out-of-order 허용")

    // 6. Transaction 스트림 생성
    val transactionStream: DataStream[Transaction] = env
      .fromSource(kafkaSource, watermarkStrategy, "Kafka Source")
      .name("Transaction Source")
      .uid("transaction-source")

    logger.info("Transaction 스트림 생성 완료")

    // 7. HighValueRule 초기화
    val highValueRule = new HighValueRule()

    // 8. 고액 거래 필터링 및 Alert 생성
    val alertStream: DataStream[Alert] = transactionStream
      .filter(highValueRule)
      .name("High Value Filter")
      .uid("high-value-filter")
      .map(transaction => {
        logger.debug(s"고액 거래 탐지: transactionId=${transaction.transactionId}")
        highValueRule.toAlert(transaction)
      })
      .name("Alert Mapper")
      .uid("alert-mapper")

    logger.info("고액 거래 탐지 파이프라인 구성 완료")

    // 9. Kafka Sink 생성 (transaction-alerts 토픽)
    val kafkaSink = KafkaSink.builder[Alert]()
      .setBootstrapServers(kafkaBootstrapServers)
      .setRecordSerializer(new AlertSerializationSchema())
      .build()

    logger.info("Kafka Sink 생성 완료: transaction-alerts 토픽")

    // 10. Alert를 Kafka로 전송
    alertStream
      .sinkTo(kafkaSink)
      .name("Alert Sink")
      .uid("alert-sink")

    logger.info("Alert Sink 연결 완료")

    // 11. Job 실행
    logger.info("=== Fraud Detection Job 실행 중 ===")
    env.execute("Fraud Detection Job - High Value Rule")
  }
}
