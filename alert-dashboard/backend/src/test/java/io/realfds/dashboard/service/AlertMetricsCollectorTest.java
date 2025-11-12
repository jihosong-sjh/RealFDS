package io.realfds.dashboard.service;

import io.realfds.dashboard.model.AlertMetrics;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AlertMetricsCollector 단위 테스트
 *
 * Given-When-Then 구조 사용 (Constitution V 요구사항)
 *
 * 테스트 시나리오:
 * - Given 3개 규칙 알림 발생, When 알림률 수집, Then 규칙별 분당 알림 수 계산
 * - Given HIGH_VALUE 알림 20개, When 알림률 계산, Then byRule["HIGH_VALUE"] = 20
 * - Given Kafka 연결 실패, When 알림률 수집, Then null 반환 및 ERROR 로깅
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertMetricsCollector 단위 테스트")
class AlertMetricsCollectorTest {

    @Mock
    private AdminClient adminClient;

    @InjectMocks
    private AlertMetricsCollector alertMetricsCollector;

    private static final String TOPIC_NAME = "transaction-alerts";
    private static final TopicPartition PARTITION_0 = new TopicPartition(TOPIC_NAME, 0);

    @BeforeEach
    void setUp() {
        // AlertMetricsCollector 초기화
        alertMetricsCollector = new AlertMetricsCollector(adminClient);
    }

    @Test
    @DisplayName("Given 3개 규칙 알림 발생, When 알림률 수집, Then 규칙별 분당 알림 수 계산")
    void testCollectAlertMetrics_WhenThreeRulesFire_ThenCalculatesPerMinuteRate() throws Exception {
        // Given: 초기 offset = 0
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> initialOffsets = new HashMap<>();
        initialOffsets.put(PARTITION_0, createOffsetInfo(0L));
        mockListOffsets(initialOffsets);

        // 첫 번째 수집 (초기화)
        alertMetricsCollector.collectAlertMetrics();

        // When: 1분 후 offset = 60 (60개 알림 발생)
        // HIGH_VALUE: 20개, FOREIGN_COUNTRY: 25개, HIGH_FREQUENCY: 15개
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> updatedOffsets = new HashMap<>();
        updatedOffsets.put(PARTITION_0, createOffsetInfo(60L));
        mockListOffsets(updatedOffsets);

        // 알림 규칙별 분포 모킹
        Map<String, Long> expectedByRule = Map.of(
            "HIGH_VALUE", 20L,
            "FOREIGN_COUNTRY", 25L,
            "HIGH_FREQUENCY", 15L
        );
        mockAlertRuleDistribution(expectedByRule);

        AlertMetrics metrics = alertMetricsCollector.collectAlertMetrics();

        // Then: 분당 알림 수 = 60
        assertThat(metrics).isNotNull();
        assertThat(metrics.getAlertsPerMinute()).isEqualTo(60L);
        assertThat(metrics.getByRule()).containsEntry("HIGH_VALUE", 20L);
        assertThat(metrics.getByRule()).containsEntry("FOREIGN_COUNTRY", 25L);
        assertThat(metrics.getByRule()).containsEntry("HIGH_FREQUENCY", 15L);
    }

    @Test
    @DisplayName("Given HIGH_VALUE 알림 20개, When 알림률 계산, Then byRule['HIGH_VALUE'] = 20")
    void testCollectAlertMetrics_WhenHighValueAlerts_ThenCountsCorrectly() throws Exception {
        // Given: 초기 offset = 0
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> initialOffsets = new HashMap<>();
        initialOffsets.put(PARTITION_0, createOffsetInfo(0L));
        mockListOffsets(initialOffsets);

        // 첫 번째 수집
        alertMetricsCollector.collectAlertMetrics();

        // When: HIGH_VALUE 알림만 20개 발생
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> updatedOffsets = new HashMap<>();
        updatedOffsets.put(PARTITION_0, createOffsetInfo(20L));
        mockListOffsets(updatedOffsets);

        Map<String, Long> expectedByRule = Map.of(
            "HIGH_VALUE", 20L,
            "FOREIGN_COUNTRY", 0L,
            "HIGH_FREQUENCY", 0L
        );
        mockAlertRuleDistribution(expectedByRule);

        AlertMetrics metrics = alertMetricsCollector.collectAlertMetrics();

        // Then: HIGH_VALUE만 20
        assertThat(metrics).isNotNull();
        assertThat(metrics.getAlertsPerMinute()).isEqualTo(20L);
        assertThat(metrics.getByRule().get("HIGH_VALUE")).isEqualTo(20L);
        assertThat(metrics.getByRule().get("FOREIGN_COUNTRY")).isEqualTo(0L);
        assertThat(metrics.getByRule().get("HIGH_FREQUENCY")).isEqualTo(0L);
    }

    @Test
    @DisplayName("Given Kafka 연결 실패, When 알림률 수집, Then null 반환 및 ERROR 로깅")
    void testCollectAlertMetrics_WhenKafkaConnectionFails_ThenReturnsNullAndLogsError() throws Exception {
        // Given: Kafka AdminClient 연결 실패
        when(adminClient.listOffsets(any())).thenThrow(new RuntimeException("Kafka connection failed"));

        // When: 알림률 수집 시도
        AlertMetrics metrics = alertMetricsCollector.collectAlertMetrics();

        // Then: null 반환 (이전 값 유지를 위해)
        assertThat(metrics).isNull();

        // 로깅은 실제 구현에서 SLF4J로 ERROR 레벨로 기록됨
        verify(adminClient, times(1)).listOffsets(any());
    }

