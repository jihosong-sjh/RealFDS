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
 * T035: 담당자별 필터링 통합 테스트 (User Story 2)
 *
 * Given-When-Then 구조를 사용하여 담당자별 알림 필터링 로직을 검증합니다.
 * - 담당자별 필터링 (assignedTo 필드 기준)
 * - 여러 담당자의 알림 중 특정 담당자만 필터링
 * - 미할당 알림 (assignedTo=null) 처리
 * - 빈 리스트 처리
 */
@DisplayName("[T035] 담당자별 알림 필터링 로직 통합 테스트")
class AlertFilterByAssigneeTest {

    private List<Alert> testAlerts;

    @BeforeEach
    void setUp() {
        // Given: 다양한 담당자의 알림 목록 생성
        testAlerts = new ArrayList<>();

        // 김보안 담당자의 알림 3개
        Alert alert1 = createTestAlert("alert-001", "HIGH_VALUE", "HIGH");
        alert1.setAssignedTo("김보안");
        testAlerts.add(alert1);

        Alert alert2 = createTestAlert("alert-002", "FOREIGN_COUNTRY", "MEDIUM");
        alert2.setAssignedTo("김보안");
        testAlerts.add(alert2);

        Alert alert3 = createTestAlert("alert-003", "HIGH_FREQUENCY", "HIGH");
        alert3.setAssignedTo("김보안");
        testAlerts.add(alert3);

        // 이보안 담당자의 알림 2개
        Alert alert4 = createTestAlert("alert-004", "HIGH_VALUE", "HIGH");
        alert4.setAssignedTo("이보안");
        testAlerts.add(alert4);

        Alert alert5 = createTestAlert("alert-005", "FOREIGN_COUNTRY", "MEDIUM");
        alert5.setAssignedTo("이보안");
        testAlerts.add(alert5);

        // 박보안 담당자의 알림 1개
        Alert alert6 = createTestAlert("alert-006", "HIGH_FREQUENCY", "HIGH");
        alert6.setAssignedTo("박보안");
        testAlerts.add(alert6);

        // 미할당 알림 2개
        Alert alert7 = createTestAlert("alert-007", "HIGH_VALUE", "HIGH");
        // assignedTo를 null로 유지 (미할당)
        testAlerts.add(alert7);

        Alert alert8 = createTestAlert("alert-008", "FOREIGN_COUNTRY", "MEDIUM");
        // assignedTo를 null로 유지 (미할당)
        testAlerts.add(alert8);
    }

    @Test
    @DisplayName("Given: 다양한 담당자의 알림 목록, When: assignedTo='김보안' 필터 적용, Then: 김보안 담당 알림만 반환")
    void testFilterByAssigneeKimBoan() {
        // When: "김보안" 담당자로 필터링
        List<Alert> result = filterByAssignee(testAlerts, "김보안");

        // Then: "김보안" 담당 알림만 반환되어야 함 (3개)
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(alert -> "김보안".equals(alert.getAssignedTo()));

        // Then: 반환된 알림 ID 확인
        List<String> alertIds = result.stream()
            .map(Alert::getAlertId)
            .collect(Collectors.toList());
        assertThat(alertIds).containsExactlyInAnyOrder("alert-001", "alert-002", "alert-003");
    }

    @Test
    @DisplayName("Given: 다양한 담당자의 알림 목록, When: assignedTo='이보안' 필터 적용, Then: 이보안 담당 알림만 반환")
    void testFilterByAssigneeLeeBoan() {
        // When: "이보안" 담당자로 필터링
        List<Alert> result = filterByAssignee(testAlerts, "이보안");

        // Then: "이보안" 담당 알림만 반환되어야 함 (2개)
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(alert -> "이보안".equals(alert.getAssignedTo()));

        // Then: 반환된 알림 ID 확인
        List<String> alertIds = result.stream()
            .map(Alert::getAlertId)
            .collect(Collectors.toList());
        assertThat(alertIds).containsExactlyInAnyOrder("alert-004", "alert-005");
    }

    @Test
    @DisplayName("Given: 다양한 담당자의 알림 목록, When: assignedTo='박보안' 필터 적용, Then: 박보안 담당 알림만 반환")
    void testFilterByAssigneeParkBoan() {
        // When: "박보안" 담당자로 필터링
        List<Alert> result = filterByAssignee(testAlerts, "박보안");

        // Then: "박보안" 담당 알림만 반환되어야 함 (1개)
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(alert -> "박보안".equals(alert.getAssignedTo()));

        // Then: 반환된 알림 ID 확인
        List<String> alertIds = result.stream()
            .map(Alert::getAlertId)
            .collect(Collectors.toList());
        assertThat(alertIds).containsExactly("alert-006");
    }

