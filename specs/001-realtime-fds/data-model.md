# Data Model: 실시간 금융 거래 탐지 시스템

**작성일**: 2025-11-06
**Phase**: Phase 1 - Design & Contracts
**목적**: 시스템에서 사용되는 모든 데이터 엔터티의 구조, 관계, 검증 규칙 정의

---

## 1. Transaction (거래)

### 설명
금융 거래를 나타내는 핵심 엔터티로, transaction-generator에서 생성되어 Kafka `virtual-transactions` 토픽으로 발행됨

### 필드

| 필드명 | 타입 | 필수 | 설명 | 제약사항 |
|--------|------|------|------|----------|
| `schemaVersion` | String | ✅ | 스키마 버전 | 고정값: "1.0" |
| `transactionId` | String (UUID) | ✅ | 거래 고유 식별자 | UUID v4 형식, 중복 불가 |
| `userId` | String | ✅ | 거래 발생 사용자 ID | "user-1" ~ "user-10" 중 하나 |
| `amount` | Long | ✅ | 거래 금액 (정수) | 1,000 ~ 1,500,000 (KRW 기준) |
| `currency` | String | ✅ | 통화 코드 | 고정값: "KRW" (한국 원화) |
| `countryCode` | String | ✅ | 거래 발생 국가 코드 | ISO 3166-1 alpha-2, "KR", "US", "JP", "CN" 중 하나 |
| `timestamp` | String (ISO 8601) | ✅ | 거래 발생 시각 | ISO 8601 형식 (예: "2025-11-06T10:30:45.123Z") |

### JSON 스키마 예시
```json
{
  "schemaVersion": "1.0",
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-3",
  "amount": 1250000,
  "currency": "KRW",
  "countryCode": "KR",
  "timestamp": "2025-11-06T10:30:45.123Z"
}
```

### 검증 규칙
- `transactionId`는 UUID v4 형식이어야 함 (정규식: `^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$`)
- `userId`는 "user-" 접두사 + 1~10 숫자여야 함 (정규식: `^user-(1[0]|[1-9])$`)
- `amount`는 양수 정수여야 함 (1 이상)
- `currency`는 정확히 "KRW"여야 함
- `countryCode`는 2글자 대문자 알파벳이어야 함 (정규식: `^[A-Z]{2}$`)
- `timestamp`는 유효한 ISO 8601 형식이어야 하며, UTC 타임존 사용

### 상태 전이
거래 엔터티는 불변(immutable)으로, 생성 후 수정되지 않음

### 관계
- **생성자**: transaction-generator
- **소비자**: fraud-detector (Flink Job)
- **저장소**: Kafka `virtual-transactions` 토픽 (1시간 보관)

---

## 2. Alert (알림)

### 설명
탐지된 의심스러운 거래 패턴을 나타내는 알림 엔터티로, fraud-detector에서 생성되어 Kafka `transaction-alerts` 토픽으로 발행됨

### 필드

| 필드명 | 타입 | 필수 | 설명 | 제약사항 |
|--------|------|------|------|----------|
| `schemaVersion` | String | ✅ | 스키마 버전 | 고정값: "1.0" |
| `alertId` | String (UUID) | ✅ | 알림 고유 식별자 | UUID v4 형식, 중복 불가 |
| `originalTransaction` | Transaction | ✅ | 탐지된 원본 거래 | Transaction 엔터티 전체 포함 |
| `ruleType` | String | ✅ | 규칙 유형 | "SIMPLE_RULE" 또는 "STATEFUL_RULE" |
| `ruleName` | String | ✅ | 탐지 규칙 이름 | "HIGH_VALUE", "FOREIGN_COUNTRY", "HIGH_FREQUENCY" 중 하나 |
| `reason` | String | ✅ | 한국어 설명 | 사용자에게 표시될 사유 (예: "고액 거래 (100만원 초과)") |
| `severity` | String | ✅ | 심각도 | "HIGH", "MEDIUM", "LOW" 중 하나 |
| `alertTimestamp` | String (ISO 8601) | ✅ | 알림 생성 시각 | ISO 8601 형식 (UTC) |

