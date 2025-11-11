# Data Model: Alert History (과거 알림 조회)

**Feature**: 003-alert-history
**Date**: 2025-11-11
**Status**: Completed

## Overview

이 문서는 Alert History 기능의 데이터 모델을 정의합니다. PostgreSQL 데이터베이스 스키마, 엔티티 관계, 검증 규칙, 상태 전이를 포함합니다.

---

## Core Entities

### 1. Alert (알림)

**설명**: 탐지된 사기 의심 거래에 대한 알림 정보를 저장하는 핵심 엔티티

#### 속성 (Attributes)

| 필드명 | 타입 | 제약 조건 | 설명 |
|--------|-----|---------|-----|
| `alertId` | UUID | PRIMARY KEY, NOT NULL | 알림 고유 식별자 |
| `schemaVersion` | VARCHAR(10) | NOT NULL | 스키마 버전 (예: "1.0") |
| `transactionId` | UUID | NOT NULL | 원본 거래 ID |
| `userId` | VARCHAR(50) | NOT NULL | 사용자 ID (user-1 ~ user-10) |
| `amount` | BIGINT | NOT NULL | 거래 금액 (KRW, 정수) |
| `currency` | VARCHAR(3) | NOT NULL | 통화 코드 (예: KRW) |
| `countryCode` | VARCHAR(2) | NOT NULL | 국가 코드 (예: KR, US, JP, CN) |
| `ruleName` | VARCHAR(100) | NOT NULL | 탐지 규칙명 (HIGH_AMOUNT, FOREIGN_COUNTRY, RAPID_TRANSACTION) |
| `reason` | TEXT | NOT NULL | 알림 발생 이유 (한국어 설명) |
| `severity` | VARCHAR(10) | NOT NULL | 심각도 (HIGH, MEDIUM, LOW) |
| `alertTimestamp` | TIMESTAMP WITH TIME ZONE | NOT NULL | 알림 발생 시각 (ISO 8601) |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'UNREAD' | 알림 처리 상태 (UNREAD, IN_PROGRESS, COMPLETED) |
| `assignedTo` | VARCHAR(50) | NULL | 담당자 ID (선택적) |
| `actionNote` | TEXT | NULL | 처리 내용 메모 (선택적) |
| `processedAt` | TIMESTAMP WITH TIME ZONE | NULL | 처리 완료 시각 (선택적) |
| `createdAt` | TIMESTAMP WITH TIME ZONE | NOT NULL, DEFAULT NOW() | 데이터베이스 생성 시각 |

#### 인덱스 (Indexes)

| 인덱스명 | 필드 | 타입 | 목적 |
|---------|-----|-----|-----|
| `pk_alerts` | `alertId` | PRIMARY KEY | 고유 식별 |
| `idx_alert_timestamp` | `alertTimestamp DESC` | B-Tree | 날짜 범위 검색, 최신 순 정렬 |
| `idx_rule_name` | `ruleName` | B-Tree | 규칙명 필터링 |
| `idx_user_id` | `userId` | B-Tree | 사용자별 필터링 |
| `idx_status` | `status` | B-Tree | 상태 필터링 |

#### 검증 규칙 (Validation Rules)

- `alertId`: 유효한 UUID v4 형식
- `schemaVersion`: "1.0" 고정 (현재 버전)
- `transactionId`: 유효한 UUID v4 형식
- `userId`: 영문자, 숫자, 하이픈만 허용 (예: user-1, user-10)
- `amount`: 양수 (> 0), 최대값: 9,223,372,036,854,775,807 (BIGINT 범위)
- `currency`: 3자리 대문자 (예: KRW, USD, JPY)
- `countryCode`: 2자리 대문자 (예: KR, US, JP)
- `ruleName`: Enum 값만 허용 (HIGH_AMOUNT, FOREIGN_COUNTRY, RAPID_TRANSACTION)
- `reason`: 최소 10자 이상, 최대 1000자
- `severity`: Enum 값만 허용 (HIGH, MEDIUM, LOW)
- `alertTimestamp`: 과거 또는 현재 시각 (미래 불가)
- `status`: Enum 값만 허용 (UNREAD, IN_PROGRESS, COMPLETED)
- `assignedTo`: 영문자, 숫자, 하이픈만 허용 (선택적)
- `actionNote`: 최대 2000자 (선택적)
- `processedAt`: `status=COMPLETED` 일 때 필수
- `createdAt`: 자동 생성 (수정 불가)

