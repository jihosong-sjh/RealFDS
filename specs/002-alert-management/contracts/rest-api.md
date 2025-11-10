# REST API Contract: 알림 확인 및 처리 시스템

**Feature**: [../spec.md](../spec.md) | **Data Model**: [../data-model.md](../data-model.md)
**Created**: 2025-11-11

## Overview

이 문서는 알림 확인 및 처리 시스템의 REST API 계약을 정의합니다. alert-service가 제공하는 모든 엔드포인트의 요청/응답 스키마, 오류 처리, 성능 요구사항을 명세합니다.

**Base URL**: `http://localhost:8081` (alert-service)

**공통 헤더**:
- `Content-Type: application/json`
- `Accept: application/json`

**인증**: 없음 (MVP - 인증은 향후 추가 예정)

---

## Endpoints

### 1. 알림 목록 조회 (확장)

**기존 엔드포인트 확장** - 필터링 파라미터 추가

#### Request

```http
GET /api/alerts?status={status}&assignedTo={assignedTo}&severity={severity}&sortBy={sortBy}
```

**Query Parameters** (모두 선택적):

| 파라미터 | 타입 | 필수 | 기본값 | 설명 | 예시 |
|---------|------|------|--------|------|------|
| `status` | string | 선택 | - | 상태 필터 (UNREAD, IN_PROGRESS, COMPLETED) | `?status=UNREAD` |
| `assignedTo` | string | 선택 | - | 담당자 필터 (정확히 일치) | `?assignedTo=김보안` |
| `severity` | string | 선택 | - | 심각도 필터 (LOW, MEDIUM, HIGH, CRITICAL) | `?severity=HIGH` |
| `sortBy` | string | 선택 | `alertTimestamp` | 정렬 기준 (alertTimestamp, severity) | `?sortBy=severity` |

**필터 조합 예시**:
```http
# 미확인 + 높은 심각도
GET /api/alerts?status=UNREAD&severity=HIGH

# 담당자 "김보안" + 확인중
GET /api/alerts?assignedTo=김보안&status=IN_PROGRESS

# 심각도순 정렬
GET /api/alerts?sortBy=severity
```

#### Response

**Status**: `200 OK`

**Body**:
```json
{
  "alerts": [
    {
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
    },
    {
      "alertId": "660e8400-e29b-41d4-a716-446655440001",
      "originalTransaction": { /* ... */ },
      "ruleName": "FOREIGN_COUNTRY",
      "reason": "해외 국가에서 거래 발생",
      "alertTimestamp": "2025-11-11T10:16:00Z",
      "severity": "MEDIUM",
      "status": "IN_PROGRESS",
      "assignedTo": "김보안",
      "actionNote": "고객 확인 중",
      "processedAt": null
    }
  ],
  "total": 2,
  "filters": {
    "status": "UNREAD",
    "assignedTo": null,
    "severity": "HIGH",
    "sortBy": "alertTimestamp"
  }
}
```

**필드 설명**:
- `alerts`: Alert 객체 배열 (최대 100개, FIFO 제한)
- `total`: 필터링된 결과 개수
- `filters`: 적용된 필터 정보 (디버깅용)

#### Errors

| Status | 오류 코드 | 설명 | 예시 |
|--------|----------|------|------|
| 400 | `INVALID_QUERY_PARAM` | 유효하지 않은 쿼리 파라미터 | `?status=INVALID` |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 | - |

**Error Response Example**:
```json
{
  "error": "INVALID_QUERY_PARAM",
  "message": "유효하지 않은 상태 값입니다: INVALID",
  "timestamp": "2025-11-11T10:20:00Z"
}
```

#### Performance

- **응답 시간**: <100ms (100개 알림 기준)
- **동시 요청**: 최대 100 req/s

---

### 2. 알림 상태 변경

알림의 처리 상태를 변경합니다.

#### Request

```http
PATCH /api/alerts/{alertId}/status
```

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `alertId` | string (UUID) | 필수 | 알림 고유 식별자 |

**Body**:
```json
{
  "status": "IN_PROGRESS"
}
```

**Body Fields**:

| 필드 | 타입 | 필수 | 허용 값 | 설명 |
|------|------|------|---------|------|
| `status` | string | 필수 | UNREAD, IN_PROGRESS, COMPLETED | 새로운 상태 |

**상태 전이 규칙**:
- UNREAD → IN_PROGRESS, COMPLETED
- IN_PROGRESS → COMPLETED, UNREAD
- COMPLETED → IN_PROGRESS

#### Response

**Status**: `200 OK`

