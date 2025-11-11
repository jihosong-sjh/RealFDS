# Alert Service (알림 저장 및 관리)

**서비스 종류**: Reactive 백엔드 서비스
**기술 스택**: Spring Boot 3.2+, Spring WebFlux
**역할**: 알림 데이터 저장 및 REST API 제공

---

## 목적

Kafka에서 탐지된 알림을 소비하여 인메모리 저장소에 저장하고, REST API를 통해 다른 서비스(websocket-gateway)와 프론트엔드에 제공하는 서비스입니다. 알림 상태 관리, 담당자 할당, 조치 내용 기록 등의 기능을 제공합니다.

---

## 주요 책임

- **Kafka Consumer**: `transaction-alerts` 토픽 구독
- **Kafka Producer**: `alert-status-changed` 토픽에 상태 변경 이벤트 발행
- **인메모리 저장소**: ConcurrentLinkedDeque (최근 100개만 유지)
- **REST API 제공**:
  - `GET /api/alerts`: 최근 알림 목록 조회 (필터링 및 정렬 지원)
  - `GET /api/alerts/{alertId}`: 특정 알림 상세 조회
  - `PATCH /api/alerts/{alertId}/status`: 알림 상태 변경
  - `PATCH /api/alerts/{alertId}/assign`: 담당자 할당
  - `POST /api/alerts/{alertId}/action`: 조치 내용 기록
  - `GET /actuator/health`: 헬스 체크
- **알림 상태 관리**: UNREAD, IN_PROGRESS, COMPLETED 상태 전이
- **담당자 할당**: 알림에 담당자 이름 할당 (최대 100자)
- **조치 내용 기록**: 처리 내역 저장 (최대 2000자)
- **Reactive 처리**: Spring WebFlux (Non-blocking I/O)

---

## 입력/출력

### 입력
- **Kafka 토픽**: `transaction-alerts` (Alert JSON - 신규 알림 수신)

### 출력
- **REST API**:
  - `GET /api/alerts` (알림 목록 - 필터링/정렬 지원)
  - `GET /api/alerts/{alertId}` (알림 상세)
  - `PATCH /api/alerts/{alertId}/status` (상태 변경 응답)
  - `PATCH /api/alerts/{alertId}/assign` (담당자 할당 응답)
  - `POST /api/alerts/{alertId}/action` (조치 기록 응답)
- **Kafka 토픽**: `alert-status-changed` (상태 변경 이벤트 발행)

---

## 로컬 실행

```bash
cd alert-service
./gradlew clean build
./gradlew bootRun
```

---

## API 테스트

### 기본 조회

```bash
# 헬스 체크
curl http://localhost:8081/actuator/health

# 알림 목록 조회
curl http://localhost:8081/api/alerts

# 특정 알림 상세 조회
curl http://localhost:8081/api/alerts/{alertId}
```

### 필터링 및 정렬

```bash
# 미확인 알림만 조회
curl "http://localhost:8081/api/alerts?status=UNREAD"

# 높은 심각도 알림만 조회
curl "http://localhost:8081/api/alerts?severity=HIGH"

# 담당자별 알림 조회
curl "http://localhost:8081/api/alerts?assignedTo=김보안"

# 심각도순 정렬
curl "http://localhost:8081/api/alerts?sortBy=severity"

# 복합 필터 (미확인 + 높은 심각도)
curl "http://localhost:8081/api/alerts?status=UNREAD&severity=HIGH"
```

### 상태 관리

```bash
# 알림 상태를 "확인중"으로 변경
curl -X PATCH http://localhost:8081/api/alerts/{alertId}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'

# 알림 상태를 "완료"로 변경
curl -X PATCH http://localhost:8081/api/alerts/{alertId}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}'
```

### 담당자 할당

```bash
# 담당자 할당
curl -X PATCH http://localhost:8081/api/alerts/{alertId}/assign \
  -H "Content-Type: application/json" \
  -d '{"assignedTo": "김보안"}'
```

### 조치 내용 기록

```bash
# 조치 내용만 기록
curl -X POST http://localhost:8081/api/alerts/{alertId}/action \
  -H "Content-Type: application/json" \
  -d '{"actionNote": "고객에게 확인 메일 발송"}'

# 조치 내용 기록 + 완료 처리
curl -X POST http://localhost:8081/api/alerts/{alertId}/action \
  -H "Content-Type: application/json" \
  -d '{
    "actionNote": "고객 확인 완료. 정상 거래로 확인됨.",
    "status": "COMPLETED"
  }'
```

---

## 환경 변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Kafka 브로커 주소 |
| `SERVER_PORT` | `8081` | 서버 포트 |

---

## 데이터 모델

### Alert 객체 (확장)

```json
{
  "alertId": "550e8400-e29b-41d4-a716-446655440000",
  "originalTransaction": { /* ... */ },
  "ruleName": "HIGH_VALUE",
  "reason": "100만원 이상 고액 거래",
  "alertTimestamp": "2025-11-11T10:15:01Z",
  "severity": "HIGH",
  "status": "UNREAD",
  "assignedTo": null,
  "actionNote": null,
  "processedAt": null
}
```

### 상태 (AlertStatus)

- **UNREAD**: 미확인 (기본값)
- **IN_PROGRESS**: 확인중
- **COMPLETED**: 완료

### 심각도 (Severity)

- **LOW**: 낮음 (파란색)
- **MEDIUM**: 보통 (노란색)
- **HIGH**: 높음 (주황색)
- **CRITICAL**: 긴급 (빨간색)

---

## 상태 관리 로직

### 상태 전이 규칙

```
UNREAD → IN_PROGRESS → COMPLETED
   ↓          ↓             ↓
   ←──────────←─────────────←
```

- **UNREAD → IN_PROGRESS**: 알림 확인 시작
- **IN_PROGRESS → COMPLETED**: 조치 완료
- **COMPLETED → IN_PROGRESS**: 재검토 필요 시
- **역방향 전이 가능**: 상태 되돌리기 지원

### 자동 처리

- **COMPLETED 상태 시**: `processedAt` 필드 자동 설정 (현재 시각)
- **Kafka 이벤트 자동 발행**: 상태 변경 시 `alert-status-changed` 토픽

---

## 참고 문서

- [전체 시스템 아키텍처](../docs/architecture.md)
- [데이터 모델 상세 (001-realtime-fds)](../specs/001-realtime-fds/data-model.md)
- [데이터 모델 상세 (002-alert-management)](../specs/002-alert-management/data-model.md)
- [REST API 계약](../specs/002-alert-management/contracts/rest-api.md)
- [빠른 시작 가이드](../specs/002-alert-management/quickstart.md)
- [개발 가이드](../docs/development.md)
