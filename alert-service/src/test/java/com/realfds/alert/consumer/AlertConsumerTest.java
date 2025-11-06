package com.realfds.alert.consumer;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.realfds.alert.model.Alert;
import com.realfds.alert.model.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AlertConsumer 단위 테스트
 *
 * AlertConsumer의 Kafka 메시지 소비 및 에러 핸들링 로직을 검증합니다.
 *
 * 테스트 케이스:
 * - test_consume_alert_success: 정상적인 Alert 메시지 수신 시 로깅 확인
 * - test_consume_alert_invalid_json: 잘못된 JSON 역직렬화 시 에러 핸들링 확인
 *
 * Given-When-Then 구조를 사용하여 테스트 시나리오를 명확히 합니다.
 */
class AlertConsumerTest {

    private AlertConsumer alertConsumer;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        // Given: AlertConsumer 인스턴스 생성
        alertConsumer = new AlertConsumer();

        // 로그 캡처를 위한 ListAppender 설정
        logger = (Logger) LoggerFactory.getLogger(AlertConsumer.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        // ListAppender 정리
        logger.detachAppender(listAppender);
    }

    /**
     * 정상적인 Alert 메시지 수신 테스트
     *
     * Given: 유효한 Alert 객체가 준비됨
     * When: consumeAlert() 메서드를 호출함
     * Then: INFO 레벨 로그가 출력되고, alertId, ruleName, reason이 포함됨
     */
    @Test
    void test_consume_alert_success() {
        // Given: 정상적인 Alert 객체 생성
        Transaction transaction = new Transaction(
                "1.0",
                "txn-123",
                "user-1",
                1500000L,
                "KRW",
                "KR",
                "2025-01-01T10:00:00Z"
        );

        Alert alert = new Alert(
                "1.0",
                "alert-123",
                transaction,
                "SIMPLE_RULE",
                "HIGH_VALUE",
                "고액 거래 탐지 (1,500,000 KRW)",
                "HIGH",
                "2025-01-01T10:00:01Z"
        );

        // When: consumeAlert 메서드 호출
        alertConsumer.consumeAlert(alert);

        // Then: INFO 레벨 로그가 출력됨
        assertThat(listAppender.list)
                .isNotEmpty()
                .anyMatch(event -> event.getLevel().toString().equals("INFO"))
                .anyMatch(event -> event.getFormattedMessage().contains("alert-123"))
                .anyMatch(event -> event.getFormattedMessage().contains("HIGH_VALUE"))
                .anyMatch(event -> event.getFormattedMessage().contains("고액 거래 탐지"));
    }

    /**
     * 잘못된 JSON 역직렬화 시 에러 핸들링 테스트
     *
     * Given: Alert 객체의 getter가 예외를 던지도록 모킹됨 (역직렬화 실패 시뮬레이션)
     * When: consumeAlert() 메서드를 호출함
     * Then: WARN 레벨 로그가 출력되고, 예외가 전파되지 않음
     */
    @Test
    void test_consume_alert_invalid_json() {
        // Given: Alert 객체의 getter가 RuntimeException을 던지도록 모킹
        // (JSON 역직렬화 실패 또는 잘못된 데이터 시뮬레이션)
        Alert mockAlert = mock(Alert.class);
        when(mockAlert.getAlertId()).thenThrow(new RuntimeException("역직렬화 실패"));

        // When: consumeAlert 메서드 호출
        alertConsumer.consumeAlert(mockAlert);

        // Then: WARN 레벨 로그가 출력되고, 에러 메시지가 포함됨
        assertThat(listAppender.list)
                .isNotEmpty()
                .anyMatch(event -> event.getLevel().toString().equals("WARN"))
                .anyMatch(event -> event.getFormattedMessage().contains("알림 처리 중 오류 발생"))
                .anyMatch(event -> event.getFormattedMessage().contains("역직렬화 실패"));
    }
}
