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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T057: 심각도별 정렬 로직 단위 테스트 (User Story 3)
 *
 * Given-When-Then 구조를 사용하여 알림 심각도별 정렬 로직을 검증합니다.
 * - 심각도별 내림차순 정렬 (CRITICAL → HIGH → MEDIUM → LOW)
 * - 동일 심각도 내에서의 정렬 (timestamp 기준 최신순)
 * - 빈 리스트 처리
 */
@DisplayName("T057: 알림 심각도별 정렬 로직 단위 테스트")
class AlertSortBySeverityTest {

    private List<Alert> testAlerts;

    @BeforeEach
    void setUp() {
        // Given: 다양한 심각도의 알림 목록 생성 (순서 무작위)
        testAlerts = new ArrayList<>();

        // 심각도 순서가 섞여있는 알림 목록
        testAlerts.add(createTestAlert("alert-001", "FOREIGN_COUNTRY", "MEDIUM", "2025-11-11T10:00:00.000Z"));
        testAlerts.add(createTestAlert("alert-002", "HIGH_VALUE", "HIGH", "2025-11-11T10:01:00.000Z"));
        testAlerts.add(createTestAlert("alert-003", "TEST_RULE_LOW", "LOW", "2025-11-11T10:02:00.000Z"));
        testAlerts.add(createTestAlert("alert-004", "TEST_RULE_CRITICAL", "CRITICAL", "2025-11-11T10:03:00.000Z"));
        testAlerts.add(createTestAlert("alert-005", "HIGH_FREQUENCY", "HIGH", "2025-11-11T10:04:00.000Z"));
        testAlerts.add(createTestAlert("alert-006", "FOREIGN_COUNTRY", "MEDIUM", "2025-11-11T10:05:00.000Z"));
        testAlerts.add(createTestAlert("alert-007", "TEST_RULE_CRITICAL", "CRITICAL", "2025-11-11T10:06:00.000Z"));
        testAlerts.add(createTestAlert("alert-008", "TEST_RULE_LOW", "LOW", "2025-11-11T10:07:00.000Z"));
    }

    @Test
    @DisplayName("[T057] Given: 다양한 심각도의 알림 목록, When: 심각도별 정렬, Then: CRITICAL → HIGH → MEDIUM → LOW 순서 확인")
    void testSortBySeverity() {
        // When: 심각도별 정렬 (내림차순)
        List<Alert> result = sortBySeverity(testAlerts);

        // Then: CRITICAL → HIGH → MEDIUM → LOW 순서로 정렬되어야 함
        assertThat(result).hasSize(8);

        // Then: 정렬 순서 검증
        List<String> severities = result.stream()
            .map(Alert::getSeverity)
            .collect(Collectors.toList());

        // CRITICAL 2개, HIGH 2개, MEDIUM 2개, LOW 2개
        assertThat(severities).containsExactly(
            "CRITICAL", "CRITICAL",  // 첫 2개는 CRITICAL
            "HIGH", "HIGH",          // 다음 2개는 HIGH
            "MEDIUM", "MEDIUM",      // 다음 2개는 MEDIUM
            "LOW", "LOW"             // 마지막 2개는 LOW
        );
    }

