package io.realfds.alert.model

import java.time.Instant

/**
 * 거래량 메트릭 데이터 포인트 (시계열)
 *
 * 초당 거래 처리량(TPS)을 시계열 데이터로 관리합니다.
 * 5초마다 Kafka 토픽의 offset 증가량을 기반으로 TPS를 계산하여 저장합니다.
 *
 * 이 클래스는 메모리에만 저장되며 데이터베이스 영속화를 하지 않습니다.
 * MetricsStore의 Circular Buffer에 최근 1시간(720개) 데이터 포인트가 유지됩니다.
 *
 * @property timestamp 메트릭 측정 시각 (5초 간격으로 정렬됨)
 * @property tps 초당 거래 수 (Transactions Per Second)
 * @property totalTransactions 시스템 시작 이후 누적 거래 수
 *
 * @see io.realfds.alert.service.KafkaMetricsCollector
 * @see io.realfds.alert.service.MetricsStore
 */
data class TransactionMetrics(
    /**
     * 메트릭 측정 시각
     * - ISO-8601 형식의 타임스탬프
     * - 5초 간격으로 정렬됨 (예: 10:00:00, 10:00:05, 10:00:10, ...)
     * - 시간대 정보(Time Zone) 포함
     */
    val timestamp: Instant,

    /**
     * 초당 거래 수 (TPS)
     * - Kafka virtual-transactions 토픽의 offset 증가량으로 계산
     * - 0 이상 10,000 이하
     * - 계산 공식: (현재 offset - 이전 offset) / 5초
     */
    val tps: Long,

    /**
     * 시스템 시작 이후 누적 거래 수
     * - Kafka 토픽의 총 offset
     * - 단조 증가 (monotonically increasing)
     * - 0 이상
     */
    val totalTransactions: Long
) {
    init {
        // TPS는 0 이상 10,000 이하
        require(tps in 0..10000) {
            "TPS는 0 이상 10000 이하여야 합니다: $tps"
        }

        // 누적 거래 수는 0 이상
        require(totalTransactions >= 0) {
            "누적 거래 수는 0 이상이어야 합니다: $totalTransactions"
        }

        // 타임스탬프는 미래일 수 없음
        require(timestamp <= Instant.now()) {
            "메트릭 측정 시각은 미래일 수 없습니다: $timestamp"
        }
    }

    companion object {
        /**
         * TPS 계산 헬퍼 함수
         *
         * @param currentOffset 현재 Kafka 토픽 offset
         * @param previousOffset 이전 폴링 시 Kafka 토픽 offset
         * @param intervalSeconds 폴링 간격 (초 단위, 기본값: 5초)
         * @return 계산된 TPS
         */
        fun calculateTps(
            currentOffset: Long,
            previousOffset: Long,
            intervalSeconds: Long = 5
        ): Long {
            require(currentOffset >= previousOffset) {
                "현재 offset은 이전 offset보다 작을 수 없습니다: current=$currentOffset, previous=$previousOffset"
            }
            require(intervalSeconds > 0) {
                "폴링 간격은 0보다 커야 합니다: $intervalSeconds"
            }

            val deltaOffset = currentOffset - previousOffset
            return deltaOffset / intervalSeconds
        }
    }
}
