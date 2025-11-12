package io.realfds.dashboard.service;

import io.realfds.dashboard.model.TransactionMetrics;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * KafkaMetricsCollector 단위 테스트
 *
 * Given-When-Then 구조 사용 (Constitution V 요구사항)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaMetricsCollector 단위 테스트")
class KafkaMetricsCollectorTest {

    @Mock
    private AdminClient adminClient;

    @InjectMocks
    private KafkaMetricsCollector kafkaMetricsCollector;

    private static final String TOPIC_NAME = "virtual-transactions";
    private static final TopicPartition PARTITION_0 = new TopicPartition(TOPIC_NAME, 0);

    @BeforeEach
    void setUp() {
        // KafkaMetricsCollector 초기화
        kafkaMetricsCollector = new KafkaMetricsCollector(adminClient);
    }

    @Test
    @DisplayName("Given Kafka 토픽 offset 100, When 5초 후 offset 150, Then TPS = 10")
    void testCalculateTps_WhenOffsetIncreasedBy50_ThenTpsIs10() throws Exception {
        // Given: 초기 offset = 100
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> initialOffsets = new HashMap<>();
        initialOffsets.put(PARTITION_0, createOffsetInfo(100L));
        mockListOffsets(initialOffsets);

        // 첫 번째 수집 (초기화)
        kafkaMetricsCollector.collectTPS();

        // When: 5초 후 offset = 150 (50개 증가)
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> updatedOffsets = new HashMap<>();
        updatedOffsets.put(PARTITION_0, createOffsetInfo(150L));
        mockListOffsets(updatedOffsets);

        TransactionMetrics metrics = kafkaMetricsCollector.collectTPS();

        // Then: TPS = (150 - 100) / 5초 = 10
        assertThat(metrics).isNotNull();
        assertThat(metrics.getTps()).isEqualTo(10L);
        assertThat(metrics.getTotalTransactions()).isEqualTo(150L);
    }

    @Test
    @DisplayName("Given Kafka 연결 실패, When TPS 수집, Then null 반환 및 ERROR 로깅")
    void testCollectTps_WhenKafkaConnectionFails_ThenReturnsNullAndLogsError() throws Exception {
        // Given: Kafka AdminClient 연결 실패
        when(adminClient.listOffsets(any())).thenThrow(new RuntimeException("Kafka connection failed"));

        // When: TPS 수집 시도
        TransactionMetrics metrics = kafkaMetricsCollector.collectTPS();

        // Then: null 반환 (이전 값 유지를 위해)
        assertThat(metrics).isNull();

        // 로깅은 실제 구현에서 SLF4J로 ERROR 레벨로 기록됨
        verify(adminClient, times(1)).listOffsets(any());
    }

    @Test
    @DisplayName("Given 첫 번째 수집, When TPS 계산, Then TPS = 0 (이전 offset 없음)")
    void testCollectTps_WhenFirstCollection_ThenTpsIsZero() throws Exception {
        // Given: 첫 번째 수집 시도, offset = 100
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> offsets = new HashMap<>();
        offsets.put(PARTITION_0, createOffsetInfo(100L));
        mockListOffsets(offsets);

        // When: TPS 수집
        TransactionMetrics metrics = kafkaMetricsCollector.collectTPS();

        // Then: 첫 번째 수집이므로 TPS = 0 (비교할 이전 offset 없음)
        assertThat(metrics).isNotNull();
        assertThat(metrics.getTps()).isEqualTo(0L);
        assertThat(metrics.getTotalTransactions()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Given 여러 파티션, When TPS 계산, Then 모든 파티션 offset 합산")
    void testCollectTps_WhenMultiplePartitions_ThenSumsAllOffsets() throws Exception {
        // Given: 3개 파티션, 각각 offset 100, 200, 300
        TopicPartition partition1 = new TopicPartition(TOPIC_NAME, 1);
        TopicPartition partition2 = new TopicPartition(TOPIC_NAME, 2);

        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> initialOffsets = new HashMap<>();
        initialOffsets.put(PARTITION_0, createOffsetInfo(100L));
        initialOffsets.put(partition1, createOffsetInfo(200L));
        initialOffsets.put(partition2, createOffsetInfo(300L));
        mockListOffsets(initialOffsets);

        // 첫 번째 수집
        kafkaMetricsCollector.collectTPS();

        // When: 5초 후 각 파티션 offset +50
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> updatedOffsets = new HashMap<>();
        updatedOffsets.put(PARTITION_0, createOffsetInfo(150L));
        updatedOffsets.put(partition1, createOffsetInfo(250L));
        updatedOffsets.put(partition2, createOffsetInfo(350L));
        mockListOffsets(updatedOffsets);

        TransactionMetrics metrics = kafkaMetricsCollector.collectTPS();

        // Then: 총 offset = 750, 이전 = 600, TPS = (750-600)/5 = 30
        assertThat(metrics).isNotNull();
        assertThat(metrics.getTps()).isEqualTo(30L);
        assertThat(metrics.getTotalTransactions()).isEqualTo(750L);
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
}
