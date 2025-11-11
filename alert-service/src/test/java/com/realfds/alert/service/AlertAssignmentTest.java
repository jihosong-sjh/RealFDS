package com.realfds.alert.service;

import com.realfds.alert.model.Alert;
import com.realfds.alert.model.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AlertAssignment 단위 테스트 (User Story 2: 담당자 할당 및 조치 기록)
 *
 * Given-When-Then 구조를 사용하여 담당자 할당 로직을 검증합니다.
 * - 담당자 할당 기능 (assignedTo 필드 저장 및 검증)
 * - 최대 100자 제한 검증
 * - 미할당 시 null 반환 검증
 */
@DisplayName("[T033] Alert 담당자 할당 로직 단위 테스트")
class AlertAssignmentTest {

    @Test
    @DisplayName("Given: 알림 생성, When: 담당자 할당, Then: assignedTo 필드 저장")
    void testAssignUser() {
        // Given: 신규 알림 생성 (담당자 미할당)
        Alert alert = createTestAlert("alert-001", "HIGH_VALUE", "HIGH");
        assertThat(alert.getAssignedTo()).isNull();

        // When: 담당자 "김보안" 할당
        alert.setAssignedTo("김보안");

        // Then: assignedTo 필드가 "김보안"으로 저장됨
        assertThat(alert.getAssignedTo()).isEqualTo("김보안");
    }

    @Test
    @DisplayName("Given: 담당자 미할당, When: 알림 조회, Then: assignedTo null 반환")
    void testUnassignedAlertReturnsNull() {
        // Given: 신규 알림 생성 (담당자 미할당)
        Alert alert = createTestAlert("alert-002", "FOREIGN_COUNTRY", "MEDIUM");

        // When: 알림 조회

        // Then: assignedTo가 null이어야 함
        assertThat(alert.getAssignedTo()).isNull();
    }

    @Test
    @DisplayName("Given: 담당자 할당, When: 다른 담당자로 재할당, Then: 새 담당자로 업데이트")
    void testReassignUser() {
        // Given: 담당자 "김보안"이 할당된 알림
        Alert alert = createTestAlert("alert-003", "HIGH_FREQUENCY", "HIGH");
        alert.setAssignedTo("김보안");
        assertThat(alert.getAssignedTo()).isEqualTo("김보안");

        // When: 담당자를 "이보안"으로 재할당
        alert.setAssignedTo("이보안");

        // Then: assignedTo가 "이보안"으로 업데이트됨
        assertThat(alert.getAssignedTo()).isEqualTo("이보안");
    }

    @Test
    @DisplayName("Given: 담당자 할당, When: null로 할당 취소, Then: assignedTo null로 설정")
    void testUnassignUser() {
        // Given: 담당자 "김보안"이 할당된 알림
        Alert alert = createTestAlert("alert-004", "HIGH_VALUE", "HIGH");
        alert.setAssignedTo("김보안");
        assertThat(alert.getAssignedTo()).isEqualTo("김보안");

        // When: 담당자를 null로 설정 (할당 취소)
        alert.setAssignedTo(null);

        // Then: assignedTo가 null로 설정됨
        assertThat(alert.getAssignedTo()).isNull();
    }

    @Test
    @DisplayName("Given: 100자 이하 담당자 이름, When: 할당, Then: 성공적으로 저장")
    void testAssignUserWithMaxLength() {
        // Given: 신규 알림 생성
        Alert alert = createTestAlert("alert-005", "FOREIGN_COUNTRY", "MEDIUM");

        // When: 100자 정확히 맞는 담당자 이름 할당
        String maxLengthName = "A".repeat(100);
        alert.setAssignedTo(maxLengthName);

        // Then: assignedTo가 정상적으로 저장됨
        assertThat(alert.getAssignedTo()).isEqualTo(maxLengthName);
        assertThat(alert.getAssignedTo().length()).isEqualTo(100);
    }

    @Test
    @DisplayName("Given: 101자 담당자 이름, When: 할당 시도, Then: IllegalArgumentException 발생")
    void testAssignUserExceedsMaxLength() {
        // Given: 신규 알림 생성
        Alert alert = createTestAlert("alert-006", "HIGH_FREQUENCY", "HIGH");

        // When & Then: 101자 담당자 이름 할당 시 예외 발생
        String tooLongName = "A".repeat(101);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> alert.setAssignedTo(tooLongName),
            "담당자 이름은 100자를 초과할 수 없습니다"
        );

        // Then: 오류 메시지 확인
        assertThat(exception.getMessage())
            .contains("담당자 이름은 100자를 초과할 수 없습니다")
            .contains("현재 길이: 101");
    }

    @Test
    @DisplayName("Given: 한글 담당자 이름, When: 할당, Then: 유니코드 정상 처리")
    void testAssignUserWithKoreanName() {
        // Given: 신규 알림 생성
        Alert alert = createTestAlert("alert-007", "HIGH_VALUE", "HIGH");

        // When: 한글 이름 할당
        String koreanName = "김보안";
        alert.setAssignedTo(koreanName);

        // Then: 한글 이름이 정상적으로 저장됨
        assertThat(alert.getAssignedTo()).isEqualTo(koreanName);
    }

    @Test
    @DisplayName("Given: 한글 50자 담당자 이름, When: 할당, Then: 100자 제한 내에서 정상 처리")
    void testAssignUserWithMaxKoreanLength() {
        // Given: 신규 알림 생성
        Alert alert = createTestAlert("alert-008", "FOREIGN_COUNTRY", "MEDIUM");

        // When: 한글 50자 (100자 제한 내)
        String koreanName = "가".repeat(50);
        alert.setAssignedTo(koreanName);

        // Then: 한글 이름이 정상적으로 저장됨
        assertThat(alert.getAssignedTo()).isEqualTo(koreanName);
        assertThat(alert.getAssignedTo().length()).isEqualTo(50);
    }

    @Test
    @DisplayName("Given: 공백 문자열 담당자 이름, When: 할당, Then: 공백 문자열 저장 (유효성 검증 없음)")
    void testAssignUserWithEmptyString() {
        // Given: 신규 알림 생성
        Alert alert = createTestAlert("alert-009", "HIGH_FREQUENCY", "HIGH");

        // When: 빈 문자열 할당
        alert.setAssignedTo("");

        // Then: 빈 문자열이 저장됨 (비즈니스 로직에서 검증 필요)
        assertThat(alert.getAssignedTo()).isEmpty();
    }

    @Test
    @DisplayName("Given: 담당자 할당된 알림, When: 다른 필드 수정, Then: assignedTo 유지")
    void testAssignedToPreservedWhenUpdatingOtherFields() {
        // Given: 담당자 "김보안"이 할당된 알림
        Alert alert = createTestAlert("alert-010", "HIGH_VALUE", "HIGH");
        alert.setAssignedTo("김보안");
        assertThat(alert.getAssignedTo()).isEqualTo("김보안");

        // When: 상태를 IN_PROGRESS로 변경
        alert.setStatus(com.realfds.alert.model.AlertStatus.IN_PROGRESS);

        // Then: assignedTo는 여전히 "김보안"으로 유지됨
        assertThat(alert.getAssignedTo()).isEqualTo("김보안");
        assertThat(alert.getStatus()).isEqualTo(com.realfds.alert.model.AlertStatus.IN_PROGRESS);
    }

    /**
     * 테스트용 Alert 객체 생성 헬퍼 메서드
     *
     * @param alertId 알림 ID
     * @param ruleName 규칙 이름
     * @param severity 심각도
     * @return 생성된 Alert 객체
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