### JSON 스키마 예시
```json
{
  "schemaVersion": "1.0",
  "alertId": "660e9511-f39c-52e5-b827-557766551111",
  "originalTransaction": {
    "schemaVersion": "1.0",
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user-3",
    "amount": 1250000,
    "currency": "KRW",
    "countryCode": "KR",
    "timestamp": "2025-11-06T10:30:45.123Z"
  },
  "ruleType": "SIMPLE_RULE",
  "ruleName": "HIGH_VALUE",
  "reason": "고액 거래 (100만원 초과): 1,250,000원",
  "severity": "HIGH",
  "alertTimestamp": "2025-11-06T10:30:47.456Z"
}
```

### 검증 규칙
- `alertId`는 UUID v4 형식이어야 함
- `originalTransaction`은 유효한 Transaction 엔터티여야 함 (위 Transaction 검증 규칙 적용)
- `ruleType`은 "SIMPLE_RULE" 또는 "STATEFUL_RULE"만 허용
- `ruleName`은 정의된 3가지 규칙명 중 하나여야 함
- `reason`은 비어있지 않아야 하며, 최대 200자
- `severity`는 "HIGH", "MEDIUM", "LOW" 중 하나여야 함
- `alertTimestamp`는 `originalTransaction.timestamp`보다 이후여야 함 (논리적 순서)

### ruleName별 매핑

| ruleName | ruleType | severity | reason 예시 |
|----------|----------|----------|-------------|
| `HIGH_VALUE` | SIMPLE_RULE | HIGH | "고액 거래 (100만원 초과): 1,250,000원" |
| `FOREIGN_COUNTRY` | SIMPLE_RULE | MEDIUM | "해외 거래 탐지 (국가: US)" |
| `HIGH_FREQUENCY` | STATEFUL_RULE | HIGH | "빈번한 거래 (1분 내 5회 초과): user-3, 6회" |

### 상태 전이
알림 엔터티는 불변(immutable)으로, 생성 후 수정되지 않음
(미래 확장: "확인됨", "처리됨" 상태 추가 가능 - 현재 Out of Scope)

### 관계
- **생성자**: fraud-detector (Flink Job)
- **소비자**: alert-service
- **저장소**:
  - Kafka `transaction-alerts` 토픽 (24시간 보관)
  - alert-service 인메모리 저장소 (최근 100개만 유지)

---

## 3. User (사용자)

### 설명
거래를 발생시키는 가상 사용자 엔터티 (실제 저장소 없음, 개념적 엔터티)

### 필드

| 필드명 | 타입 | 필수 | 설명 | 제약사항 |
|--------|------|------|------|----------|
| `userId` | String | ✅ | 사용자 고유 식별자 | "user-1" ~ "user-10" |
| `recentTransactions` | List<Transaction> | ❌ | 최근 거래 목록 (빈번한 거래 탐지용) | Flink 상태로만 존재, 영속성 없음 |

### 특징
- 사용자 엔터티는 **실제 저장소에 저장되지 않음**
- `userId`는 Transaction 엔터티의 필드로만 존재
- `recentTransactions`는 fraud-detector의 Flink State Backend (RocksDB)에서만 관리됨
- 빈번한 거래 탐지를 위해 1분 윈도우 내의 거래만 상태로 유지

### 상태 관리 (Flink)
```scala
// HighFrequencyRule에서의 사용
case class UserState(
  userId: String,
  transactions: List[Transaction], // 1분 내 거래 목록
  windowStart: Long,
  windowEnd: Long
)
```

### 관계
- **참조자**: Transaction (userId 필드)
- **상태 관리자**: fraud-detector (RocksDB State Backend)

---

## 4. DetectionRule (탐지 규칙)

### 설명
의심스러운 패턴을 정의하는 비즈니스 로직 엔터티 (코드로만 존재, 데이터 아님)

### 규칙 정의

