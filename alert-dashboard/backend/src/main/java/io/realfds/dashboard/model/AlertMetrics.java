package io.realfds.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 알림 발생률 메트릭 데이터 포인트 (시계열)
 *
 * Constitution V 준수:
 * - 각 필드에 한국어 주석
 * - 명확한 Validation Rules
 *
 * @see io.realfds.dashboard.service.AlertMetricsCollector
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertMetrics {
    /**
     * 메트릭 측정 시각 (5초 간격, ISO-8601 형식)
     */
    private Instant timestamp;

    /**
     * 분당 총 알림 발생 수
     * 0 이상 1,000 이하
     */
    private long alertsPerMinute;

    /**
     * 규칙별 알림 발생 수
     * Keys: HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY
     * Values: 각 규칙의 알림 수 (0 이상)
     *
     * 제약: sum(byRule.values()) == alertsPerMinute
     */
    private Map<String, Long> byRule;
}
