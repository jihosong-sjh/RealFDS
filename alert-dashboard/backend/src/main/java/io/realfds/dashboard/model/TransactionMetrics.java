package io.realfds.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 거래량 메트릭 데이터 포인트 (시계열)
 *
 * Constitution V 준수:
 * - 각 필드에 한국어 주석
 * - 명확한 Validation Rules
 *
 * @see io.realfds.dashboard.service.KafkaMetricsCollector
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMetrics {
    /**
     * 메트릭 측정 시각 (5초 간격, ISO-8601 형식)
     */
    private Instant timestamp;

    /**
     * 초당 거래 수 (TPS)
     * 0 이상 10,000 이하
     */
    private long tps;

    /**
     * 시스템 시작 이후 누적 거래 수
     * 단조 증가 (monotonically increasing)
     */
    private long totalTransactions;
}
