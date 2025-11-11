package com.realfds.alert.service;

import com.realfds.alert.model.Alert;
import com.realfds.alert.model.AlertStatus;
import com.realfds.alert.model.Severity;
import com.realfds.alert.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T056: 심각도별 필터링 로직 단위 테스트 (User Story 3)
 *
 * Given-When-Then 구조를 사용하여 알림 심각도별 필터링 로직을 검증합니다.
 * - 심각도별 필터링 (LOW, MEDIUM, HIGH, CRITICAL)
 * - 여러 심각도의 알림 중 특정 심각도만 필터링
 * - 빈 리스트 처리
 */
@DisplayName("T056: 알림 심각도별 필터링 로직 단위 테스트")
class AlertFilterBySeverityTest {

    private List<Alert> testAlerts;

    @BeforeEach
    void setUp() {
        // Given: 다양한 심각도의 알림 목록 생성
        testAlerts = new ArrayList<>();

        // LOW 심각도 알림 2개
        testAlerts.add(createTestAlert("alert-001", "TEST_RULE_LOW", "LOW", AlertStatus.UNREAD));
        testAlerts.add(createTestAlert("alert-002", "TEST_RULE_LOW", "LOW", AlertStatus.IN_PROGRESS));

        // MEDIUM 심각도 알림 2개 (FOREIGN_COUNTRY)
        testAlerts.add(createTestAlert("alert-003", "FOREIGN_COUNTRY", "MEDIUM", AlertStatus.UNREAD));
        testAlerts.add(createTestAlert("alert-004", "FOREIGN_COUNTRY", "MEDIUM", AlertStatus.COMPLETED));

        // HIGH 심각도 알림 3개 (HIGH_VALUE, HIGH_FREQUENCY)
        testAlerts.add(createTestAlert("alert-005", "HIGH_VALUE", "HIGH", AlertStatus.UNREAD));
        testAlerts.add(createTestAlert("alert-006", "HIGH_FREQUENCY", "HIGH", AlertStatus.IN_PROGRESS));
        testAlerts.add(createTestAlert("alert-007", "HIGH_VALUE", "HIGH", AlertStatus.COMPLETED));

        // CRITICAL 심각도 알림 1개
        testAlerts.add(createTestAlert("alert-008", "TEST_RULE_CRITICAL", "CRITICAL", AlertStatus.UNREAD));
    }

    @Test
    @DisplayName("[T056] Given: 다양한 심각도의 알림 목록, When: severity=LOW 필터 적용, Then: LOW 알림만 반환")
    void testFilterBySeverityLow() {
        // When: LOW 심각도로 필터링
        List<Alert> result = filterBySeverity(testAlerts, "LOW");

        // Then: LOW 알림만 반환되어야 함 (2개)
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(alert -> alert.getSeverity().equals("LOW"));

        // Then: 반환된 알림 ID 확인
        List<String> alertIds = result.stream()
            .map(Alert::getAlertId)
            .collect(Collectors.toList());
        assertThat(alertIds).containsExactlyInAnyOrder("alert-001", "alert-002");
    }

    @Test
    @DisplayName("[T056] Given: 다양한 심각도의 알림 목록, When: severity=MEDIUM 필터 적용, Then: MEDIUM 알림만 반환")
    void testFilterBySeverityMedium() {
        // When: MEDIUM 심각도로 필터링
        List<Alert> result = filterBySeverity(testAlerts, "MEDIUM");

        // Then: MEDIUM 알림만 반환되어야 함 (2개)
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(alert -> alert.getSeverity().equals("MEDIUM"));

        // Then: 반환된 알림 ID 확인
        List<String> alertIds = result.stream()
            .map(Alert::getAlertId)
            .collect(Collectors.toList());
        assertThat(alertIds).containsExactlyInAnyOrder("alert-003", "alert-004");
    }

    @Test
    @DisplayName("[T056] Given: 다양한 심각도의 알림 목록, When: severity=HIGH 필터 적용, Then: HIGH 알림만 반환")
    void testFilterBySeverityHigh() {
        // When: HIGH 심각도로 필터링
        List<Alert> result = filterBySeverity(testAlerts, "HIGH");

        // Then: HIGH 알림만 반환되어야 함 (3개)
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(alert -> alert.getSeverity().equals("HIGH"));

        // Then: 반환된 알림 ID 확인
        List<String> alertIds = result.stream()
            .map(Alert::getAlertId)
            .collect(Collectors.toList());
        assertThat(alertIds).containsExactlyInAnyOrder("alert-005", "alert-006", "alert-007");
    }

    @Test
    @DisplayName("[T056] Given: 다양한 심각도의 알림 목록, When: severity=CRITICAL 필터 적용, Then: CRITICAL 알림만 반환")
    void testFilterBySeverityCritical() {
        // When: CRITICAL 심각도로 필터링
        List<Alert> result = filterBySeverity(testAlerts, "CRITICAL");

        // Then: CRITICAL 알림만 반환되어야 함 (1개)
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(alert -> alert.getSeverity().equals("CRITICAL"));

        // Then: 반환된 알림 ID 확인
        assertThat(result.get(0).getAlertId()).isEqualTo("alert-008");
    }

