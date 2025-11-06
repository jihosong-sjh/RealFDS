# WebSocket API Contract: 실시간 알림 스트리밍

**작성일**: 2025-11-06
**Phase**: Phase 1 - Design & Contracts
**목적**: WebSocket 연결, 메시지 형식, 클라이언트/서버 계약 정의

---

## 1. WebSocket 엔드포인트

### 1.1 연결 URL
```
ws://localhost:8082/ws/alerts
```

**서비스**: websocket-gateway
**프로토콜**: WebSocket (RFC 6455)
**포트**: 8082

---

## 2. 연결 흐름

### 2.1 핸드셰이크
```
Client → Server: GET /ws/alerts HTTP/1.1
                 Upgrade: websocket
                 Connection: Upgrade
                 Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
                 Sec-WebSocket-Version: 13

Server → Client: HTTP/1.1 101 Switching Protocols
                 Upgrade: websocket
                 Connection: Upgrade
                 Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

### 2.2 연결 유지
- **Ping/Pong**: 30초마다 ping 전송, pong 미수신 시 연결 종료
- **타임아웃**: 60초 idle 시 연결 종료

---

## 3. 메시지 형식

### 3.1 서버 → 클라이언트 (Alert 메시지)

**타입**: Text Frame (JSON)

```json
{
  "type": "alert",
  "data": {
    "alertId": "660e9511-f39c-52e5-b827-557766551111",
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user-3",
    "amount": 1250000,
    "currency": "KRW",
    "countryCode": "KR",
    "ruleName": "HIGH_VALUE",
    "reason": "고액 거래 (100만원 초과): 1,250,000원",
    "severity": "HIGH",
    "timestamp": "2025-11-06T10:30:45.123Z",
    "alertTimestamp": "2025-11-06T10:30:47.456Z"
  }
}
```

**필드 설명**:
- `type`: 메시지 유형 (항상 "alert")
- `data`: Alert 엔터티 (원본 거래 정보 + 알림 정보)

### 3.2 서버 → 클라이언트 (연결 확인)

```json
{
  "type": "connection",
  "data": {
    "status": "connected",
    "timestamp": "2025-11-06T10:30:00.000Z",
    "sessionId": "ws-session-12345"
  }
}
```

### 3.3 클라이언트 → 서버 (Ping)

```json
{
  "type": "ping"
}
```

### 3.4 서버 → 클라이언트 (Pong)

```json
{
  "type": "pong",
  "timestamp": "2025-11-06T10:30:30.000Z"
}
```

---

## 4. TypeScript 클라이언트 구현

### 4.1 useWebSocket Hook

```typescript
// frontend-dashboard/src/hooks/useWebSocket.ts
import { useState, useEffect, useRef } from 'react';

interface Alert {
  alertId: string;
  transactionId: string;
  userId: string;
  amount: number;
  currency: string;
  countryCode: string;
  ruleName: string;
  reason: string;
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
  timestamp: string;
  alertTimestamp: string;
}

type ConnectionStatus = 'connected' | 'disconnected' | 'connecting';