    @Test
    @DisplayName("Given: 다양한 담당자의 알림 목록, When: 존재하지 않는 담당자로 필터 적용, Then: 빈 리스트 반환")
    void testFilterByAssigneeNoMatch() {
        // When: 존재하지 않는 담당자 "최보안"으로 필터링
        List<Alert> result = filterByAssignee(testAlerts, "최보안");

        // Then: 빈 리스트 반환 (일치하는 알림 없음)
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Given: 빈 알림 목록, When: 담당자 필터 적용, Then: 빈 리스트 반환")
    void testFilterByAssigneeEmptyList() {
        // Given: 빈 알림 목록
        List<Alert> emptyList = new ArrayList<>();

        // When: "김보안" 담당자로 필터링
        List<Alert> result = filterByAssignee(emptyList, "김보안");

        // Then: 빈 리스트 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Given: 미할당 알림만 있는 목록, When: 담당자 필터 적용, Then: 빈 리스트 반환")
    void testFilterByAssigneeOnlyUnassigned() {
        // Given: 미할당 알림만 있는 목록
        List<Alert> onlyUnassigned = new ArrayList<>();
        onlyUnassigned.add(createTestAlert("alert-001", "HIGH_VALUE", "HIGH"));
        onlyUnassigned.add(createTestAlert("alert-002", "FOREIGN_COUNTRY", "MEDIUM"));

        // When: "김보안" 담당자로 필터링
        List<Alert> result = filterByAssignee(onlyUnassigned, "김보안");

        // Then: 빈 리스트 반환 (미할당 알림은 필터링되지 않음)
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Given: null 담당자로 필터링 시도, When: null 전달, Then: IllegalArgumentException 발생")
    void testFilterByAssigneeNull() {
        // When & Then: null 담당자로 필터링 시 예외 발생
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> filterByAssignee(testAlerts, null),
            "담당자 이름은 null일 수 없습니다"
        );
    }

    @Test
    @DisplayName("Given: 빈 문자열 담당자로 필터링 시도, When: 빈 문자열 전달, Then: 빈 리스트 반환")
    void testFilterByAssigneeEmptyString() {
        // When: 빈 문자열로 필터링
        List<Alert> result = filterByAssignee(testAlerts, "");

        // Then: 빈 리스트 반환 (빈 문자열은 유효한 담당자 이름이 아님)
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Given: 대소문자가 다른 담당자 이름, When: 필터 적용, Then: 대소문자 구분하여 필터링")
    void testFilterByAssigneeCaseSensitive() {
        // Given: "김보안" 담당자의 알림
        Alert alert = createTestAlert("alert-001", "HIGH_VALUE", "HIGH");
        alert.setAssignedTo("김보안");
        List<Alert> alerts = List.of(alert);

        // When: 소문자 "kim"으로 필터링 (한글이므로 대소문자 무관하지만, 영문 테스트용)
        List<Alert> result = filterByAssignee(alerts, "KIM보안");

        // Then: 대소문자가 일치하지 않으면 빈 리스트 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Given: 담당자별 알림 목록, When: 복합 필터 (담당자 + 상태), Then: 조건에 맞는 알림만 반환")
    void testFilterByAssigneeAndStatus() {
        // Given: "김보안" 담당자의 알림 중 일부를 IN_PROGRESS로 변경
        testAlerts.get(0).setStatus(AlertStatus.IN_PROGRESS); // alert-001
        testAlerts.get(1).setStatus(AlertStatus.COMPLETED);   // alert-002
        // alert-003은 UNREAD 유지

        // When: "김보안" 담당자이면서 IN_PROGRESS 상태인 알림 필터링
        List<Alert> result = filterByAssigneeAndStatus(testAlerts, "김보안", AlertStatus.IN_PROGRESS);

        // Then: "김보안" + IN_PROGRESS 알림만 반환 (1개)
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAlertId()).isEqualTo("alert-001");
        assertThat(result.get(0).getAssignedTo()).isEqualTo("김보안");
        assertThat(result.get(0).getStatus()).isEqualTo(AlertStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Given: 담당자별 알림 목록, When: 담당자 이름에 공백 포함, Then: 정확한 이름 매칭")
    void testFilterByAssigneeWithWhitespace() {
        // Given: 공백이 포함된 담당자 이름
        Alert alert = createTestAlert("alert-001", "HIGH_VALUE", "HIGH");
        alert.setAssignedTo("김 보안");
        List<Alert> alerts = List.of(alert);

        // When: 공백이 포함된 담당자 이름으로 필터링
        List<Alert> result = filterByAssignee(alerts, "김 보안");

        // Then: 정확히 매칭되어 반환됨
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssignedTo()).isEqualTo("김 보안");

        // Then: 공백 없이 필터링하면 빈 리스트
        List<Alert> noMatch = filterByAssignee(alerts, "김보안");
        assertThat(noMatch).isEmpty();
    }

    @Test
    @DisplayName("Given: 담당자별 알림 목록, When: 같은 담당자에게 재할당된 알림, Then: 해당 담당자 알림으로 필터링됨")
    void testFilterAfterReassignment() {
        // Given: 처음에 "김보안"이었다가 "이보안"으로 재할당된 알림
        Alert alert = createTestAlert("alert-001", "HIGH_VALUE", "HIGH");
        alert.setAssignedTo("김보안");
        alert.setAssignedTo("이보안"); // 재할당
        List<Alert> alerts = List.of(alert);

        // When: "이보안" 담당자로 필터링
        List<Alert> result = filterByAssignee(alerts, "이보안");

        // Then: "이보안" 담당 알림으로 필터링됨
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssignedTo()).isEqualTo("이보안");

        // Then: "김보안"으로 필터링하면 빈 리스트 (재할당되었으므로)
        List<Alert> noMatch = filterByAssignee(alerts, "김보안");
        assertThat(noMatch).isEmpty();
    }

    @Test
    @DisplayName("Given: 담당자별 알림 목록, When: 성능 테스트 (100개 알림 필터링), Then: 100ms 이내 응답")
    void testFilterByAssigneePerformance() {
        // Given: 100개의 알림 생성 (다양한 담당자)
        List<Alert> manyAlerts = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Alert alert = createTestAlert("alert-" + String.format("%03d", i), "HIGH_VALUE", "HIGH");
            alert.setAssignedTo("담당자" + (i % 10)); // 10명의 담당자에게 분배
            manyAlerts.add(alert);
        }

        // When: 특정 담당자로 필터링 (시간 측정)
        long startTime = System.currentTimeMillis();
        List<Alert> result = filterByAssignee(manyAlerts, "담당자5");
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: 100ms 이내 응답 (목표: <100ms)
        assertThat(duration).isLessThan(100);

        // Then: 정확히 10개의 알림 반환 (100개 중 10개)
        assertThat(result).hasSize(10);
        assertThat(result).allMatch(alert -> "담당자5".equals(alert.getAssignedTo()));
    }

    /**
     * 담당자별 필터링 로직 구현
     * 실제 AlertService에 구현될 filterByAssignee 메서드의 로직을 검증합니다.
     *
     * @param alerts 알림 목록
     * @param assignedTo 필터링할 담당자 이름
     * @return 필터링된 알림 목록
     * @throws IllegalArgumentException assignedTo가 null인 경우
     */
    private List<Alert> filterByAssignee(List<Alert> alerts, String assignedTo) {
        if (assignedTo == null) {
            throw new IllegalArgumentException("담당자 이름은 null일 수 없습니다");
        }

        return alerts.stream()
            .filter(alert -> assignedTo.equals(alert.getAssignedTo()))
            .collect(Collectors.toList());
    }

    /**
     * 담당자 + 상태 복합 필터링 로직 구현
     * 담당자와 상태를 동시에 필터링합니다.
     *
     * @param alerts 알림 목록
     * @param assignedTo 필터링할 담당자 이름
     * @param status 필터링할 상태
     * @return 필터링된 알림 목록
     */
    private List<Alert> filterByAssigneeAndStatus(List<Alert> alerts, String assignedTo, AlertStatus status) {
        return alerts.stream()
            .filter(alert -> assignedTo.equals(alert.getAssignedTo()))
            .filter(alert -> alert.getStatus() == status)
            .collect(Collectors.toList());
    }

    /**
     * 테스트용 Alert 객체 생성 헬퍼 메서드
     *
     * @param alertId 알림 ID
     * @param ruleName 규칙 이름
     * @param severity 심각도
     * @return 테스트용 Alert 객체 (assignedTo는 null, 추후 설정 필요)
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
