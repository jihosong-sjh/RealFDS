package io.realfds.dashboard.service;

import io.realfds.dashboard.model.TransactionMetrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Kafka 메트릭 수집기
 *
 * virtual-transactions 토픽의 offset 증가량으로 TPS(Transactions Per Second) 계산
 *
 * 수집 주기: 5초마다 (@Scheduled에서 호출)
 * TPS 계산: (현재 offset - 이전 offset) / 5초
 *
 * Constitution V 준수:
 * - SLF4J 로깅 (INFO: TPS 수집, WARN: Kafka 연결 지연, ERROR: Kafka 연결 실패)
 * - 함수 길이 ≤50줄
 * - 한국어 주석
 *
 * @see MetricsScheduler
 * @see TransactionMetrics
 */
@Slf4j
@Service
public class KafkaMetricsCollector {

    private static final String TOPIC_NAME = "virtual-transactions";
    private static final int COLLECTION_INTERVAL_SECONDS = 5;

    private final AdminClient adminClient;
    private final Map<TopicPartition, Long> previousOffsets = new HashMap<>();

    @Value("${kafka.topic.virtual-transactions:virtual-transactions}")
    private String topicName;

    public KafkaMetricsCollector(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    /**
     * TPS(Transactions Per Second) 수집
     *
     * Kafka AdminClient로 virtual-transactions 토픽의 모든 파티션 offset 조회 후
     * 이전 offset과 비교하여 TPS 계산
     *
     * @return TransactionMetrics (TPS, 총 거래 수) 또는 null (Kafka 연결 실패 시)
     */
    public TransactionMetrics collectTPS() {
        try {
            // 1. 토픽의 모든 파티션 조회
            Map<TopicPartition, OffsetSpec> topicPartitions = getTopicPartitions();

            if (topicPartitions.isEmpty()) {
                log.warn("Kafka 토픽 '{}'의 파티션을 찾을 수 없습니다", TOPIC_NAME);
                return null;
            }

            // 2. 각 파티션의 최신 offset 조회
            Map<TopicPartition, Long> currentOffsets = fetchLatestOffsets(topicPartitions);

            // 3. TPS 계산
            TransactionMetrics metrics = calculateMetrics(currentOffsets);

            // 4. 다음 수집을 위해 현재 offset 저장
            previousOffsets.putAll(currentOffsets);

            log.info("TPS 수집 완료: tps={}, 총 거래 수={}", metrics.getTps(), metrics.getTotalTransactions());
            return metrics;

        } catch (Exception e) {
            log.error("Kafka 메트릭 수집 실패: {}", e.getMessage(), e);
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
        var topicDescription = adminClient.describeTopics(java.util.List.of(TOPIC_NAME))
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
     * TPS 계산 및 TransactionMetrics 생성
     *
     * TPS = (현재 총 offset - 이전 총 offset) / 5초
     * 첫 번째 수집 시에는 TPS = 0 (이전 offset 없음)
     *
     * @param currentOffsets 현재 각 파티션의 offset
     * @return TransactionMetrics 객체
     */
    private TransactionMetrics calculateMetrics(Map<TopicPartition, Long> currentOffsets) {
        // 모든 파티션의 총 offset
        long totalCurrentOffset = currentOffsets.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        // 이전 offset 합계 계산
        long totalPreviousOffset = previousOffsets.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        // TPS 계산 (첫 수집 시에는 0)
        long tps = 0L;
        if (!previousOffsets.isEmpty()) {
            long offsetDelta = totalCurrentOffset - totalPreviousOffset;
            tps = offsetDelta / COLLECTION_INTERVAL_SECONDS;
        }

        return TransactionMetrics.builder()
                .timestamp(Instant.now())
                .tps(tps)
                .totalTransactions(totalCurrentOffset)
                .build();
    }
}
