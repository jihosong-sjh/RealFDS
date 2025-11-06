package com.realfds.detector.rules

import com.realfds.detector.models.{Alert, Transaction}
import org.apache.flink.api.common.functions.FilterFunction
import org.slf4j.LoggerFactory

/**
 * 고액 거래 탐지 규칙 (HighValueRule)
 *
 * 거래 금액이 100만원(1,000,000원)을 초과하는 거래를 탐지합니다.
 *
 * **비즈니스 로직**:
 * - 조건: amount > 1,000,000
 * - 규칙 유형: SIMPLE_RULE (상태 없는 단순 필터)
 * - 심각도: HIGH
 *
 * **사용 예시**:
 * ```scala
 * val rule = new HighValueRule()
 * val highValueTransactions = transactionStream
 *   .filter(rule)
 *   .map(rule.toAlert)
 * ```
 *
 * @see Alert
 * @see Transaction
 */
class HighValueRule extends FilterFunction[Transaction] {

  @transient private lazy val logger = LoggerFactory.getLogger(getClass)

  /**
   * 고액 거래 탐지 임계값 (100만원)
   * 환경 변수로 설정 가능하도록 나중에 외부화 가능
   */
  private val HIGH_VALUE_THRESHOLD = 1000000L

  /**
   * 거래가 고액 거래인지 필터링
   *
   * @param transaction 검증할 거래
   * @return 거래 금액이 100만원을 초과하면 true, 그렇지 않으면 false
   */
  override def filter(transaction: Transaction): Boolean = {
    val isHighValue = transaction.amount > HIGH_VALUE_THRESHOLD

    if (isHighValue) {
      logger.debug(
        s"고액 거래 탐지: transactionId=${transaction.transactionId}, " +
        s"userId=${transaction.userId}, amount=${transaction.amount}"
      )
    }

    isHighValue
  }

  /**
   * 고액 거래를 Alert 객체로 변환
   *
   * @param transaction 탐지된 고액 거래
   * @return 생성된 Alert 객체
   */
  def toAlert(transaction: Transaction): Alert = {
    // 포맷팅된 금액 (예: "1,250,000원")
    val formattedAmount = f"${transaction.amount}%,d원"

    // 한국어 알림 사유 생성
    val reason = s"고액 거래 (100만원 초과): $formattedAmount"

    // Alert 생성 (팩토리 메서드 사용)
    val alert = Alert.create(
      transaction = transaction,
      ruleType = Alert.SIMPLE_RULE,
      ruleName = "HIGH_VALUE",
      reason = reason,
      severity = Alert.SEVERITY_HIGH
    )

    logger.info(
      s"고액 거래 알림 생성: alertId=${alert.alertId}, " +
      s"userId=${transaction.userId}, amount=${transaction.amount}"
    )

    alert
  }
}

/**
 * HighValueRule 컴패니언 객체
 */
object HighValueRule {

  /**
   * 새 HighValueRule 인스턴스 생성
   *
   * @return HighValueRule 인스턴스
   */
  def apply(): HighValueRule = new HighValueRule()

  /**
   * 고액 거래 임계값 (100만원)
   */
  val THRESHOLD = 1000000L
}
