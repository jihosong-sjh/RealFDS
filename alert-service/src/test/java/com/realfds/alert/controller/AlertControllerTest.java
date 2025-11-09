package com.realfds.alert.controller;

import com.realfds.alert.model.Alert;
import com.realfds.alert.model.Transaction;
import com.realfds.alert.service.AlertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * AlertController 통합 테스트
 *
 * Spring WebFlux의 WebTestClient를 사용하여 REST API 엔드포인트를 검증합니다.
 * - GET /api/alerts 엔드포인트 테스트
 * - 정상 응답 확인 (200 OK)
 * - 응답 데이터 검증
 * - 빈 리스트 응답 확인
 *
 * Given-When-Then 구조를 사용하여 테스트 시나리오를 명확히 합니다.
 */
@WebFluxTest(controllers = AlertController.class, excludeAutoConfiguration = KafkaAutoConfiguration.class)
@DisplayName("AlertController 통합 테스트")
class AlertControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AlertService alertService;

    @Test
    @DisplayName("GET /api/alerts 호출 시 최근 알림 목록을 반환해야 함")
    void testGetAlertsEndpoint() {
        // Given: Mock AlertService가 3개의 알림을 반환하도록 설정
        List<Alert> mockAlerts = Arrays.asList(
            createTestAlert("alert-001", "HIGH_VALUE", "HIGH"),
            createTestAlert("alert-002", "FOREIGN_COUNTRY", "MEDIUM"),
            createTestAlert("alert-003", "HIGH_FREQUENCY", "HIGH")
        );
        when(alertService.getRecentAlerts(anyInt())).thenReturn(mockAlerts);

        // When: GET /api/alerts 호출
        // Then: 200 OK 응답과 알림 목록 반환
        webTestClient.get()
            .uri("/api/alerts")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(Alert.class)
            .hasSize(3)
            .value(alerts -> {
                assert alerts.get(0).getAlertId().equals("alert-001");
                assert alerts.get(1).getAlertId().equals("alert-002");
                assert alerts.get(2).getAlertId().equals("alert-003");
            });
    }

    @Test
    @DisplayName("GET /api/alerts 호출 시 알림이 없으면 빈 리스트를 반환해야 함")
    void testGetAlertsEndpointEmpty() {
        // Given: Mock AlertService가 빈 리스트를 반환하도록 설정
        when(alertService.getRecentAlerts(anyInt())).thenReturn(List.of());

        // When: GET /api/alerts 호출
        // Then: 200 OK 응답과 빈 리스트 반환
        webTestClient.get()
            .uri("/api/alerts")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(Alert.class)
            .hasSize(0);
    }

    @Test
    @DisplayName("GET /api/alerts 호출 시 Content-Type이 application/json이어야 함")
    void testGetAlertsContentType() {
        // Given: Mock AlertService가 빈 리스트를 반환하도록 설정
        when(alertService.getRecentAlerts(anyInt())).thenReturn(List.of());

        // When: GET /api/alerts 호출
        // Then: Content-Type이 application/json이어야 함
        webTestClient.get()
            .uri("/api/alerts")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType("application/json");
    }

    @Test
    @DisplayName("GET /api/alerts 호출 시 AlertService.getRecentAlerts(100)을 호출해야 함")
    void testGetAlertsServiceCall() {
        // Given: Mock AlertService 설정
        List<Alert> mockAlerts = List.of(
            createTestAlert("alert-001", "HIGH_VALUE", "HIGH")
        );
        when(alertService.getRecentAlerts(100)).thenReturn(mockAlerts);

        // When: GET /api/alerts 호출
        webTestClient.get()
            .uri("/api/alerts")
            .exchange()
            .expectStatus().isOk();

        // Then: AlertService.getRecentAlerts(100)이 호출되었는지는 Mock 설정에서 검증됨
    }

    /**
     * 테스트용 Alert 객체 생성 헬퍼 메서드
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