#### 4.1 HIGH_VALUE (고액 거래)
- **유형**: SIMPLE_RULE (상태 없음)
- **조건**: `transaction.amount > 1,000,000`
- **심각도**: HIGH
- **reason 템플릿**: `"고액 거래 (100만원 초과): {amount}원"`

#### 4.2 FOREIGN_COUNTRY (해외 거래)
- **유형**: SIMPLE_RULE (상태 없음)
- **조건**: `transaction.countryCode != "KR"`
- **심각도**: MEDIUM
- **reason 템플릿**: `"해외 거래 탐지 (국가: {countryCode})"`

#### 4.3 HIGH_FREQUENCY (빈번한 거래)
- **유형**: STATEFUL_RULE (상태 있음)
- **조건**: 1분 윈도우 내 동일 userId의 거래가 5회 초과
- **심각도**: HIGH
- **reason 템플릿**: `"빈번한 거래 (1분 내 5회 초과): {userId}, {count}회"`
- **윈도우**: 1분 텀블링 윈도우 (TumblingEventTimeWindows)

### 구현 위치
- **fraud-detector**: `src/main/scala/com/realfds/detector/rules/`
  - `HighValueRule.scala`
  - `ForeignCountryRule.scala`
  - `HighFrequencyRule.scala`

### 확장 가능성
새로운 규칙 추가 시 다음을 정의해야 함:
1. `ruleName` (예: "MULTI_COUNTRY")
2. `ruleType` (SIMPLE_RULE 또는 STATEFUL_RULE)
3. `severity` (HIGH, MEDIUM, LOW)
4. 탐지 조건 (Scala FilterFunction 또는 ProcessFunction)
5. `reason` 템플릿 (한국어)

---

## 5. ConnectionStatus (연결 상태)

### 설명
프론트엔드에서 WebSocket 연결 상태를 나타내는 클라이언트 측 엔터티

### 필드

| 필드명 | 타입 | 필수 | 설명 | 가능한 값 |
|--------|------|------|------|----------|
| `status` | String | ✅ | 연결 상태 | "connected", "disconnected", "connecting" |
| `lastConnectedAt` | String (ISO 8601) | ❌ | 마지막 연결 시각 | ISO 8601 형식 (선택적) |
| `reconnectAttempts` | Number | ❌ | 재연결 시도 횟수 | 0 이상 정수 |

### TypeScript 타입 정의
```typescript
// frontend-dashboard/src/types/connectionStatus.ts
export type ConnectionStatus = 'connected' | 'disconnected' | 'connecting';

export interface ConnectionState {
  status: ConnectionStatus;
  lastConnectedAt?: string;
  reconnectAttempts: number;
}
```

### 상태 전이
```
초기 상태: disconnected
  ↓ (WebSocket 연결 시도)
connecting
  ↓ (연결 성공)
connected
  ↓ (연결 끊김)
disconnected → (5초 후 자동 재연결) → connecting
```

### 관계
- **관리자**: frontend-dashboard (`useWebSocket` hook)
- **표시**: ConnectionStatus 컴포넌트

---

## 6. 엔터티 관계도 (ERD)

```
┌─────────────────────┐
│  Transaction        │
│  (Kafka Topic)      │
│                     │
│  - transactionId PK │
│  - userId           │───┐
│  - amount           │   │
│  - countryCode      │   │
│  - timestamp        │   │
└─────────────────────┘   │
          │               │
          │ consumed by   │
          ↓               │
┌─────────────────────┐   │
│  fraud-detector     │   │
│  (Flink Job)        │   │
│                     │   │
│  [Rules]            │   │
│  - HighValueRule    │   │
│  - ForeignCountry   │   │
│  - HighFrequency    │   │ maintains state for
└─────────────────────┘   │
          │               │
          │ produces      │
          ↓               │
┌─────────────────────┐   │
│  Alert              │   │
│  (Kafka Topic)      │   │
│                     │   │
│  - alertId PK       │   │
│  - originalTx   ────┼───┘ embeds Transaction
│  - ruleName         │
│  - reason           │
│  - alertTimestamp   │
└─────────────────────┘
          │
          │ consumed by
          ↓
┌─────────────────────┐
│  alert-service      │
│  (In-Memory)        │
│                     │
│  [최근 100개 유지]   │
│  - ConcurrentDeque  │
└─────────────────────┘
          │
          │ polled by
          ↓
┌─────────────────────┐
│ websocket-gateway   │
│                     │
│ [WebSocket 연결 관리]│
└─────────────────────┘
          │
          │ pushes via WebSocket
          ↓
┌─────────────────────┐
│ frontend-dashboard  │
│                     │
│ [React State]       │
│ - alerts[]          │
│ - connectionStatus  │
└─────────────────────┘
```

