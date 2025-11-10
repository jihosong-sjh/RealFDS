# WebSocket API Contract: 알림 확인 및 처리 시스템

**Feature**: [../spec.md](../spec.md) | **Data Model**: [../data-model.md](../data-model.md)
**Created**: 2025-11-11

## Overview

이 문서는 알림 확인 및 처리 시스템의 WebSocket API 계약을 정의합니다. websocket-gateway가 제공하는 실시간 이벤트 스키마, 연결 프로토콜, 오류 처리를 명세합니다.

**WebSocket URL**: `ws://localhost:8082/ws/alerts` (websocket-gateway)

**프로토콜**: WebSocket (RFC 6455)

**인증**: 없음 (MVP - 인증은 향후 추가 예정)

---

## Connection

### 연결 설정

**클라이언트 → 서버 연결**:
```javascript
const ws = new WebSocket('ws://localhost:8082/ws/alerts');

ws.onopen = () => {
  console.log('WebSocket 연결 성공');
};

ws.onerror = (error) => {
  console.error('WebSocket 오류:', error);
};

ws.onclose = () => {
  console.log('WebSocket 연결 종료');
};
```

### 연결 상태

| 상태 | 설명 | 클라이언트 동작 |
|------|------|----------------|
| `CONNECTING (0)` | 연결 시도 중 | 대기 |
| `OPEN (1)` | 연결 성공 | 이벤트 수신 가능 |
| `CLOSING (2)` | 연결 종료 중 | 재연결 준비 |
| `CLOSED (3)` | 연결 종료됨 | 재연결 시도 (exponential backoff) |

### 재연결 정책

**자동 재연결** (클라이언트 구현 권장):
- 초기 대기: 1초
- 최대 대기: 30초
- Exponential backoff: 1s → 2s → 4s → 8s → 16s → 30s

```javascript
// 재연결 예시 (React hook)
function useWebSocket(url) {
  const [ws, setWs] = useState(null);
  const [reconnectDelay, setReconnectDelay] = useState(1000);

  const connect = useCallback(() => {
    const socket = new WebSocket(url);

    socket.onopen = () => {
      setReconnectDelay(1000); // 재연결 성공 시 초기화
    };

    socket.onclose = () => {
      // 재연결 시도
      setTimeout(() => {
        setReconnectDelay(prev => Math.min(prev * 2, 30000));
        connect();
      }, reconnectDelay);
    };

    setWs(socket);
  }, [url, reconnectDelay]);

  useEffect(() => {
    connect();
    return () => ws?.close();
  }, [connect]);

  return ws;
}
```

---

## Events

### 1. NEW_ALERT (기존)

**설명**: 신규 알림 발생 시 브로드캐스트 (001-realtime-fds MVP에서 제공)

**Direction**: Server → Client (브로드캐스트)

**Event Schema**:
```json
{
  "type": "NEW_ALERT",
  "alert": {
    "alertId": "550e8400-e29b-41d4-a716-446655440000",
    "originalTransaction": {
      "transactionId": "txn-12345",
      "userId": "user-001",
      "cardNumber": "1234-****-****-5678",
      "amount": 1500000,
      "merchantName": "해외 쇼핑몰",
      "country": "US",
      "timestamp": "2025-11-11T10:15:00Z"
    },
    "ruleName": "HIGH_VALUE",
    "reason": "100만원 이상 고액 거래",
    "alertTimestamp": "2025-11-11T10:15:01Z",
    "severity": "HIGH",
    "status": "UNREAD",
    "assignedTo": null,
    "actionNote": null,
    "processedAt": null
  }
}
```

**필드 변경**:
- `severity` 필드 추가 (신규)
- `status` 필드 추가 (신규, 기본값 UNREAD)
- `assignedTo` 필드 추가 (신규, 기본값 null)
- `actionNote` 필드 추가 (신규, 기본값 null)
- `processedAt` 필드 추가 (신규, 기본값 null)

**클라이언트 처리**:
```javascript
ws.onmessage = (event) => {
  const data = JSON.parse(event.data);

  if (data.type === 'NEW_ALERT') {
    // 알림 목록에 추가
    addAlert(data.alert);

    // 알림음 재생 (severity에 따라 다른 소리)
    if (data.alert.severity === 'CRITICAL') {
      playUrgentSound();
    } else {
      playNormalSound();
    }

    // 브라우저 알림 표시
    showNotification(data.alert);
  }
};
```

