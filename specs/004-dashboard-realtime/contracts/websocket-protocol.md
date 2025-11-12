# WebSocket Protocol Specification
# 실시간 시스템 대시보드 WebSocket 프로토콜

**Version**: 1.0
**Last Updated**: 2025-11-12
**Feature**: 004-dashboard-realtime

---

## 개요 (Overview)

본 문서는 실시간 시스템 대시보드의 WebSocket 프로토콜을 정의합니다. 서버와 클라이언트 간 양방향 통신을 통해 메트릭 데이터를 실시간으로 전송하며, 연결 끊김 시 백필(backfill) 메커니즘을 통해 누락된 데이터를 복구합니다.

**핵심 특징**:
- **양방향 통신**: 서버 → 클라이언트 (메트릭 푸시), 클라이언트 → 서버 (백필 요청)
- **브로드캐스트 패턴**: 모든 연결된 클라이언트에게 동일한 메트릭 전송
- **자동 재연결**: Exponential Backoff 전략으로 안정적인 연결 유지
- **백필 메커니즘**: 재연결 시 누락된 데이터 자동 복구

---

## 연결 엔드포인트 (Connection Endpoint)

### WebSocket URL

```
ws://localhost:8083/ws/metrics
```

**프로토콜**: WebSocket (RFC 6455)
**서버 포트**: 8083 (websocket-gateway 서비스)
**경로**: `/ws/metrics`

**Docker Compose 환경**:
```
ws://websocket-gateway:8083/ws/metrics
```

---

## 핸드셰이크 (Handshake)

### 클라이언트 → 서버 (HTTP Upgrade 요청)

```http
GET /ws/metrics HTTP/1.1
Host: localhost:8083
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13
Origin: http://localhost:3000
```

**요청 헤더**:
- `Upgrade: websocket`: WebSocket 프로토콜로 업그레이드 요청
- `Connection: Upgrade`: 연결 유지 및 프로토콜 전환
- `Sec-WebSocket-Key`: 클라이언트 생성 랜덤 키 (Base64 인코딩)
- `Sec-WebSocket-Version: 13`: WebSocket 프로토콜 버전 (RFC 6455)
- `Origin`: 요청 출처 (CORS 검증 용도, 현재는 검증 미수행)

### 서버 → 클라이언트 (Upgrade 응답)

```http
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

**응답 헤더**:
- `HTTP/1.1 101 Switching Protocols`: 프로토콜 전환 성공
- `Sec-WebSocket-Accept`: 서버가 생성한 수락 키 (SHA-1 해시)

**연결 성공 후**:
- TCP 연결이 WebSocket 프레임으로 전환
- 양방향 메시지 전송 가능
- 연결은 클라이언트 또는 서버가 명시적으로 닫을 때까지 유지

---

## 인증 (Authentication)

**현재 버전**: 인증 없음 (Constitution VI - 인증 및 권한 관리 Out of Scope)

**향후 확장 고려사항**:
- JWT 토큰 기반 인증 (`Sec-WebSocket-Protocol` 헤더 사용)
- 쿼리 파라미터를 통한 API Key 전달 (`ws://host/ws/metrics?token=xxx`)

---

## 메시지 형식 (Message Format)

### 공통 규칙

- **프로토콜**: JSON (RFC 8259)
- **인코딩**: UTF-8
- **프레임 타입**: Text Frame (WebSocket OpCode 0x1)
- **최대 메시지 크기**: 1MB (1,048,576 bytes)

**메시지 구조**:
```json
{
  "type": "MESSAGE_TYPE",
  "timestamp": "2025-11-12T10:30:05Z",
  "data": { /* 메시지별 데이터 */ }
}
```

**필수 필드**:
- `type` (string): 메시지 타입 식별자 (METRICS_UPDATE, BACKFILL_REQUEST, BACKFILL_RESPONSE, ERROR)
- `timestamp` (string, ISO-8601): 메시지 생성 시각

**선택 필드**:
- `data` (object): 메시지별 페이로드 (메시지 타입에 따라 달라짐)

---

## 메시지 타입 (Message Types)

### 1. METRICS_UPDATE (서버 → 클라이언트)

**목적**: 5초마다 최신 메트릭 데이터를 모든 클라이언트에 브로드캐스트

**전송 주기**: 5초 (고정 간격)