---

## 7. 데이터 흐름 (Data Flow)

### 7.1 정상 흐름
```
1. transaction-generator
   → Transaction 생성
   → Kafka Producer
   → virtual-transactions 토픽

2. fraud-detector (Flink)
   → Kafka Consumer (virtual-transactions)
   → 3가지 규칙 적용
   → Alert 생성 (조건 충족 시)
   → Kafka Producer
   → transaction-alerts 토픽

3. alert-service
   → Kafka Consumer (transaction-alerts)
   → 인메모리 저장소에 추가 (최근 100개)
   → REST API (/api/alerts) 제공

4. websocket-gateway
   → alert-service REST API 폴링 (1초마다)
   → 새 알림 감지
   → 연결된 모든 WebSocket 클라이언트에 브로드캐스트

5. frontend-dashboard
   → WebSocket 연결 수립
   → 알림 수신
   → React State 업데이트
   → UI 렌더링 (AlertList 컴포넌트)
```

### 7.2 에러 흐름
```
시나리오 1: Kafka 연결 실패
  → fraud-detector가 재연결 시도 (Circuit Breaker)
  → 최대 3회 재시도, 실패 시 ERROR 로그
  → 서비스 DOWN 상태

시나리오 2: 잘못된 Transaction 데이터
  → fraud-detector에서 JSON 파싱 실패
  → WARN 로그 기록, 해당 메시지 스킵
  → Dead Letter Queue (DLQ) 전송 (추후 구현 고려)

시나리오 3: WebSocket 연결 끊김
  → frontend-dashboard에서 감지
  → ConnectionStatus를 "disconnected"로 업데이트
  → 5초 후 자동 재연결 시도
```

---

## 8. 스키마 버전 관리

### 현재 버전
- **Transaction**: v1.0
- **Alert**: v1.0

### 버전 업그레이드 전략
1. **하위 호환성 유지**: 필드 추가만 허용, 필드 제거/이름 변경 금지
2. **schemaVersion 필드 필수**: 모든 이벤트에 버전 명시
3. **점진적 배포**:
   - 새 스키마로 Consumer 먼저 배포 (이전 버전도 처리 가능하도록)
   - 새 스키마로 Producer 배포
   - 일정 기간 후 이전 버전 지원 중단

### 예시: Transaction v1.1 추가 (가상)
```json
{
  "schemaVersion": "1.1",
  "transactionId": "...",
  "userId": "...",
  "amount": 1250000,
  "currency": "KRW",
  "countryCode": "KR",
  "timestamp": "...",
  "merchantId": "merchant-123",  // 신규 필드 (선택적)
  "category": "RETAIL"            // 신규 필드 (선택적)
}
```

### 하위 호환성 처리
```scala
// fraud-detector에서 v1.0과 v1.1 모두 처리
def parseTransaction(json: String): Transaction = {
  val parsed = JSON.parse(json)
  parsed.schemaVersion match {
    case "1.0" => parseV10(parsed)
    case "1.1" => parseV11(parsed)
    case unknown => throw new UnsupportedSchemaException(s"Unknown version: $unknown")
  }
}
```

---

## 9. 데이터 검증 체크리스트

