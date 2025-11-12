package io.realfds.alert.model

import java.time.Instant

/**
 * 알림 발생률 메트릭 데이터 포인트 (시계열)
 *
 * 분당 알림 발생 수를 규칙별로 분류하여 관리합니다.
 * 5초마다 Kafka transaction-alerts 토픽의 메시지 수를 집계하여 저장합니다.
 *
 * 이 클래스는 메모리에만 저장되며 데이터베이스 영속화를 하지 않습니다.
 * MetricsStore의 Circular Buffer에 최근 1시간(720개) 데이터 포인트가 유지됩니다.
 *
 * @property timestamp 메트릭 측정 시각 (5초 간격으로 정렬됨)
 * @property alertsPerMinute 분당 총 알림 발생 수
 * @property byRule 규칙별 알림 발생 수 (HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY)
 *
 * @see io.realfds.alert.service.AlertMetricsCollector
 * @see io.realfds.alert.service.MetricsStore
 */
data class AlertMetrics(
    /**
     * 메트릭 측정 시각
     * - ISO-8601 형식의 타임스탬프
     * - 5초 간격으로 정렬됨 (예: 10:00:00, 10:00:05, 10:00:10, ...)
     * - 시간대 정보(Time Zone) 포함
     */
    val timestamp: Instant,

    /**
     * 분당 총 알림 발생 수
     * - Kafka transaction-alerts 토픽의 메시지 수 집계
     * - 0 이상 1,000 이하
     * - sum(byRule.values)와 일치해야 함
     */
    val alertsPerMinute: Long,

    /**
     * 규칙별 알림 발생 수
     * - HIGH_VALUE: 고액 거래 탐지 규칙
     * - FOREIGN_COUNTRY: 해외 거래 탐지 규칙
     * - HIGH_FREQUENCY: 고빈도 거래 탐지 규칙
     * - 각 값은 0 이상
     * - 3개 규칙의 합은 alertsPerMinute와 일치해야 함
     */
    val byRule: Map<String, Long>
) {
    init {
        // 분당 알림 수는 0 이상 1,000 이하
        require(alertsPerMinute in 0..1000) {
            "분당 알림 수는 0 이상 1000 이하여야 합니다: $alertsPerMinute"
        }

        // byRule은 정확히 3개 규칙을 포함해야 함
        val expectedRules = setOf("HIGH_VALUE", "FOREIGN_COUNTRY", "HIGH_FREQUENCY")
        require(byRule.keys == expectedRules) {
            "규칙 유형이 유효하지 않습니다: ${byRule.keys} (필요: $expectedRules)"
        }

        // 각 규칙의 알림 수는 0 이상
        byRule.forEach { (rule, count) ->
            require(count >= 0) {
                "알림 수는 음수일 수 없습니다: $rule=$count"
            }
        }

        // 규칙별 알림 수의 합은 총 알림 수와 일치해야 함
        val totalByRule = byRule.values.sum()
        require(totalByRule == alertsPerMinute) {
            "규칙별 알림 수의 합($totalByRule)이 총 알림 수($alertsPerMinute)와 일치하지 않습니다"
        }

        // 타임스탬프는 미래일 수 없음
        require(timestamp <= Instant.now()) {
            "메트릭 측정 시각은 미래일 수 없습니다: $timestamp"
        }
    }

    /**
     * 특정 규칙의 알림 수를 조회합니다.
     *
     * @param ruleName 규칙 이름 (HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY)
     * @return 해당 규칙의 알림 수
     */
    fun getAlertCountByRule(ruleName: String): Long {
        return byRule[ruleName] ?: throw IllegalArgumentException(
            "유효하지 않은 규칙 이름입니다: $ruleName"
        )
    }

    /**
     * 가장 많은 알림이 발생한 규칙을 조회합니다.
     *
     * @return 최다 알림 발생 규칙 이름 (동점 시 첫 번째 규칙 반환)
     */
    fun getTopRule(): String? {
        return byRule.maxByOrNull { it.value }?.key
    }

    companion object {
        /**
         * 규칙별 알림 수를 집계하여 AlertMetrics를 생성합니다.
         *
         * @param timestamp 메트릭 측정 시각
         * @param highValueCount HIGH_VALUE 규칙 알림 수
         * @param foreignCountryCount FOREIGN_COUNTRY 규칙 알림 수
         * @param highFrequencyCount HIGH_FREQUENCY 규칙 알림 수
         * @return 생성된 AlertMetrics 인스턴스
         */
        fun create(
            timestamp: Instant,
            highValueCount: Long,
            foreignCountryCount: Long,
            highFrequencyCount: Long
        ): AlertMetrics {
            val totalAlerts = highValueCount + foreignCountryCount + highFrequencyCount
            val byRule = mapOf(
                "HIGH_VALUE" to highValueCount,
                "FOREIGN_COUNTRY" to foreignCountryCount,
                "HIGH_FREQUENCY" to highFrequencyCount
            )

            return AlertMetrics(
                timestamp = timestamp,
                alertsPerMinute = totalAlerts,
                byRule = byRule
            )
        }
    }
}