**트리거 조건**:
- fraud-detector가 Kafka `fraud-alerts` 토픽에 알림 발행
- websocket-gateway가 Kafka 구독 후 모든 클라이언트에 브로드캐스트

**성능 요구사항**:
- 발행 → 수신: <1초 (p95)
- 동시 연결: 최대 1000개

---

### 2. ALERT_STATUS_CHANGED (신규)

**설명**: 알림 상태 변경 시 브로드캐스트 (담당자 할당, 조치 기록 포함)

**Direction**: Server → Client (브로드캐스트)

**Event Schema**:
```json
{
  "type": "ALERT_STATUS_CHANGED",
  "alertId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "IN_PROGRESS",
  "assignedTo": "김보안",
  "actionNote": "고객 확인 중",
  "processedAt": null
}
```

**필드 정의**:

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `type` | string | 필수 | 이벤트 타입 (항상 "ALERT_STATUS_CHANGED") |
| `alertId` | string (UUID) | 필수 | 알림 고유 식별자 |
| `status` | string | 선택 | 변경된 상태 (UNREAD, IN_PROGRESS, COMPLETED) |
| `assignedTo` | string \| null | 선택 | 할당된 담당자 이름 |
| `actionNote` | string \| null | 선택 | 조치 내용 |
| `processedAt` | string \| null | 선택 | 처리 완료 시각 (ISO 8601) |

**이벤트 시나리오**:

#### 2.1. 상태만 변경
```json
{
  "type": "ALERT_STATUS_CHANGED",
  "alertId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "IN_PROGRESS",
  "assignedTo": null,
  "actionNote": null,
  "processedAt": null
}
```

#### 2.2. 담당자만 할당
```json
{
  "type": "ALERT_STATUS_CHANGED",
  "alertId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "UNREAD",
  "assignedTo": "김보안",
  "actionNote": null,
  "processedAt": null
}
```

#### 2.3. 조치 내용 기록 + 완료 처리
```json
{
  "type": "ALERT_STATUS_CHANGED",
  "alertId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "assignedTo": "김보안",
  "actionNote": "고객 확인 완료. 정상 거래로 확인됨.",
  "processedAt": "2025-11-11T10:30:00Z"
}
```

**클라이언트 처리**:
```javascript
ws.onmessage = (event) => {
  const data = JSON.parse(event.data);

  if (data.type === 'ALERT_STATUS_CHANGED') {
    // 알림 목록에서 해당 알림 찾기
    const alert = findAlertById(data.alertId);

    // 변경된 필드만 업데이트
    if (data.status !== undefined) {
      alert.status = data.status;
    }
    if (data.assignedTo !== undefined) {
      alert.assignedTo = data.assignedTo;
    }
    if (data.actionNote !== undefined) {
      alert.actionNote = data.actionNote;
    }
    if (data.processedAt !== undefined) {
      alert.processedAt = data.processedAt;
    }

    // UI 업데이트
    updateAlertInList(alert);

    // 모달이 열려 있으면 모달도 업데이트
    if (isModalOpen(data.alertId)) {
      updateAlertModal(alert);
    }
  }
};
```

**트리거 조건**:
1. **REST API 호출 시**:
   - `PATCH /api/alerts/{alertId}/status`
   - `PATCH /api/alerts/{alertId}/assign`
   - `POST /api/alerts/{alertId}/action`
2. **alert-service → Kafka 이벤트 발행**:
   - Topic: `alert-status-changed`
3. **websocket-gateway → WebSocket 브로드캐스트**:
   - 모든 연결된 클라이언트에 전송

**성능 요구사항**:
- REST API 호출 → WebSocket 수신: <1초 (p95)
- 브로드캐스트 지연: <100ms
- 동시 브로드캐스트: 최대 1000개 클라이언트

---

### 3. PING / PONG (Heartbeat)

**설명**: 연결 상태 확인을 위한 heartbeat (선택적 구현)

**Direction**: Bidirectional (양방향)

#### 3.1. PING (Server → Client)

**Event Schema**:
```json
{
  "type": "PING",
  "timestamp": "2025-11-11T10:20:00Z"
}
```

