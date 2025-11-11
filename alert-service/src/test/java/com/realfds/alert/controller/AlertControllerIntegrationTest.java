package com.realfds.alert.controller;

import com.realfds.alert.model.Alert;
import com.realfds.alert.model.AlertStatus;
import com.realfds.alert.model.Transaction;
import com.realfds.alert.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;

/**
 * T015: REST API 엔드포인트 통합 테스트 (User Story 1)
 *
 * Spring Boot 통합 테스트를 사용하여 REST API 엔드포인트와 실제 컴포넌트 간 통합을 검증합니다.
 * - PATCH /api/alerts/{id}/status: 알림 상태 변경
 * - 200 OK 응답 확인
 * - 상태 변경 확인
 * - processedAt 자동 설정 확인
 *
 * Given-When-Then 구조를 사용하여 API 계약을 명확히 검증합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@DisplayName("T015: REST API 엔드포인트 통합 테스트")
class AlertControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AlertRepository alertRepository;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 repository 초기화 (인메모리 저장소)
        // AlertRepository에 clear 메서드가 있다면 사용
    }

    @Test
    @DisplayName("[T015] Given: 테스트 알림 생성, When: PATCH /api/alerts/{id}/status 호출 (IN_PROGRESS), Then: 200 OK 및 상태 변경 확인")
    void testUpdateAlertStatusToInProgress() {
        // Given: 테스트 알림 생성 및 저장
        Alert testAlert = createTestAlert("test-alert-001", "HIGH_VALUE", "HIGH");
        alertRepository.addAlert(testAlert);

        // When: PATCH /api/alerts/{id}/status 호출 (IN_PROGRESS)
        String requestBody = """
            {
                "status": "IN_PROGRESS"
            }
            """;

        webTestClient.patch()
            .uri("/api/alerts/{alertId}/status", testAlert.getAlertId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            // Then: 200 OK 응답
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            // Then: 응답 본문 검증
            .jsonPath("$.alertId").isEqualTo(testAlert.getAlertId())
            .jsonPath("$.status").isEqualTo("IN_PROGRESS")
            .jsonPath("$.processedAt").doesNotExist(); // IN_PROGRESS는 processedAt null

        // Then: repository에서 상태 변경 확인
        Alert updatedAlert = alertRepository.getRecentAlerts(100).stream()
            .filter(a -> a.getAlertId().equals(testAlert.getAlertId()))
            .findFirst()
            .orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals(AlertStatus.IN_PROGRESS, updatedAlert.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(updatedAlert.getProcessedAt());
    }

    @Test
    @DisplayName("[T015] Given: 테스트 알림 생성, When: PATCH /api/alerts/{id}/status 호출 (COMPLETED), Then: 200 OK 및 processedAt 자동 설정 확인")
    void testUpdateAlertStatusToCompleted() {
        // Given: 테스트 알림 생성 및 저장
        Alert testAlert = createTestAlert("test-alert-002", "FOREIGN_COUNTRY", "MEDIUM");
        alertRepository.addAlert(testAlert);

        // When: PATCH /api/alerts/{id}/status 호출 (COMPLETED)
        String requestBody = """
            {
                "status": "COMPLETED"
            }
            """;

        webTestClient.patch()
            .uri("/api/alerts/{alertId}/status", testAlert.getAlertId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            // Then: 200 OK 응답
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            // Then: 응답 본문 검증
            .jsonPath("$.alertId").isEqualTo(testAlert.getAlertId())
            .jsonPath("$.status").isEqualTo("COMPLETED")
            .jsonPath("$.processedAt").exists(); // COMPLETED는 processedAt 자동 설정

        // Then: repository에서 상태 및 processedAt 확인
        Alert updatedAlert = alertRepository.getRecentAlerts(100).stream()
            .filter(a -> a.getAlertId().equals(testAlert.getAlertId()))
            .findFirst()
            .orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals(AlertStatus.COMPLETED, updatedAlert.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(updatedAlert.getProcessedAt());
    }

    @Test
    @DisplayName("[T015] Given: 존재하지 않는 알림, When: PATCH /api/alerts/{id}/status 호출, Then: 404 Not Found")
    void testUpdateAlertStatusNotFound() {
        // Given: 존재하지 않는 알림 ID
        String nonExistentAlertId = "non-existent-alert-id";

        // When: PATCH /api/alerts/{id}/status 호출
        String requestBody = """
            {
                "status": "IN_PROGRESS"
            }
            """;

        webTestClient.patch()
            .uri("/api/alerts/{alertId}/status", nonExistentAlertId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            // Then: 404 Not Found 응답
            .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("[T015] Given: 테스트 알림, When: 유효하지 않은 상태로 PATCH 호출, Then: 400 Bad Request")
    void testUpdateAlertStatusInvalidStatus() {
        // Given: 테스트 알림 생성 및 저장
        Alert testAlert = createTestAlert("test-alert-003", "HIGH_FREQUENCY", "HIGH");
        alertRepository.addAlert(testAlert);

        // When: 유효하지 않은 상태로 PATCH 호출
        String requestBody = """
            {
                "status": "INVALID_STATUS"
            }
            """;

        webTestClient.patch()
            .uri("/api/alerts/{alertId}/status", testAlert.getAlertId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            // Then: 400 Bad Request 응답
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("[T015] Given: 테스트 알림, When: status 필드 누락된 요청, Then: 400 Bad Request")
    void testUpdateAlertStatusMissingField() {
        // Given: 테스트 알림 생성 및 저장
        Alert testAlert = createTestAlert("test-alert-004", "HIGH_VALUE", "HIGH");
        alertRepository.addAlert(testAlert);

        // When: status 필드가 없는 요청
        String requestBody = """
            {
            }
            """;

        webTestClient.patch()
            .uri("/api/alerts/{alertId}/status", testAlert.getAlertId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            // Then: 400 Bad Request 응답
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("[T015] Given: UNREAD 알림, When: UNREAD → IN_PROGRESS → COMPLETED 순차 변경, Then: 각 단계별 상태 및 processedAt 확인")
    void testUpdateAlertStatusSequential() {
        // Given: UNREAD 상태의 테스트 알림 생성
        Alert testAlert = createTestAlert("test-alert-005", "HIGH_VALUE", "HIGH");
        alertRepository.addAlert(testAlert);

        // When: Step 1 - UNREAD → IN_PROGRESS
        String requestBody1 = """
            {
                "status": "IN_PROGRESS"
            }
            """;

        webTestClient.patch()
            .uri("/api/alerts/{alertId}/status", testAlert.getAlertId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody1)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("IN_PROGRESS")
            .jsonPath("$.processedAt").doesNotExist();

        // When: Step 2 - IN_PROGRESS → COMPLETED
        String requestBody2 = """
            {
                "status": "COMPLETED"
            }
            """;

        webTestClient.patch()
            .uri("/api/alerts/{alertId}/status", testAlert.getAlertId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody2)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("COMPLETED")
            .jsonPath("$.processedAt").exists();

        // Then: 최종 상태 확인
        Alert finalAlert = alertRepository.getRecentAlerts(100).stream()
            .filter(a -> a.getAlertId().equals(testAlert.getAlertId()))
            .findFirst()
            .orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals(AlertStatus.COMPLETED, finalAlert.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(finalAlert.getProcessedAt());
    }

    /**
     * 테스트용 Alert 객체 생성 헬퍼 메서드
     *
     * @param alertId 알림 ID
     * @param ruleName 규칙 이름
     * @param severity 심각도
     * @return 테스트용 Alert 객체 (초기 상태: UNREAD)
     */
    private Alert createTestAlert(String alertId, String ruleName, String severity) {
        Transaction transaction = new Transaction(
            "1.0",
            "txn-" + alertId,
            "user-001",
            1500000L,
            "KRW",
            "KR",
            Instant.now().toString()
        );

        return new Alert(
            "1.0",
            alertId,
            transaction,
            "SIMPLE_RULE",
            ruleName,
            "테스트 알림: " + ruleName,
            severity,
            Instant.now().toString()
        );
    }
}
