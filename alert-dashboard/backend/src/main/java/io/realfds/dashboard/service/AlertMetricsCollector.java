package io.realfds.dashboard.service;

import io.realfds.dashboard.model.AlertMetrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * 알림 메트릭 수집기
 *
 * transaction-alerts 토픽의 offset 증가량으로 분당 알림 발생 수 계산
 * 규칙별(HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY) 알림 수 집계
 *
 * 수집 주기: 5초마다 (@Scheduled에서 호출)
 * 알림률 계산: (현재 offset - 이전 offset) / 5초 × 12 = 분당 알림 수
 *
 * Constitution V 준수:
 * - SLF4J 로깅 (INFO: 알림률 수집, WARN: Kafka 연결 지연, ERROR: Kafka 연결 실패)
 * - 함수 길이 ≤50줄
 * - 한국어 주석
 *
 * @see MetricsScheduler
 * @see AlertMetrics
 */
@Slf4j
@Service
public class AlertMetricsCollector {

    private static final String TOPIC_NAME = "transaction-alerts";
    private static final int COLLECTION_INTERVAL_SECONDS = 5;

    private final AdminClient adminClient;
    private final Map<TopicPartition, Long> previousOffsets = new HashMap<>();
    private final Map<String, Long> previousRuleCounts = new HashMap<>();

    @Value("${kafka.topic.transaction-alerts:transaction-alerts}")
    private String topicName;

    public AlertMetricsCollector(AdminClient adminClient) {
        this.adminClient = adminClient;
        initializeRuleCounts();
    }

    /**
     * 규칙별 카운터 초기화
     */
    private void initializeRuleCounts() {
        previousRuleCounts.put("HIGH_VALUE", 0L);
        previousRuleCounts.put("FOREIGN_COUNTRY", 0L);
        previousRuleCounts.put("HIGH_FREQUENCY", 0L);
    }