#### 상태 전이 (State Transitions)

```
UNREAD (초기 상태)
  ↓ (보안 담당자가 알림을 확인하고 조사 시작)
IN_PROGRESS
  ↓ (조사 완료 또는 오탐 확인)
COMPLETED (최종 상태)
```

**전이 규칙**:
- `UNREAD → IN_PROGRESS`: 담당자 할당 시 (`assignedTo` 설정)
- `IN_PROGRESS → COMPLETED`: 처리 완료 시 (`actionNote` + `processedAt` 설정)
- `UNREAD → COMPLETED`: 즉시 처리 (오탐 확인 등)
- 역방향 전이 불가: `COMPLETED → IN_PROGRESS` 또는 `COMPLETED → UNREAD` 금지

#### Kotlin Entity 예시

```kotlin
@Table("alerts")
data class Alert(
    @Id
    val alertId: UUID = UUID.randomUUID(),

    val schemaVersion: String = "1.0",

    val transactionId: UUID,
    val userId: String,
    val amount: Long,
    val currency: String,
    val countryCode: String,

    val ruleName: String,
    val reason: String,
    val severity: Severity,

    val alertTimestamp: Instant,
    val status: AlertStatus = AlertStatus.UNREAD,

    val assignedTo: String? = null,
    val actionNote: String? = null,
    val processedAt: Instant? = null,

    val createdAt: Instant = Instant.now()
)

enum class AlertStatus {
    UNREAD, IN_PROGRESS, COMPLETED
}

enum class Severity {
    HIGH, MEDIUM, LOW
}
```

---

### 2. AlertSearchCriteria (검색 조건)

**설명**: 알림 검색 시 사용되는 필터 조건 (Value Object)

#### 속성 (Attributes)

| 필드명 | 타입 | 제약 조건 | 기본값 | 설명 |
|--------|-----|---------|-------|-----|
| `startDate` | Instant | NULL | 7일 전 | 검색 시작 날짜 (포함) |
| `endDate` | Instant | NULL | 현재 | 검색 종료 날짜 (포함) |
| `ruleName` | String | NULL | - | 규칙명 필터 |
| `userId` | String | NULL | - | 사용자 ID 필터 |
| `status` | AlertStatus | NULL | - | 상태 필터 |
| `page` | Int | ≥ 0 | 0 | 페이지 번호 (0부터 시작) |
| `size` | Int | 1 ≤ size ≤ 100 | 50 | 페이지 크기 |

#### 검증 규칙 (Validation Rules)

- `startDate` ≤ `endDate`: 시작 날짜가 종료 날짜보다 늦을 수 없음
- `startDate` ≤ NOW(): 미래 날짜 불가
- `endDate` ≤ NOW(): 미래 날짜 불가
- `ruleName`: Enum 값만 허용 (선택적)
- `userId`: 영문자, 숫자, 하이픈만 허용 (선택적)
- `status`: Enum 값만 허용 (선택적)
- `page`: 음수 불가
- `size`: 1 ~ 100 범위

#### Kotlin Data Class 예시