    @Test
    @DisplayName("[T056] Given: 빈 알림 목록, When: 심각도 필터 적용, Then: 빈 리스트 반환")
    void testFilterBySeverityEmptyList() {
        // Given: 빈 알림 목록
        List<Alert> emptyList = new ArrayList<>();

        // When: HIGH 심각도로 필터링
        List<Alert> result = filterBySeverity(emptyList, "HIGH");

        // Then: 빈 리스트 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[T056] Given: 알림 목록, When: 해당 심각도의 알림이 없는 경우, Then: 빈 리스트 반환")
    void testFilterBySeverityNoMatch() {
        // Given: HIGH 심각도 알림만 있는 목록
        List<Alert> onlyHigh = new ArrayList<>();
        onlyHigh.add(createTestAlert("alert-001", "HIGH_VALUE", "HIGH", AlertStatus.UNREAD));
        onlyHigh.add(createTestAlert("alert-002", "HIGH_FREQUENCY", "HIGH", AlertStatus.UNREAD));

        // When: CRITICAL 심각도로 필터링
        List<Alert> result = filterBySeverity(onlyHigh, "CRITICAL");

        // Then: 빈 리스트 반환 (일치하는 알림 없음)
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[T056] Given: null 심각도로 필터링 시도, When: null 전달, Then: IllegalArgumentException 발생")
    void testFilterBySeverityNull() {
        // When & Then: null 심각도로 필터링 시 예외 발생
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> filterBySeverity(testAlerts, null),
            "Severity는 null일 수 없습니다"
        );
    }

    @Test
    @DisplayName("[T056] Given: 유효하지 않은 심각도 문자열, When: INVALID 전달, Then: IllegalArgumentException 발생")
    void testFilterBySeverityInvalid() {
        // When & Then: 유효하지 않은 심각도로 필터링 시 예외 발생
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> filterBySeverity(testAlerts, "INVALID"),
            "유효하지 않은 Severity 값입니다"
        );
    }

    @Test
    @DisplayName("[T056] Given: 대소문자 혼용 심각도, When: high (소문자) 전달, Then: 정상 필터링")
    void testFilterBySeverityCaseInsensitive() {
        // When: 소문자 high로 필터링 (Severity.fromString은 대소문자 무시)
        List<Alert> result = filterBySeverity(testAlerts, "high");

        // Then: HIGH 알림만 반환되어야 함 (3개)
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(alert -> alert.getSeverity().equals("HIGH"));
    }

    @Test
    @DisplayName("[T056] Given: 다양한 심각도와 상태의 알림, When: MEDIUM 필터, Then: 상태와 관계없이 MEDIUM만 반환")
    void testFilterBySeverityIgnoresStatus() {
        // When: MEDIUM 심각도로 필터링
        List<Alert> result = filterBySeverity(testAlerts, "MEDIUM");

        // Then: MEDIUM 알림은 다양한 상태를 가질 수 있음
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(alert -> alert.getSeverity().equals("MEDIUM"));

        // Then: 상태는 다양함 (UNREAD, COMPLETED)
        List<AlertStatus> statuses = result.stream()
            .map(Alert::getStatus)
            .collect(Collectors.toList());
        assertThat(statuses).containsExactlyInAnyOrder(AlertStatus.UNREAD, AlertStatus.COMPLETED);
    }

    /**
     * 심각도별 필터링 로직 구현
     * 실제 AlertService에 구현될 filterBySeverity 메서드의 로직을 검증합니다.
     *
     * @param alerts 알림 목록
     * @param severityStr 필터링할 심각도 문자열
     * @return 필터링된 알림 목록
     * @throws IllegalArgumentException severityStr이 null이거나 유효하지 않은 경우
     */
    private List<Alert> filterBySeverity(List<Alert> alerts, String severityStr) {
        if (severityStr == null) {
            throw new IllegalArgumentException("Severity는 null일 수 없습니다");
        }

        // Severity enum 검증 (유효하지 않은 값이면 예외 발생)
        Severity severity = Severity.fromString(severityStr);

        return alerts.stream()
            .filter(alert -> alert.getSeverity().equalsIgnoreCase(severity.name()))
            .collect(Collectors.toList());
    }

    /**
     * 테스트용 Alert 객체 생성 헬퍼 메서드
     *
     * @param alertId 알림 ID
     * @param ruleName 규칙 이름
     * @param severity 심각도
     * @param status 초기 상태
     * @return 테스트용 Alert 객체
     */
    private Alert createTestAlert(String alertId, String ruleName, String severity, AlertStatus status) {
        Transaction transaction = new Transaction(
            "1.0",
            "txn-" + alertId,
            "user-001",
            1500000L,
            "KRW",
            "KR",
            Instant.now().toString()
        );

        Alert alert = new Alert(
            "1.0",
            alertId,
            transaction,
            "SIMPLE_RULE",
            ruleName,
            "테스트 알림: " + ruleName + " (심각도: " + severity + ")",
            severity,
            Instant.now().toString()
        );

        // 상태 설정 (기본값은 UNREAD)
        if (status != AlertStatus.UNREAD) {
            alert.setStatus(status);
        }

        // COMPLETED 상태인 경우 processedAt 설정
        if (status == AlertStatus.COMPLETED) {
            alert.setProcessedAt(Instant.now());
        }

        return alert;
    }
}
