# Data Model: 알림 확인 및 처리 시스템 (Alert Management System)

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)
**Created**: 2025-11-11

## Overview

이 문서는 알림 확인 및 처리 시스템의 데이터 모델을 정의합니다. 기존 MVP(001-realtime-fds)의 Alert 모델을 확장하고, DetectionRule에 심각도 필드를 추가하여 알림 상태 관리, 담당자 할당, 조치 기록, 우선순위 표시 기능을 지원합니다.

## Entities

### 1. Alert (확장)

**설명**: 거래 알림 엔터티 - 기존 모델에 상태 관리 필드 추가

**위치**:
- Backend: `alert-service/src/main/java/com/realfds/alert/model/Alert.java`
- Frontend: `frontend-dashboard/src/types/alert.ts`

#### 필드 정의

| 필드명 | 타입 | 필수 | 기본값 | 설명 | 변경 |
|--------|------|------|--------|------|------|
| `alertId` | String (UUID) | 필수 | auto-generated | 알림 고유 식별자 | 기존 |
| `originalTransaction` | Transaction | 필수 | - | 원본 거래 정보 | 기존 |
| `ruleName` | String | 필수 | - | 탐지 규칙 이름 (예: HIGH_VALUE, FOREIGN_COUNTRY) | 기존 |
| `reason` | String | 필수 | - | 탐지 사유 (한국어) | 기존 |
| `alertTimestamp` | Timestamp | 필수 | auto-generated | 알림 생성 시각 | 기존 |
| `severity` | Severity enum | 필수 | - | 심각도 (LOW, MEDIUM, HIGH, CRITICAL) | **신규** |
| `status` | AlertStatus enum | 필수 | UNREAD | 처리 상태 (UNREAD, IN_PROGRESS, COMPLETED) | **신규** |
| `assignedTo` | String | 선택 | null | 담당자 이름 (최대 100자) | **신규** |
| `actionNote` | String | 선택 | null | 조치 내용 (최대 2000자) | **신규** |
| `processedAt` | Timestamp | 선택 | null | 처리 완료 시각 (status=COMPLETED 시 자동 설정) | **신규** |

#### Java 타입 정의 (예시)

```java
// alert-service/src/main/java/com/realfds/alert/model/Alert.java
public class Alert {
    private String alertId;
    private Transaction originalTransaction;
    private String ruleName;
    private String reason;
    private Instant alertTimestamp;

    // 신규 필드
    private Severity severity;
    private AlertStatus status = AlertStatus.UNREAD;
    private String assignedTo;    // max 100자
    private String actionNote;    // max 2000자
    private Instant processedAt;

    // getters, setters, constructors
}
```

#### TypeScript 타입 정의 (예시)

```typescript
// frontend-dashboard/src/types/alert.ts
export interface Alert {
  alertId: string;
  originalTransaction: Transaction;
  ruleName: string;
  reason: string;
  alertTimestamp: string;

  // 신규 필드
  severity: Severity;
  status: AlertStatus;
  assignedTo: string | null;
  actionNote: string | null;
  processedAt: string | null;
}
```

#### 유효성 검증 규칙

1. **alertId**: UUID v4 형식, 중복 불가
2. **severity**: Severity enum 값 중 하나 필수
3. **status**: AlertStatus enum 값 중 하나, 기본값 UNREAD
4. **assignedTo**:
   - 최대 100자
   - null 허용 (미할당 상태)
   - 유니코드(한글) 지원
5. **actionNote**:
   - 최대 2000자
   - null 허용
   - 유니코드(한글) 지원
6. **processedAt**:
   - status가 COMPLETED일 때 자동 설정
   - status가 COMPLETED가 아니면 null
   - ISO 8601 형식 (예: 2025-11-11T10:30:00Z)

---

### 2. AlertStatus (신규 enum)

**설명**: 알림 처리 상태를 나타내는 열거형

**위치**:
- Backend: `alert-service/src/main/java/com/realfds/alert/model/AlertStatus.java`
- Frontend: `frontend-dashboard/src/types/alertStatus.ts`

#### 값 정의