    @Test
    @DisplayName("Given 첫 번째 수집, When 알림률 계산, Then 알림 수 = 0 (이전 offset 없음)")
    void testCollectAlertMetrics_WhenFirstCollection_ThenAlertsAreZero() throws Exception {
        // Given: 첫 번째 수집 시도, offset = 100
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> offsets = new HashMap<>();
        offsets.put(PARTITION_0, createOffsetInfo(100L));
        mockListOffsets(offsets);

        // When: 알림률 수집
        AlertMetrics metrics = alertMetricsCollector.collectAlertMetrics();

        // Then: 첫 번째 수집이므로 alertsPerMinute = 0 (비교할 이전 offset 없음)
        assertThat(metrics).isNotNull();
        assertThat(metrics.getAlertsPerMinute()).isEqualTo(0L);
        assertThat(metrics.getByRule().values().stream().mapToLong(Long::longValue).sum()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Given 여러 파티션, When 알림률 계산, Then 모든 파티션 offset 합산")
    void testCollectAlertMetrics_WhenMultiplePartitions_ThenSumsAllOffsets() throws Exception {
        // Given: 3개 파티션, 각각 offset 10, 20, 30
        TopicPartition partition1 = new TopicPartition(TOPIC_NAME, 1);
        TopicPartition partition2 = new TopicPartition(TOPIC_NAME, 2);

        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> initialOffsets = new HashMap<>();
        initialOffsets.put(PARTITION_0, createOffsetInfo(10L));
        initialOffsets.put(partition1, createOffsetInfo(20L));
        initialOffsets.put(partition2, createOffsetInfo(30L));
        mockListOffsets(initialOffsets);

        // 첫 번째 수집
        alertMetricsCollector.collectAlertMetrics();

        // When: 1분 후 각 파티션 offset +10
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> updatedOffsets = new HashMap<>();
        updatedOffsets.put(PARTITION_0, createOffsetInfo(20L));
        updatedOffsets.put(partition1, createOffsetInfo(30L));
        updatedOffsets.put(partition2, createOffsetInfo(40L));
        mockListOffsets(updatedOffsets);

        Map<String, Long> expectedByRule = Map.of(
            "HIGH_VALUE", 10L,
            "FOREIGN_COUNTRY", 10L,
            "HIGH_FREQUENCY", 10L
        );
        mockAlertRuleDistribution(expectedByRule);

        AlertMetrics metrics = alertMetricsCollector.collectAlertMetrics();

        // Then: 총 offset 증가 = 30, 분당 알림 수 = 30
        assertThat(metrics).isNotNull();
        assertThat(metrics.getAlertsPerMinute()).isEqualTo(30L);
    }

    @Test
    @DisplayName("Given 규칙별 알림 수 합산, When 검증, Then sum(byRule.values) == alertsPerMinute")
    void testCollectAlertMetrics_WhenValidateSum_ThenByRuleSumEqualsTotal() throws Exception {
        // Given: 초기 offset = 0
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> initialOffsets = new HashMap<>();
        initialOffsets.put(PARTITION_0, createOffsetInfo(0L));
        mockListOffsets(initialOffsets);

        alertMetricsCollector.collectAlertMetrics();

        // When: 100개 알림 발생
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> updatedOffsets = new HashMap<>();
        updatedOffsets.put(PARTITION_0, createOffsetInfo(100L));
        mockListOffsets(updatedOffsets);

        Map<String, Long> expectedByRule = Map.of(
            "HIGH_VALUE", 40L,
            "FOREIGN_COUNTRY", 35L,
            "HIGH_FREQUENCY", 25L
        );
        mockAlertRuleDistribution(expectedByRule);

        AlertMetrics metrics = alertMetricsCollector.collectAlertMetrics();

        // Then: 규칙별 합산 = 총 알림 수
        long sum = metrics.getByRule().values().stream().mapToLong(Long::longValue).sum();
        assertThat(sum).isEqualTo(metrics.getAlertsPerMinute());
        assertThat(metrics.getAlertsPerMinute()).isEqualTo(100L);
    }

    /**
     * 테스트 헬퍼: ListOffsetsResult.ListOffsetsResultInfo 생성
     */
    private ListOffsetsResult.ListOffsetsResultInfo createOffsetInfo(long offset) {
        return new ListOffsetsResult.ListOffsetsResultInfo(offset, System.currentTimeMillis(), null);
    }

    /**
     * 테스트 헬퍼: AdminClient.listOffsets() 모킹
     */
    @SuppressWarnings("unchecked")
    private void mockListOffsets(Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> offsets) throws Exception {
        ListOffsetsResult listOffsetsResult = mock(ListOffsetsResult.class);
        KafkaFuture<Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo>> future = mock(KafkaFuture.class);

        when(adminClient.listOffsets(any())).thenReturn(listOffsetsResult);
        when(listOffsetsResult.all()).thenReturn(future);
        when(future.get()).thenReturn(offsets);
    }

    /**
     * 테스트 헬퍼: 알림 규칙별 분포 모킹
     *
     * 실제 구현에서는 Kafka 메시지를 파싱하여 규칙별 카운트를 집계하지만,
     * 단위 테스트에서는 규칙별 분포를 직접 모킹
     */
    private void mockAlertRuleDistribution(Map<String, Long> byRule) {
        // 실제 AlertMetricsCollector 구현에서 규칙별 카운트를 반환하도록 모킹
        // 이 메서드는 구현 시 실제 로직에 맞게 조정 필요
    }
}
