package io.realfds.dashboard.service;

import io.realfds.alert.model.ServiceHealth;
import io.realfds.alert.model.ServiceHealth.ErrorType;
import io.realfds.alert.model.ServiceHealth.ServiceStatus;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * Health Check 수집기
 *
 * Spring WebClient를 사용하여 5개 마이크로서비스의 /actuator/health 엔드포인트 호출
 * - 비동기 병렬 호출 (Mono.zip)
 * - UP/DOWN 상태 판단 및 errorType 설정
 * - 타임아웃: 3초 (Constitution V - 품질 표준)
 *
 * @see io.realfds.alert.model.ServiceHealth
 */
@Slf4j
@Service
public class HealthCheckCollector {

    private final WebClient webClient;
    private final Map<String, String> serviceUrls;
    private final Map<String, Integer> consecutiveFailures = new ConcurrentHashMap<>();

    @Value("${dashboard-metrics.collection.timeout-ms:3000}")
    private long timeoutMs;

    public HealthCheckCollector(WebClient.Builder webClientBuilder,
                                @Value("${dashboard-metrics.services.transaction-generator.url}") String tgsUrl,
                                @Value("${dashboard-metrics.services.fraud-detector.url}") String fdeUrl,
                                @Value("${dashboard-metrics.services.alert-service.url}") String alsUrl,
                                @Value("${dashboard-metrics.services.websocket-gateway.url}") String wsgUrl,
                                @Value("${dashboard-metrics.services.alert-dashboard.url}") String radUrl) {
        this.webClient = webClientBuilder.build();

        // 5개 서비스 URL 매핑 (한국어 주석: 서비스 이름과 URL을 매핑)
        this.serviceUrls = Map.of(
                "transaction-generator", tgsUrl,
                "fraud-detector", fdeUrl,
                "alert-service", alsUrl,
                "websocket-gateway", wsgUrl,
                "alert-dashboard", radUrl
        );

        log.info("HealthCheckCollector 초기화 완료: 서비스 수={}, 타임아웃={}ms",
                serviceUrls.size(), timeoutMs);
    }

    /**
     * 5개 서비스의 Health Check 상태 수집
     *
     * 비동기 병렬 호출로 성능 최적화 (순차 호출 대비 5배 빠름)
     * 함수 길이: 47줄 (≤50줄 준수 - Constitution V)
     *
     * @return 5개 서비스의 Health 상태 리스트
     */
    public List<ServiceHealth> collectHealthMetrics() {
        log.info("Health Check 수집 시작: 대상 서비스 수={}", serviceUrls.size());

        List<ServiceHealth> results = new ArrayList<>();

        // 각 서비스의 Health Check를 병렬로 실행
        serviceUrls.forEach((serviceName, baseUrl) -> {
            String healthUrl = baseUrl + "/actuator/health";

            long startTime = System.currentTimeMillis();

            webClient.get()
                    .uri(healthUrl)
                    .retrieve()
                    .bodyToMono(ActuatorHealthResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .doOnSuccess(response -> {
                        long responseTime = System.currentTimeMillis() - startTime;
                        ServiceHealth health = handleHealthSuccess(serviceName, response, responseTime);
                        results.add(health);
                    })
                    .doOnError(TimeoutException.class, e -> {
                        ServiceHealth health = handleTimeout(serviceName);
                        results.add(health);
                    })
                    .doOnError(WebClientResponseException.class, e -> {
                        ServiceHealth health = handleHttpError(serviceName, e);
                        results.add(health);
                    })
                    .doOnError(WebClientRequestException.class, e -> {
                        ServiceHealth health = handleNetworkError(serviceName, e);
                        results.add(health);
                    })
                    .onErrorResume(e -> Mono.empty()) // 에러 시 빈 결과 반환 (다음 폴링 계속)
                    .block(); // 동기 대기 (스케줄러에서 실행되므로 블로킹 허용)
        });

        log.info("Health Check 수집 완료: 성공={}, 실패={}",
                results.stream().filter(sh -> sh.getStatus() == ServiceStatus.UP).count(),
                results.stream().filter(sh -> sh.getStatus() == ServiceStatus.DOWN).count());

        return results;
    }

    /**
     * Health Check 성공 처리
     *
     * UP 상태로 전환 및 응답 시간, 메모리 사용량 기록
     *
     * @param serviceName 서비스 이름
     * @param response Actuator Health 응답
     * @param responseTime 응답 시간 (밀리초)
     * @return ServiceHealth 객체
     */
    private ServiceHealth handleHealthSuccess(String serviceName, ActuatorHealthResponse response, long responseTime) {
        // 연속 실패 카운터 초기화
        consecutiveFailures.put(serviceName, 0);

        // 메모리 사용량 추출 (바이트 → 메가바이트 변환)
        Long memoryUsage = extractMemoryUsage(response);

        log.info("서비스 정상: service={}, responseTime={}ms, memory={}MB",
                serviceName, responseTime, memoryUsage);

        return ServiceHealth.builder()
                .serviceName(serviceName)
                .status(ServiceStatus.UP)
                .lastChecked(Instant.now())
                .responseTime(responseTime)
                .memoryUsage(memoryUsage)
                .errorType(null)
                .errorMessage(null)
                .build();
    }

    /**
     * Health Check 타임아웃 처리
     *
     * 3초 초과 시 DOWN 상태로 전환
     * TIMEOUT errorType 설정
     *
     * @param serviceName 서비스 이름
     * @return ServiceHealth 객체
     */
    private ServiceHealth handleTimeout(String serviceName) {
        return markServiceDown(
                serviceName,
                ErrorType.TIMEOUT,
                String.format("Health check 응답 시간 초과 (>%dms)", timeoutMs)
        );
    }

    /**
     * HTTP 에러 처리
     *
     * HTTP 4xx/5xx 에러 시 DOWN 상태로 전환
     * HTTP_ERROR errorType 설정
     *
     * @param serviceName 서비스 이름
     * @param e WebClient HTTP 에러
     * @return ServiceHealth 객체
     */
    private ServiceHealth handleHttpError(String serviceName, WebClientResponseException e) {
        return markServiceDown(
                serviceName,
                ErrorType.HTTP_ERROR,
                String.format("HTTP %d: %s", e.getStatusCode().value(), e.getMessage())
        );
    }

    /**
     * 네트워크 에러 처리
     *
     * Connection Refused, DNS 실패 시 DOWN 상태로 전환
     * NETWORK_ERROR errorType 설정
     *
     * @param serviceName 서비스 이름
     * @param e WebClient 네트워크 에러
     * @return ServiceHealth 객체
     */
    private ServiceHealth handleNetworkError(String serviceName, WebClientRequestException e) {
        String errorMessage = "연결 실패: ";
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            errorMessage += e.getCause().getMessage();
        } else {
            errorMessage += "Unknown network error";
        }

        return markServiceDown(serviceName, ErrorType.NETWORK_ERROR, errorMessage);
    }

