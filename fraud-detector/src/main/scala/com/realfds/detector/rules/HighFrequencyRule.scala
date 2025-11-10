package com.realfds.detector.rules

import com.realfds.detector.models.{Alert, Transaction}
import org.apache.flink.api.common.state.{MapState, MapStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector
import org.slf4j.LoggerFactory

/**
 * 빈번한 거래 탐지 규칙 (HighFrequencyRule)
 *
 * 1분 윈도우 내에 동일한 사용자가 5회를 초과하는 거래를 발생시키면 탐지합니다.
 *
 * **비즈니스 로직**:
 * - 조건: 1분 윈도우 내 거래 수 > 5회
 * - 규칙 유형: STATEFUL_RULE (상태 저장 규칙)
 * - 심각도: HIGH
 * - 상태 관리: MapState[Long, Int] (timestamp → count)
 *
 * **상태 기반 처리**:
 * - userId별로 키를 지정하여 각 사용자의 거래 이력을 추적
 * - 1분(60초) 윈도우 내의 거래만 상태로 유지
 * - 1분이 경과한 거래는 자동으로 상태에서 제거
 *
 * **사용 예시**:
 * ```scala
 * val rule = new HighFrequencyRule()
 * val highFrequencyAlerts = transactionStream
 *   .keyBy(_.userId)
 *   .process(rule)
 * ```
 *
 * @see Alert
 * @see Transaction
 * @see KeyedProcessFunction
 */
class HighFrequencyRule extends KeyedProcessFunction[String, Transaction, Alert] {

  @transient private lazy val logger = LoggerFactory.getLogger(getClass)

  /**
   * 빈번한 거래 탐지 임계값 (5회)
   * 환경 변수로 설정 가능하도록 나중에 외부화 가능
   */
  private val FREQUENCY_THRESHOLD = 5

  /**
   * 윈도우 크기 (1분 = 60,000 밀리초)
   */
  private val WINDOW_SIZE_MS = 60000L

  /**
   * 사용자별 거래 이력을 저장하는 MapState
   * Key: 거래 발생 타임스탬프 (milliseconds)
   * Value: 해당 타임스탬프에 발생한 거래 수 (항상 1)
   */
  @transient private var transactionTimestamps: MapState[Long, Int] = _

  /**
   * ProcessFunction 초기화 시 호출
   * MapState 초기화
   *
   * @param parameters 설정 파라미터
   */
  override def open(parameters: Configuration): Unit = {
    val descriptor = new MapStateDescriptor[Long, Int](
      "transaction-timestamps", // 상태 이름
      classOf[Long], // 타임스탬프 타입
      classOf[Int] // 카운트 타입
    )

    transactionTimestamps = getRuntimeContext.getMapState(descriptor)

    logger.info("HighFrequencyRule 초기화 완료")
  }

  /**
   * 각 거래를 처리하고 빈번한 거래 패턴을 탐지
   *
   * @param transaction 입력 거래
   * @param ctx 컨텍스트 (타이머 등록 가능)
   * @param out Alert 출력 Collector
   */
  override def processElement(
    transaction: Transaction,
    ctx: KeyedProcessFunction[String, Transaction, Alert]#Context,
    out: Collector[Alert]
  ): Unit = {
    // 거래 발생 시각을 타임스탬프로 변환
    val transactionTimestamp = try {
      java.time.Instant.parse(transaction.timestamp).toEpochMilli
    } catch {
      case e: Exception =>
        logger.warn(s"타임스탬프 파싱 실패: ${transaction.timestamp}, 현재 시간 사용", e)
        System.currentTimeMillis()
    }

    // 1분 윈도우 시작 시점 계산
    val windowStart = transactionTimestamp - WINDOW_SIZE_MS

    // 1. 현재 거래를 상태에 추가
    transactionTimestamps.put(transactionTimestamp, 1)

    logger.debug(
      s"거래 추가: userId=${transaction.userId}, " +
      s"transactionId=${transaction.transactionId}, " +
      s"timestamp=$transactionTimestamp"
    )

    // 2. 1분 이상 지난 거래를 상태에서 제거
    val iterator = transactionTimestamps.entries().iterator()
    var removedCount = 0

    while (iterator.hasNext) {
      val entry = iterator.next()
      if (entry.getKey < windowStart) {
        iterator.remove()
        removedCount += 1
      }
    }

    if (removedCount > 0) {
      logger.debug(s"만료된 거래 제거: userId=${transaction.userId}, count=$removedCount")
    }

    // 3. 현재 윈도우 내 거래 수 계산
    val currentCount = countTransactionsInWindow()

    logger.debug(
      s"현재 윈도우 거래 수: userId=${transaction.userId}, count=$currentCount"
    )

    // 4. 임계값 초과 시 Alert 생성
    if (currentCount > FREQUENCY_THRESHOLD) {
      val alert = createAlert(transaction, currentCount)
      out.collect(alert)

      logger.info(
        s"빈번한 거래 알림 생성: alertId=${alert.alertId}, " +
        s"userId=${transaction.userId}, count=$currentCount"
      )
    }

    // 5. 정리 타이머 등록 (1분 후 상태 정리)
    ctx.timerService().registerEventTimeTimer(transactionTimestamp + WINDOW_SIZE_MS)
  }

  /**
   * 타이머 이벤트 처리 (1분 경과 시 호출)
   * 만료된 거래를 상태에서 제거
   *
   * @param timestamp 타이머 타임스탬프
   * @param ctx 컨텍스트
   * @param out Alert 출력 Collector
   */
  override def onTimer(
    timestamp: Long,
    ctx: KeyedProcessFunction[String, Transaction, Alert]#OnTimerContext,
    out: Collector[Alert]
  ): Unit = {
    // 타이머가 만료된 시점 이전의 거래 제거
    val iterator = transactionTimestamps.entries().iterator()
    var removedCount = 0

    while (iterator.hasNext) {
      val entry = iterator.next()
      if (entry.getKey <= timestamp - WINDOW_SIZE_MS) {
        iterator.remove()
        removedCount += 1
      }
    }

    if (removedCount > 0) {
      logger.debug(s"타이머로 만료된 거래 제거: count=$removedCount")
    }
  }

  /**
   * 현재 윈도우 내 거래 수를 계산
   *
   * @return 거래 수
   */
  private def countTransactionsInWindow(): Int = {
    var count = 0
    val iterator = transactionTimestamps.entries().iterator()

    while (iterator.hasNext) {
      iterator.next()
      count += 1
    }

    count
  }

  /**
   * 빈번한 거래 알림을 생성
   *
   * @param transaction 탐지된 거래
   * @param count 1분 내 거래 수
   * @return 생성된 Alert 객체
   */
  def createAlert(transaction: Transaction, count: Int): Alert = {
    // 한국어 알림 사유 생성
    val reason = s"빈번한 거래 (1분 내 ${FREQUENCY_THRESHOLD}회 초과): ${transaction.userId}, ${count}회"

    // Alert 생성 (팩토리 메서드 사용)
    Alert.create(
      transaction = transaction,
      ruleType = Alert.STATEFUL_RULE,
      ruleName = "HIGH_FREQUENCY",
      reason = reason,
      severity = Alert.SEVERITY_HIGH
    )
  }
}

/**
 * HighFrequencyRule 컴패니언 객체
 */
object HighFrequencyRule {

  /**
   * 새 HighFrequencyRule 인스턴스 생성
   *
   * @return HighFrequencyRule 인스턴스
   */
  def apply(): HighFrequencyRule = new HighFrequencyRule()

  /**
   * 빈번한 거래 임계값 (5회)
   */
  val THRESHOLD = 5

  /**
   * 윈도우 크기 (1분 = 60초)
   */
  val WINDOW_SIZE_SECONDS = 60
}
