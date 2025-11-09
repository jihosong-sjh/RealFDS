package com.realfds.alert.repository;

import com.realfds.alert.model.Alert;
import com.realfds.alert.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AlertRepository 단위 테스트
 *
 * Given-When-Then 구조를 사용하여 인메모리 저장소의 동작을 검증합니다.
 * - 알림 추가 기능
 * - 최대 100개 제한 기능
 * - 최근 알림 조회 기능
 */
@DisplayName("AlertRepository 단위 테스트")
class AlertRepositoryTest {

    private AlertRepository alertRepository;

    @BeforeEach
    void setUp() {
        // Given: 각 테스트 전에 새로운 AlertRepository 인스턴스 생성
        alertRepository = new AlertRepository();
    }

    @Test
    @DisplayName("알림 추가 시 저장소에 정상적으로 추가되어야 함")
    void testAddAlert() {
        // Given: 테스트용 알림 생성
        Alert alert = createTestAlert("alert-001", "HIGH_VALUE", "HIGH");

        // When: 알림 추가
        alertRepository.addAlert(alert);

        // Then: 알림이 저장소에 추가되었는지 확인
        List<Alert> recentAlerts = alertRepository.getRecentAlerts(10);
        assertThat(recentAlerts).hasSize(1);
        assertThat(recentAlerts.get(0).getAlertId()).isEqualTo("alert-001");
    }

    @Test
    @DisplayName("최근 알림이 맨 앞에 위치해야 함")
    void testRecentAlertFirst() {
        // Given: 여러 개의 알림 생성
        Alert alert1 = createTestAlert("alert-001", "HIGH_VALUE", "HIGH");
        Alert alert2 = createTestAlert("alert-002", "FOREIGN_COUNTRY", "MEDIUM");
        Alert alert3 = createTestAlert("alert-003", "HIGH_FREQUENCY", "HIGH");

        // When: 순서대로 알림 추가
        alertRepository.addAlert(alert1);
        alertRepository.addAlert(alert2);
        alertRepository.addAlert(alert3);

        // Then: 가장 최근 알림(alert3)이 맨 앞에 위치
        List<Alert> recentAlerts = alertRepository.getRecentAlerts(10);
        assertThat(recentAlerts).hasSize(3);
        assertThat(recentAlerts.get(0).getAlertId()).isEqualTo("alert-003");
        assertThat(recentAlerts.get(1).getAlertId()).isEqualTo("alert-002");
        assertThat(recentAlerts.get(2).getAlertId()).isEqualTo("alert-001");
    }

    @Test
    @DisplayName("101개 추가 시 가장 오래된 알림이 제거되어야 함")
    void testMaxCapacity100() {
        // Given: 101개의 알림 생성 및 추가
        for (int i = 1; i <= 101; i++) {
            Alert alert = createTestAlert("alert-" + String.format("%03d", i), "HIGH_VALUE", "HIGH");
            alertRepository.addAlert(alert);
        }

        // When: 저장소에서 모든 알림 조회
        List<Alert> recentAlerts = alertRepository.getRecentAlerts(200);

        // Then: 최대 100개만 유지되고, 가장 오래된 알림(alert-001)은 제거됨
        assertThat(recentAlerts).hasSize(100);
        assertThat(recentAlerts.get(0).getAlertId()).isEqualTo("alert-101"); // 가장 최근
        assertThat(recentAlerts.get(99).getAlertId()).isEqualTo("alert-002"); // 가장 오래된 것

        // alert-001은 제거되었어야 함
        boolean hasAlert001 = recentAlerts.stream()
                .anyMatch(alert -> "alert-001".equals(alert.getAlertId()));
        assertThat(hasAlert001).isFalse();
    }

    @Test
    @DisplayName("getRecentAlerts로 제한된 개수만큼 조회되어야 함")
    void testGetRecentAlertsLimit() {
        // Given: 10개의 알림 추가
        for (int i = 1; i <= 10; i++) {
            Alert alert = createTestAlert("alert-" + String.format("%03d", i), "HIGH_VALUE", "HIGH");
            alertRepository.addAlert(alert);
        }

        // When: 최근 5개만 조회
        List<Alert> recentAlerts = alertRepository.getRecentAlerts(5);

        // Then: 5개만 반환되어야 함
        assertThat(recentAlerts).hasSize(5);
        assertThat(recentAlerts.get(0).getAlertId()).isEqualTo("alert-010"); // 가장 최근
        assertThat(recentAlerts.get(4).getAlertId()).isEqualTo("alert-006");
    }

    @Test
    @DisplayName("저장소가 비어있을 때 빈 리스트를 반환해야 함")
    void testGetRecentAlertsEmpty() {
        // Given: 비어있는 저장소

        // When: 최근 알림 조회
        List<Alert> recentAlerts = alertRepository.getRecentAlerts(10);

        // Then: 빈 리스트 반환
        assertThat(recentAlerts).isEmpty();
    }

    @Test
    @DisplayName("limit이 저장된 알림 개수보다 클 때 모든 알림을 반환해야 함")
    void testGetRecentAlertsLimitExceedsSize() {
        // Given: 3개의 알림 추가
        for (int i = 1; i <= 3; i++) {
            Alert alert = createTestAlert("alert-" + String.format("%03d", i), "HIGH_VALUE", "HIGH");
            alertRepository.addAlert(alert);
        }

        // When: 10개를 요청 (저장된 개수보다 많음)
        List<Alert> recentAlerts = alertRepository.getRecentAlerts(10);

        // Then: 저장된 3개 모두 반환
        assertThat(recentAlerts).hasSize(3);
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
