package io.realfds.dashboard.service;

import io.realfds.alert.model.ServiceHealth;
import io.realfds.alert.model.ServiceHealth.ServiceStatus;
import io.realfds.alert.model.ServiceHealth.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * HealthCheckCollector 단위 테스트
 *
 * Given-When-Then 구조를 사용하여 헬스 체크 수집 로직 검증
 *
 * @see io.realfds.dashboard.service.HealthCheckCollector
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthCheckCollector 단위 테스트")
class HealthCheckCollectorTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private HealthCheckCollector healthCheckCollector;

    @BeforeEach
    void setUp() {
        // WebClient 체인 모킹 설정
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("Given 모든 서비스 정상 작동, When Health Check 수집, Then 5개 서비스 모두 UP 상태")
    void testAllServicesUp() {
        // Given: 모든 서비스가 정상 응답
        ActuatorHealthResponse healthyResponse = ActuatorHealthResponse.builder()
                .status("UP")
                .build();

        when(responseSpec.bodyToMono(ActuatorHealthResponse.class))
                .thenReturn(Mono.just(healthyResponse));

        // When: Health Check 수집 실행
        List<ServiceHealth> results = healthCheckCollector.collectHealthMetrics();

        // Then: 5개 서비스 모두 UP 상태
        assertThat(results).hasSize(5);
        assertThat(results).allMatch(sh -> sh.getStatus() == ServiceStatus.UP);
        assertThat(results).allMatch(sh -> sh.getErrorType() == null);
        assertThat(results).allMatch(sh -> sh.getErrorMessage() == null);

        // Then: responseTime과 memoryUsage가 설정됨
        assertThat(results).allMatch(sh -> sh.getResponseTime() != null);
        assertThat(results).allMatch(sh -> sh.getResponseTime() >= 0 && sh.getResponseTime() <= 5000);
    }

    @Test
    @DisplayName("Given 한 서비스 중단, When Health Check 수집, Then 해당 서비스 DOWN, errorType 설정")
    void testOneServiceDown() {
        // Given: fraud-detector 서비스가 HTTP 에러 반환
        when(responseSpec.bodyToMono(ActuatorHealthResponse.class))
                .thenReturn(
                    Mono.just(ActuatorHealthResponse.builder().status("UP").build()), // transaction-generator
                    Mono.error(WebClientResponseException.create(500, "Internal Server Error", null, null, null)), // fraud-detector (DOWN)
                    Mono.just(ActuatorHealthResponse.builder().status("UP").build()), // alert-service
                    Mono.just(ActuatorHealthResponse.builder().status("UP").build()), // websocket-gateway
                    Mono.just(ActuatorHealthResponse.builder().status("UP").build())  // frontend-dashboard
                );

        // When: Health Check 수집 실행
        List<ServiceHealth> results = healthCheckCollector.collectHealthMetrics();

        // Then: 1개 서비스가 DOWN 상태
        long downCount = results.stream()
                .filter(sh -> sh.getStatus() == ServiceStatus.DOWN)
                .count();
        assertThat(downCount).isEqualTo(1);

        // Then: DOWN 서비스에 errorType과 errorMessage 설정됨
        ServiceHealth downService = results.stream()
                .filter(sh -> sh.getStatus() == ServiceStatus.DOWN)
                .findFirst()
                .orElseThrow();

        assertThat(downService.getErrorType()).isEqualTo(ErrorType.HTTP_ERROR);
        assertThat(downService.getErrorMessage()).contains("500");
        assertThat(downService.getResponseTime()).isNull();
        assertThat(downService.getMemoryUsage()).isNull();
    }

    @Test
    @DisplayName("Given 한 서비스 타임아웃, When Health Check 수집, Then 3초 이내 타임아웃 처리")
    void testServiceTimeout() {
        // Given: alert-service가 3초 이상 응답 지연
        when(responseSpec.bodyToMono(ActuatorHealthResponse.class))
                .thenReturn(
                    Mono.just(ActuatorHealthResponse.builder().status("UP").build()),
                    Mono.just(ActuatorHealthResponse.builder().status("UP").build()),
                    Mono.error(new TimeoutException("Health check 응답 시간 초과")), // alert-service (TIMEOUT)
                    Mono.just(ActuatorHealthResponse.builder().status("UP").build()),
                    Mono.just(ActuatorHealthResponse.builder().status("UP").build())
                );

        // When: Health Check 수집 실행
        long startTime = System.currentTimeMillis();
        List<ServiceHealth> results = healthCheckCollector.collectHealthMetrics();
        long elapsedTime = System.currentTimeMillis() - startTime;

        // Then: 3초 이내에 타임아웃 처리됨
        assertThat(elapsedTime).isLessThan(3500); // 3.5초 이내 (여유 포함)

        // Then: 타임아웃 서비스가 DOWN 상태로 전환
        ServiceHealth timeoutService = results.stream()
                .filter(sh -> sh.getErrorType() == ErrorType.TIMEOUT)
                .findFirst()
                .orElseThrow();

        assertThat(timeoutService.getStatus()).isEqualTo(ServiceStatus.DOWN);
        assertThat(timeoutService.getErrorMessage()).contains("응답 시간 초과");
    }

    @Test
    @DisplayName("Given 네트워크 연결 실패, When Health Check 수집, Then NETWORK_ERROR errorType 설정")
    void testNetworkError() {
        // Given: websocket-gateway가 연결 거부
        when(responseSpec.bodyToMono(ActuatorHealthResponse.class))
                .thenReturn(
                    Mono.just(ActuatorHealthResponse.builder().status("UP").build()),
                    Mono.just(ActuatorHealthResponse.builder().status("UP").build()),
                    Mono.just(ActuatorHealthResponse.builder().status("UP").build()),
                    Mono.error(new WebClientRequestException(new RuntimeException("Connection refused"), null, null, null)), // NETWORK_ERROR
                    Mono.just(ActuatorHealthResponse.builder().status("UP").build())
                );

        // When: Health Check 수집 실행
        List<ServiceHealth> results = healthCheckCollector.collectHealthMetrics();

        // Then: 네트워크 에러로 DOWN 상태 전환
        ServiceHealth networkErrorService = results.stream()
                .filter(sh -> sh.getErrorType() == ErrorType.NETWORK_ERROR)
                .findFirst()
                .orElseThrow();

        assertThat(networkErrorService.getStatus()).isEqualTo(ServiceStatus.DOWN);
        assertThat(networkErrorService.getErrorMessage()).contains("Connection refused");
    }

    @Test
    @DisplayName("Given Health Check 성공, When 응답 시간 측정, Then responseTime이 0-5000ms 범위")
    void testResponseTimeMeasurement() {
        // Given: 정상 응답
        when(responseSpec.bodyToMono(ActuatorHealthResponse.class))
                .thenReturn(Mono.just(ActuatorHealthResponse.builder().status("UP").build())
                        .delayElement(Duration.ofMillis(150))); // 150ms 지연

        // When: Health Check 수집 실행
        List<ServiceHealth> results = healthCheckCollector.collectHealthMetrics();

        // Then: 응답 시간이 합리적인 범위
        assertThat(results).allMatch(sh -> {
            if (sh.getStatus() == ServiceStatus.UP) {
                return sh.getResponseTime() != null &&
                       sh.getResponseTime() >= 0 &&
                       sh.getResponseTime() <= 5000;
            }
            return true;
        });
    }

    @Test
    @DisplayName("Given lastChecked 필드, When Health Check 수집, Then 현재 시각으로부터 10초 이내")
    void testLastCheckedTimestamp() {
        // Given: 정상 응답
        when(responseSpec.bodyToMono(ActuatorHealthResponse.class))
                .thenReturn(Mono.just(ActuatorHealthResponse.builder().status("UP").build()));

        // When: Health Check 수집 실행
        Instant beforeCheck = Instant.now();
        List<ServiceHealth> results = healthCheckCollector.collectHealthMetrics();
        Instant afterCheck = Instant.now();

        // Then: lastChecked가 현재 시각 범위 내
        assertThat(results).allMatch(sh -> {
            Instant lastChecked = sh.getLastChecked();
            return !lastChecked.isBefore(beforeCheck) && !lastChecked.isAfter(afterCheck);
        });
    }

    /**
     * Actuator Health Response DTO (테스트용)
     */
    @lombok.Data
    @lombok.Builder
    static class ActuatorHealthResponse {
        private String status;
        private Components components;

        @lombok.Data
        @lombok.Builder
        static class Components {
            private Memory memory;
        }

        @lombok.Data
        @lombok.Builder
        static class Memory {
            private Details details;
        }

        @lombok.Data
        @lombok.Builder
        static class Details {
            private long used;
        }
    }
}
