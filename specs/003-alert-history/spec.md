# Feature Specification: 알림 이력 조회 시스템 (Alert History System)

**Feature Branch**: `003-alert-history`
**Created**: 2025-11-10
**Status**: Draft
**Prerequisites**: 001-realtime-fds (MVP 완료), 002-alert-management (알림 상태 관리)

## 비전 및 목적

### 무엇을 만드는가
모든 알림을 PostgreSQL 데이터베이스에 영속적으로 저장하고, 날짜 범위, 규칙명, 사용자 ID, 상태 등 다양한 조건으로 과거 알림을 검색하고 조회할 수 있는 시스템. 대량의 알림 데이터를 효율적으로 처리하기 위한 페이지네이션 지원.

### 왜 만드는가
- **영속성 확보**: 시스템 재시작 후에도 알림 데이터 보존
- **감사 추적**: 과거 사기 패턴 분석 및 컴플라이언스 요구사항 충족
- **패턴 분석**: 이력 데이터 기반 사기 패턴 발견 및 대응 전략 수립
- **업무 연속성**: 100개 제한 없이 모든 알림 조회 가능
- **향후 기능 기반**: 통계 분석(005), 머신러닝 학습 데이터 제공

### 성공 지표
- 10,000개 알림에서 검색 속도 **<500ms**
- 시스템 재시작 후에도 **100%** 데이터 보존
- 페이지네이션으로 **50개/페이지** 효율적 로딩
- 복합 필터 검색 정확도 **100%**

## 사용자 및 페르소나