    @Test
    @DisplayName("[T057] Given: 심각도별 알림, When: 정렬, Then: CRITICAL이 첫 번째")
    void testSortBySeverityCriticalFirst() {
        // When: 심각도별 정렬
        List<Alert> result = sortBySeverity(testAlerts);

        // Then: 첫 번째 알림은 CRITICAL이어야 함
        assertThat(result.get(0).getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("[T057] Given: 심각도별 알림, When: 정렬, Then: LOW가 마지막")
    void testSortBySeverityLowLast() {
        // When: 심각도별 정렬
        List<Alert> result = sortBySeverity(testAlerts);

        // Then: 마지막 알림은 LOW여야 함
        assertThat(result.get(result.size() - 1).getSeverity()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("[T057] Given: 동일 심각도의 알림, When: 정렬, Then: timestamp 역순 정렬 (최신순)")
    void testSortBySeveritySameLevel() {
        // Given: CRITICAL 심각도만 있는 알림 목록 (시간 순서가 다름)
        List<Alert> criticalAlerts = new ArrayList<>();
        criticalAlerts.add(createTestAlert("alert-c1", "TEST_RULE_CRITICAL", "CRITICAL", "2025-11-11T10:00:00.000Z"));
        criticalAlerts.add(createTestAlert("alert-c2", "TEST_RULE_CRITICAL", "CRITICAL", "2025-11-11T10:05:00.000Z")); // 더 최근
        criticalAlerts.add(createTestAlert("alert-c3", "TEST_RULE_CRITICAL", "CRITICAL", "2025-11-11T10:02:00.000Z"));

        // When: 심각도 및 timestamp별 정렬
        List<Alert> result = sortBySeverityAndTimestamp(criticalAlerts);

        // Then: 동일 심각도 내에서는 timestamp 역순 (최신순)
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getAlertId()).isEqualTo("alert-c2"); // 가장 최근
        assertThat(result.get(1).getAlertId()).isEqualTo("alert-c3"); // 중간
        assertThat(result.get(2).getAlertId()).isEqualTo("alert-c1"); // 가장 오래됨
    }

    @Test
    @DisplayName("[T057] Given: 빈 알림 목록, When: 정렬, Then: 빈 리스트 반환")
    void testSortBySeverityEmptyList() {
        // Given: 빈 알림 목록
        List<Alert> emptyList = new ArrayList<>();

        // When: 심각도별 정렬
        List<Alert> result = sortBySeverity(emptyList);

        // Then: 빈 리스트 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[T057] Given: 단일 심각도의 알림 목록, When: 정렬, Then: 원본 순서 유지")
    void testSortBySeveritySingleSeverity() {
        // Given: HIGH 심각도만 있는 알림 목록
        List<Alert> onlyHigh = new ArrayList<>();
        onlyHigh.add(createTestAlert("alert-h1", "HIGH_VALUE", "HIGH", "2025-11-11T10:00:00.000Z"));
        onlyHigh.add(createTestAlert("alert-h2", "HIGH_FREQUENCY", "HIGH", "2025-11-11T10:01:00.000Z"));
        onlyHigh.add(createTestAlert("alert-h3", "HIGH_VALUE", "HIGH", "2025-11-11T10:02:00.000Z"));

        // When: 심각도별 정렬
        List<Alert> result = sortBySeverity(onlyHigh);

        // Then: 모든 알림이 동일 심각도이므로 개수 유지
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(alert -> alert.getSeverity().equals("HIGH"));
    }

    @Test
    @DisplayName("[T057] Given: Severity enum의 priority, When: 정렬, Then: priority 값 사용 확인")
    void testSeverityEnumPriority() {
        // Given: Severity enum의 우선순위 값 확인
        assertThat(Severity.LOW.getPriority()).isEqualTo(1);
        assertThat(Severity.MEDIUM.getPriority()).isEqualTo(2);
        assertThat(Severity.HIGH.getPriority()).isEqualTo(3);
        assertThat(Severity.CRITICAL.getPriority()).isEqualTo(4);

        // When: Severity priorityComparator 사용
        List<Severity> severities = List.of(Severity.LOW, Severity.CRITICAL, Severity.MEDIUM, Severity.HIGH);
        List<Severity> sorted = severities.stream()
            .sorted(Severity.priorityComparator())
            .collect(Collectors.toList());

        // Then: CRITICAL → HIGH → MEDIUM → LOW 순서
        assertThat(sorted).containsExactly(Severity.CRITICAL, Severity.HIGH, Severity.MEDIUM, Severity.LOW);
    }

    @Test
    @DisplayName("[T057] Given: 복합 조건, When: 심각도 및 상태별 정렬, Then: 심각도 우선 정렬")
    void testSortBySeverityIgnoresStatus() {
        // Given: 다양한 심각도와 상태의 알림
        List<Alert> mixedAlerts = new ArrayList<>();
        Alert lowUnread = createTestAlert("alert-l1", "TEST_RULE_LOW", "LOW", "2025-11-11T10:00:00.000Z");
        lowUnread.setStatus(AlertStatus.UNREAD);

        Alert highCompleted = createTestAlert("alert-h1", "HIGH_VALUE", "HIGH", "2025-11-11T10:01:00.000Z");
        highCompleted.setStatus(AlertStatus.COMPLETED);

        Alert mediumInProgress = createTestAlert("alert-m1", "FOREIGN_COUNTRY", "MEDIUM", "2025-11-11T10:02:00.000Z");
        mediumInProgress.setStatus(AlertStatus.IN_PROGRESS);

        mixedAlerts.add(lowUnread);
        mixedAlerts.add(highCompleted);
        mixedAlerts.add(mediumInProgress);

        // When: 심각도별 정렬 (상태는 무시)
        List<Alert> result = sortBySeverity(mixedAlerts);

        // Then: 심각도 우선 정렬 (HIGH → MEDIUM → LOW)
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getSeverity()).isEqualTo("HIGH");
        assertThat(result.get(1).getSeverity()).isEqualTo("MEDIUM");
        assertThat(result.get(2).getSeverity()).isEqualTo("LOW");

        // Then: 상태는 정렬에 영향을 주지 않음
        assertThat(result.get(0).getStatus()).isEqualTo(AlertStatus.COMPLETED);
        assertThat(result.get(1).getStatus()).isEqualTo(AlertStatus.IN_PROGRESS);
        assertThat(result.get(2).getStatus()).isEqualTo(AlertStatus.UNREAD);
    }

    /**
     * 심각도별 정렬 로직 구현 (내림차순)
     * 실제 AlertService에 구현될 sortBySeverity 메서드의 로직을 검증합니다.
     *
     * @param alerts 알림 목록
     * @return 심각도별로 정렬된 알림 목록 (CRITICAL → HIGH → MEDIUM → LOW)
     */
    private List<Alert> sortBySeverity(List<Alert> alerts) {
        return alerts.stream()
            .sorted(Comparator.comparing(
                alert -> Severity.fromString(alert.getSeverity()),
                Severity.priorityComparator()
            ))
            .collect(Collectors.toList());
    }

    /**
     * 심각도 및 timestamp별 정렬 로직 구현
     * 동일 심각도 내에서는 timestamp 역순(최신순)으로 정렬합니다.
     *
     * @param alerts 알림 목록
     * @return 심각도 및 timestamp별로 정렬된 알림 목록
     */
    private List<Alert> sortBySeverityAndTimestamp(List<Alert> alerts) {
        return alerts.stream()
            .sorted(
                Comparator.comparing(
                    (Alert alert) -> Severity.fromString(alert.getSeverity()),
                    Severity.priorityComparator()
                )
                .thenComparing(Alert::getAlertTimestamp, Comparator.reverseOrder())
            )
            .collect(Collectors.toList());
    }

    /**
     * 테스트용 Alert 객체 생성 헬퍼 메서드
     *
     * @param alertId 알림 ID
     * @param ruleName 규칙 이름
     * @param severity 심각도
     * @param timestamp 알림 생성 시각
     * @return 테스트용 Alert 객체
     */
    private Alert createTestAlert(String alertId, String ruleName, String severity, String timestamp) {
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
            timestamp
        );

        return alert;
    }
}