**메시지 예시**:
```json
{
  "type": "METRICS_UPDATE",
  "timestamp": "2025-11-12T10:30:05Z",
  "data": {
    "services": [
      {
        "serviceName": "transaction-generator",
        "status": "UP",
        "lastChecked": "2025-11-12T10:30:05Z",
        "responseTime": 45,
        "memoryUsage": 128,
        "errorType": null,
        "errorMessage": null
      },
      {
        "serviceName": "fraud-detector",
        "status": "DOWN",
        "lastChecked": "2025-11-12T10:30:02Z",
        "responseTime": null,
        "memoryUsage": null,
        "errorType": "TIMEOUT",
        "errorMessage": "Health check 응답 시간 초과 (>3초)"
      }
      // ... 나머지 3개 서비스
    ],
    "tps": {
      "current": 87,
      "average": 65,
      "max": 150,
      "history": [
        {
          "timestamp": "2025-11-12T09:30:05Z",
          "tps": 60,
          "totalTransactions": 216000
        },
        {
          "timestamp": "2025-11-12T09:30:10Z",
          "tps": 65,
          "totalTransactions": 216325
        }
        // ... 최근 1시간(720개) 데이터 포인트
      ]
    },
    "alerts": {
      "current": 12,
      "average": 8,
      "max": 25,
      "byRule": {
        "HIGH_VALUE": 5,
        "FOREIGN_COUNTRY": 4,
        "HIGH_FREQUENCY": 3
      },
      "history": [
        {
          "timestamp": "2025-11-12T09:30:05Z",
          "alertsPerMinute": 8,
          "byRule": {
            "HIGH_VALUE": 3,
            "FOREIGN_COUNTRY": 3,
            "HIGH_FREQUENCY": 2
          }
        }
        // ... 최근 1시간(720개) 데이터 포인트
      ]
    }
  }
}
```

**데이터 구조**:
- `data.services`: 5개 서비스의 현재 상태 (배열)
  - `serviceName`: 서비스 식별자 (enum: transaction-generator, fraud-detector, alert-service, websocket-gateway, frontend-dashboard)
  - `status`: 현재 상태 (enum: UP, DOWN)
  - `lastChecked`: 마지막 Health Check 시각 (ISO-8601)
  - `responseTime`: 응답 시간 (ms, DOWN 시 null)
  - `memoryUsage`: 메모리 사용량 (MB, DOWN 시 null)
  - `errorType`: 오류 유형 (enum: TIMEOUT, HTTP_ERROR, NETWORK_ERROR, UP 시 null)
  - `errorMessage`: 오류 메시지 (최대 256자, UP 시 null)

- `data.tps`: 거래량 메트릭
  - `current`: 현재 TPS (0 ≤ value ≤ 10000)
  - `average`: 1시간 평균 TPS
  - `max`: 1시간 최대 TPS
  - `history`: 최근 1시간 TPS 시계열 데이터 (배열, 최대 720개)
    - `timestamp`: 측정 시각 (5초 간격)
    - `tps`: 초당 거래 수
    - `totalTransactions`: 누적 거래 수

- `data.alerts`: 알림 발생률 메트릭
  - `current`: 현재 분당 알림 수 (0 ≤ value ≤ 1000)
  - `average`: 1시간 평균 알림 발생률
  - `max`: 1시간 최대 알림 발생률
  - `byRule`: 규칙별 알림 수 (객체)
    - `HIGH_VALUE`: 고액 거래 규칙
    - `FOREIGN_COUNTRY`: 해외 국가 규칙
    - `HIGH_FREQUENCY`: 고빈도 거래 규칙
  - `history`: 최근 1시간 알림 발생률 시계열 데이터 (배열, 최대 720개)
    - `timestamp`: 측정 시각 (5초 간격)
    - `alertsPerMinute`: 분당 알림 수
    - `byRule`: 규칙별 알림 수 (객체)

**클라이언트 처리**:
1. JSON 파싱
2. `timestamp` 저장 (백필 요청 시 사용)
3. React State 업데이트
4. 차트 및 카드 컴포넌트 리렌더링

---

### 2. BACKFILL_REQUEST (클라이언트 → 서버)

**목적**: 연결 끊김 후 재연결 시 누락된 메트릭 데이터 요청