**클라이언트 처리**:
```javascript
ws.onmessage = (event) => {
  const data = JSON.parse(event.data);

  if (data.type === 'PING') {
    // PONG 응답
    ws.send(JSON.stringify({
      type: 'PONG',
      timestamp: new Date().toISOString()
    }));
  }
};
```

#### 3.2. PONG (Client → Server)

**Event Schema**:
```json
{
  "type": "PONG",
  "timestamp": "2025-11-11T10:20:00Z"
}
```

**주의**: MVP에서는 heartbeat를 구현하지 않을 수 있으며, 브라우저 기본 keep-alive에 의존

---

## Error Events

### ERROR

**설명**: 서버에서 발생한 오류를 클라이언트에 전송

**Direction**: Server → Client

**Event Schema**:
```json
{
  "type": "ERROR",
  "code": "INTERNAL_ERROR",
  "message": "서버 내부 오류가 발생했습니다",
  "timestamp": "2025-11-11T10:20:00Z"
}
```

**Error Codes**:

| 코드 | 설명 | 클라이언트 동작 |
|------|------|----------------|
| `CONNECTION_ERROR` | 연결 오류 | 재연결 시도 |
| `INTERNAL_ERROR` | 서버 내부 오류 | 오류 표시 + 재연결 |
| `KAFKA_ERROR` | Kafka 연결 오류 | 서버 복구 대기 |

**클라이언트 처리**:
```javascript
ws.onmessage = (event) => {
  const data = JSON.parse(event.data);

  if (data.type === 'ERROR') {
    console.error('WebSocket 오류:', data);
    showErrorNotification(data.message);

    // 재연결 가능한 오류인 경우
    if (data.code === 'CONNECTION_ERROR' || data.code === 'INTERNAL_ERROR') {
      ws.close(); // 재연결 트리거
    }
  }
};
```

---

## Message Format

### 공통 스키마

모든 WebSocket 메시지는 JSON 형식을 사용합니다.

```typescript
// TypeScript 타입 정의
interface WebSocketEvent {
  type: 'NEW_ALERT' | 'ALERT_STATUS_CHANGED' | 'PING' | 'PONG' | 'ERROR';
  // 이벤트 타입에 따라 추가 필드
}

interface NewAlertEvent extends WebSocketEvent {
  type: 'NEW_ALERT';
  alert: Alert;
}

interface AlertStatusChangedEvent extends WebSocketEvent {
  type: 'ALERT_STATUS_CHANGED';
  alertId: string;
  status?: AlertStatus;
  assignedTo?: string | null;
  actionNote?: string | null;
  processedAt?: string | null;
}

interface ErrorEvent extends WebSocketEvent {
  type: 'ERROR';
  code: string;
  message: string;
  timestamp: string;
}
```

---

## Client Implementation

### React Hook 예시

```typescript
// frontend-dashboard/src/hooks/useWebSocket.ts
import { useEffect, useRef, useState } from 'react';
import { Alert, WebSocketEvent } from '../types';

export function useWebSocket(url: string) {
  const ws = useRef<WebSocket | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [alerts, setAlerts] = useState<Alert[]>([]);

  useEffect(() => {
    let reconnectTimeout: NodeJS.Timeout;
    let reconnectDelay = 1000;

    const connect = () => {
      ws.current = new WebSocket(url);

      ws.current.onopen = () => {
        console.log('[WebSocket] 연결 성공');
        setIsConnected(true);
        reconnectDelay = 1000; // 초기화
      };

      ws.current.onmessage = (event) => {
        const data: WebSocketEvent = JSON.parse(event.data);

        switch (data.type) {
          case 'NEW_ALERT':
            setAlerts(prev => [data.alert, ...prev].slice(0, 100));
            break;

          case 'ALERT_STATUS_CHANGED':
            setAlerts(prev => prev.map(alert =>
              alert.alertId === data.alertId
                ? { ...alert, ...data }
                : alert
            ));
            break;

          case 'ERROR':
            console.error('[WebSocket] 오류:', data);
            break;
        }
      };

      ws.current.onerror = (error) => {
        console.error('[WebSocket] 오류:', error);
      };

      ws.current.onclose = () => {
        console.log('[WebSocket] 연결 종료 - 재연결 시도');
        setIsConnected(false);

        // 재연결
        reconnectTimeout = setTimeout(() => {
          reconnectDelay = Math.min(reconnectDelay * 2, 30000);
          connect();
        }, reconnectDelay);
      };
    };

    connect();

    return () => {
      clearTimeout(reconnectTimeout);
      ws.current?.close();
    };
  }, [url]);

  return { isConnected, alerts };
}
```