```kotlin
data class AlertSearchCriteria(
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    val ruleName: String? = null,
    val userId: String? = null,
    val status: AlertStatus? = null,
    val page: Int = 0,
    val size: Int = 50
) {
    init {
        require(page >= 0) { "페이지 번호는 0 이상이어야 합니다" }
        require(size in 1..100) { "페이지 크기는 1~100 사이여야 합니다" }

        if (startDate != null && endDate != null) {
            require(startDate <= endDate) { "시작 날짜는 종료 날짜보다 늦을 수 없습니다" }
        }

        val now = Instant.now()
        if (startDate != null) {
            require(startDate <= now) { "시작 날짜는 미래일 수 없습니다" }
        }
        if (endDate != null) {
            require(endDate <= now) { "종료 날짜는 미래일 수 없습니다" }
        }
    }
}
```

---

### 3. PagedAlertResult (페이지네이션 결과)

**설명**: 검색 결과와 페이지네이션 메타데이터를 포함하는 응답 객체

#### 속성 (Attributes)

| 필드명 | 타입 | 설명 |
|--------|-----|-----|
| `content` | List<Alert> | 현재 페이지의 알림 목록 |
| `totalElements` | Long | 전체 알림 개수 |
| `totalPages` | Int | 전체 페이지 수 |
| `currentPage` | Int | 현재 페이지 번호 (0부터 시작) |
| `pageSize` | Int | 페이지 크기 |
| `hasNext` | Boolean | 다음 페이지 존재 여부 |
| `hasPrevious` | Boolean | 이전 페이지 존재 여부 |

#### Kotlin Data Class 예시

```kotlin
data class PagedAlertResult(
    val content: List<Alert>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int
) {
    val hasNext: Boolean = currentPage < totalPages - 1
    val hasPrevious: Boolean = currentPage > 0
}
```

#### JSON Response 예시

```json
{
  "content": [
    {
      "alertId": "550e8400-e29b-41d4-a716-446655440000",
      "schemaVersion": "1.0",
      "transactionId": "660e8400-e29b-41d4-a716-446655440001",
      "userId": "user-5",
      "amount": 1500000,
      "currency": "KRW",
      "countryCode": "KR",
      "ruleName": "HIGH_AMOUNT",
      "reason": "거래 금액이 설정된 임계값(1,000,000원)을 초과했습니다",
      "severity": "HIGH",
      "alertTimestamp": "2025-11-11T12:34:56Z",
      "status": "UNREAD",
      "assignedTo": null,
      "actionNote": null,
      "processedAt": null,
      "createdAt": "2025-11-11T12:34:57Z"
    }
  ],
  "totalElements": 1250,
  "totalPages": 25,
  "currentPage": 0,
  "pageSize": 50,
  "hasNext": true,
  "hasPrevious": false
}
```

---

## Database Schema

### PostgreSQL DDL (Flyway Migration)

**파일 경로**: `alert-dashboard/backend/src/main/resources/db/migration/V1__create_alerts_table.sql`