**전송 시점**: WebSocket 재연결 직후 (onopen 이벤트 핸들러)

**메시지 예시**:
```json
{
  "type": "BACKFILL_REQUEST",
  "lastReceivedTimestamp": "2025-11-12T10:25:00Z"
}
```

**필드**:
- `lastReceivedTimestamp` (string, ISO-8601): 클라이언트가 마지막으로 수신한 METRICS_UPDATE의 timestamp

**서버 처리**:
1. `lastReceivedTimestamp` 파싱
2. `MetricsStore.getDataSince(lastReceivedTimestamp)` 호출
3. 누락된 데이터 포인트 조회
4. `BACKFILL_RESPONSE` 메시지 생성 및 전송

**제약사항**:
- `lastReceivedTimestamp`는 현재 시각으로부터 1시간 이내여야 함
- 1시간 이전 데이터는 이미 삭제되어 백필 불가능
- 1시간 이상 연결이 끊어진 경우, 전체 데이터를 새로 수신해야 함

---

### 3. BACKFILL_RESPONSE (서버 → 클라이언트)

**목적**: 누락된 메트릭 데이터 전송

**전송 시점**: BACKFILL_REQUEST 수신 후 즉시

**메시지 예시**:
```json
{
  "type": "BACKFILL_RESPONSE",
  "data": [
    {
      "timestamp": "2025-11-12T10:25:05Z",
      "tps": 65,
      "totalTransactions": 234000,
      "alertsPerMinute": 8,
      "byRule": {
        "HIGH_VALUE": 3,
        "FOREIGN_COUNTRY": 3,
        "HIGH_FREQUENCY": 2
      }
    },
    {
      "timestamp": "2025-11-12T10:25:10Z",
      "tps": 70,
      "totalTransactions": 234350,
      "alertsPerMinute": 9,
      "byRule": {
        "HIGH_VALUE": 4,
        "FOREIGN_COUNTRY": 3,
        "HIGH_FREQUENCY": 2
      }
    }
    // ... lastReceivedTimestamp 이후의 모든 데이터 포인트
  ]
}
```

**데이터 구조**:
- `data`: 누락된 메트릭 데이터 포인트 배열 (시간 순서 정렬)
  - `timestamp`: 측정 시각 (ISO-8601)
  - `tps`: 초당 거래 수
  - `totalTransactions`: 누적 거래 수
  - `alertsPerMinute`: 분당 알림 수
  - `byRule`: 규칙별 알림 수

**클라이언트 처리**:
1. `data` 배열 순회
2. 각 데이터 포인트를 차트 히스토리에 삽입
3. 중복 방지: `timestamp`를 기준으로 이미 존재하는 데이터는 스킵
4. 차트 업데이트 (부드러운 애니메이션 적용)

---

### 4. ERROR (서버 → 클라이언트)

**목적**: 서버 측 오류 발생 시 클라이언트에 통지

**전송 시점**:
- 잘못된 메시지 형식 수신 시
- BACKFILL_REQUEST 처리 실패 시
- 서버 내부 오류 발생 시

**메시지 예시**:
```json
{
  "type": "ERROR",
  "timestamp": "2025-11-12T10:30:05Z",
  "error": {
    "code": "INVALID_MESSAGE_FORMAT",
    "message": "메시지 형식이 유효하지 않습니다",
    "details": "JSON 파싱 실패: Unexpected token at position 10"
  }
}
```

**에러 코드**:
| 코드 | 설명 | HTTP 유사 코드 |
|------|------|----------------|
| `INVALID_MESSAGE_FORMAT` | 메시지 형식 오류 | 400 Bad Request |
| `BACKFILL_TOO_OLD` | 백필 요청 시각이 1시간 이상 이전 | 400 Bad Request |
| `INTERNAL_SERVER_ERROR` | 서버 내부 오류 | 500 Internal Server Error |
| `SERVICE_UNAVAILABLE` | 서비스 일시 중단 | 503 Service Unavailable |

**클라이언트 처리**:
1. 에러 코드 확인
2. 에러 메시지를 화면에 표시 (Toast 또는 Alert)
3. `BACKFILL_TOO_OLD`인 경우, 재연결 시도 중단하고 페이지 새로고침 권장

---

## 연결 수명 주기 (Connection Lifecycle)

