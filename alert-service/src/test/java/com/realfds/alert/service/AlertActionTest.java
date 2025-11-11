package com.realfds.alert.service;

import com.realfds.alert.model.Alert;
import com.realfds.alert.model.AlertStatus;
import com.realfds.alert.model.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AlertAction 단위 테스트 (User Story 2: 담당자 할당 및 조치 기록)
 *
 * Given-When-Then 구조를 사용하여 조치 내용 기록 로직을 검증합니다.
 * - 조치 내용 입력 기능 (actionNote 필드 저장 및 검증)
 * - 최대 2000자 제한 검증
 * - 완료 처리 시 status=COMPLETED 및 processedAt 자동 설정 검증
 */
@DisplayName("[T034] Alert 조치 내용 기록 로직 단위 테스트")
class AlertActionTest {

    @Test
    @DisplayName("Given: 알림 생성, When: 조치 내용 입력 (최대 2000자), Then: actionNote 필드 저장")
    void testRecordActionNote() {
        // Given: 신규 알림 생성 (조치 내용 없음)
        Alert alert = createTestAlert("alert-001", "HIGH_VALUE", "HIGH");
        assertThat(alert.getActionNote()).isNull();

        // When: 조치 내용 입력
        String actionNote = "고객에게 확인 전화. 정상 거래로 확인됨.";
        alert.setActionNote(actionNote);

        // Then: actionNote 필드가 저장됨
        assertThat(alert.getActionNote()).isEqualTo(actionNote);
    }

    @Test
    @DisplayName("Given: 조치 내용 입력, When: 완료 처리, Then: status=COMPLETED 및 processedAt 자동 설정")
    void testRecordActionNoteAndComplete() {
        // Given: IN_PROGRESS 상태의 알림 생성
        Alert alert = createTestAlert("alert-002", "FOREIGN_COUNTRY", "MEDIUM");
        alert.setStatus(AlertStatus.IN_PROGRESS);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.IN_PROGRESS);
        assertThat(alert.getProcessedAt()).isNull();

        Instant beforeComplete = Instant.now();

        // When: 조치 내용 입력 후 완료 처리
        alert.setActionNote("고객 확인 완료. 정상 거래.");
        alert.setStatus(AlertStatus.COMPLETED);

        Instant afterComplete = Instant.now();