```sql
-- Alert History 테이블 생성
CREATE TABLE alerts (
    alert_id UUID PRIMARY KEY,
    schema_version VARCHAR(10) NOT NULL,

    -- 거래 정보
    transaction_id UUID NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    country_code VARCHAR(2) NOT NULL,

    -- 탐지 규칙 정보
    rule_name VARCHAR(100) NOT NULL,
    reason TEXT NOT NULL CHECK (LENGTH(reason) BETWEEN 10 AND 1000),
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('HIGH', 'MEDIUM', 'LOW')),

    -- 타임스탬프
    alert_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,

    -- 상태 관리
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD' CHECK (status IN ('UNREAD', 'IN_PROGRESS', 'COMPLETED')),
    assigned_to VARCHAR(50),
    action_note TEXT CHECK (LENGTH(action_note) <= 2000),
    processed_at TIMESTAMP WITH TIME ZONE,

    -- 생성 시각
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- 제약 조건
    CONSTRAINT check_processed_at CHECK (
        (status = 'COMPLETED' AND processed_at IS NOT NULL) OR
        (status != 'COMPLETED')
    )
);

-- 인덱스 생성
CREATE INDEX idx_alert_timestamp ON alerts(alert_timestamp DESC);
CREATE INDEX idx_rule_name ON alerts(rule_name);
CREATE INDEX idx_user_id ON alerts(user_id);
CREATE INDEX idx_status ON alerts(status);

-- 코멘트 추가
COMMENT ON TABLE alerts IS '사기 탐지 알림 이력';
COMMENT ON COLUMN alerts.alert_id IS '알림 고유 식별자';
COMMENT ON COLUMN alerts.schema_version IS '스키마 버전';
COMMENT ON COLUMN alerts.transaction_id IS '원본 거래 ID';
COMMENT ON COLUMN alerts.user_id IS '사용자 ID';
COMMENT ON COLUMN alerts.amount IS '거래 금액 (KRW)';
COMMENT ON COLUMN alerts.currency IS '통화 코드';
COMMENT ON COLUMN alerts.country_code IS '국가 코드';
COMMENT ON COLUMN alerts.rule_name IS '탐지 규칙명';
COMMENT ON COLUMN alerts.reason IS '알림 발생 이유 (한국어)';
COMMENT ON COLUMN alerts.severity IS '심각도 (HIGH, MEDIUM, LOW)';
COMMENT ON COLUMN alerts.alert_timestamp IS '알림 발생 시각';
COMMENT ON COLUMN alerts.status IS '처리 상태 (UNREAD, IN_PROGRESS, COMPLETED)';
COMMENT ON COLUMN alerts.assigned_to IS '담당자 ID';
COMMENT ON COLUMN alerts.action_note IS '처리 내용 메모';
COMMENT ON COLUMN alerts.processed_at IS '처리 완료 시각';
COMMENT ON COLUMN alerts.created_at IS '데이터베이스 생성 시각';
```

### Sample Data (테스트용)

**파일 경로**: `alert-dashboard/backend/src/main/resources/db/migration/V2__insert_sample_alerts.sql`

```sql
-- 샘플 알림 데이터 삽입 (개발/테스트 환경 전용)
INSERT INTO alerts (
    alert_id, schema_version, transaction_id, user_id, amount, currency, country_code,
    rule_name, reason, severity, alert_timestamp, status, created_at
) VALUES
    (
        '550e8400-e29b-41d4-a716-446655440001', '1.0',
        '660e8400-e29b-41d4-a716-446655440001', 'user-5',
        1500000, 'KRW', 'KR',
        'HIGH_AMOUNT', '거래 금액이 설정된 임계값(1,000,000원)을 초과했습니다',
        'HIGH', '2025-11-11T12:00:00Z', 'UNREAD', '2025-11-11T12:00:01Z'
    ),
    (
        '550e8400-e29b-41d4-a716-446655440002', '1.0',
        '660e8400-e29b-41d4-a716-446655440002', 'user-3',
        850000, 'KRW', 'CN',
        'FOREIGN_COUNTRY', '해외(CN) 국가에서 거래가 발생했습니다',
        'MEDIUM', '2025-11-11T11:30:00Z', 'IN_PROGRESS', '2025-11-11T11:30:01Z'
    ),
    (
        '550e8400-e29b-41d4-a716-446655440003', '1.0',
        '660e8400-e29b-41d4-a716-446655440003', 'user-7',
        200000, 'KRW', 'KR',
        'RAPID_TRANSACTION', '1분 동안 5건 이상의 거래가 발생했습니다',
        'LOW', '2025-11-11T10:45:00Z', 'COMPLETED', '2025-11-11T10:45:01Z'
    );
```

---

## Entity Relationships

### ERD (Entity Relationship Diagram)