### Transaction 검증 (transaction-generator)
- [ ] `transactionId`가 UUID v4 형식인가?
- [ ] `userId`가 "user-1" ~ "user-10" 범위인가?
- [ ] `amount`가 1,000 ~ 1,500,000 범위인가?
- [ ] `currency`가 정확히 "KRW"인가?
- [ ] `countryCode`가 2글자 대문자인가?
- [ ] `timestamp`가 유효한 ISO 8601 형식인가?

### Alert 검증 (fraud-detector)
- [ ] `alertId`가 UUID v4 형식인가?
- [ ] `originalTransaction`이 유효한 Transaction인가?
- [ ] `ruleType`이 "SIMPLE_RULE" 또는 "STATEFUL_RULE"인가?
- [ ] `ruleName`이 정의된 3가지 규칙 중 하나인가?
- [ ] `reason`이 비어있지 않고 200자 이하인가?
- [ ] `severity`가 "HIGH", "MEDIUM", "LOW" 중 하나인가?
- [ ] `alertTimestamp`가 `originalTransaction.timestamp`보다 이후인가?

---

## 10. 테스트 데이터 예시

### Transaction 샘플
```json
// 정상 거래 (알림 미발생)
{
  "schemaVersion": "1.0",
  "transactionId": "111e1111-e11b-41d4-a716-111111111111",
  "userId": "user-5",
  "amount": 50000,
  "currency": "KRW",
  "countryCode": "KR",
  "timestamp": "2025-11-06T10:00:00.000Z"
}

// 고액 거래 (HIGH_VALUE 알림 발생)
{
  "schemaVersion": "1.0",
  "transactionId": "222e2222-e22b-42d4-a716-222222222222",
  "userId": "user-7",
  "amount": 1200000,
  "currency": "KRW",
  "countryCode": "KR",
  "timestamp": "2025-11-06T10:01:00.000Z"
}

// 해외 거래 (FOREIGN_COUNTRY 알림 발생)
{
  "schemaVersion": "1.0",
  "transactionId": "333e3333-e33b-43d4-a716-333333333333",
  "userId": "user-2",
  "amount": 75000,
  "currency": "KRW",
  "countryCode": "US",
  "timestamp": "2025-11-06T10:02:00.000Z"
}
```

### Alert 샘플
```json
// HIGH_VALUE 알림
{
  "schemaVersion": "1.0",
  "alertId": "aaa1111a-a11a-41a1-a111-aaa111111111",
  "originalTransaction": { /* Transaction 222e2222 */ },
  "ruleType": "SIMPLE_RULE",
  "ruleName": "HIGH_VALUE",
  "reason": "고액 거래 (100만원 초과): 1,200,000원",
  "severity": "HIGH",
  "alertTimestamp": "2025-11-06T10:01:01.234Z"
}

// FOREIGN_COUNTRY 알림
{
  "schemaVersion": "1.0",
  "alertId": "bbb2222b-b22b-42b2-b222-bbb222222222",
  "originalTransaction": { /* Transaction 333e3333 */ },
  "ruleType": "SIMPLE_RULE",
  "ruleName": "FOREIGN_COUNTRY",
  "reason": "해외 거래 탐지 (국가: US)",
  "severity": "MEDIUM",
  "alertTimestamp": "2025-11-06T10:02:01.567Z"
}

// HIGH_FREQUENCY 알림 (user-3이 1분 내 6번째 거래)
{
  "schemaVersion": "1.0",
  "alertId": "ccc3333c-c33c-43c3-c333-ccc333333333",
  "originalTransaction": { /* 6번째 Transaction */ },
  "ruleType": "STATEFUL_RULE",
  "ruleName": "HIGH_FREQUENCY",
  "reason": "빈번한 거래 (1분 내 5회 초과): user-3, 6회",
  "severity": "HIGH",
  "alertTimestamp": "2025-11-06T10:03:01.890Z"
}
```

---

**데이터 모델 정의 완료**: Phase 1 계속 진행 (contracts/, quickstart.md)