**Body**:
```json
{
  "alertId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "IN_PROGRESS",
  "processedAt": null
}
```

**COMPLETED 상태로 변경 시**:
```json
{
  "alertId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "processedAt": "2025-11-11T10:30:00Z"  // 자동 설정
}
```

#### Errors

| Status | 오류 코드 | 설명 |
|--------|----------|------|
| 400 | `INVALID_STATUS` | 유효하지 않은 상태 값 |
| 404 | `ALERT_NOT_FOUND` | 알림을 찾을 수 없음 |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |

**Error Response Examples**:

```json
// 404 - Alert Not Found
{
  "error": "ALERT_NOT_FOUND",
  "message": "알림을 찾을 수 없습니다: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-11-11T10:20:00Z"
}

// 400 - Invalid Status
{
  "error": "INVALID_STATUS",
  "message": "유효하지 않은 상태 값입니다: INVALID",
  "timestamp": "2025-11-11T10:20:00Z"
}
```

#### Performance

- **응답 시간**: <50ms (평균), <200ms (p95)
- **동시 요청**: 최대 50 req/s

#### Side Effects

1. **Kafka 이벤트 발행**:
   - Topic: `alert-status-changed`
   - Event: `{ alertId, status, processedAt }`
2. **WebSocket 브로드캐스트**:
   - 모든 연결된 클라이언트에 상태 변경 알림

---

### 3. 담당자 할당

알림에 담당자를 할당합니다.

#### Request

```http
PATCH /api/alerts/{alertId}/assign
```

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `alertId` | string (UUID) | 필수 | 알림 고유 식별자 |

**Body**:
```json
{
  "assignedTo": "김보안"
}
```

**Body Fields**:

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `assignedTo` | string | 필수 | 최대 100자 | 담당자 이름 (한글 약 50자) |

#### Response

**Status**: `200 OK`

**Body**:
```json
{
  "alertId": "550e8400-e29b-41d4-a716-446655440000",
  "assignedTo": "김보안"
}
```

#### Errors

| Status | 오류 코드 | 설명 |
|--------|----------|------|
| 400 | `ASSIGNEE_TOO_LONG` | 담당자 이름이 100자 초과 |
| 400 | `INVALID_REQUEST` | assignedTo 필드 누락 |
| 404 | `ALERT_NOT_FOUND` | 알림을 찾을 수 없음 |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |

**Error Response Example**:
```json
{
  "error": "ASSIGNEE_TOO_LONG",
  "message": "담당자 이름은 100자를 초과할 수 없습니다",
  "timestamp": "2025-11-11T10:20:00Z"
}
```

#### Performance

- **응답 시간**: <50ms (평균)
- **동시 요청**: 최대 50 req/s

#### Side Effects

1. **Kafka 이벤트 발행**:
   - Topic: `alert-status-changed`
   - Event: `{ alertId, assignedTo }`
2. **WebSocket 브로드캐스트**:
   - 담당자 할당 알림

---

### 4. 조치 내용 기록

알림에 조치 내용을 기록하고, 선택적으로 완료 처리합니다.

#### Request

```http
POST /api/alerts/{alertId}/action
```

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `alertId` | string (UUID) | 필수 | 알림 고유 식별자 |

**Body**:
```json
{
  "actionNote": "고객 확인 완료. 정상 거래로 확인되어 별도 조치 불필요.",
  "status": "COMPLETED"
}
```

**Body Fields**:

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `actionNote` | string | 필수 | 최대 2000자 | 조치 내용 (한글 약 1000자) |
| `status` | string | 선택 | COMPLETED만 허용 | 완료 처리 여부 (생략 시 상태 변경 없음) |

**사용 시나리오**:
1. **조치 내용만 기록** (상태 변경 없음):
   ```json
   {
     "actionNote": "고객에게 확인 메일 발송"
   }
   ```

2. **조치 내용 기록 + 완료 처리**:
   ```json
   {
     "actionNote": "고객 확인 완료. 정상 거래.",
     "status": "COMPLETED"
   }
   ```

#### Response

**Status**: `200 OK`

**Body** (조치 내용만 기록):
```json
{
  "alertId": "550e8400-e29b-41d4-a716-446655440000",
  "actionNote": "고객에게 확인 메일 발송",
  "status": "IN_PROGRESS",
  "processedAt": null
}
```

**Body** (완료 처리 포함):
```json
{
  "alertId": "550e8400-e29b-41d4-a716-446655440000",
  "actionNote": "고객 확인 완료. 정상 거래.",
  "status": "COMPLETED",
  "processedAt": "2025-11-11T10:30:00Z"  // 자동 설정
}
```

