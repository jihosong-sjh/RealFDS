package com.realfds.detector.rules

import com.realfds.detector.models.{Alert, Transaction}
import org.apache.flink.api.common.functions.FilterFunction
import org.slf4j.LoggerFactory

/**
 * 해외 거래 탐지 규칙 (ForeignCountryRule)
 *
 * 한국(KR) 외의 국가에서 발생한 거래를 탐지합니다.
 *
 * **비즈니스 로직**:
 * - 조건: countryCode != "KR"
 * - 규칙 유형: SIMPLE_RULE (상태 없는 단순 필터)
 * - 심각도: MEDIUM
 *
 * **사용 예시**:
 * ```scala
 * val rule = new ForeignCountryRule()
 * val foreignTransactions = transactionStream
 *   .filter(rule)
 *   .map(rule.toAlert)
 * ```
 *
 * @see Alert
 * @see Transaction
 */
class ForeignCountryRule extends FilterFunction[Transaction] {

  @transient private lazy val logger = LoggerFactory.getLogger(getClass)

  /**
   * 허용되는 국가 코드 (한국)
   * 환경 변수로 설정 가능하도록 나중에 외부화 가능
   */
  private val ALLOWED_COUNTRY = "KR"

  /**
   * 거래가 해외 거래인지 필터링
   *
   * @param transaction 검증할 거래
   * @return 거래가 한국 외 국가에서 발생했으면 true, 그렇지 않으면 false
   */
  override def filter(transaction: Transaction): Boolean = {
    val isForeign = transaction.countryCode != ALLOWED_COUNTRY

    if (isForeign) {
      logger.debug(
        s"해외 거래 탐지: transactionId=${transaction.transactionId}, " +
        s"userId=${transaction.userId}, countryCode=${transaction.countryCode}"
      )
    }

    isForeign
  }

  /**
   * 해외 거래를 Alert 객체로 변환
   *
   * @param transaction 탐지된 해외 거래
   * @return 생성된 Alert 객체
   */
  def toAlert(transaction: Transaction): Alert = {
    // 한국어 알림 사유 생성
    val reason = s"해외 거래 탐지 (국가: ${transaction.countryCode})"

    // Alert 생성 (팩토리 메서드 사용)
    val alert = Alert.create(
      transaction = transaction,
      ruleType = Alert.SIMPLE_RULE,
      ruleName = "FOREIGN_COUNTRY",
      reason = reason,
      severity = Alert.SEVERITY_MEDIUM
    )

    logger.info(
      s"해외 거래 알림 생성: alertId=${alert.alertId}, " +
      s"userId=${transaction.userId}, countryCode=${transaction.countryCode}"
    )

    alert
  }
}

/**
 * ForeignCountryRule 컴패니언 객체
 */
object ForeignCountryRule {

  /**
   * 새 ForeignCountryRule 인스턴스 생성
   *
   * @return ForeignCountryRule 인스턴스
   */
  def apply(): ForeignCountryRule = new ForeignCountryRule()

  /**
   * 허용되는 국가 코드 (한국)
   */
  val ALLOWED_COUNTRY = "KR"
}
