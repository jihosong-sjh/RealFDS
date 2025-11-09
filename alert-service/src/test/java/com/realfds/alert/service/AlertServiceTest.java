package com.realfds.alert.service;

import com.realfds.alert.model.Alert;
import com.realfds.alert.model.Transaction;
import com.realfds.alert.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * AlertService 단위 테스트
 *
 * Given-When-Then 구조를 사용하여 AlertService의 비즈니스 로직을 검증합니다.
 * - 알림 처리 기능 (processAlert)
 * - 최근 알림 조회 기능 (getRecentAlerts)
 * - AlertRepository와의 연동 확인
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertService 단위 테스트")
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private AlertService alertService;

    @BeforeEach
    void setUp() {
        // Mock 초기화는 @ExtendWith(MockitoExtension.class)로 자동 처리
    }

    @Test
    @DisplayName("알림 처리 시 repository에 알림이 저장되어야 함")
    void testProcessAlert() {
        // Given: 테스트용 알림 생성
        Alert alert = createTestAlert("alert-001", "HIGH_VALUE", "HIGH");

        // When: 알림 처리
        alertService.processAlert(alert);

        // Then: repository의 addAlert가 호출되었는지 확인
        verify(alertRepository, times(1)).addAlert(alert);
    }

    @Test
    @DisplayName("null 알림 처리 시 repository에 저장되지 않아야 함")
    void testProcessAlertWithNull() {
        // Given: null 알림

        // When: null 알림 처리
        alertService.processAlert(null);

        // Then: repository의 addAlert가 호출되지 않아야 함
        verify(alertRepository, never()).addAlert(any());
    }

    @Test
    @DisplayName("최근 알림 조회 시 repository에서 알림을 가져와야 함")
    void testGetRecentAlerts() {
        // Given: Mock repository가 3개의 알림을 반환하도록 설정
        List<Alert> mockAlerts = Arrays.asList(
            createTestAlert("alert-001", "HIGH_VALUE", "HIGH"),
            createTestAlert("alert-002", "FOREIGN_COUNTRY", "MEDIUM"),
            createTestAlert("alert-003", "HIGH_FREQUENCY", "HIGH")
        );
        when(alertRepository.getRecentAlerts(anyInt())).thenReturn(mockAlerts);

        // When: 최근 알림 10개 조회
        List<Alert> result = alertService.getRecentAlerts(10);

        // Then: repository에서 가져온 알림이 반환되어야 함
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getAlertId()).isEqualTo("alert-001");
        assertThat(result.get(1).getAlertId()).isEqualTo("alert-002");
        assertThat(result.get(2).getAlertId()).isEqualTo("alert-003");

        // Then: repository의 getRecentAlerts가 올바른 파라미터로 호출되었는지 확인
        verify(alertRepository, times(1)).getRecentAlerts(10);
    }

    @Test
    @DisplayName("limit이 0 이하일 때 빈 리스트를 반환해야 함")
    void testGetRecentAlertsWithInvalidLimit() {
        // Given: limit이 0인 경우

        // When: limit 0으로 조회
        List<Alert> result = alertService.getRecentAlerts(0);

        // Then: 빈 리스트 반환되고 repository는 호출되지 않아야 함
        assertThat(result).isEmpty();
        verify(alertRepository, never()).getRecentAlerts(anyInt());
    }

    @Test
    @DisplayName("repository가 빈 리스트를 반환하면 빈 리스트를 반환해야 함")
    void testGetRecentAlertsEmpty() {
        // Given: Mock repository가 빈 리스트를 반환하도록 설정
        when(alertRepository.getRecentAlerts(anyInt())).thenReturn(List.of());

        // When: 최근 알림 조회
        List<Alert> result = alertService.getRecentAlerts(10);

        // Then: 빈 리스트 반환
        assertThat(result).isEmpty();
        verify(alertRepository, times(1)).getRecentAlerts(10);
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
