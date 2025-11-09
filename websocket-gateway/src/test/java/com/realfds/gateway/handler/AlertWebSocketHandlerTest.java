package com.realfds.gateway.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AlertWebSocketHandler 단위 테스트
 *
 * Given-When-Then 구조를 따르며 한국어 주석으로 작성됩니다.
 */
class AlertWebSocketHandlerTest {

    private AlertWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        // Given: AlertWebSocketHandler 인스턴스 생성
        handler = new AlertWebSocketHandler();
    }

    @Test
    @DisplayName("연결 수립 시 세션이 저장되어야 함")
    void test_after_connection_established() throws Exception {
        // Given: Mock WebSocket 세션 생성
        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("session-123");
        when(mockSession.isOpen()).thenReturn(true);

        // When: WebSocket 연결 수립
        handler.afterConnectionEstablished(mockSession);

        // Then: 세션이 저장되었는지 확인
        Set<WebSocketSession> sessions = handler.getSessions();
        assertEquals(1, sessions.size(), "세션 개수는 1개여야 함");
        assertTrue(sessions.contains(mockSession), "세션이 저장되어 있어야 함");
    }

    @Test
    @DisplayName("연결 종료 시 세션이 제거되어야 함")
    void test_after_connection_closed() throws Exception {
        // Given: WebSocket 세션이 이미 연결되어 있음
        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("session-123");
        when(mockSession.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(mockSession);

        // When: WebSocket 연결 종료
        handler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);

        // Then: 세션이 제거되었는지 확인
        Set<WebSocketSession> sessions = handler.getSessions();
        assertEquals(0, sessions.size(), "세션 개수는 0개여야 함");
        assertFalse(sessions.contains(mockSession), "세션이 제거되어 있어야 함");
    }

    @Test
    @DisplayName("여러 세션 동시 연결 및 종료 테스트")
    void test_multiple_sessions() throws Exception {
        // Given: 여러 개의 Mock WebSocket 세션 생성
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);
        WebSocketSession session3 = mock(WebSocketSession.class);

        when(session1.getId()).thenReturn("session-1");
        when(session2.getId()).thenReturn("session-2");
        when(session3.getId()).thenReturn("session-3");
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
        when(session3.isOpen()).thenReturn(true);

        // When: 3개의 세션 연결
        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);
        handler.afterConnectionEstablished(session3);

        // Then: 3개의 세션이 모두 저장되었는지 확인
        Set<WebSocketSession> sessions = handler.getSessions();
        assertEquals(3, sessions.size(), "세션 개수는 3개여야 함");

        // When: 세션 2 종료
        handler.afterConnectionClosed(session2, CloseStatus.NORMAL);

        // Then: 세션 2만 제거되고 나머지는 유지
        sessions = handler.getSessions();
        assertEquals(2, sessions.size(), "세션 개수는 2개여야 함");
        assertTrue(sessions.contains(session1), "세션 1은 유지되어야 함");
        assertFalse(sessions.contains(session2), "세션 2는 제거되어야 함");
        assertTrue(sessions.contains(session3), "세션 3은 유지되어야 함");
    }
}