```
┌─────────────────────────────────────────────────────────────┐
│                        alerts (Table)                        │
├─────────────────────────────────────────────────────────────┤
│ PK  alert_id: UUID                                           │
│     schema_version: VARCHAR(10)                              │
│                                                              │
│     transaction_id: UUID                                     │
│     user_id: VARCHAR(50)                                     │
│     amount: BIGINT                                           │
│     currency: VARCHAR(3)                                     │
│     country_code: VARCHAR(2)                                 │
│                                                              │
│     rule_name: VARCHAR(100)                                  │
│     reason: TEXT                                             │
│     severity: VARCHAR(10)                                    │
│                                                              │
│     alert_timestamp: TIMESTAMP WITH TIME ZONE                │
│     status: VARCHAR(20)                                      │
│     assigned_to: VARCHAR(50) [NULLABLE]                      │
│     action_note: TEXT [NULLABLE]                             │
│     processed_at: TIMESTAMP WITH TIME ZONE [NULLABLE]        │
│     created_at: TIMESTAMP WITH TIME ZONE                     │
│                                                              │
│ Indexes:                                                     │
│   - idx_alert_timestamp (alert_timestamp DESC)               │
│   - idx_rule_name (rule_name)                                │
│   - idx_user_id (user_id)                                    │
│   - idx_status (status)                                      │
└─────────────────────────────────────────────────────────────┘
```

**참고**: 현재 Alert History 기능은 단일 테이블로 구성됩니다. 향후 확장 시 다음과 같은 관계를 고려할 수 있습니다:
- `alerts` ↔ `transactions` (1:N): 하나의 거래에 여러 알림 발생 가능
- `alerts` ↔ `users` (N:1): 여러 알림이 한 사용자와 연관
- `alerts` ↔ `audit_logs` (1:N): 알림 상태 변경 이력 추적

---

## Query Patterns

### 1. 날짜 범위 검색

```sql
SELECT * FROM alerts
WHERE alert_timestamp BETWEEN :startDate AND :endDate
ORDER BY alert_timestamp DESC
LIMIT :limit OFFSET :offset;
```

**인덱스 사용**: `idx_alert_timestamp`

---

### 2. 규칙명 필터링

```sql
SELECT * FROM alerts
WHERE rule_name = :ruleName
ORDER BY alert_timestamp DESC
LIMIT :limit OFFSET :offset;
```

**인덱스 사용**: `idx_rule_name`

---

### 3. 다중 조건 검색 (날짜 + 규칙명 + 상태)

```sql
SELECT * FROM alerts
WHERE alert_timestamp BETWEEN :startDate AND :endDate
  AND rule_name = :ruleName
  AND status = :status
ORDER BY alert_timestamp DESC
LIMIT :limit OFFSET :offset;
```

**인덱스 사용**: `idx_alert_timestamp` (주 인덱스), `idx_rule_name`, `idx_status` (보조 인덱스)

---

### 4. 전체 개수 조회 (페이지네이션)

```sql
SELECT COUNT(*) FROM alerts
WHERE alert_timestamp BETWEEN :startDate AND :endDate
  AND rule_name = :ruleName
  AND status = :status;
```

---

### 5. 상태별 통계 (향후 Analytics 기능)

```sql
SELECT status, COUNT(*) as count
FROM alerts
WHERE alert_timestamp >= :startDate
GROUP BY status;
```

---

## Data Migration Strategy

### Phase 1: 초기 마이그레이션 (V1)
- `alerts` 테이블 생성
- 인덱스 생성
- 제약 조건 설정

### Phase 2: 샘플 데이터 (V2, 개발 환경 전용)
- 테스트용 샘플 알림 삽입

### Phase 3: 기존 데이터 마이그레이션 (해당 없음)
- 현재 인메모리에 알림 데이터가 저장되지 않으므로 마이그레이션 불필요
- 새로운 알림부터 PostgreSQL에 저장

### 향후 스키마 변경 (예시)
- V3: `alert_history` 테이블 추가 (상태 변경 이력 추적)
- V4: `alerts.priority` 컬럼 추가 (우선순위 필드)

