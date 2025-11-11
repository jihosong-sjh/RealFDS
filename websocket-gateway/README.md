# WebSocket Gateway (WebSocket 연결 관리)

**서비스 종류**: Reactive WebSocket 게이트웨이
**기술 스택**: Spring Boot 3.2+, Spring WebFlux, Spring WebSocket
**역할**: 실시간 알림 브로드캐스트

---

## 목적

alert-service로부터 알림을 폴링하여 연결된 모든 WebSocket 클라이언트에 실시간으로 브로드캐스트하는 서비스입니다.

---

## 주요 책임

- **WebSocket 연결 관리**: `ws://localhost:8082/ws/alerts` 엔드포인트
- **Kafka Consumer**: `alert-status-changed` 토픽 구독 (상태 변경 이벤트)
- **실시간 브로드캐스트**: 모든 활성 WebSocket 세션에 알림 및 상태 변경 전송
- **이벤트 타입 지원**:
  - `NEW_ALERT`: 신규 알림 (fraud-detector → Kafka → websocket-gateway)
  - `ALERT_STATUS_CHANGED`: 상태 변경 (alert-service → Kafka → websocket-gateway)
- **자동 세션 정리**: 전송 실패 시 해당 세션 제거

---

## WebSocket API

### 엔드포인트

```
ws://localhost:8082/ws/alerts
```

### 메시지 형식 (서버 → 클라이언트)

#### NEW_ALERT (신규 알림)

```json
{
  "type": "NEW_ALERT",
  "alert": {
    "alertId": "660e9511-f39c-52e5-b827-557766551111",
    "originalTransaction": { ... },
    "ruleName": "HIGH_VALUE",
    "reason": "고액 거래 (100만원 초과): 1,250,000원",
    "severity": "HIGH",
    "status": "UNREAD",
    "assignedTo": null,
    "actionNote": null,
    "processedAt": null,
    "alertTimestamp": "2025-11-06T10:30:47.456Z"
  }
}
```

#### ALERT_STATUS_CHANGED (상태 변경)

```json
{
  "type": "ALERT_STATUS_CHANGED",
  "alertId": "660e9511-f39c-52e5-b827-557766551111",
  "status": "IN_PROGRESS",
  "assignedTo": "김보안",
  "actionNote": "고객 확인 중",
  "processedAt": null
}
```

---

## 로컬 실행

```bash
cd websocket-gateway
./gradlew clean build
./gradlew bootRun
```

---

## WebSocket 연결 테스트

```bash
# wscat 설치
npm install -g wscat

# WebSocket 연결
wscat -c ws://localhost:8082/ws/alerts

# 연결 성공 시 실시간 알림 수신
```

---

## 환경 변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `ALERT_SERVICE_URL` | `http://alert-service:8081` | alert-service 주소 |
| `SERVER_PORT` | `8082` | 서버 포트 |

---

## 이벤트 스키마

### NEW_ALERT

**설명**: 신규 알림 발생 시 브로드캐스트

**필드**:
- `type`: "NEW_ALERT"
- `alert`: Alert 객체 (전체 정보 포함)
  - `alertId`: 알림 ID
  - `severity`: 심각도 (LOW, MEDIUM, HIGH, CRITICAL)
  - `status`: 상태 (기본값: UNREAD)
  - `assignedTo`: 담당자 (기본값: null)
  - `actionNote`: 조치 내용 (기본값: null)
  - `processedAt`: 처리 시각 (기본값: null)

### ALERT_STATUS_CHANGED

**설명**: 알림 상태 변경 시 브로드캐스트 (담당자 할당, 조치 기록 포함)

**필드**:
- `type`: "ALERT_STATUS_CHANGED"
- `alertId`: 변경된 알림 ID
- `status`: 변경된 상태 (UNREAD, IN_PROGRESS, COMPLETED)
- `assignedTo`: 할당된 담당자 이름 (null 가능)
- `actionNote`: 조치 내용 (null 가능)
- `processedAt`: 처리 완료 시각 (COMPLETED 시 자동 설정)

**트리거 조건**:
1. `PATCH /api/alerts/{alertId}/status` 호출
2. `PATCH /api/alerts/{alertId}/assign` 호출
3. `POST /api/alerts/{alertId}/action` 호출

**성능**:
- REST API → WebSocket 수신: <1초 (p95)
- 브로드캐스트 지연: <100ms

---

## 클라이언트 구현 예시

### React Hook

```typescript
// frontend-dashboard/src/hooks/useWebSocket.ts
ws.onmessage = (event) => {
  const data = JSON.parse(event.data);

  switch (data.type) {
    case 'NEW_ALERT':
      // 신규 알림 추가
      setAlerts(prev => [data.alert, ...prev].slice(0, 100));
      break;

    case 'ALERT_STATUS_CHANGED':
      // 기존 알림 업데이트
      setAlerts(prev => prev.map(alert =>
        alert.alertId === data.alertId
          ? { ...alert, ...data }
          : alert
      ));
      break;
  }
};
```

---

## 참고 문서

- [전체 시스템 아키텍처](../docs/architecture.md)
- [데이터 모델 상세 (001-realtime-fds)](../specs/001-realtime-fds/data-model.md)
- [데이터 모델 상세 (002-alert-management)](../specs/002-alert-management/data-model.md)
- [WebSocket API 계약](../specs/002-alert-management/contracts/websocket-api.md)
- [빠른 시작 가이드](../specs/002-alert-management/quickstart.md)
- [개발 가이드](../docs/development.md)