#### Errors

| Status | 오류 코드 | 설명 |
|--------|----------|------|
| 400 | `ACTION_NOTE_TOO_LONG` | 조치 내용이 2000자 초과 |
| 400 | `INVALID_REQUEST` | actionNote 필드 누락 |
| 400 | `INVALID_STATUS` | status 필드가 COMPLETED 아님 |
| 404 | `ALERT_NOT_FOUND` | 알림을 찾을 수 없음 |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |

**Error Response Examples**:

```json
// 400 - Action Note Too Long
{
  "error": "ACTION_NOTE_TOO_LONG",
  "message": "조치 내용은 2000자를 초과할 수 없습니다",
  "timestamp": "2025-11-11T10:20:00Z"
}

// 400 - Invalid Status
{
  "error": "INVALID_STATUS",
  "message": "status 필드는 'COMPLETED'만 허용됩니다",
  "timestamp": "2025-11-11T10:20:00Z"
}
```

#### Performance

- **응답 시간**: <50ms (평균)
- **동시 요청**: 최대 50 req/s

#### Side Effects

1. **Kafka 이벤트 발행**:
   - Topic: `alert-status-changed`
   - Event: `{ alertId, actionNote, status, processedAt }`
2. **WebSocket 브로드캐스트**:
   - 조치 내용 및 상태 변경 알림

---

### 5. 알림 상세 조회 (기존)

특정 알림의 상세 정보를 조회합니다.

#### Request

```http
GET /api/alerts/{alertId}
```

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `alertId` | string (UUID) | 필수 | 알림 고유 식별자 |

#### Response

**Status**: `200 OK`

**Body**:
```json
{
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
  "status": "COMPLETED",
  "assignedTo": "김보안",
  "actionNote": "고객 확인 완료. 정상 거래로 확인됨.",
  "processedAt": "2025-11-11T10:30:00Z"
}
```

#### Errors

| Status | 오류 코드 | 설명 |
|--------|----------|------|
| 404 | `ALERT_NOT_FOUND` | 알림을 찾을 수 없음 |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |

#### Performance

- **응답 시간**: <20ms (평균)
- **동시 요청**: 최대 100 req/s

---

## Common Response Schemas

### Alert Object

```json
{
  "alertId": "string (UUID)",
  "originalTransaction": {
    "transactionId": "string",
    "userId": "string",
    "cardNumber": "string",
    "amount": "number",
    "merchantName": "string",
    "country": "string (ISO 3166-1 alpha-2)",
    "timestamp": "string (ISO 8601)"
  },
  "ruleName": "string",
  "reason": "string",
  "alertTimestamp": "string (ISO 8601)",
  "severity": "LOW | MEDIUM | HIGH | CRITICAL",
  "status": "UNREAD | IN_PROGRESS | COMPLETED",
  "assignedTo": "string | null",
  "actionNote": "string | null",
  "processedAt": "string (ISO 8601) | null"
}
```

### Error Object

```json
{
  "error": "string (error code)",
  "message": "string (user-friendly message)",
  "timestamp": "string (ISO 8601)",
  "details": "object (optional, additional debug info)"
}
```

---

## Error Codes

| 코드 | HTTP Status | 설명 | 해결 방법 |
|------|-------------|------|----------|
| `ALERT_NOT_FOUND` | 404 | 알림을 찾을 수 없음 | alertId 확인 |
| `INVALID_STATUS` | 400 | 유효하지 않은 상태 값 | UNREAD, IN_PROGRESS, COMPLETED 중 하나 사용 |
| `INVALID_QUERY_PARAM` | 400 | 유효하지 않은 쿼리 파라미터 | 파라미터 값 확인 |
| `ASSIGNEE_TOO_LONG` | 400 | 담당자 이름 100자 초과 | 이름 길이 줄이기 |
| `ACTION_NOTE_TOO_LONG` | 400 | 조치 내용 2000자 초과 | 내용 길이 줄이기 |
| `INVALID_REQUEST` | 400 | 필수 필드 누락 | 요청 body 확인 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 | 서버 로그 확인 |

---

## Rate Limiting

**전역 제한**:
- 최대 100 req/s (IP당)
- 초과 시 `429 Too Many Requests` 응답

**엔드포인트별 제한**:
- `GET /api/alerts`: 100 req/s
- `GET /api/alerts/{alertId}`: 100 req/s
- `PATCH /api/alerts/{alertId}/status`: 50 req/s
- `PATCH /api/alerts/{alertId}/assign`: 50 req/s
- `POST /api/alerts/{alertId}/action`: 50 req/s