---

## Testing & Validation

### 1. 연결 테스트

**wscat 사용** (Node.js CLI 도구):
```bash
# wscat 설치
npm install -g wscat

# WebSocket 연결
wscat -c ws://localhost:8082/ws/alerts

# 이벤트 수신 대기
# (서버에서 알림 발생 시 자동 수신)
```

### 2. 신규 알림 테스트

**시나리오**:
1. transaction-generator로 거래 생성
2. fraud-detector가 알림 감지
3. WebSocket으로 NEW_ALERT 이벤트 수신

**검증**:
- 이벤트 타입 확인: `type === 'NEW_ALERT'`
- alert 객체에 severity, status 필드 포함 확인
- 브라우저에서 알림 표시 확인

### 3. 상태 변경 테스트

**시나리오**:
1. REST API로 상태 변경: `PATCH /api/alerts/{alertId}/status`
2. WebSocket으로 ALERT_STATUS_CHANGED 이벤트 수신
3. UI 자동 업데이트 확인

**검증**:
- 이벤트 타입 확인: `type === 'ALERT_STATUS_CHANGED'`
- 변경된 필드만 포함 확인
- 다른 브라우저에서도 동일하게 업데이트 확인

### 4. 재연결 테스트

**시나리오**:
1. WebSocket 연결
2. 서버 재시작 또는 네트워크 끊김
3. 클라이언트 자동 재연결 확인

**검증**:
- 재연결 시도 확인 (exponential backoff)
- 재연결 후 이벤트 정상 수신 확인

---

## Performance Requirements

### Latency

| 이벤트 | 목표 | p95 | p99 |
|--------|------|-----|-----|
| NEW_ALERT | <1초 | <2초 | <3초 |
| ALERT_STATUS_CHANGED | <500ms | <1초 | <2초 |

### Throughput

- **동시 연결**: 최대 1000개
- **메시지 처리**: 최대 1000 msg/s (브로드캐스트)
- **메모리 사용**: <512MB (websocket-gateway)

### Connection Limits

- **최대 연결 수**: 1000 (MVP 제한)
- **연결 타임아웃**: 30초 (idle)
- **메시지 최대 크기**: 64KB

---

## Security Considerations (향후)

**현재 (MVP)**:
- 인증 없음 (localhost 전용)
- 암호화 없음 (ws://)

**향후 개선**:
- JWT 토큰 기반 인증
- WSS (WebSocket Secure) 사용
- Origin 검증
- Rate limiting (연결당 메시지 제한)

---

## Monitoring & Logging

### 서버 로깅

**websocket-gateway 로그 예시**:
```json
{
  "level": "INFO",
  "message": "WebSocket 연결 수립",
  "clientId": "client-001",
  "remoteAddress": "127.0.0.1:54321",
  "timestamp": "2025-11-11T10:15:00Z"
}

{
  "level": "INFO",
  "message": "이벤트 브로드캐스트 성공",
  "eventType": "ALERT_STATUS_CHANGED",
  "alertId": "550e8400-e29b-41d4-a716-446655440000",
  "clientCount": 5,
  "latency": "50ms",
  "timestamp": "2025-11-11T10:20:00Z"
}
```

### 클라이언트 로깅

**브라우저 콘솔 로그 예시**:
```javascript
console.log('[WebSocket] 연결 성공');
console.log('[WebSocket] 이벤트 수신:', event);
console.error('[WebSocket] 오류:', error);
```

---

## Notes

- 모든 메시지는 JSON 형식
- 이벤트는 브로드캐스트 (모든 클라이언트 수신)
- 재연결 정책은 클라이언트에서 구현
- MVP에서는 인증 생략 (localhost 전용)
- heartbeat는 선택적 구현 (브라우저 keep-alive에 의존)

---

**문서 상태**: ✅ 완료
**최종 업데이트**: 2025-11-11
**테스트 필요**: websocket-gateway 구현 후 계약 준수 검증