### 1. 초기 연결 (Initial Connection)

**클라이언트 동작**:
```typescript
const ws = new WebSocket('ws://localhost:8083/ws/metrics');

ws.onopen = () => {
  console.log('WebSocket 연결 성공');
  setConnectionStatus('connected');
};
```

**서버 동작**:
1. WebSocket 핸드셰이크 수락
2. 세션을 내부 Set에 추가: `sessions.add(session)`
3. 로그 출력: `"WebSocket 연결: sessionId={}, 총 연결 수={}"`

**첫 메시지**:
- 서버는 연결 후 최대 5초 내에 첫 `METRICS_UPDATE` 메시지 전송
- 클라이언트는 첫 메시지 수신 후 `lastReceivedTimestamp` 저장

---

### 2. 정상 작동 (Normal Operation)

**서버 → 클라이언트 (5초마다)**:
```
[10:30:00] METRICS_UPDATE (서비스 상태, TPS, 알림률)
[10:30:05] METRICS_UPDATE
[10:30:10] METRICS_UPDATE
[10:30:15] METRICS_UPDATE
...
```

**클라이언트 동작**:
```typescript
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);

  if (message.type === 'METRICS_UPDATE') {
    setLastReceivedTimestamp(message.timestamp);
    updateMetrics(message.data);
  }
};
```

**데이터 플로우**:
```
[서버] MetricsScheduler (5초마다)
   ↓
[서버] MetricsStore 조회 + 집계
   ↓
[서버] JSON 직렬화
   ↓
[서버] WebSocketHandler.broadcast()
   ↓
[네트워크] WebSocket 프레임 전송
   ↓
[클라이언트] onmessage 이벤트
   ↓
[클라이언트] JSON 파싱
   ↓
[클라이언트] React State 업데이트
   ↓
[클라이언트] 차트 리렌더링
```

---

### 3. 연결 끊김 (Disconnection)

**원인**:
- 네트워크 불안정
- 서버 재시작
- 클라이언트 브라우저 탭 비활성화 (모바일에서 흔함)

**클라이언트 감지**:
```typescript
ws.onclose = (event) => {
  console.log('WebSocket 연결 종료:', event.code, event.reason);
  setConnectionStatus('disconnected');

  // 재연결 시도
  scheduleReconnect();
};

ws.onerror = (error) => {
  console.error('WebSocket 오류:', error);
  setConnectionStatus('error');
};
```

**Close 코드**:
| 코드 | 이름 | 설명 |
|------|------|------|
| 1000 | Normal Closure | 정상 종료 (클라이언트 또는 서버가 의도적으로 종료) |
| 1001 | Going Away | 서버 종료 또는 브라우저 페이지 이동 |
| 1006 | Abnormal Closure | 비정상 종료 (네트워크 끊김, TCP 타임아웃) |
| 1011 | Internal Error | 서버 내부 오류 |

---

### 4. 재연결 (Reconnection)

**Exponential Backoff 전략**:
```typescript
const reconnectDelays = [1000, 2000, 4000, 8000, 16000, 32000]; // 밀리초
let reconnectAttempt = 0;

const scheduleReconnect = () => {
  const delay = reconnectDelays[Math.min(reconnectAttempt, reconnectDelays.length - 1)];
  reconnectAttempt++;

  console.log(`${delay}ms 후 재연결 시도 (${reconnectAttempt}번째)`);

  setTimeout(() => {
    connect();
  }, delay);
};
```

**재연결 성공 후**:
```typescript
ws.onopen = () => {
  console.log('재연결 성공');
  setConnectionStatus('connected');
  reconnectAttempt = 0; // 재연결 카운터 초기화

  // 백필 요청
  if (lastReceivedTimestamp) {
    ws.send(JSON.stringify({
      type: 'BACKFILL_REQUEST',
      lastReceivedTimestamp: lastReceivedTimestamp
    }));
  }
};
```

**백필 프로세스**:
```
[클라이언트] 재연결 성공
   ↓
[클라이언트] BACKFILL_REQUEST 전송 (lastReceivedTimestamp)
   ↓
[서버] MetricsStore.getDataSince(timestamp) 조회
   ↓
[서버] BACKFILL_RESPONSE 전송 (누락된 데이터)
   ↓
[클라이언트] 누락 데이터를 차트에 삽입
   ↓
[클라이언트] 정상 작동 재개 (METRICS_UPDATE 수신)
```

