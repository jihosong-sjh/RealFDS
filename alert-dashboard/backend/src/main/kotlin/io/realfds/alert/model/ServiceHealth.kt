package io.realfds.alert.model

import java.time.Instant

/**
 * 마이크로서비스의 Health Check 상태를 나타내는 엔티티
 *
 * 5개 마이크로서비스(transaction-generator, fraud-detector, alert-service,
 * websocket-gateway, frontend-dashboard)의 실시간 Health Check 결과를 저장합니다.
 *
 * 이 클래스는 메모리에만 저장되며 데이터베이스 영속화를 하지 않습니다.
 *
 * @property serviceName 서비스 식별자 (5개 중 하나)
 * @property status 현재 서비스 상태 (UP 또는 DOWN)
 * @property lastChecked 마지막 Health Check 수행 시각
 * @property responseTime Health Check 응답 시간 (밀리초 단위). DOWN 상태 시 null
 * @property memoryUsage JVM 메모리 사용량 (메가바이트 단위). DOWN 상태 시 null
 * @property errorType DOWN 상태 시 오류 유형. UP 상태 시 null
 * @property errorMessage DOWN 상태 시 오류 상세 메시지. UP 상태 시 null
 *
 * @see io.realfds.alert.service.HealthCheckCollector
 */
data class ServiceHealth(
    /**
     * 서비스 식별자 (5개 중 하나)
     * - transaction-generator: 거래 생성 서비스
     * - fraud-detector: 사기 탐지 서비스
     * - alert-service: 알림 서비스
     * - websocket-gateway: WebSocket 게이트웨이
     * - frontend-dashboard: 프론트엔드 대시보드
     */
    val serviceName: String,

    /**
     * 현재 서비스 상태
     * UP: 정상 작동 중
     * DOWN: 중단 또는 오류 상태
     */
    val status: ServiceStatus,

    /**
     * 마지막 Health Check 수행 시각
     * ISO-8601 형식의 타임스탬프입니다.
     */
    val lastChecked: Instant,

    /**
     * Health Check 응답 시간 (밀리초 단위)
     * - 정상 응답: 0 ~ 5000ms
     * - DOWN 상태일 경우 null
     * - 타임아웃(3초 초과)일 경우 null
     */
    val responseTime: Long? = null,

    /**
     * JVM 메모리 사용량 (메가바이트 단위)
     * - 정상 범위: 0 ~ 8192 MB
     * - DOWN 상태일 경우 null
     * - Spring Boot Actuator의 /actuator/health 응답에서 추출
     */
    val memoryUsage: Long? = null,

    /**
     * DOWN 상태 시 오류 유형
     * - TIMEOUT: Health Check 응답 시간 초과 (>3초)
     * - HTTP_ERROR: HTTP 4xx/5xx 에러
     * - NETWORK_ERROR: 네트워크 연결 실패 (Connection Refused, DNS 실패)
     * - UP 상태일 경우 null
     */
    val errorType: ErrorType? = null,

    /**
     * DOWN 상태 시 오류 상세 메시지
     * - 최대 256자
     * - UP 상태일 경우 null
     * - 예: "Health check 응답 시간 초과 (>3초)", "HTTP 503: Service Unavailable"
     */
    val errorMessage: String? = null
) {
    init {
        // 서비스명은 5개 중 하나여야 함
        require(
            serviceName in setOf(
                "transaction-generator",
                "fraud-detector",
                "alert-service",
                "websocket-gateway",
                "frontend-dashboard"
            )
        ) { "유효하지 않은 서비스 이름입니다: $serviceName" }

        // lastChecked는 현재 시각으로부터 10초 이내 (5초 폴링 + 5초 여유)
        val tenSecondsAgo = Instant.now().minusSeconds(10)
        require(lastChecked.isAfter(tenSecondsAgo)) {
            "Health Check 시각이 너무 오래되었습니다: $lastChecked"
        }

        // UP 상태일 때 responseTime과 memoryUsage는 non-null
        if (status == ServiceStatus.UP) {
            requireNotNull(responseTime) { "UP 상태일 때 응답 시간이 필요합니다" }
            requireNotNull(memoryUsage) { "UP 상태일 때 메모리 사용량이 필요합니다" }

            // 응답 시간은 5000ms 이하
            require(responseTime in 0..5000) {
                "응답 시간이 유효하지 않습니다: ${responseTime}ms (0 ~ 5000ms)"
            }

            // 메모리 사용량은 8192MB 이하
            require(memoryUsage in 0..8192) {
                "메모리 사용량이 유효하지 않습니다: ${memoryUsage}MB (0 ~ 8192MB)"
            }
        }

        // DOWN 상태일 때 errorType과 errorMessage는 non-null
        if (status == ServiceStatus.DOWN) {
            requireNotNull(errorType) { "DOWN 상태일 때 오류 유형이 필요합니다" }
            requireNotNull(errorMessage) { "DOWN 상태일 때 오류 메시지가 필요합니다" }

            // 오류 메시지는 최대 256자
            require(errorMessage.length <= 256) {
                "오류 메시지는 최대 256자까지 입력 가능합니다"
            }
        }
    }

    /**
     * 서비스 상태 Enum
     * UP: 정상 작동 중
     * DOWN: 중단 또는 오류 상태
     */
    enum class ServiceStatus {
        UP, DOWN
    }

    /**
     * 오류 유형 Enum
     * TIMEOUT: Health Check 응답 시간 초과 (>3초)
     * HTTP_ERROR: HTTP 4xx/5xx 에러
     * NETWORK_ERROR: 네트워크 연결 실패
     */
    enum class ErrorType {
        TIMEOUT,        // Health Check 응답 시간 초과 (>3초)
        HTTP_ERROR,     // HTTP 4xx/5xx 에러
        NETWORK_ERROR   // 네트워크 연결 실패 (Connection Refused, DNS 실패)
    }

    companion object {
        /**
         * Java 호환을 위한 Builder 패턴 지원
         */
        @JvmStatic
        fun builder(): ServiceHealthBuilder = ServiceHealthBuilder()
    }

    /**
     * Java 호환 Builder 클래스
     */
    class ServiceHealthBuilder {
        private var serviceName: String? = null
        private var status: ServiceStatus? = null
        private var lastChecked: Instant? = null
        private var responseTime: Long? = null
        private var memoryUsage: Long? = null
        private var errorType: ErrorType? = null
        private var errorMessage: String? = null

        fun serviceName(serviceName: String) = apply { this.serviceName = serviceName }
        fun status(status: ServiceStatus) = apply { this.status = status }
        fun lastChecked(lastChecked: Instant) = apply { this.lastChecked = lastChecked }
        fun responseTime(responseTime: Long?) = apply { this.responseTime = responseTime }
        fun memoryUsage(memoryUsage: Long?) = apply { this.memoryUsage = memoryUsage }
        fun errorType(errorType: ErrorType?) = apply { this.errorType = errorType }
        fun errorMessage(errorMessage: String?) = apply { this.errorMessage = errorMessage }

        fun build(): ServiceHealth {
            return ServiceHealth(
                serviceName = requireNotNull(serviceName) { "serviceName is required" },
                status = requireNotNull(status) { "status is required" },
                lastChecked = requireNotNull(lastChecked) { "lastChecked is required" },
                responseTime = responseTime,
                memoryUsage = memoryUsage,
                errorType = errorType,
                errorMessage = errorMessage
            )
        }
    }
}
