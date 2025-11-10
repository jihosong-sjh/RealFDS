package com.realfds.alert.service;

import com.realfds.alert.model.Alert;
import com.realfds.alert.model.AlertStatus;
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
 * T014: 상태별 필터링 로직 단위 테스트 (User Story 1)
 *
 * Given-When-Then 구조를 사용하여 알림 상태별 필터링 로직을 검증합니다.
 * - 상태별 필터링 (UNREAD, IN_PROGRESS, COMPLETED)
 * - 여러 상태의 알림 중 특정 상태만 필터링
 * - 빈 리스트 처리
 */
@DisplayName("T014: 알림 상태별 필터링 로직 단위 테스트")
class AlertFilterTest {

    private List<Alert> testAlerts;

    @BeforeEach
    void setUp() {
        // Given: 다양한 상태의 알림 목록 생성
        testAlerts = new ArrayList<>();

        // UNREAD 상태 알림 3개
        testAlerts.add(createTestAlert("alert-001", "HIGH_VALUE", "HIGH", AlertStatus.UNREAD));
        testAlerts.add(createTestAlert("alert-002", "FOREIGN_COUNTRY", "MEDIUM", AlertStatus.UNREAD));
        testAlerts.add(createTestAlert("alert-003", "HIGH_FREQUENCY", "HIGH", AlertStatus.UNREAD));

        // IN_PROGRESS 상태 알림 2개
        Alert inProgress1 = createTestAlert("alert-004", "HIGH_VALUE", "HIGH", AlertStatus.UNREAD);
        inProgress1.setStatus(AlertStatus.IN_PROGRESS);
        testAlerts.add(inProgress1);

        Alert inProgress2 = createTestAlert("alert-005", "FOREIGN_COUNTRY", "MEDIUM", AlertStatus.UNREAD);
        inProgress2.setStatus(AlertStatus.IN_PROGRESS);
        testAlerts.add(inProgress2);

        // COMPLETED 상태 알림 2개
        Alert completed1 = createTestAlert("alert-006", "HIGH_FREQUENCY", "HIGH", AlertStatus.UNREAD);
        completed1.setStatus(AlertStatus.COMPLETED);
        testAlerts.add(completed1);

        Alert completed2 = createTestAlert("alert-007", "HIGH_VALUE", "HIGH", AlertStatus.UNREAD);
        completed2.setStatus(AlertStatus.COMPLETED);
        testAlerts.add(completed2);
    }

    @Test
    @DisplayName("[T014] Given: 다양한 상태의 알림 목록, When: status=UNREAD 필터 적용, Then: UNREAD 알림만 반환")
    void testFilterByStatusUnread() {
        // When: UNREAD 상태로 필터링
        List<Alert> result = filterByStatus(testAlerts, AlertStatus.UNREAD);

        // Then: UNREAD 알림만 반환되어야 함 (3개)
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(alert -> alert.getStatus() == AlertStatus.UNREAD);

        // Then: 반환된 알림 ID 확인
        List<String> alertIds = result.stream()
            .map(Alert::getAlertId)
            .collect(Collectors.toList());
        assertThat(alertIds).containsExactlyInAnyOrder("alert-001", "alert-002", "alert-003");
    }

    @Test
    @DisplayName("[T014] Given: 다양한 상태의 알림 목록, When: status=IN_PROGRESS 필터 적용, Then: IN_PROGRESS 알림만 반환")
    void testFilterByStatusInProgress() {
        // When: IN_PROGRESS 상태로 필터링
        List<Alert> result = filterByStatus(testAlerts, AlertStatus.IN_PROGRESS);

        // Then: IN_PROGRESS 알림만 반환되어야 함 (2개)
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(alert -> alert.getStatus() == AlertStatus.IN_PROGRESS);

        // Then: 반환된 알림 ID 확인
        List<String> alertIds = result.stream()
            .map(Alert::getAlertId)
            .collect(Collectors.toList());
        assertThat(alertIds).containsExactlyInAnyOrder("alert-004", "alert-005");
    }

    @Test
    @DisplayName("[T014] Given: 다양한 상태의 알림 목록, When: status=COMPLETED 필터 적용, Then: COMPLETED 알림만 반환")
    void testFilterByStatusCompleted() {
        // When: COMPLETED 상태로 필터링
        List<Alert> result = filterByStatus(testAlerts, AlertStatus.COMPLETED);

        // Then: COMPLETED 알림만 반환되어야 함 (2개)
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(alert -> alert.getStatus() == AlertStatus.COMPLETED);
        assertThat(result).allMatch(alert -> alert.getProcessedAt() != null);

        // Then: 반환된 알림 ID 확인
        List<String> alertIds = result.stream()
            .map(Alert::getAlertId)
            .collect(Collectors.toList());
        assertThat(alertIds).containsExactlyInAnyOrder("alert-006", "alert-007");
    }

    @Test
    @DisplayName("[T014] Given: 빈 알림 목록, When: 필터 적용, Then: 빈 리스트 반환")
    void testFilterByStatusEmptyList() {
        // Given: 빈 알림 목록
        List<Alert> emptyList = new ArrayList<>();

        // When: UNREAD 상태로 필터링
        List<Alert> result = filterByStatus(emptyList, AlertStatus.UNREAD);

        // Then: 빈 리스트 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[T014] Given: 알림 목록, When: 해당 상태의 알림이 없는 경우, Then: 빈 리스트 반환")
    void testFilterByStatusNoMatch() {
        // Given: UNREAD 상태 알림만 있는 목록
        List<Alert> onlyUnread = new ArrayList<>();
        onlyUnread.add(createTestAlert("alert-001", "HIGH_VALUE", "HIGH", AlertStatus.UNREAD));
        onlyUnread.add(createTestAlert("alert-002", "FOREIGN_COUNTRY", "MEDIUM", AlertStatus.UNREAD));

        // When: COMPLETED 상태로 필터링
        List<Alert> result = filterByStatus(onlyUnread, AlertStatus.COMPLETED);

        // Then: 빈 리스트 반환 (일치하는 알림 없음)
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[T014] Given: null 상태로 필터링 시도, When: null 전달, Then: IllegalArgumentException 발생")
    void testFilterByStatusNull() {
        // When & Then: null 상태로 필터링 시 예외 발생
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> filterByStatus(testAlerts, null),
            "Status는 null일 수 없습니다"
        );
    }

    /**
     * 상태별 필터링 로직 구현
     * 실제 AlertService에 구현될 filterByStatus 메서드의 로직을 검증합니다.
     *
     * @param alerts 알림 목록
     * @param status 필터링할 상태
     * @return 필터링된 알림 목록
     * @throws IllegalArgumentException status가 null인 경우
     */
    private List<Alert> filterByStatus(List<Alert> alerts, AlertStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status는 null일 수 없습니다");
        }

        return alerts.stream()
            .filter(alert -> alert.getStatus() == status)
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
            "테스트 알림: " + ruleName,
            severity,
            Instant.now().toString()
        );

        // 초기 상태 설정은 생성자에서 UNREAD로 자동 설정되므로
        // status가 UNREAD가 아닌 경우만 setStatus 호출
        if (status != AlertStatus.UNREAD) {
            alert.setStatus(status);
        }

        return alert;
    }
}