---

### 5. 연결 종료 (Graceful Shutdown)

**클라이언트 측 종료**:
```typescript
// 컴포넌트 언마운트 시
useEffect(() => {
  const ws = connect();

  return () => {
    ws.close(1000, 'Component unmounted');
  };
}, []);
```

**서버 측 종료**:
```java
@PreDestroy
public void shutdown() {
    sessions.forEach(session -> {
        try {
            session.close(CloseStatus.GOING_AWAY.withReason("서버 종료"));
        } catch (IOException e) {
            log.error("세션 종료 실패: sessionId={}", session.getId(), e);
        }
    });
    log.info("모든 WebSocket 세션 종료 완료");
}
```

**Close Frame 전송**:
- OpCode: 0x8 (Close)
- Status Code: 1000 (Normal Closure)
- Reason: "서버 종료" (UTF-8 인코딩, 최대 123 bytes)

---

## Heartbeat / Ping-Pong 메커니즘

### 목적

- **연결 상태 확인**: 네트워크가 살아있는지 정기적으로 확인
- **타임아웃 감지**: 일정 시간 응답이 없으면 연결 끊김으로 간주
- **방화벽 우회**: 일부 방화벽은 idle 연결을 자동으로 종료하므로 주기적 트래픽 필요

### 서버 → 클라이언트 (Ping)

**전송 주기**: 30초 (메트릭 업데이트 5초 간격보다 긴 주기)

**서버 코드**:
```java
@Scheduled(fixedRate = 30000)
public void sendPing() {
    sessions.forEach(session -> {
        try {
            session.sendMessage(new PingMessage());
            log.trace("Ping 전송: sessionId={}", session.getId());
        } catch (IOException e) {
            log.warn("Ping 전송 실패, 세션 제거: sessionId={}", session.getId());
            sessions.remove(session);
        }
    });
}
```

**Ping 프레임**:
- OpCode: 0x9 (Ping)
- Payload: 비어있음 (또는 타임스탬프)

### 클라이언트 → 서버 (Pong)

**자동 응답**: 브라우저의 WebSocket API가 자동으로 Pong 프레임 응답

**Pong 프레임**:
- OpCode: 0xA (Pong)
- Payload: Ping의 Payload를 그대로 반환

**타임아웃 감지**:
```java
@Override
protected void handlePongMessage(WebSocketSession session, PongMessage message) {
    log.trace("Pong 수신: sessionId={}", session.getId());
    // 마지막 Pong 시각 업데이트
    lastPongTime.put(session.getId(), Instant.now());
}

@Scheduled(fixedRate = 60000)
public void checkTimeouts() {
    Instant timeout = Instant.now().minus(Duration.ofSeconds(90));
    sessions.removeIf(session -> {
        Instant lastPong = lastPongTime.get(session.getId());
        if (lastPong != null && lastPong.isBefore(timeout)) {
            log.warn("세션 타임아웃, 연결 종료: sessionId={}", session.getId());
            return true;
        }
        return false;
    });
}
```

---

## 에러 처리 (Error Handling)

### 클라이언트 측 에러

| 에러 상황 | 감지 방법 | 처리 방법 |
|-----------|-----------|-----------|
| 연결 실패 | `onerror` 이벤트 | "연결 끊김" 배너 표시, 재연결 시도 |
| 연결 끊김 | `onclose` 이벤트 | Exponential Backoff 재연결 |
| JSON 파싱 오류 | `try-catch` | 에러 로그 출력, 메시지 무시 |
| 타임스탬프 검증 실패 | 로직 검증 | 경고 로그, 데이터 무시 |
| 메모리 부족 | 브라우저 경고 | 1시간 이전 데이터 자동 삭제 |

**에러 처리 코드 예시**:
```typescript
ws.onmessage = (event) => {
  try {
    const message = JSON.parse(event.data);

    if (message.type === 'ERROR') {
      handleServerError(message.error);
      return;
    }

    if (message.type === 'METRICS_UPDATE') {
      // 타임스탬프 검증
      const messageTime = new Date(message.timestamp).getTime();
      const now = Date.now();

      if (Math.abs(now - messageTime) > 60000) {
        console.warn('메시지 타임스탬프가 현재 시각과 1분 이상 차이:', message.timestamp);
      }

      updateMetrics(message.data);
      setLastReceivedTimestamp(message.timestamp);
    }
  } catch (error) {
    console.error('메시지 처리 오류:', error, event.data);
  }
};
```

