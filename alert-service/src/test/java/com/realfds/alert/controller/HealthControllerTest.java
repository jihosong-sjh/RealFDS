package com.realfds.alert.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * HealthController 단위 테스트
 *
 * HealthController의 health check 엔드포인트를 검증합니다.
 *
 * 테스트 케이스:
 * - test_health_endpoint_returns_up_status: GET /actuator/health 호출 시 {"status": "UP"} 응답 확인
 *
 * Given-When-Then 구조를 사용하여 테스트 시나리오를 명확히 합니다.
 */
class HealthControllerTest {

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        // WebTestClient를 HealthController와 함께 바인딩
        this.webTestClient = WebTestClient
                .bindToController(new HealthController())
                .build();
    }

    /**
     * Health endpoint 정상 응답 테스트
     *
     * Given: HealthController가 등록되어 있음
     * When: GET /actuator/health를 호출함
     * Then: HTTP 200 OK 응답과 {"status": "UP"} JSON을 반환함
     */
    @Test
    void test_health_endpoint_returns_up_status() {
        // When: GET /actuator/health 호출
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                // Then: 200 OK 응답
                .expectStatus().isOk()
                // Then: Content-Type이 application/json
                .expectHeader().contentType("application/json")
                // Then: response body에 status 필드가 "UP"
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}
