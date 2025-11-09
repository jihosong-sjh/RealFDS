package com.realfds.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realfds.gateway.handler.AlertWebSocketHandler;
import com.realfds.gateway.model.Alert;
import com.realfds.gateway.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BroadcastService 단위 테스트
 *
 * Given-When-Then 구조를 따르며 한국어 주석으로 작성됩니다.
 */
class BroadcastServiceTest {

    private BroadcastService broadcastService;
    private AlertWebSocketHandler mockHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Given: Mock AlertWebSocketHandler와 ObjectMapper 생성
        mockHandler = mock(AlertWebSocketHandler.class);
        objectMapper = new ObjectMapper();
        broadcastService = new BroadcastService(mockHandler, objectMapper);
    }

    @Test
    @DisplayName("모든 세션에 알림을 브로드캐스트해야 함")
    void test_broadcast_to_all_sessions() throws Exception {
        // Given: 3개의 Mock WebSocket 세션 생성
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);
        WebSocketSession session3 = mock(WebSocketSession.class);

        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
        when(session3.isOpen()).thenReturn(true);
        when(session1.getId()).thenReturn("session-1");
        when(session2.getId()).thenReturn("session-2");
        when(session3.getId()).thenReturn("session-3");

        // Mock handler의 getSessions()가 3개의 세션을 반환하도록 설정
        when(mockHandler.getSessions())
            .thenReturn(java.util.Set.of(session1, session2, session3));

        // 테스트용 Alert 생성
        Transaction transaction = new Transaction(
            "1.0", "tx-123", "user-1", 1500000L, "KRW", "KR", "2025-11-09T10:00:00Z"
        );
        Alert alert = new Alert(
            "1.0", "alert-123", transaction, "SIMPLE_RULE", "HIGH_VALUE",
            "고액 거래 (100만원 초과): 1500000원", "HIGH", "2025-11-09T10:00:00Z"
        );

        // When: 알림 브로드캐스트
        broadcastService.broadcast(alert);

        // Then: 모든 세션에 메시지가 전송되었는지 확인
        verify(session1, times(1)).sendMessage(any(TextMessage.class));
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
        verify(session3, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("전송 실패 시 해당 세션을 제거해야 함")
    void test_broadcast_remove_closed_session() throws Exception {
        // Given: 정상 세션 1개와 전송 실패 세션 1개
        WebSocketSession normalSession = mock(WebSocketSession.class);
        WebSocketSession failedSession = mock(WebSocketSession.class);

        when(normalSession.isOpen()).thenReturn(true);
        when(failedSession.isOpen()).thenReturn(true);
        when(normalSession.getId()).thenReturn("normal-session");
        when(failedSession.getId()).thenReturn("failed-session");

        // failedSession은 메시지 전송 시 IOException 발생
        doThrow(new IOException("Connection closed")).when(failedSession).sendMessage(any(TextMessage.class));

        when(mockHandler.getSessions())
            .thenReturn(java.util.Set.of(normalSession, failedSession));

        // 테스트용 Alert 생성
        Transaction transaction = new Transaction(
            "1.0", "tx-123", "user-1", 1500000L, "KRW", "KR", "2025-11-09T10:00:00Z"
        );
        Alert alert = new Alert(
            "1.0", "alert-123", transaction, "SIMPLE_RULE", "HIGH_VALUE",
            "고액 거래 (100만원 초과): 1500000원", "HIGH", "2025-11-09T10:00:00Z"
        );

        // When: 알림 브로드캐스트
        broadcastService.broadcast(alert);

        // Then: 정상 세션은 메시지를 받고, 실패한 세션은 닫혔는지 확인
        verify(normalSession, times(1)).sendMessage(any(TextMessage.class));
        verify(failedSession, times(1)).sendMessage(any(TextMessage.class));
        verify(failedSession, times(1)).close();  // 실패 시 세션 닫기
    }

    @Test
    @DisplayName("열려있지 않은 세션은 건너뛰어야 함")
    void test_broadcast_skip_closed_session() throws Exception {
        // Given: 닫힌 세션 1개와 열린 세션 1개
        WebSocketSession closedSession = mock(WebSocketSession.class);
        WebSocketSession openSession = mock(WebSocketSession.class);

        when(closedSession.isOpen()).thenReturn(false);
        when(openSession.isOpen()).thenReturn(true);
        when(closedSession.getId()).thenReturn("closed-session");
        when(openSession.getId()).thenReturn("open-session");

        when(mockHandler.getSessions())
            .thenReturn(java.util.Set.of(closedSession, openSession));

        // 테스트용 Alert 생성
        Transaction transaction = new Transaction(
            "1.0", "tx-123", "user-1", 1500000L, "KRW", "KR", "2025-11-09T10:00:00Z"
        );
        Alert alert = new Alert(
            "1.0", "alert-123", transaction, "SIMPLE_RULE", "HIGH_VALUE",
            "고액 거래 (100만원 초과): 1500000원", "HIGH", "2025-11-09T10:00:00Z"
        );

        // When: 알림 브로드캐스트
        broadcastService.broadcast(alert);

        // Then: 열린 세션만 메시지를 받아야 함
        verify(closedSession, never()).sendMessage(any(TextMessage.class));
        verify(openSession, times(1)).sendMessage(any(TextMessage.class));
    }
}
