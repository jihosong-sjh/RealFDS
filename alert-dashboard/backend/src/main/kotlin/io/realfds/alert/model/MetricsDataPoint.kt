package io.realfds.alert.model

import java.time.Instant

/**
 * 시계열 메트릭 데이터 포인트 (통합 wrapper)
 *
 * TPS와 알림 발생률을 하나의 데이터 포인트로 통합하여 관리합니다.
 * MetricsStore의 Circular Buffer에 저장되며, 5초마다 새로운 데이터 포인트가 추가됩니다.
 *
 * 이 클래스는 TransactionMetrics와 AlertMetrics를 하나의 타임스탬프로 결합하여
 * WebSocket 브로드캐스트 및 백필 처리를 간소화합니다.
 *
 * @property timestamp 메트릭 측정 시각 (5초 간격으로 정렬됨)
 * @property services 5개 서비스의 Health Check 상태 목록
 * @property transactionMetrics 거래량 메트릭
 * @property alertMetrics 알림 발생률 메트릭
 *
 * @see io.realfds.alert.service.MetricsStore
 * @see io.realfds.alert.model.TransactionMetrics
 * @see io.realfds.alert.model.AlertMetrics
 * @see io.realfds.alert.model.ServiceHealth
 */
data class MetricsDataPoint(
    /**
     * 메트릭 측정 시각
     * - ISO-8601 형식의 타임스탬프
     * - 5초 간격으로 정렬됨 (예: 10:00:00, 10:00:05, 10:00:10, ...)
     * - 모든 메트릭(services, transactionMetrics, alertMetrics)이 동일한 시각 공유
     */
    val timestamp: Instant,

    /**
     * 5개 마이크로서비스의 Health Check 상태
     * - transaction-generator
     * - fraud-detector
     * - alert-service
     * - websocket-gateway
     * - frontend-dashboard
     *
     * 각 서비스는 UP/DOWN 상태, 응답 시간, 메모리 사용량 등을 포함합니다.
     */
    val services: List<ServiceHealth>,

    /**
     * 거래량 메트릭
     * - 초당 거래 수 (TPS)
     * - 누적 거래 수
     */
    val transactionMetrics: TransactionMetrics,

    /**
     * 알림 발생률 메트릭
     * - 분당 총 알림 수
     * - 규칙별 알림 수 (HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY)
     */
    val alertMetrics: AlertMetrics
) {
    init {
        // 타임스탬프는 미래일 수 없음
        require(timestamp <= Instant.now()) {
            "메트릭 측정 시각은 미래일 수 없습니다: $timestamp"
        }

        // 서비스 목록은 정확히 5개여야 함
        require(services.size == 5) {
            "서비스 목록은 정확히 5개여야 합니다: ${services.size}"
        }

        // 모든 서비스 이름이 고유해야 함
        val serviceNames = services.map { it.serviceName }.toSet()
        require(serviceNames.size == 5) {
            "중복된 서비스 이름이 있습니다: ${services.map { it.serviceName }}"
        }

        // 모든 메트릭의 타임스탬프가 동일해야 함
        require(transactionMetrics.timestamp == timestamp) {
            "거래량 메트릭의 타임스탬프가 일치하지 않습니다: ${transactionMetrics.timestamp} != $timestamp"
        }
        require(alertMetrics.timestamp == timestamp) {
            "알림 메트릭의 타임스탬프가 일치하지 않습니다: ${alertMetrics.timestamp} != $timestamp"
        }
    }

    /**
     * 특정 서비스의 Health Check 상태를 조회합니다.
     *
     * @param serviceName 서비스 이름
     * @return 해당 서비스의 Health Check 상태
     * @throws NoSuchElementException 서비스가 존재하지 않을 경우
     */
    fun getServiceHealth(serviceName: String): ServiceHealth {
        return services.find { it.serviceName == serviceName }
            ?: throw NoSuchElementException("서비스를 찾을 수 없습니다: $serviceName")
    }

    /**
     * DOWN 상태인 서비스 목록을 조회합니다.
     *
     * @return DOWN 상태인 서비스 목록
     */
    fun getDownServices(): List<ServiceHealth> {
        return services.filter { it.status == ServiceHealth.ServiceStatus.DOWN }
    }

    /**
     * 모든 서비스가 정상 작동 중인지 확인합니다.
     *
     * @return 모든 서비스가 UP 상태이면 true, 그렇지 않으면 false
     */
    fun isAllServicesUp(): Boolean {
        return services.all { it.status == ServiceHealth.ServiceStatus.UP }
    }

    companion object {
        /**
         * 메모리 크기 추정 (bytes)
         * - Instant timestamp: 8 bytes
         * - List<ServiceHealth>: 5 × 100 bytes = 500 bytes
         * - TransactionMetrics: ~50 bytes
         * - AlertMetrics: ~100 bytes (Map 포함)
         * - 오버헤드: ~100 bytes
         * ----------------------------------------------
         * Total: ~750 bytes per data point
         */
        const val ESTIMATED_SIZE_BYTES = 750L

        /**
         * 1시간 데이터 포인트 개수
         * 5초 간격 × 720개 = 1시간 (3600초)
         */
        const val ONE_HOUR_DATA_POINTS = 720

        /**
         * 1시간 데이터 메모리 사용량 추정 (bytes)
         * 720개 × 750 bytes = 540,000 bytes ≈ 540 KB
         */
        const val ONE_HOUR_MEMORY_BYTES = ONE_HOUR_DATA_POINTS * ESTIMATED_SIZE_BYTES
    }
}