**주의**: MVP에서는 rate limiting을 구현하지 않으며, 향후 프로덕션에서 추가 예정

---

## CORS Configuration

**허용된 Origin**:
- `http://localhost:5173` (frontend-dashboard 개발 서버)
- `http://localhost:3000` (대체 프론트엔드 포트)

**허용된 Methods**:
- `GET`, `POST`, `PATCH`, `OPTIONS`

**허용된 Headers**:
- `Content-Type`, `Accept`

---

## Testing & Validation

### 1. 상태 관리 테스트 시나리오

```bash
# 1. 알림 상태를 IN_PROGRESS로 변경
curl -X PATCH http://localhost:8081/api/alerts/{alertId}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'

# 2. 상태 변경 확인
curl http://localhost:8081/api/alerts/{alertId}

# 3. COMPLETED로 변경 (processedAt 자동 설정 확인)
curl -X PATCH http://localhost:8081/api/alerts/{alertId}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}'
```

### 2. 담당자 할당 테스트

```bash
# 1. 담당자 할당
curl -X PATCH http://localhost:8081/api/alerts/{alertId}/assign \
  -H "Content-Type: application/json" \
  -d '{"assignedTo": "김보안"}'

# 2. 담당자별 필터링
curl "http://localhost:8081/api/alerts?assignedTo=김보안"
```

### 3. 조치 내용 기록 테스트

```bash
# 1. 조치 내용 기록 + 완료 처리
curl -X POST http://localhost:8081/api/alerts/{alertId}/action \
  -H "Content-Type: application/json" \
  -d '{
    "actionNote": "고객 확인 완료. 정상 거래.",
    "status": "COMPLETED"
  }'

# 2. 완료 처리 확인 (processedAt 자동 설정 확인)
curl http://localhost:8081/api/alerts/{alertId}
```

### 4. 필터링 및 정렬 테스트

```bash
# 1. 미확인 알림 필터링
curl "http://localhost:8081/api/alerts?status=UNREAD"

# 2. 높은 심각도 알림 필터링
curl "http://localhost:8081/api/alerts?severity=HIGH"

# 3. 심각도순 정렬
curl "http://localhost:8081/api/alerts?sortBy=severity"

# 4. 복합 필터 (미확인 + 높은 심각도)
curl "http://localhost:8081/api/alerts?status=UNREAD&severity=HIGH"
```

---

## Performance Requirements

### Response Time SLA

| 엔드포인트 | 평균 | p95 | p99 | 목표 |
|-----------|------|-----|-----|------|
| `GET /api/alerts` | <50ms | <100ms | <200ms | <100ms |
| `GET /api/alerts/{id}` | <20ms | <50ms | <100ms | <20ms |
| `PATCH .../status` | <30ms | <50ms | <100ms | <50ms |
| `PATCH .../assign` | <30ms | <50ms | <100ms | <50ms |
| `POST .../action` | <30ms | <50ms | <100ms | <50ms |

### Throughput

- **최대 동시 요청**: 100 req/s (전체)
- **알림 개수**: 최대 100개 (FIFO 제한)
- **필터링 성능**: O(n) 선형 탐색 (n=100, <10ms)

---

## OpenAPI Specification

**OpenAPI 3.0 스키마** (별도 파일 생성 권장):

```yaml
# specs/002-alert-management/contracts/openapi.yaml
openapi: 3.0.3
info:
  title: Alert Management API
  version: 1.0.0
  description: 알림 확인 및 처리 시스템 REST API
servers:
  - url: http://localhost:8081
    description: alert-service (로컬 개발)
paths:
  /api/alerts:
    get:
      summary: 알림 목록 조회 (필터링 지원)
      # ... (상세 스키마 생략)
  /api/alerts/{alertId}/status:
    patch:
      summary: 알림 상태 변경
      # ... (상세 스키마 생략)
  # ... (기타 엔드포인트)
```

---

## Notes

- 모든 타임스탬프는 ISO 8601 형식 (UTC)
- 알림 개수는 최대 100개로 제한 (인메모리 FIFO)
- 인증은 MVP에서 생략, 향후 JWT 기반 인증 추가 예정
- Rate limiting은 MVP에서 생략, 향후 추가 예정
- 상태 변경 시 Kafka 이벤트 자동 발행 (WebSocket 동기화)

---

**문서 상태**: ✅ 완료
**최종 업데이트**: 2025-11-11
**테스트 필요**: alert-service 구현 후 계약 준수 검증