### 서버 측 에러

| 에러 상황 | 감지 방법 | 처리 방법 |
|-----------|-----------|-----------|
| 잘못된 메시지 형식 | JSON 파싱 예외 | ERROR 메시지 전송, 연결 유지 |
| 백필 범위 초과 | 타임스탬프 검증 | ERROR (BACKFILL_TOO_OLD) 전송 |
| 메트릭 수집 실패 | 예외 캐치 | 에러 로그, 이전 데이터 재전송 |
| WebSocket 전송 실패 | `IOException` | 해당 세션 제거 |

**에러 처리 코드 예시**:
```java
@Override
protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    try {
        BackfillRequest request = objectMapper.readValue(message.getPayload(), BackfillRequest.class);

        Instant lastReceived = Instant.parse(request.getLastReceivedTimestamp());
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));

        if (lastReceived.isBefore(cutoff)) {
            sendError(session, "BACKFILL_TOO_OLD",
                "백필 요청 시각이 1시간 이상 이전입니다. 페이지를 새로고침하세요.");
            return;
        }

        List<MetricsDataPoint> backfillData = metricsStore.getDataSince(lastReceived);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
            new BackfillResponse(backfillData)
        )));

    } catch (JsonProcessingException e) {
        log.error("메시지 파싱 오류: sessionId={}, payload={}", session.getId(), message.getPayload(), e);
        sendError(session, "INVALID_MESSAGE_FORMAT", "메시지 형식이 유효하지 않습니다");
    } catch (IOException e) {
        log.error("메시지 전송 오류: sessionId={}", session.getId(), e);
        sessions.remove(session);
    }
}

private void sendError(WebSocketSession session, String code, String message) {
    try {
        ErrorMessage error = ErrorMessage.builder()
            .type("ERROR")
            .timestamp(Instant.now())
            .error(ErrorDetails.builder()
                .code(code)
                .message(message)
                .build())
            .build();

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
    } catch (IOException e) {
        log.error("에러 메시지 전송 실패: sessionId={}", session.getId(), e);
    }
}
```

---

## 성능 특성 (Performance Characteristics)

### 메시지 크기

| 메시지 타입 | 평균 크기 | 최대 크기 |
|-------------|-----------|-----------|
| METRICS_UPDATE | ~100 KB | ~150 KB (720개 데이터 포인트) |
| BACKFILL_REQUEST | ~100 bytes | ~200 bytes |
| BACKFILL_RESPONSE | ~50 KB | ~100 KB (누락된 데이터) |
| ERROR | ~500 bytes | ~1 KB |

**압축 고려사항**:
- 현재: 압축 미사용 (텍스트 JSON)
- 향후: `permessage-deflate` 확장 사용 시 약 70% 크기 감소 가능

### 전송 빈도

- **METRICS_UPDATE**: 5초마다 (고정 간격)
- **Ping**: 30초마다 (Heartbeat)
- **BACKFILL_REQUEST/RESPONSE**: 재연결 시 1회

**대역폭 사용량** (클라이언트 1명):
```
메시지 크기: 100 KB
전송 주기: 5초
대역폭: 100 KB / 5초 = 20 KB/s = 160 Kbps

1시간 데이터 전송량: 20 KB/s × 3600초 = 72 MB
```

**서버 대역폭** (동시 접속자 5명):
```
총 대역폭: 20 KB/s × 5명 = 100 KB/s = 800 Kbps
1시간 데이터 전송량: 360 MB (5명 합계)
```

### 지연 시간 (Latency)

| 단계 | 지연 시간 |
|------|-----------|
| 메트릭 수집 (서버) | ~100 ms |
| JSON 직렬화 (서버) | ~10 ms |
| WebSocket 전송 (네트워크) | ~50 ms (로컬), ~200 ms (인터넷) |
| JSON 파싱 (클라이언트) | ~5 ms |
| React 렌더링 (클라이언트) | ~50 ms |
| **총 지연 시간** | **~215 ms** (로컬), **~365 ms** (인터넷) |