export const useWebSocket = (url: string) => {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [status, setStatus] = useState<ConnectionStatus>('disconnected');
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    const connect = () => {
      setStatus('connecting');

      const ws = new WebSocket(url);
      wsRef.current = ws;

      ws.onopen = () => {
        console.log('[WebSocket] Connected');
        setStatus('connected');
      };

      ws.onmessage = (event) => {
        const message = JSON.parse(event.data);

        if (message.type === 'alert') {
          const alert = message.data;
          setAlerts((prev) => [alert, ...prev].slice(0, 100)); // 최근 100개
        } else if (message.type === 'connection') {
          console.log('[WebSocket] Connection confirmed:', message.data);
        }
      };

      ws.onerror = (error) => {
        console.error('[WebSocket] Error:', error);
      };

      ws.onclose = () => {
        console.log('[WebSocket] Disconnected');
        setStatus('disconnected');
        wsRef.current = null;

        // 5초 후 재연결
        reconnectTimeoutRef.current = setTimeout(() => {
          console.log('[WebSocket] Reconnecting...');
          connect();
        }, 5000);
      };
    };

    connect();

    return () => {
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [url]);

  return { alerts, status };
};
```

---

## 5. Spring WebSocket 서버 구현

### 5.1 WebSocket Configuration

```java
// websocket-gateway/src/main/java/com/realfds/gateway/config/WebSocketConfig.java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private AlertWebSocketHandler alertWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(alertWebSocketHandler, "/ws/alerts")
                .setAllowedOrigins("*"); // 프로덕션에서는 특정 도메인만 허용
    }
}
```

### 5.2 WebSocket Handler

```java
// websocket-gateway/src/main/java/com/realfds/gateway/handler/AlertWebSocketHandler.java
@Component
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private final BroadcastService broadcastService;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("WebSocket connected: sessionId={}", session.getId());

        // 연결 확인 메시지 전송
        String message = """
            {
              "type": "connection",
              "data": {
                "status": "connected",
                "timestamp": "%s",
                "sessionId": "%s"
              }
            }
            """.formatted(Instant.now().toString(), session.getId());

        session.sendMessage(new TextMessage(message));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket disconnected: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received message: {}", payload);

        // Ping 처리
        if (payload.contains("\"type\":\"ping\"")) {
            session.sendMessage(new TextMessage("""
                {"type":"pong","timestamp":"%s"}
                """.formatted(Instant.now().toString())));
        }
    }

    public void broadcast(Alert alert) {
        String message = buildAlertMessage(alert);

        sessions.parallelStream()
                .filter(WebSocketSession::isOpen)
                .forEach(session -> {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        log.error("Failed to send message to session: {}", session.getId(), e);
                    }
                });
    }

    private String buildAlertMessage(Alert alert) {
        return """
            {
              "type": "alert",
              "data": {
                "alertId": "%s",
                "transactionId": "%s",
                "userId": "%s",
                "amount": %d,
                "currency": "%s",
                "countryCode": "%s",
                "ruleName": "%s",
                "reason": "%s",
                "severity": "%s",
                "timestamp": "%s",
                "alertTimestamp": "%s"
              }
            }
            """.formatted(
                alert.getAlertId(),
                alert.getOriginalTransaction().getTransactionId(),
                alert.getOriginalTransaction().getUserId(),
                alert.getOriginalTransaction().getAmount(),
                alert.getOriginalTransaction().getCurrency(),
                alert.getOriginalTransaction().getCountryCode(),
                alert.getRuleName(),
                alert.getReason(),
                alert.getSeverity(),
                alert.getOriginalTransaction().getTimestamp(),
                alert.getAlertTimestamp()
            );
    }
}
```

---

## 6. 에러 처리

### 6.1 클라이언트 에러
- **연결 실패**: 5초 후 자동 재연결
- **JSON 파싱 실패**: 콘솔 경고, 메시지 무시
- **네트워크 끊김**: onclose 이벤트 → 자동 재연결

### 6.2 서버 에러
- **세션 전송 실패**: 로그 기록, 해당 세션은 제거 (다음 broadcast에서 제외)
- **메모리 부족**: 최대 100개 연결 제한 (설정 가능)

---

## 7. 성능 목표

- **동시 연결 수**: 최소 10개, 최대 100개
- **메시지 지연 시간**: <500ms (alert-service에서 websocket-gateway 전송 후 클라이언트 수신)
- **브로드캐스트 처리량**: 초당 100개 메시지

---

## 8. 테스트

### 8.1 wscat 테스트

```bash
# wscat 설치
npm install -g wscat

# WebSocket 연결
wscat -c ws://localhost:8082/ws/alerts

# 연결 확인 메시지 수신
< {"type":"connection","data":{"status":"connected",...}}

# 알림 수신 대기
< {"type":"alert","data":{...}}
```

---

**WebSocket API Contract 완료**
