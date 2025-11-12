package io.realfds.alert.websocket

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

/**
 * 메트릭 데이터 WebSocket 핸들러 (Placeholder)
 *
 * 실시간 메트릭 데이터를 모든 연결된 클라이언트에 브로드캐스트합니다.
 *
 * 이 클래스는 Phase 2 (Foundational)에서 placeholder로 생성되었으며,
 * Phase 6 (WebSocket 실시간 브로드캐스트)에서 완전히 구현될 예정입니다.
 *
 * ## 주요 기능 (Phase 6에서 구현 예정)
 * - WebSocket 연결 관리
 * - 5초마다 메트릭 데이터 브로드캐스트
 * - 백필 요청 처리
 * - Ping-pong heartbeat (30초마다)
 *
 * @see io.realfds.alert.config.WebSocketConfig
 */
@Component
class MetricsWebSocketHandler : TextWebSocketHandler() {
    private val logger = LoggerFactory.getLogger(MetricsWebSocketHandler::class.java)

    /**
     * 연결된 WebSocket 세션 목록 (Thread-safe)
     */
    private val sessions = ConcurrentHashMap.newKeySet<WebSocketSession>()

    /**
     * WebSocket 연결 수립 시 호출
     *
     * @param session 새로 연결된 WebSocket 세션
     */
    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions.add(session)
        logger.info(
            "WebSocket 연결: sessionId={}, 원격 주소={}, 총 연결 수={}",
            session.id,
            session.remoteAddress,
            sessions.size
        )
    }

    /**
     * WebSocket 연결 종료 시 호출
     *
     * @param session 종료된 WebSocket 세션
     * @param status 종료 상태 코드
     */
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.remove(session)
        logger.info(
            "WebSocket 연결 종료: sessionId={}, status={}, 총 연결 수={}",
            session.id,
            status,
            sessions.size
        )
    }

    /**
     * 텍스트 메시지 수신 시 호출 (Phase 6에서 구현 예정)
     *
     * @param session 메시지를 보낸 WebSocket 세션
     * @param message 수신된 텍스트 메시지
     */
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        logger.debug(
            "WebSocket 메시지 수신: sessionId={}, payload={}",
            session.id,
            message.payload
        )
        // TODO: Phase 6에서 BACKFILL_REQUEST 처리 구현
    }

    /**
     * WebSocket 전송 오류 발생 시 호출
     *
     * @param session 오류가 발생한 WebSocket 세션
     * @param exception 발생한 예외
     */
    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        logger.error(
            "WebSocket 전송 오류: sessionId={}, message={}",
            session.id,
            exception.message,
            exception
        )
        sessions.remove(session)
    }

    // TODO: Phase 6에서 구현 예정
    // - broadcast(message: MetricsUpdate): 모든 세션에 메트릭 데이터 전송
    // - handleBackfillRequest(session, request): 백필 요청 처리
    // - sendHeartbeat(): Ping-pong heartbeat (30초마다)
}