**실시간성 보장**:
- FR-003: 5초마다 자동 업데이트
- SC-001: 평균 4초, 최대 5초 이내 완료
- 실제 측정: 약 5.2초 (5초 수집 주기 + 0.2초 전송 지연)

---

## 보안 고려사항 (Security Considerations)

### 현재 구현

- **인증 없음**: Constitution VI에 따라 인증 및 권한 관리는 Out of Scope
- **암호화 없음**: `ws://` 프로토콜 사용 (평문 전송)
- **CORS 미검증**: Origin 헤더 검증 생략

### 향후 보안 강화 (Production 배포 시)

1. **WSS 프로토콜 사용** (WebSocket over TLS/SSL):
   ```
   wss://dashboard.example.com/ws/metrics
   ```
   - 중간자 공격(MITM) 방지
   - 데이터 암호화 (AES-256)

2. **JWT 토큰 인증**:
   ```typescript
   const ws = new WebSocket('wss://host/ws/metrics', ['jwt', token]);
   ```
   - 서버에서 `Sec-WebSocket-Protocol` 헤더 검증
   - 토큰 만료 시 연결 종료 (Close Code 1008 - Policy Violation)

3. **Origin 검증**:
   ```java
   @Override
   public void afterConnectionEstablished(WebSocketSession session) {
       String origin = session.getHandshakeHeaders().getOrigin();
       if (!allowedOrigins.contains(origin)) {
           session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid origin"));
           return;
       }
       sessions.add(session);
   }
   ```

4. **Rate Limiting**:
   - 클라이언트당 최대 연결 수 제한 (1개)
   - 재연결 시도 제한 (5분 내 최대 10회)

---

## 테스트 시나리오 (Testing Scenarios)

### 단위 테스트

**WebSocket Handler**:
```java
@Test
void testBroadcastMetrics() {
    // Given
    WebSocketSession session1 = mock(WebSocketSession.class);
    WebSocketSession session2 = mock(WebSocketSession.class);
    handler.sessions.add(session1);
    handler.sessions.add(session2);

    // When
    handler.broadcastMetrics(metricsUpdate);

    // Then
    verify(session1, times(1)).sendMessage(any(TextMessage.class));
    verify(session2, times(1)).sendMessage(any(TextMessage.class));
}
```

### 통합 테스트

**백필 프로세스**:
```typescript
test('재연결 시 누락된 데이터 백필', async () => {
  // Given
  const ws = new WebSocket('ws://localhost:8083/ws/metrics');
  let lastTimestamp = null;

  ws.onmessage = (event) => {
    const message = JSON.parse(event.data);
    if (message.type === 'METRICS_UPDATE') {
      lastTimestamp = message.timestamp;
    }
  };

  await waitForConnection(ws);
  await wait(10000); // 10초 동안 메시지 수신

  // When: 연결 끊김 및 재연결
  ws.close();
  await wait(5000); // 5초 대기 (누락 데이터 발생)

  const ws2 = new WebSocket('ws://localhost:8083/ws/metrics');
  ws2.onopen = () => {
    ws2.send(JSON.stringify({
      type: 'BACKFILL_REQUEST',
      lastReceivedTimestamp: lastTimestamp
    }));
  };

  // Then: 백필 응답 수신
  ws2.onmessage = (event) => {
    const message = JSON.parse(event.data);
    expect(message.type).toBe('BACKFILL_RESPONSE');
    expect(message.data.length).toBeGreaterThan(0);
  };
});
```

---

## 참고 자료 (References)

### 표준 문서
- [RFC 6455 - The WebSocket Protocol](https://datatracker.ietf.org/doc/html/rfc6455)
- [WebSocket API (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [Spring WebSocket Support](https://docs.spring.io/spring-framework/reference/web/websocket.html)

### 모범 사례
- [WebSocket Architecture Best Practices - Ably](https://ably.com/topic/websocket-architecture-best-practices)
- [Real-time Dashboard with WebSockets - Tutorial](https://ably.com/blog/websockets-react-tutorial)
- [Exponential Backoff and Jitter - AWS Architecture Blog](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/)

---

**문서 완료**: 이 WebSocket 프로토콜 명세는 Phase 1 (Design & Contracts)의 핵심 문서로, 백엔드 및 프론트엔드 구현의 기준이 됩니다.
