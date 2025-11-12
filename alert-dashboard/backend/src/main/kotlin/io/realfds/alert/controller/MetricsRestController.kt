package io.realfds.alert.controller

import io.realfds.alert.model.MetricsDataPoint
import io.realfds.alert.model.ServiceHealth
import io.realfds.alert.service.MetricsStore
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 메트릭 REST API 컨트롤러
 *
 * 실시간 대시보드의 메트릭 데이터를 REST API로 제공합니다.
 * WebSocket이 주 데이터 소스이지만, 초기 로드 및 백업 용도로 REST API를 제공합니다.
 *
 * ## 엔드포인트
 * - **GET /api/v1/services**: 5개 서비스 목록 조회
 * - **GET /api/v1/metrics/current**: 현재 메트릭 스냅샷 조회
 *
 * ## CORS 설정
 * - 로컬 개발용: http://localhost:3000 (React 개발 서버)
 * - 운영 환경: 별도 CORS 설정 필요
 *
 * @see MetricsStore - 메트릭 데이터 저장소
 * @see MetricsDataPoint - 메트릭 데이터 포인트
 * @see ServiceHealth - 서비스 Health Check 상태
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:8083"])
class MetricsRestController(
    private val metricsStore: MetricsStore
) {
    private val logger = LoggerFactory.getLogger(MetricsRestController::class.java)

    /**
     * GET /api/v1/services
     *
     * 5개 마이크로서비스의 목록을 조회합니다.
     * 현재 Health Check 상태와 함께 반환됩니다.
     *
     * ## 사용 사례
     * - 대시보드 초기 로딩 시 서비스 목록 조회
     * - WebSocket 연결 실패 시 대체 API
     *
     * ## 응답 예시
     * ```json
     * [
     *   {
     *     "serviceName": "transaction-generator",
     *     "status": "UP",
     *     "lastChecked": "2025-11-12T10:30:05Z",
     *     "responseTime": 45,
     *     "memoryUsage": 128,
     *     "errorType": null,
     *     "errorMessage": null
     *   },
     *   ...
     * ]
     * ```
     *
     * @return 서비스 Health Check 상태 목록 (200 OK)
     *         데이터가 없을 경우 빈 배열 (200 OK)
     */
    @GetMapping("/services")
    fun getServices(): ResponseEntity<List<ServiceHealth>> {
        logger.info("서비스 목록 조회 요청 수신")

        return try {
            // MetricsStore에서 가장 최근 데이터 포인트 조회
            val latestDataPoint = metricsStore.getRecent(1).firstOrNull()

            if (latestDataPoint != null) {
                logger.debug(
                    "서비스 목록 조회 성공: {} 개 서비스, timestamp={}",
                    latestDataPoint.services.size,
                    latestDataPoint.timestamp
                )
                ResponseEntity.ok(latestDataPoint.services)
            } else {
                // 데이터가 없을 경우 빈 배열 반환
                logger.warn("메트릭 데이터가 아직 수집되지 않았습니다 (빈 배열 반환)")
                ResponseEntity.ok(emptyList())
            }
        } catch (e: Exception) {
            logger.error("서비스 목록 조회 중 오류 발생", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(emptyList())
        }
    }

    /**
     * GET /api/v1/metrics/current
     *
     * 현재 메트릭 스냅샷을 조회합니다.
     * 가장 최근에 수집된 메트릭 데이터 포인트를 반환합니다.
     *
     * ## 사용 사례
     * - 대시보드 초기 로딩 시 전체 메트릭 조회
     * - WebSocket 연결 실패 시 대체 API
     * - 외부 모니터링 시스템 연동 (optional)
     *
     * ## 응답 예시
     * ```json
     * {
     *   "timestamp": "2025-11-12T10:30:05Z",
     *   "services": [ ... ],
     *   "transactionMetrics": {
     *     "timestamp": "2025-11-12T10:30:05Z",
     *     "tps": 87,
     *     "totalTransactions": 314280
     *   },
     *   "alertMetrics": {
     *     "timestamp": "2025-11-12T10:30:05Z",
     *     "alertsPerMinute": 12,
     *     "byRule": {
     *       "HIGH_VALUE": 5,
     *       "FOREIGN_COUNTRY": 4,
     *       "HIGH_FREQUENCY": 3
     *     }
     *   }
     * }
     * ```
     *
     * @return 현재 메트릭 스냅샷 (200 OK)
     *         데이터가 없을 경우 204 No Content
     */
    @GetMapping("/metrics/current")
    fun getCurrentMetrics(): ResponseEntity<MetricsDataPoint> {
        logger.info("현재 메트릭 스냅샷 조회 요청 수신")

        return try {
            // MetricsStore에서 가장 최근 데이터 포인트 조회
            val latestDataPoint = metricsStore.getRecent(1).firstOrNull()

            if (latestDataPoint != null) {
                logger.debug(
                    "현재 메트릭 조회 성공: timestamp={}, tps={}, alertsPerMinute={}",
                    latestDataPoint.timestamp,
                    latestDataPoint.transactionMetrics.tps,
                    latestDataPoint.alertMetrics.alertsPerMinute
                )
                ResponseEntity.ok(latestDataPoint)
            } else {
                // 데이터가 아직 수집되지 않았을 경우 204 No Content
                logger.warn("메트릭 데이터가 아직 수집되지 않았습니다 (204 No Content)")
                ResponseEntity.noContent().build()
            }
        } catch (e: Exception) {
            logger.error("현재 메트릭 조회 중 오류 발생", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * GET /api/v1/metrics/stats
     *
     * 메트릭 저장소 통계 정보를 조회합니다 (optional, 디버깅 용도).
     *
     * ## 응답 예시
     * ```json
     * {
     *   "totalDataPoints": 720,
     *   "oldestTimestamp": "2025-11-12T09:30:05Z",
     *   "latestTimestamp": "2025-11-12T10:30:05Z",
     *   "estimatedMemoryBytes": 540000
     * }
     * ```
     *
     * @return 저장소 통계 정보 (200 OK)
     */
    @GetMapping("/metrics/stats")
    fun getMetricsStats(): ResponseEntity<MetricsStore.StoreStats> {
        logger.info("메트릭 저장소 통계 조회 요청 수신")

        return try {
            val stats = metricsStore.getStats()

            logger.debug(
                "저장소 통계 조회 성공: totalDataPoints={}, estimatedMemoryBytes={}",
                stats.totalDataPoints,
                stats.estimatedMemoryBytes
            )

            ResponseEntity.ok(stats)
        } catch (e: Exception) {
            logger.error("저장소 통계 조회 중 오류 발생", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Health Check 엔드포인트
     *
     * 컨트롤러가 정상 작동하는지 확인합니다.
     * Spring Boot Actuator의 /actuator/health와 별개로,
     * REST API 자체의 health check입니다.
     *
     * @return 정상 작동 시 "OK" (200 OK)
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "MetricsRestController",
                "dataPointsCount" to metricsStore.size().toString()
            )
        )
    }
}
