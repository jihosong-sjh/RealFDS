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
- **alert-service 폴링**: 1초마다 REST API 호출
- **실시간 브로드캐스트**: 모든 활성 WebSocket 세션에 알림 전송
- **자동 세션 정리**: 전송 실패 시 해당 세션 제거

---

## WebSocket API

### 엔드포인트

```
ws://localhost:8082/ws/alerts
```

### 메시지 형식 (서버 → 클라이언트)

```json
{
  "schemaVersion": "1.0",
  "alertId": "660e9511-f39c-52e5-b827-557766551111",
  "originalTransaction": { ... },
  "ruleName": "HIGH_VALUE",
  "reason": "고액 거래 (100만원 초과): 1,250,000원",
  "severity": "HIGH",
  "alertTimestamp": "2025-11-06T10:30:47.456Z"
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

## 참고 문서

- [전체 시스템 아키텍처](../docs/architecture.md)
- [데이터 모델 상세](../specs/001-realtime-fds/data-model.md)
- [개발 가이드](../docs/development.md)