    /**
     * 알림 메트릭 수집
     *
     * Kafka AdminClient로 transaction-alerts 토픽의 모든 파티션 offset 조회 후
     * 이전 offset과 비교하여 분당 알림 발생 수 계산
     *
     * @return AlertMetrics (분당 알림 수, 규칙별 알림 수) 또는 null (Kafka 연결 실패 시)
     */
    public AlertMetrics collectAlertMetrics() {
        try {
            // 1. 토픽의 모든 파티션 조회
            Map<TopicPartition, OffsetSpec> topicPartitions = getTopicPartitions();

            if (topicPartitions.isEmpty()) {
                log.warn("Kafka 토픽 '{}'의 파티션을 찾을 수 없습니다", TOPIC_NAME);
                return null;
            }

            // 2. 각 파티션의 최신 offset 조회
            Map<TopicPartition, Long> currentOffsets = fetchLatestOffsets(topicPartitions);

            // 3. 알림 발생 수 계산
            AlertMetrics metrics = calculateMetrics(currentOffsets);

            // 4. 다음 수집을 위해 현재 offset 저장
            previousOffsets.putAll(currentOffsets);

            log.info("알림률 수집 완료: 분당 알림 수={}, 규칙별=[HIGH_VALUE={}, FOREIGN_COUNTRY={}, HIGH_FREQUENCY={}]",
                    metrics.getAlertsPerMinute(),
                    metrics.getByRule().get("HIGH_VALUE"),
                    metrics.getByRule().get("FOREIGN_COUNTRY"),
                    metrics.getByRule().get("HIGH_FREQUENCY"));

            return metrics;

        } catch (Exception e) {
            log.error("Kafka 알림 메트릭 수집 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 토픽의 모든 파티션 정보 조회
     *
     * @return Map<TopicPartition, OffsetSpec> (파티션 → LATEST offset 스펙)
     */
    private Map<TopicPartition, OffsetSpec> getTopicPartitions() throws ExecutionException, InterruptedException {
        // 토픽의 파티션 개수 조회
        var topicDescription = adminClient.describeTopics(List.of(TOPIC_NAME))
                .allTopicNames()
                .get();

        var partitions = topicDescription.get(TOPIC_NAME).partitions();

        // 각 파티션에 대한 OffsetSpec.latest() 생성
        return partitions.stream()
                .map(partition -> new TopicPartition(TOPIC_NAME, partition.partition()))
                .collect(Collectors.toMap(
                        tp -> tp,
                        tp -> OffsetSpec.latest()
                ));
    }

    /**
     * 각 파티션의 최신 offset 조회
     *
     * @param topicPartitions 조회할 토픽 파티션 목록
     * @return Map<TopicPartition, Long> (파티션 → 최신 offset)
     */
    private Map<TopicPartition, Long> fetchLatestOffsets(Map<TopicPartition, OffsetSpec> topicPartitions)
            throws ExecutionException, InterruptedException {

        ListOffsetsResult offsetsResult = adminClient.listOffsets(topicPartitions);
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> offsetInfoMap = offsetsResult.all().get();

        return offsetInfoMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().offset()
                ));
    }

    /**
     * 알림 발생률 계산 및 AlertMetrics 생성
     *
     * 분당 알림 수 = (현재 총 offset - 이전 총 offset) / 5초 × 12
     * 첫 번째 수집 시에는 알림 수 = 0 (이전 offset 없음)
     *
     * @param currentOffsets 현재 각 파티션의 offset
     * @return AlertMetrics 객체
     */
    private AlertMetrics calculateMetrics(Map<TopicPartition, Long> currentOffsets) {
        // 모든 파티션의 총 offset
        long totalCurrentOffset = currentOffsets.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        // 이전 offset 합계 계산
        long totalPreviousOffset = previousOffsets.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        // 분당 알림 수 계산 (첫 수집 시에는 0)
        long alertsPerMinute = 0L;
        if (!previousOffsets.isEmpty()) {
            long offsetDelta = totalCurrentOffset - totalPreviousOffset;
            // 5초 간격 수집이므로 12배하여 분당 알림 수 계산
            alertsPerMinute = offsetDelta * (60 / COLLECTION_INTERVAL_SECONDS);
        }

        // 규칙별 알림 수 집계 (간단한 균등 분배로 시뮬레이션)
        // 실제 구현에서는 Kafka 메시지를 consume하여 ruleType 필드로 집계
        Map<String, Long> byRule = calculateRuleDistribution(alertsPerMinute);

        return AlertMetrics.builder()
                .timestamp(Instant.now())
                .alertsPerMinute(alertsPerMinute)
                .byRule(byRule)
                .build();
    }

    /**
     * 규칙별 알림 수 분배 계산
     *
     * 간단한 시뮬레이션: 전체 알림의 40%(HIGH_VALUE), 35%(FOREIGN_COUNTRY), 25%(HIGH_FREQUENCY)
     * 실제 구현에서는 Kafka Consumer로 메시지를 읽고 ruleType 필드를 집계해야 함
     *
     * @param totalAlerts 총 알림 수
     * @return 규칙별 알림 수 Map
     */
    private Map<String, Long> calculateRuleDistribution(long totalAlerts) {
        Map<String, Long> byRule = new HashMap<>();

        // 간단한 비율 분배 (실제로는 Kafka 메시지 파싱 필요)
        byRule.put("HIGH_VALUE", Math.round(totalAlerts * 0.40));           // 40%
        byRule.put("FOREIGN_COUNTRY", Math.round(totalAlerts * 0.35));      // 35%
        byRule.put("HIGH_FREQUENCY", Math.round(totalAlerts * 0.25));       // 25%

        // 반올림 오차로 인한 총합 불일치 보정
        long sum = byRule.values().stream().mapToLong(Long::longValue).sum();
        if (sum != totalAlerts) {
            // 가장 큰 값에 차이를 더하거나 뺌
            String maxRule = byRule.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("HIGH_VALUE");
            byRule.put(maxRule, byRule.get(maxRule) + (totalAlerts - sum));
        }

        return byRule;
    }
}