| 값 | 한글 표시 | 색상 | 설명 |
|----|----------|------|------|
| `UNREAD` | 미확인 | 회색 (#gray-500) | 신규 알림, 아직 확인하지 않음 |
| `IN_PROGRESS` | 확인중 | 파란색 (#blue-500) | 담당자가 확인하고 조사 중 |
| `COMPLETED` | 완료 | 초록색 (#green-500) | 조치 완료, processedAt 자동 설정 |

#### 상태 전이 규칙

**허용되는 전이**:
- UNREAD → IN_PROGRESS
- UNREAD → COMPLETED (직접 완료 가능)
- IN_PROGRESS → COMPLETED
- COMPLETED → IN_PROGRESS (재조사 필요 시 역방향 가능)
- IN_PROGRESS → UNREAD (취소 시 역방향 가능)

**상태 전이 다이어그램**:
```
UNREAD ←→ IN_PROGRESS ←→ COMPLETED
  ↓________________________↑
```

#### Java 정의 (예시)

```java
// alert-service/src/main/java/com/realfds/alert/model/AlertStatus.java
public enum AlertStatus {
    UNREAD("미확인"),
    IN_PROGRESS("확인중"),
    COMPLETED("완료");

    private final String displayName;

    AlertStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

#### TypeScript 정의 (예시)

```typescript
// frontend-dashboard/src/types/alertStatus.ts
export enum AlertStatus {
  UNREAD = 'UNREAD',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
}

export const alertStatusDisplay: Record<AlertStatus, string> = {
  [AlertStatus.UNREAD]: '미확인',
  [AlertStatus.IN_PROGRESS]: '확인중',
  [AlertStatus.COMPLETED]: '완료',
};
```

---

### 3. Severity (신규 enum)

**설명**: 알림 심각도를 나타내는 열거형

**위치**:
- Backend: `alert-service/src/main/java/com/realfds/alert/model/Severity.java`
- fraud-detector: `fraud-detector/src/main/scala/com/realfds/detector/models/Severity.scala`
- Frontend: `frontend-dashboard/src/types/severity.ts`

#### 값 정의

| 값 | 한글 표시 | 색상 | 배경색 | 설명 |
|----|----------|------|--------|------|
| `LOW` | 낮음 | 파란색 (#blue-600) | 연한 파란색 (#blue-50) | 낮은 위험도 |
| `MEDIUM` | 보통 | 노란색 (#yellow-600) | 연한 노란색 (#yellow-50) | 중간 위험도 |
| `HIGH` | 높음 | 주황색 (#orange-600) | 연한 주황색 (#orange-50) | 높은 위험도 |
| `CRITICAL` | 긴급 | 빨간색 (#red-600) | 연한 빨간색 (#red-50) | 긴급 대응 필요 |

#### 규칙별 Severity 매핑

| 탐지 규칙 (ruleName) | Severity | 근거 |
|---------------------|----------|------|
| `HIGH_VALUE` | HIGH | 100만원 이상 고액 거래, 사기 가능성 높음 |
| `FOREIGN_COUNTRY` | MEDIUM | 해외 거래, 위험도 중간 |
| `HIGH_FREQUENCY` | HIGH | 1분 내 3회 이상 거래, 비정상 패턴 |

**주의**: 향후 규칙 추가 시 담당자가 수동으로 severity 설정 (feature 006-dynamic-rules에서 자동화 예정)

#### Java 정의 (예시)

```java
// alert-service/src/main/java/com/realfds/alert/model/Severity.java
public enum Severity {
    LOW("낮음", 1),
    MEDIUM("보통", 2),
    HIGH("높음", 3),
    CRITICAL("긴급", 4);

    private final String displayName;
    private final int priority;

    Severity(String displayName, int priority) {
        this.displayName = displayName;
        this.priority = priority;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPriority() {
        return priority;
    }
}
```

#### Scala 정의 (예시)

```scala
// fraud-detector/src/main/scala/com/realfds/detector/models/Severity.scala
sealed trait Severity {
  def displayName: String
  def priority: Int
}

object Severity {
  case object LOW extends Severity {
    override val displayName = "낮음"
    override val priority = 1
  }

  case object MEDIUM extends Severity {
    override val displayName = "보통"
    override val priority = 2
  }

  case object HIGH extends Severity {
    override val displayName = "높음"
    override val priority = 3
  }

  case object CRITICAL extends Severity {
    override val displayName = "긴급"
    override val priority = 4
  }
}
```

#### TypeScript 정의 (예시)

```typescript
// frontend-dashboard/src/types/severity.ts
export enum Severity {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

export const severityDisplay: Record<Severity, string> = {
  [Severity.LOW]: '낮음',
  [Severity.MEDIUM]: '보통',
  [Severity.HIGH]: '높음',
  [Severity.CRITICAL]: '긴급',
};

export const severityPriority: Record<Severity, number> = {
  [Severity.LOW]: 1,
  [Severity.MEDIUM]: 2,
  [Severity.HIGH]: 3,
  [Severity.CRITICAL]: 4,
};
```

---

### 4. DetectionRule (확장)

**설명**: 탐지 규칙 엔터티 - severity 필드 추가

**위치**: `fraud-detector/src/main/scala/com/realfds/detector/models/DetectionRule.scala`

#### 필드 정의

| 필드명 | 타입 | 필수 | 기본값 | 설명 | 변경 |
|--------|------|------|--------|------|------|
| `ruleName` | String | 필수 | - | 규칙 이름 (예: HIGH_VALUE) | 기존 |
| `description` | String | 필수 | - | 규칙 설명 | 기존 |
| `evaluationLogic` | Function | 필수 | - | 탐지 로직 | 기존 |
| `severity` | Severity enum | 필수 | - | 심각도 (LOW, MEDIUM, HIGH, CRITICAL) | **신규** |

#### 기존 규칙 업데이트

```scala
// fraud-detector/src/main/scala/com/realfds/detector/rules/HighValueRule.scala
object HighValueRule extends DetectionRule {
  override val ruleName = "HIGH_VALUE"
  override val description = "100만원 이상 고액 거래 탐지"
  override val severity = Severity.HIGH  // 신규

  override def evaluate(transaction: Transaction): Boolean = {
    transaction.amount >= 1000000
  }
}

// fraud-detector/src/main/scala/com/realfds/detector/rules/ForeignCountryRule.scala
object ForeignCountryRule extends DetectionRule {
  override val ruleName = "FOREIGN_COUNTRY"
  override val description = "해외 거래 탐지"
  override val severity = Severity.MEDIUM  // 신규

  override def evaluate(transaction: Transaction): Boolean = {
    transaction.country != "KR"
  }
}

// fraud-detector/src/main/scala/com/realfds/detector/rules/HighFrequencyRule.scala
object HighFrequencyRule extends DetectionRule {
  override val ruleName = "HIGH_FREQUENCY"
  override val description = "1분 내 3회 이상 거래 탐지"
  override val severity = Severity.HIGH  // 신규

  override def evaluate(window: TimeWindow[Transaction]): Boolean = {
    window.count >= 3
  }
}
```

---

## Relationships

### Entity Relationship Diagram

```
DetectionRule (1) ──generates──> (n) Alert
   │                                │
   │ severity                       │ status, assignedTo, actionNote
   │                                │
   └─────────────────────────────> UI Display
                                   (색상, 정렬, 필터링)
```

**관계 설명**:
1. **DetectionRule → Alert**:
   - 하나의 DetectionRule은 여러 Alert을 생성
   - DetectionRule의 severity가 Alert의 severity로 자동 복사

2. **Alert 상태 관리**:
   - Alert.status는 UNREAD → IN_PROGRESS → COMPLETED 순으로 전이
   - Alert.processedAt는 status=COMPLETED 시 자동 설정

3. **Alert 담당자 할당**:
   - Alert.assignedTo는 수동으로 할당 (자동 할당 없음)
   - 미할당 시 null

---

## Storage & Persistence

### 현재 버전 (002-alert-management)

**스토리지 타입**: 인메모리 (In-Memory)

**구현 위치**: `alert-service/src/main/java/com/realfds/alert/repository/AlertRepository.java`

**특징**:
- 최근 100개 알림만 유지 (FIFO 방식)
- 시스템 재시작 시 모든 알림 상태 초기화
- 빠른 조회 및 필터링 (<100ms 목표)

**제약사항**:
- 영속성 없음 (재시작 시 소실)
- 메모리 제한 (최대 100개)
- 이력 조회 불가

### 향후 버전 (003-alert-history)

**스토리지 타입**: 관계형 데이터베이스 (PostgreSQL 예정)

**계획**:
- 모든 알림 영구 저장
- 상태 변경 이력 추적
- 장기 이력 조회 및 통계 분석

---

## Data Model Changes Summary

### 변경 사항 요약

| 엔터티 | 변경 타입 | 변경 내용 |
|--------|----------|----------|
| Alert | 확장 | 5개 필드 추가 (severity, status, assignedTo, actionNote, processedAt) |
| AlertStatus | 신규 | enum 생성 (UNREAD, IN_PROGRESS, COMPLETED) |
| Severity | 신규 | enum 생성 (LOW, MEDIUM, HIGH, CRITICAL) |
| DetectionRule | 확장 | 1개 필드 추가 (severity) |

### 기존 MVP 호환성

- **100% 하위 호환**: 기존 Alert 필드는 모두 유지
- **기본값 보장**: 신규 필드는 모두 기본값 또는 null 허용
- **마이그레이션 불필요**: 인메모리 저장소는 재시작 시 초기화

---

## Business Rules

### 1. 상태 관리 규칙

- **초기 상태**: 모든 신규 알림은 UNREAD로 생성
- **자동 처리**: status=COMPLETED 시 processedAt 자동 설정 (현재 시각)
- **역방향 전이**: COMPLETED → IN_PROGRESS 가능 (재조사 시나리오)
- **상태 유지**: 브라우저 새로고침 시 상태 유지 (인메모리 저장소)

### 2. 담당자 할당 규칙

- **수동 할당**: 담당자는 수동으로만 할당 (자동 할당 없음)
- **미할당 허용**: assignedTo=null 허용 ("미할당" 표시)
- **최대 길이**: 100자 제한 (한글 약 50자)
- **유효성 검증**: 클라이언트 + 서버 양쪽에서 검증

### 3. 조치 내용 규칙

- **선택 사항**: actionNote는 선택 사항 (입력 권장 메시지만 표시)
- **최대 길이**: 2000자 제한 (한글 약 1000자)
- **완료 시 권장**: status=COMPLETED 시 조치 내용 입력 권장

### 4. 심각도 규칙

- **자동 할당**: DetectionRule의 severity가 Alert에 자동 복사
- **변경 불가**: 사용자가 Alert의 severity 수동 변경 불가
- **정렬 순서**: CRITICAL → HIGH → MEDIUM → LOW (priority 기준)

---

## Validation Rules

### Alert 필드 검증

| 필드 | 검증 규칙 | 오류 메시지 |
|------|----------|------------|
| alertId | UUID v4 형식 | "유효하지 않은 알림 ID입니다" |
| severity | Severity enum | "유효하지 않은 심각도입니다" |
| status | AlertStatus enum | "유효하지 않은 상태입니다" |
| assignedTo | ≤100자, null 허용 | "담당자 이름은 100자를 초과할 수 없습니다" |
| actionNote | ≤2000자, null 허용 | "조치 내용은 2000자를 초과할 수 없습니다" |
| processedAt | ISO 8601 형식, status=COMPLETED 시만 | "완료 상태가 아니면 처리 시각을 설정할 수 없습니다" |

### 상태 전이 검증

```java
// 예시: 상태 전이 검증 로직
public void validateStatusTransition(AlertStatus currentStatus, AlertStatus newStatus) {
    if (currentStatus == null) {
        throw new IllegalArgumentException("현재 상태가 없습니다");
    }

    // 모든 전이는 허용 (역방향 포함)
    // 추가 비즈니스 규칙이 필요하면 여기에 추가
}
```

---

## Performance Considerations

### 메모리 사용량 추정

**Alert 객체 크기** (대략):
- 기존 필드: ~500 bytes
- 신규 필드: ~150 bytes (status, assignedTo, actionNote, processedAt, severity)
- **총 크기**: ~650 bytes per alert

**100개 알림 기준**:
- 총 메모리: ~65 KB (매우 작음)
- alert-service 메모리 영향: 무시 가능 (<1MB)

### 조회 성능

**필터링**:
- O(n) 선형 탐색 (n=100)
- 예상 응답 시간: <10ms (목표: <100ms)

**정렬**:
- O(n log n) 정렬 (severity 기준)
- 예상 응답 시간: <5ms (목표: <100ms)

---

## Migration Path

### 현재 버전 (002-alert-management)

**마이그레이션 불필요**:
- 인메모리 저장소는 시스템 재시작 시 초기화
- 기존 Alert 모델에 신규 필드 추가만
- 기본값으로 자동 초기화 (status=UNREAD, severity=규칙에서 자동 설정)

### 향후 버전 (003-alert-history)

**DB 마이그레이션 필요**:
- PostgreSQL 스키마 생성
- 기존 인메모리 데이터는 마이그레이션 대상 아님 (휘발성)
- 새로운 알림부터 DB에 저장

---

## Testing Scenarios

### 1. Alert 상태 관리 테스트

**Given**: UNREAD 상태의 알림
**When**: IN_PROGRESS로 상태 변경
**Then**:
- status=IN_PROGRESS로 업데이트
- processedAt=null 유지

**Given**: IN_PROGRESS 상태의 알림
**When**: COMPLETED로 상태 변경
**Then**:
- status=COMPLETED로 업데이트
- processedAt=현재 시각 자동 설정

### 2. 담당자 할당 테스트

**Given**: 신규 알림 (assignedTo=null)
**When**: 담당자 "김보안" 할당
**Then**:
- assignedTo="김보안" 저장
- UI에 "김보안" 표시

**Given**: 101자 담당자 이름 입력
**When**: 할당 시도
**Then**:
- 400 Bad Request 오류
- "담당자 이름은 100자를 초과할 수 없습니다" 메시지

### 3. 심각도 테스트

**Given**: HighValueRule 실행
**When**: 알림 생성
**Then**:
- severity=HIGH
- UI에 주황색 배경 + "높음" 뱃지

**Given**: 다양한 심각도의 알림 목록
**When**: 심각도별 정렬
**Then**:
- CRITICAL → HIGH → MEDIUM → LOW 순서

---

## Notes

- 이 데이터 모델은 인메모리 저장소 기반 (영속성 없음)
- 003-alert-history에서 PostgreSQL 스키마로 확장 예정
- 모든 필드는 JSON 직렬화 가능 (WebSocket 전송)
- TypeScript 타입은 OpenAPI 스키마에서 자동 생성 권장

---

**문서 상태**: ✅ 완료
**최종 업데이트**: 2025-11-11