---

## Performance Considerations

### 인덱스 성능

| 쿼리 패턴 | 예상 성능 (10,000개 기준) | 인덱스 |
|----------|------------------------|--------|
| 날짜 범위 검색 | <50ms | `idx_alert_timestamp` |
| 규칙명 필터링 | <30ms | `idx_rule_name` |
| 사용자 ID 필터링 | <30ms | `idx_user_id` |
| 상태 필터링 | <30ms | `idx_status` |
| 복합 검색 (날짜+규칙+상태) | <100ms | 다중 인덱스 사용 |

### 스케일 업 전략 (100,000개 이상)

1. **복합 인덱스 추가**:
   ```sql
   CREATE INDEX idx_alert_timestamp_rule_name ON alerts(alert_timestamp DESC, rule_name);
   ```

2. **파티셔닝**:
   ```sql
   -- 월별 파티셔닝
   CREATE TABLE alerts_2025_11 PARTITION OF alerts
   FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
   ```

3. **아카이빙**:
   - 90일 이상 오래된 알림을 별도 테이블로 이동
   - `alerts_archive` 테이블 생성

---

## Data Integrity Rules

### 1. 참조 무결성
- 현재 외래 키 없음 (느슨한 결합)
- 향후 `transactions` 테이블 추가 시 외래 키 고려

### 2. 도메인 무결성
- `amount`: 양수 제약 (`CHECK (amount > 0)`)
- `status`: Enum 값 제약 (`CHECK (status IN (...))`)
- `severity`: Enum 값 제약 (`CHECK (severity IN (...))`)

### 3. 비즈니스 규칙
- `status=COMPLETED` 일 때 `processed_at` 필수 (`CONSTRAINT check_processed_at`)
- `reason` 최소 10자 이상 (`CHECK (LENGTH(reason) BETWEEN 10 AND 1000)`)

---

## Testing Data Requirements

### 단위 테스트
- 최소 데이터셋: 10개 알림 (다양한 규칙, 상태, 날짜)
- 엣지 케이스: 빈 결과, 단일 결과, 대량 결과 (1,000개)

### 통합 테스트
- 중간 데이터셋: 100개 알림
- 다양한 검색 조건 조합 테스트

### 성능 테스트
- 대규모 데이터셋: 10,000개 알림
- 인덱스 효과 검증

### 테스트 데이터 생성 스크립트 (Kotlin)

```kotlin
fun generateTestAlerts(count: Int): List<Alert> {
    val ruleNames = listOf("HIGH_AMOUNT", "FOREIGN_COUNTRY", "RAPID_TRANSACTION")
    val statuses = listOf(AlertStatus.UNREAD, AlertStatus.IN_PROGRESS, AlertStatus.COMPLETED)
    val severities = listOf(Severity.HIGH, Severity.MEDIUM, Severity.LOW)

    return (1..count).map { i ->
        Alert(
            alertId = UUID.randomUUID(),
            transactionId = UUID.randomUUID(),
            userId = "user-${(i % 10) + 1}",
            amount = (100_000L..2_000_000L).random(),
            currency = "KRW",
            countryCode = listOf("KR", "US", "JP", "CN").random(),
            ruleName = ruleNames.random(),
            reason = "테스트 알림 #$i",
            severity = severities.random(),
            alertTimestamp = Instant.now().minus(Duration.ofHours(i.toLong())),
            status = statuses.random()
        )
    }
}
```

---

## References

- [PostgreSQL Data Types](https://www.postgresql.org/docs/15/datatype.html)
- [Spring Data R2DBC Entities](https://docs.spring.io/spring-data/r2dbc/reference/r2dbc/entity-persistence.html)
- [UUID Best Practices](https://www.postgresql.org/docs/15/datatype-uuid.html)

---

**Data Model Status**: ✅ Completed
**Author**: Project Team
**Last Updated**: 2025-11-11
