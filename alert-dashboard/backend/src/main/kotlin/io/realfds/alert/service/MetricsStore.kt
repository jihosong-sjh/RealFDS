package io.realfds.alert.service

import io.realfds.alert.model.MetricsDataPoint
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * 메모리 기반 메트릭 데이터 저장소
 *
 * ConcurrentLinkedDeque를 사용한 Circular Buffer 구현체입니다.
 * 최근 1시간(720개) 메트릭 데이터 포인트를 메모리에 보관하며,
 * 1시간 이전 데이터는 자동으로 제거됩니다 (FIFO).
 *
 * ## 성능 특성
 * - **삽입 (addDataPoint)**: O(1) - addLast() 상수 시간
 * - **삭제 (자동)**: O(1) - removeFirst() 상수 시간
 * - **조회 (getAll)**: O(n) - 전체 데이터 순회, n=720
 * - **필터링 (getDataSince)**: O(n) - 조건부 필터링
 * - **스레드 안전성**: Lock-free CAS (Compare-And-Swap) 알고리즘
 *
 * ## 메모리 사용량
 * - 단일 데이터 포인트: ~750 bytes
 * - 1시간 데이터 (720개): ~540 KB
 * - Deque 노드 오버헤드: ~50% → 총 ~800 KB
 *
 * @see MetricsDataPoint
 * @see io.realfds.alert.service.MetricsScheduler
 */
@Service
class MetricsStore {
    private val logger = LoggerFactory.getLogger(MetricsStore::class.java)

    /**
     * 시계열 데이터 저장소 (Thread-safe, Lock-free)
     * - FIFO (First-In-First-Out) 방식
     * - 가장 오래된 데이터가 앞(first), 가장 최신 데이터가 뒤(last)
     */
    private val dataPoints = ConcurrentLinkedDeque<MetricsDataPoint>()

    /**
     * 새로운 메트릭 데이터 포인트 추가
     *
     * 1시간(RETENTION_PERIOD) 이전 데이터를 자동 삭제합니다 (FIFO).
     * 스레드 안전하며, 여러 스레드가 동시에 호출 가능합니다.
     *
     * 시간 복잡도: O(1) - addLast()
     * 공간 복잡도: O(720) - 고정 크기
     *
     * @param point 추가할 메트릭 데이터 포인트
     */
    fun addDataPoint(point: MetricsDataPoint) {
        // 최신 데이터를 뒤에 추가
        dataPoints.addLast(point)

        // 1시간 이전 데이터 자동 삭제
        val cutoff = Instant.now().minus(RETENTION_PERIOD)
        var removedCount = 0

        while (dataPoints.isNotEmpty() && dataPoints.peekFirst()?.timestamp?.isBefore(cutoff) == true) {
            dataPoints.removeFirst()
            removedCount++
        }

        if (removedCount > 0) {
            logger.trace(
                "오래된 데이터 포인트 {}개 삭제 (기준: {} 이전)",
                removedCount,
                cutoff
            )
        }

        logger.debug(
            "메트릭 데이터 추가: timestamp={}, tps={}, alertsPerMinute={}, 총 데이터 포인트={}",
            point.timestamp,
            point.transactionMetrics.tps,
            point.alertMetrics.alertsPerMinute,
            dataPoints.size
        )
    }

    /**
     * 모든 메트릭 데이터 조회 (최근 1시간)
     *
     * 불변 리스트(스냅샷)를 반환하므로 반환된 리스트 수정은 원본에 영향을 주지 않습니다.
     * 시간 순서대로 정렬되어 있습니다 (오래된 데이터 → 최신 데이터).
     *
     * 시간 복잡도: O(n), n=720
     *
     * @return 메트릭 데이터 포인트 리스트 (시간 순서 정렬)
     */
    fun getAll(): List<MetricsDataPoint> {
        return ArrayList(dataPoints) // 불변 스냅샷
    }

    /**
     * 특정 시각 이후의 메트릭 데이터 조회 (백필 용도)
     *
     * WebSocket 재연결 시 클라이언트가 마지막으로 수신한 타임스탬프 이후의
     * 누락된 데이터를 조회하는 데 사용됩니다.
     *
     * 시간 복잡도: O(n), n=720
     *
     * @param since 조회 시작 시각 (이 시각 이후의 데이터만 반환)
     * @return since 이후의 메트릭 데이터 포인트 리스트
     */
    fun getDataSince(since: Instant): List<MetricsDataPoint> {
        val result = dataPoints.filter { it.timestamp.isAfter(since) }

        logger.debug(
            "백필 데이터 조회: since={}, 결과 개수={}",
            since,
            result.size
        )

        return result
    }

    /**
     * 가장 최근 N개의 메트릭 데이터 조회
     *
     * @param count 조회할 데이터 개수
     * @return 최근 N개 메트릭 데이터 포인트 리스트
     */
    fun getRecent(count: Int): List<MetricsDataPoint> {
        require(count > 0) { "조회 개수는 0보다 커야 합니다: $count" }

        return dataPoints.toList().takeLast(count)
    }

    /**
     * 현재 저장된 데이터 포인트 수 조회
     *
     * @return 데이터 포인트 수 (0 ≤ count ≤ 720)
     */
    fun size(): Int {
        return dataPoints.size
    }

    /**
     * 모든 데이터 삭제 (테스트 용도)
     *
     * 운영 환경에서는 사용하지 않으며, 단위 테스트에서 테스트 간 격리를 위해 사용됩니다.
     */
    fun clear() {
        dataPoints.clear()
        logger.info("메트릭 데이터 저장소 초기화 완료")
    }

    /**
     * 저장소가 비어있는지 확인
     *
     * @return 데이터가 없으면 true, 있으면 false
     */
    fun isEmpty(): Boolean {
        return dataPoints.isEmpty()
    }

    /**
     * 데이터 저장소 통계 정보 조회
     *
     * @return 저장소 통계 정보
     */
    fun getStats(): StoreStats {
        val dataList = dataPoints.toList()
        val oldestTimestamp = dataList.firstOrNull()?.timestamp
        val latestTimestamp = dataList.lastOrNull()?.timestamp

        return StoreStats(
            totalDataPoints = dataList.size,
            oldestTimestamp = oldestTimestamp,
            latestTimestamp = latestTimestamp,
            estimatedMemoryBytes = dataList.size * MetricsDataPoint.ESTIMATED_SIZE_BYTES
        )
    }

    /**
     * 저장소 통계 정보
     */
    data class StoreStats(
        val totalDataPoints: Int,
        val oldestTimestamp: Instant?,
        val latestTimestamp: Instant?,
        val estimatedMemoryBytes: Long
    )

    companion object {
        /**
         * 최대 데이터 포인트 수
         * 1시간 = 3600초 / 5초 간격 = 720개
         */
        const val MAX_DATA_POINTS = 720

        /**
         * 데이터 보관 기간
         * 1시간 (3600초)
         */
        val RETENTION_PERIOD: Duration = Duration.ofHours(1)
    }
}