        // Then: actionNote가 저장되고, status=COMPLETED, processedAt 자동 설정됨
        assertThat(alert.getActionNote()).isEqualTo("고객 확인 완료. 정상 거래.");
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.COMPLETED);
        assertThat(alert.getProcessedAt()).isNotNull();
        assertThat(alert.getProcessedAt()).isBetween(beforeComplete, afterComplete);
    }

    @Test
    @DisplayName("Given: 2000자 정확히 맞는 조치 내용, When: 입력, Then: 성공적으로 저장")
    void testRecordActionNoteWithMaxLength() {
        // Given: 신규 알림 생성
        Alert alert = createTestAlert("alert-003", "HIGH_FREQUENCY", "HIGH");

        // When: 2000자 정확히 맞는 조치 내용 입력
        String maxLengthNote = "가".repeat(2000);
        alert.setActionNote(maxLengthNote);

        // Then: actionNote가 정상적으로 저장됨
        assertThat(alert.getActionNote()).isEqualTo(maxLengthNote);
        assertThat(alert.getActionNote().length()).isEqualTo(2000);
    }

    @Test
    @DisplayName("Given: 2001자 조치 내용, When: 입력 시도, Then: IllegalArgumentException 발생")
    void testRecordActionNoteExceedsMaxLength() {
        // Given: 신규 알림 생성
        Alert alert = createTestAlert("alert-004", "HIGH_VALUE", "HIGH");

        // When & Then: 2001자 조치 내용 입력 시 예외 발생
        String tooLongNote = "가".repeat(2001);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> alert.setActionNote(tooLongNote),
            "조치 내용은 2000자를 초과할 수 없습니다"
        );

        // Then: 오류 메시지 확인
        assertThat(exception.getMessage())
            .contains("조치 내용은 2000자를 초과할 수 없습니다")
            .contains("현재 길이: 2001");
    }

    @Test
    @DisplayName("Given: 조치 내용 없음, When: 알림 조회, Then: actionNote null 반환")
    void testActionNoteIsNullByDefault() {
        // Given: 신규 알림 생성 (조치 내용 없음)
        Alert alert = createTestAlert("alert-005", "FOREIGN_COUNTRY", "MEDIUM");

        // When: 알림 조회

        // Then: actionNote가 null이어야 함
        assertThat(alert.getActionNote()).isNull();
    }

    @Test
    @DisplayName("Given: 조치 내용 입력, When: null로 초기화, Then: actionNote null로 설정")
    void testClearActionNote() {
        // Given: 조치 내용이 입력된 알림
        Alert alert = createTestAlert("alert-006", "HIGH_FREQUENCY", "HIGH");
        alert.setActionNote("이전 조치 내용");
        assertThat(alert.getActionNote()).isEqualTo("이전 조치 내용");

        // When: 조치 내용을 null로 설정 (초기화)
        alert.setActionNote(null);

        // Then: actionNote가 null로 설정됨
        assertThat(alert.getActionNote()).isNull();
    }

    @Test
    @DisplayName("Given: 조치 내용 입력, When: 다른 조치 내용으로 업데이트, Then: 새 조치 내용으로 업데이트")
    void testUpdateActionNote() {
        // Given: 조치 내용이 입력된 알림
        Alert alert = createTestAlert("alert-007", "HIGH_VALUE", "HIGH");
        alert.setActionNote("1차 조치: 고객 연락 시도");
        assertThat(alert.getActionNote()).isEqualTo("1차 조치: 고객 연락 시도");

        // When: 조치 내용 업데이트
        alert.setActionNote("2차 조치: 고객 확인 완료. 정상 거래.");

        // Then: actionNote가 새 내용으로 업데이트됨
        assertThat(alert.getActionNote()).isEqualTo("2차 조치: 고객 확인 완료. 정상 거래.");
    }

    @Test
    @DisplayName("Given: 공백 문자열 조치 내용, When: 입력, Then: 공백 문자열 저장 (유효성 검증 없음)")
    void testRecordEmptyActionNote() {
        // Given: 신규 알림 생성
        Alert alert = createTestAlert("alert-008", "FOREIGN_COUNTRY", "MEDIUM");

        // When: 빈 문자열 입력
        alert.setActionNote("");

        // Then: 빈 문자열이 저장됨 (비즈니스 로직에서 검증 필요)
        assertThat(alert.getActionNote()).isEmpty();
    }

    @Test
    @DisplayName("Given: 조치 내용 입력 없이, When: 완료 처리, Then: status=COMPLETED 및 processedAt 자동 설정, actionNote null")
    void testCompleteWithoutActionNote() {
        // Given: UNREAD 상태의 알림 생성 (조치 내용 없음)
        Alert alert = createTestAlert("alert-009", "HIGH_FREQUENCY", "HIGH");
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.UNREAD);
        assertThat(alert.getActionNote()).isNull();
        assertThat(alert.getProcessedAt()).isNull();

        Instant beforeComplete = Instant.now();

        // When: 조치 내용 없이 바로 완료 처리 (조치 불필요한 경우)
        alert.setStatus(AlertStatus.COMPLETED);

        Instant afterComplete = Instant.now();

        // Then: status=COMPLETED, processedAt 자동 설정, actionNote는 null 유지
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.COMPLETED);
        assertThat(alert.getProcessedAt()).isNotNull();
        assertThat(alert.getProcessedAt()).isBetween(beforeComplete, afterComplete);
        assertThat(alert.getActionNote()).isNull();
    }

    @Test
    @DisplayName("Given: 한글 조치 내용, When: 입력, Then: 유니코드 정상 처리")
    void testRecordActionNoteWithKorean() {
        // Given: 신규 알림 생성
        Alert alert = createTestAlert("alert-010", "HIGH_VALUE", "HIGH");

        // When: 한글 조치 내용 입력
        String koreanNote = "고객에게 확인 메일을 발송하였으며, 정상 거래임을 확인하였습니다.";
        alert.setActionNote(koreanNote);

        // Then: 한글 조치 내용이 정상적으로 저장됨
        assertThat(alert.getActionNote()).isEqualTo(koreanNote);
    }

    @Test
    @DisplayName("Given: 조치 내용 입력, When: 다른 필드 수정, Then: actionNote 유지")
    void testActionNotePreservedWhenUpdatingOtherFields() {
        // Given: 조치 내용이 입력된 알림
        Alert alert = createTestAlert("alert-011", "FOREIGN_COUNTRY", "MEDIUM");
        String actionNote = "고객 확인 완료. 정상 거래.";
        alert.setActionNote(actionNote);
        assertThat(alert.getActionNote()).isEqualTo(actionNote);

        // When: 담당자를 "김보안"으로 할당
        alert.setAssignedTo("김보안");

        // Then: actionNote는 여전히 유지됨
        assertThat(alert.getActionNote()).isEqualTo(actionNote);
        assertThat(alert.getAssignedTo()).isEqualTo("김보안");
    }

    @Test
    @DisplayName("Given: 완료 상태의 알림, When: 조치 내용 수정, Then: 조치 내용 업데이트 가능")
    void testUpdateActionNoteAfterCompletion() {
        // Given: 완료 상태의 알림 (조치 내용 있음)
        Alert alert = createTestAlert("alert-012", "HIGH_FREQUENCY", "HIGH");
        alert.setActionNote("1차 조치: 정상 거래 확인");
        alert.setStatus(AlertStatus.COMPLETED);
        Instant originalProcessedAt = alert.getProcessedAt();
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.COMPLETED);
        assertThat(alert.getProcessedAt()).isNotNull();

        // When: 조치 내용 추가/수정
        alert.setActionNote("1차 조치: 정상 거래 확인\n2차 조치: 추가 모니터링 실시");

        // Then: actionNote가 업데이트되고, processedAt은 유지됨
        assertThat(alert.getActionNote()).contains("2차 조치: 추가 모니터링 실시");
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.COMPLETED);
        assertThat(alert.getProcessedAt()).isEqualTo(originalProcessedAt);
    }

    @Test
    @DisplayName("Given: 긴 조치 내용 (1000자), When: 입력, Then: 성공적으로 저장")
    void testRecordLongActionNote() {
        // Given: 신규 알림 생성
        Alert alert = createTestAlert("alert-013", "HIGH_VALUE", "HIGH");

        // When: 1000자 조치 내용 입력
        String longNote = "고객 확인: " + "테스트 내용. ".repeat(140); // 약 1000자
        alert.setActionNote(longNote);

        // Then: 긴 조치 내용이 정상적으로 저장됨
        assertThat(alert.getActionNote()).isEqualTo(longNote);
        assertThat(alert.getActionNote().length()).isLessThanOrEqualTo(2000);
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