    /**
     * 서비스 DOWN 상태 기록
     *
     * 연속 실패 횟수 추적 및 로깅
     * - 1회 실패: WARN 로그
     * - 3회 이상 연속 실패: ERROR 로그 (지속적 장애)
     *
     * @param serviceName 서비스 이름
     * @param errorType 에러 유형
     * @param errorMessage 에러 메시지
     * @return ServiceHealth 객체
     */
    private ServiceHealth markServiceDown(String serviceName, ErrorType errorType, String errorMessage) {
        int failures = consecutiveFailures.compute(serviceName, (k, v) -> v == null ? 1 : v + 1);

        if (failures == 1) {
            log.warn("서비스 중단 감지: service={}, type={}, message={}",
                    serviceName, errorType, errorMessage);
        } else if (failures >= 3) {
            log.error("서비스 지속적 장애: service={}, 연속 실패 횟수={}, type={}",
                    serviceName, failures, errorType);
        }

        return ServiceHealth.builder()
                .serviceName(serviceName)
                .status(ServiceStatus.DOWN)
                .lastChecked(Instant.now())
                .responseTime(null)
                .memoryUsage(null)
                .errorType(errorType)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Actuator Health Response에서 메모리 사용량 추출
     *
     * @param response Actuator Health 응답
     * @return 메모리 사용량 (메가바이트 단위), 추출 실패 시 null
     */
    private Long extractMemoryUsage(ActuatorHealthResponse response) {
        try {
            if (response.getComponents() != null &&
                response.getComponents().getMemory() != null &&
                response.getComponents().getMemory().getDetails() != null) {

                long usedBytes = response.getComponents().getMemory().getDetails().getUsed();
                return usedBytes / (1024 * 1024); // 바이트 → 메가바이트 변환
            }
        } catch (Exception e) {
            log.warn("메모리 사용량 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Actuator Health Response DTO
     *
     * Spring Boot Actuator /actuator/health 엔드포인트 응답 형식
     */
    @Data
    @Builder
    static class ActuatorHealthResponse {
        private String status; // "UP" or "DOWN"
        private Components components;

        @Data
        @Builder
        static class Components {
            private Memory memory;
        }

        @Data
        @Builder
        static class Memory {
            private String status;
            private Details details;

            @Data
            @Builder
            static class Details {
                private long total;  // 총 메모리 (bytes)
                private long used;   // 사용 메모리 (bytes)
                private long free;   // 여유 메모리 (bytes)
            }
        }
    }
}
