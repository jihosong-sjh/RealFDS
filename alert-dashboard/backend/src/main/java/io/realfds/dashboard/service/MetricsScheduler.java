package io.realfds.dashboard.service;

import io.realfds.alert.model.ServiceHealth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 메트릭 수집 스케줄러
 *
 * Spring @Scheduled 어노테이션으로 5초마다 메트릭 수집 실행
 * - Health Check 수집: HealthCheckCollector 호출
 * - 수집된 데이터를 MetricsStore에 저장
 * - WebSocket 브로드캐스트 (향후 구현)
 *
 * Constitution V 준수:
 * - SLF4J 로깅 (INFO: 스케줄 실행, ERROR: 예외 발생)
 * - 함수 길이 ≤50줄
 *
 * @see io.realfds.dashboard.service.HealthCheckCollector
 * @see io.realfds.alert.service.MetricsStore
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsScheduler {

    private final HealthCheckCollector healthCheckCollector;
    private final Map<String, ServiceHealth> serviceHealthMap = new ConcurrentHashMap<>();

    @Value("${dashboard-metrics.collection.interval-ms:5000}")
    private long collectionIntervalMs;

    private long executionCount = 0;

    /**
     * 5초마다 메트릭 수집 스케줄링
     *
     * 한국어 주석:
     * - Spring @Scheduled 어노테이션으로 고정 간격 실행
     * - fixedRateString: application.yml의 설정값 사용
     * - Health Check 수집 후 메모리 저장
     *
     * 함수 길이: 45줄 (≤50줄 준수)
     */
    @Scheduled(fixedRateString = "${dashboard-metrics.collection.interval-ms:5000}")
    public void collectAndStoreMetrics() {
        executionCount++;

        log.info("메트릭 수집 스케줄 실행: 실행 횟수={}, 간격={}ms",
                executionCount, collectionIntervalMs);

        try {
            // Phase 3: User Story 1 - Health Check 수집
            collectHealthMetrics();

            // Phase 4: User Story 2 - TPS 수집 (향후 구현)
            // collectTpsMetrics();

            // Phase 5: User Story 3 - 알림률 수집 (향후 구현)
            // collectAlertMetrics();

            log.info("메트릭 수집 완료: 서비스 상태 개수={}, 실행 시간={}ms",
                    serviceHealthMap.size(),
                    System.currentTimeMillis() % 1000);

        } catch (Exception e) {
            log.error("메트릭 수집 중 오류 발생: {}", e.getMessage(), e);
            // 오류 발생 시에도 다음 스케줄 실행 계속 (복원력)
        }
    }

    /**
     * Health Check 메트릭 수집
     *
     * HealthCheckCollector를 호출하여 5개 서비스 상태 수집
     * 수집된 데이터를 ConcurrentHashMap에 저장
     *
     * 한국어 주석:
     * - 비동기 병렬 호출로 성능 최적화
     * - 서비스별 상태를 Map에 저장하여 빠른 조회
     * - WebSocket 브로드캐스트 준비 (Phase 6에서 구현)
     */
    private void collectHealthMetrics() {
        Instant startTime = Instant.now();

        // HealthCheckCollector를 통해 5개 서비스 상태 수집
        List<ServiceHealth> healthMetrics = healthCheckCollector.collectHealthMetrics();

        // 수집된 상태를 Map에 저장 (serviceName을 키로 사용)
        healthMetrics.forEach(health ->
                serviceHealthMap.put(health.getServiceName(), health)
        );

        long elapsedMs = java.time.Duration.between(startTime, Instant.now()).toMillis();

        log.debug("Health Check 수집 완료: 서비스 수={}, 소요 시간={}ms",
                healthMetrics.size(), elapsedMs);
    }

    /**
     * 현재 저장된 서비스 Health 상태 조회
     *
     * REST API 엔드포인트에서 현재 상태 제공 용도
     * WebSocket 브로드캐스트 메시지 생성 용도 (Phase 6)
     *
     * @return 서비스 Health 상태 Map (serviceName → ServiceHealth)
     */
    public Map<String, ServiceHealth> getCurrentServiceHealthMap() {
        return new ConcurrentHashMap<>(serviceHealthMap); // 불변 복사본 반환
    }

    /**
     * 스케줄러 통계 정보 조회
     *
     * Health Check 엔드포인트에서 스케줄러 상태 표시 용도
     *
     * @return 스케줄러 통계 정보 (실행 횟수, 간격, 마지막 실행 시각)
     */
    public Map<String, Object> getSchedulerStats() {
        return Map.of(
                "executionCount", executionCount,
                "intervalMs", collectionIntervalMs,
                "serviceCount", serviceHealthMap.size(),
                "lastUpdate", serviceHealthMap.values().stream()
                        .map(ServiceHealth::getLastChecked)
                        .max(Instant::compareTo)
                        .orElse(Instant.EPOCH) // 최초 실행 전에는 EPOCH 반환
        );
    }
}