### 주요 사용자: 금융 보안 담당자 (Security Analyst)
**기존 페르소나 참조**: [001-realtime-fds/spec.md](../001-realtime-fds/spec.md#사용자-및-페르소나)

**추가 니즈**:
- 특정 날짜의 알림 이력을 조회하여 패턴 분석
- 특정 사용자의 과거 거래 이력 추적
- 규칙별 알림 발생 빈도 확인
- 완료된 알림의 조치 내용 재확인

### 부차적 사용자: 컴플라이언스 담당자
- **목표**: 감사 목적의 알림 이력 조회
- **문제점**: 실시간 데이터만으로는 과거 추적 불가
- **기술 수준**: 웹 인터페이스 사용 가능

## User Scenarios & Testing

### User Story 1 - 날짜 범위 검색 (Priority: P1)

보안 담당자가 특정 날짜 범위의 알림을 조회하여 해당 기간의 사기 패턴을 분석하는 기능

**Why this priority**: 가장 기본적이고 자주 사용되는 검색 조건. 시계열 분석의 기반.

**Independent Test**: 특정 날짜 범위를 입력하고 검색 버튼을 클릭하여 해당 기간의 알림만 표시되는지 확인.

**Acceptance Scenarios**:

1. **Given** 알림 검색 페이지에 접속했을 때, **When** 시작일(2025-11-01)과 종료일(2025-11-07)을 입력하고 검색하면, **Then** 해당 기간에 발생한 알림만 표시되고 500ms 이내에 응답함

2. **Given** 날짜 범위를 입력하지 않았을 때, **When** 검색 버튼을 클릭하면, **Then** 최근 7일간의 알림이 기본으로 표시됨

3. **Given** 검색 결과가 100개 이상일 때, **When** 검색하면, **Then** 페이지네이션이 표시되고 한 페이지에 50개씩 표시됨

4. **Given** 두 번째 페이지를 조회할 때, **When** "다음" 버튼을 클릭하면, **Then** 51번째부터 100번째까지의 알림이 표시됨

5. **Given** 검색 결과가 없을 때, **When** 검색하면, **Then** "검색 결과가 없습니다" 메시지가 표시됨

---

### User Story 2 - 복합 필터 검색 (Priority: P1)

보안 담당자가 여러 조건을 조합하여 알림을 검색하는 기능 (규칙명, 사용자 ID, 상태, 심각도)

**Why this priority**: 정밀한 분석을 위한 필수 기능. 다양한 조건 조합으로 특정 패턴 추적.

**Independent Test**: 날짜 + 규칙명 + 사용자 ID를 동시에 입력하여 모든 조건을 만족하는 알림만 표시되는지 확인.

**Acceptance Scenarios**:

1. **Given** 알림 검색 페이지에서, **When** 규칙명 "HIGH_VALUE"를 선택하고 검색하면, **Then** 고액 거래 알림만 표시됨

2. **Given** 알림 검색 페이지에서, **When** 사용자 ID "user-5"를 입력하고 검색하면, **Then** 해당 사용자의 알림만 표시됨

3. **Given** 알림 검색 페이지에서, **When** 상태 "COMPLETED"를 선택하고 검색하면, **Then** 완료된 알림만 표시됨

4. **Given** 알림 검색 페이지에서, **When** 날짜(2025-11-01~07) + 규칙명(HIGH_VALUE) + 상태(COMPLETED)를 모두 입력하고 검색하면, **Then** 모든 조건을 만족하는 알림만 표시됨

5. **Given** 필터를 적용한 상태에서, **When** "필터 초기화" 버튼을 클릭하면, **Then** 모든 필터가 해제되고 전체 알림이 표시됨

---

### User Story 3 - 페이지네이션 (Priority: P2)

대량의 검색 결과를 효율적으로 탐색할 수 있도록 페이지 단위로 데이터를 로딩하는 기능

**Why this priority**: 성능 최적화 및 사용자 경험 향상. 수천 개의 알림을 한 번에 로딩하지 않음.

**Independent Test**: 1000개 이상의 알림이 있는 상태에서 검색하고, 페이지 이동 시 로딩 시간과 정확성을 확인.

**Acceptance Scenarios**:

1. **Given** 검색 결과가 200개일 때, **When** 첫 페이지를 조회하면, **Then** "1 / 4" 페이지 정보와 함께 50개의 알림이 표시됨

2. **Given** 두 번째 페이지를 조회할 때, **When** "다음" 버튼을 클릭하면, **Then** 200ms 이내에 다음 50개의 알림이 로딩됨

3. **Given** 페이지네이션 네비게이션에서, **When** 특정 페이지 번호(예: 3)를 클릭하면, **Then** 해당 페이지로 즉시 이동함

4. **Given** 마지막 페이지(4/4)에 있을 때, **When** "다음" 버튼을 확인하면, **Then** 버튼이 비활성화되어 있음

5. **Given** 첫 페이지(1/4)에 있을 때, **When** "이전" 버튼을 확인하면, **Then** 버튼이 비활성화되어 있음

---

### Edge Cases

- **대량 검색 결과**: 100,000개 이상의 알림을 검색하면? → 페이지네이션으로 분할 로딩, DB 인덱스로 성능 보장
- **동시 검색**: 여러 사용자가 동시에 복잡한 검색을 실행하면? → DB 연결 풀로 관리, 최대 동시 쿼리 제한
- **특수 문자 검색**: 사용자 ID에 특수 문자(예: user-5#)가 포함되면? → SQL 인젝션 방지 위해 파라미터 바인딩 사용
- **시간대 처리**: 알림 발생 시각이 다른 시간대로 표시되면? → 모든 시간을 UTC로 저장, 클라이언트에서 로컬 시간으로 변환
- **페이지 범위 초과**: 존재하지 않는 페이지 번호(예: 100)를 요청하면? → 마지막 페이지로 리디렉션

## Requirements

### Functional Requirements

#### 데이터 영속성
- **FR-001**: 모든 알림은 PostgreSQL 데이터베이스에 저장되어야 함
- **FR-002**: alert-service가 Kafka에서 알림을 수신하면 DB에 즉시 저장해야 함
- **FR-003**: 시스템 재시작 후에도 모든 알림 데이터가 보존되어야 함
- **FR-004**: 최근 100개 알림은 인메모리 캐시에도 유지되어야 함 (실시간 표시용)

#### 검색 기능
- **FR-005**: 날짜 범위로 알림을 검색할 수 있어야 함 (시작일, 종료일)
- **FR-006**: 규칙명으로 알림을 필터링할 수 있어야 함
- **FR-007**: 사용자 ID로 알림을 필터링할 수 있어야 함
- **FR-008**: 알림 상태(UNREAD/IN_PROGRESS/COMPLETED)로 필터링할 수 있어야 함
- **FR-009**: 심각도(LOW/MEDIUM/HIGH/CRITICAL)로 필터링할 수 있어야 함
- **FR-010**: 여러 필터를 조합하여 검색할 수 있어야 함 (AND 조건)
- **FR-011**: 필터가 없으면 최근 7일간의 알림을 기본으로 표시해야 함

#### 페이지네이션
- **FR-012**: 검색 결과는 한 페이지에 50개씩 표시되어야 함
- **FR-013**: 총 페이지 수, 현재 페이지, 전체 알림 개수가 표시되어야 함
- **FR-014**: "이전", "다음" 버튼으로 페이지 이동이 가능해야 함
- **FR-015**: 특정 페이지 번호를 클릭하여 직접 이동할 수 있어야 함
- **FR-016**: 첫 페이지에서는 "이전" 버튼이 비활성화되어야 함
- **FR-017**: 마지막 페이지에서는 "다음" 버튼이 비활성화되어야 함

#### 성능
- **FR-018**: 10,000개 알림에서 검색 속도는 500ms 이내여야 함
- **FR-019**: 페이지 이동 시 로딩 시간은 200ms 이내여야 함
- **FR-020**: DB 쿼리 최적화를 위한 인덱스가 적용되어야 함

### Key Entities

- **Alert (DB 저장)**:
  - 모든 필드 (002-alert-management의 확장 포함)
  - DB 전용 필드:
    - `id`: SERIAL PRIMARY KEY (자동 증가)
    - `created_at`: 데이터 생성 시각 (DB 레벨)

- **AlertSearchQuery**:
  - `startDate`: Date | null
  - `endDate`: Date | null
  - `ruleName`: string | null
  - `userId`: string | null
  - `status`: AlertStatus | null
  - `severity`: Severity | null
  - `page`: number (기본값: 1)
  - `size`: number (기본값: 50)

- **PagedAlertResponse**:
  - `content`: Alert[]
  - `totalElements`: number
  - `totalPages`: number
  - `currentPage`: number
  - `pageSize`: number

## Success Criteria

### Measurable Outcomes

- **SC-001**: 10,000개 알림에서 검색 속도 평균 300ms, p95 500ms 이내
- **SC-002**: 페이지 이동 시 로딩 시간 평균 100ms, p95 200ms 이내
- **SC-003**: 복합 필터 검색 정확도 100% (모든 조건 만족하는 알림만 반환)
- **SC-004**: 100,000개 알림에서도 페이지네이션으로 부드러운 탐색 가능
- **SC-005**: DB 저장 실패율 <0.01% (재시도 로직 포함)
- **SC-006**: 시스템 재시작 후 데이터 손실 0%

## 시스템 경계

### In Scope (포함)
✅ PostgreSQL 데이터베이스 도입
✅ 모든 알림의 영속적 저장
✅ 날짜 범위 검색
✅ 규칙명, 사용자 ID, 상태, 심각도 필터링
✅ 복합 필터 조합 (AND 조건)
✅ 페이지네이션 (50개/페이지)
✅ DB 인덱스 최적화
✅ 인메모리 캐시 병행 (최근 100개)

### Out of Scope (제외)
❌ 전문 검색 (Full-text search)
❌ 조치 내용 텍스트 검색
❌ 금액 범위 검색 (예: 100만원~200만원)
❌ 정렬 기능 (알림 발생 시각 DESC 고정)
❌ 알림 내보내기 (CSV, PDF 등) → 005-alert-analytics에서 구현
❌ 알림 삭제 기능
❌ 알림 수정 기능 (상태 변경은 002에서 구현)
❌ OR 조건 검색 (현재는 AND만 지원)
❌ 저장된 검색 조건 (북마크)

## 제약사항 및 가정

### Technical Constraints
- **PostgreSQL 15**: 공식 Docker 이미지 사용
- **R2DBC**: Spring Data R2DBC로 Reactive 프로그래밍
- **Flyway**: 마이그레이션 관리
- **메모리 제한**: PostgreSQL 컨테이너 1GB 이하

### Environmental Constraints
- Docker Compose에 PostgreSQL 컨테이너 추가
- 총 메모리 사용량 <6GB (기존 4GB + PostgreSQL 1GB + 여유 1GB)

### Assumptions
- 알림 발생 빈도: 초당 10개 이하 (MVP 범위)
- 데이터 보관 기간: 무제한 (디스크 용량 허용 범위 내)
- 검색 빈도: 분당 10회 이하
- 동시 검색 사용자: 5명 이하

## 향후 개선 사항 (현재 범위 외)

1. **전문 검색**: Elasticsearch 도입하여 조치 내용 텍스트 검색
2. **고급 필터**: 금액 범위, 국가 코드, 시간대별 검색
3. **정렬 옵션**: 금액순, 심각도순, 상태순 정렬
4. **저장된 검색**: 자주 사용하는 검색 조건 북마크
5. **알림 통합**: 여러 알림을 하나로 묶어서 표시 (예: 동일 사용자의 연속 알림)
6. **실시간 업데이트**: 검색 결과 페이지에서도 새 알림 실시간 추가
7. **데이터 아카이빙**: 90일 이상 오래된 알림을 별도 테이블로 이동
8. **읽기 전용 레플리카**: 검색 성능 향상을 위한 읽기 전용 DB
9. **쿼리 캐싱**: 동일한 검색 조건에 대한 결과 캐싱
10. **CSV 일괄 다운로드**: 검색 결과 전체를 CSV로 내보내기

## 위험 및 열린 질문

### Known Risks

| 위험 | 영향 | 확률 | 완화 방안 |
|------|------|------|-----------|
| DB 저장 실패 | 알림 유실 | 낮음 | 재시도 로직, 실패 시 로그 기록 및 알림 |
| DB 연결 폭증 | 검색 속도 저하 | 중 | Connection pool 설정, 최대 동시 쿼리 제한 |
| 인덱스 미최화 | 검색 속도 저하 | 중 | 성능 테스트 후 인덱스 튜닝 |
| 디스크 용량 부족 | 시스템 중단 | 낮음 | 디스크 사용량 모니터링, 알림 설정 |

### Open Questions

1. **데이터 보관 기간**: 알림 데이터를 영구 보관할 것인가, 아니면 일정 기간 후 삭제할 것인가?
   - 제안: 현재 버전에서는 영구 보관, 향후 아카이빙 정책 수립

2. **검색 기본값**: 날짜 범위를 입력하지 않으면 최근 며칠을 보여줄 것인가?
   - 제안: 최근 7일 (변경 가능)

3. **페이지 크기**: 한 페이지에 50개가 적절한가?
   - 제안: 50개 고정, 향후 사용자 설정으로 변경 가능 (10/25/50/100)

4. **인메모리 캐시 유지**: 실시간 표시용 최근 100개 캐시를 계속 유지할 것인가?
   - 제안: 유지. WebSocket 실시간 알림은 캐시에서 제공

## Dependencies

### Prerequisite Features
- **001-realtime-fds**: Alert 모델, Kafka 인프라
- **002-alert-management**: Alert 모델 확장 (status, assignedTo 등)

### Dependent Features
- **005-alert-analytics**: 이 feature의 DB를 활용한 통계 분석
- **006-dynamic-rules**: 규칙 저장을 위한 DB 인프라 재사용

## Database Schema (간략)

```sql
CREATE TABLE alerts (
  id SERIAL PRIMARY KEY,
  alert_id VARCHAR(36) UNIQUE NOT NULL,
  schema_version VARCHAR(10),

  -- Transaction 정보
  transaction_id VARCHAR(36),
  user_id VARCHAR(50),
  amount DECIMAL(15,2),
  currency VARCHAR(10),
  country_code VARCHAR(10),
  transaction_timestamp TIMESTAMP,

  -- Alert 정보
  rule_name VARCHAR(50),
  rule_type VARCHAR(50),
  reason TEXT,
  severity VARCHAR(20),
  alert_timestamp TIMESTAMP NOT NULL,

  -- 관리 정보 (002-alert-management)
  status VARCHAR(20) DEFAULT 'UNREAD',
  assigned_to VARCHAR(100),
  action_note TEXT,
  processed_at TIMESTAMP,

  -- 시스템 필드
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스
CREATE INDEX idx_alert_timestamp ON alerts(alert_timestamp DESC);
CREATE INDEX idx_rule_name ON alerts(rule_name);
CREATE INDEX idx_user_id ON alerts(user_id);
CREATE INDEX idx_status ON alerts(status);
CREATE INDEX idx_severity ON alerts(severity);
CREATE INDEX idx_created_at ON alerts(created_at DESC);

-- 복합 인덱스 (자주 사용되는 조합)
CREATE INDEX idx_timestamp_rule ON alerts(alert_timestamp DESC, rule_name);
CREATE INDEX idx_timestamp_status ON alerts(alert_timestamp DESC, status);
```

## API Design (간략)

```
GET /api/alerts/search?startDate={}&endDate={}&ruleName={}&userId={}&status={}&severity={}&page={}&size={}
  Response: {
    "content": Alert[],
    "totalElements": 1234,
    "totalPages": 25,
    "currentPage": 1,
    "pageSize": 50
  }

GET /api/alerts/count?startDate={}&endDate={}
  Response: { "count": 1234 }
```

---

**Note**: 이 spec은 골격 버전입니다. `/speckit.specify` 명령으로 검증 및 개선 가능합니다.
