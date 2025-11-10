# Feature Specification: 동적 탐지 규칙 관리 (Dynamic Detection Rules Management)

**Feature Branch**: `006-dynamic-rules`
**Created**: 2025-11-10
**Status**: Draft
**Prerequisites**: 003-alert-history (PostgreSQL 필요)

## 비전 및 목적

### 무엇을 만드는가
웹 UI에서 사기 탐지 규칙을 추가, 수정, 삭제할 수 있으며, 시스템 재시작 없이 규칙 변경이 즉시 반영되는 동적 규칙 관리 시스템. 규칙 구문 검증 및 샘플 거래로 테스트 가능.

### 왜 만드는가
- **민첩성**: 새로운 사기 패턴 발견 시 즉시 규칙 추가
- **유연성**: 코드 수정 없이 규칙 조건 변경 (예: 고액 거래 기준 조정)
- **실험**: 새로운 규칙을 프로덕션에 배포하기 전에 테스트
- **운영 효율성**: 개발자 없이 보안 팀이 직접 규칙 관리

### 성공 지표
- 규칙 추가 후 **5초 이내** 탐지 시작
- 규칙 구문 검증 정확도 **100%** (잘못된 규칙 차단)
- 규칙 테스트 기능 정확도 **100%**
- UI에서 규칙 CRUD 완료 시간 **<1초**

## 사용자 및 페르소나

### 주요 사용자: 금융 보안 담당자 (고급)
- **이름**: 최규칙 (32세, 5년 경력, 기술 이해도 높음)
- **목표**:
  - 새로운 사기 패턴 발견 시 즉시 탐지 규칙 추가
  - 고액 거래 기준(100만원)을 상황에 따라 조정
  - 샘플 거래로 규칙을 미리 테스트하여 오탐 방지
  - 효과 없는 규칙을 비활성화하여 성능 최적화
- **문제점**:
  - 현재는 코드 수정이 필요하여 개발자에게 의존
  - 규칙 변경 후 시스템 재시작 필요 (다운타임)
  - 새 규칙 테스트가 어려워 오탐 위험

### 부차적 사용자: 시스템 개발자
- **목표**: 복잡한 규칙은 코드로 작성, 간단한 규칙은 UI로 관리
- **니즈**: 규칙 변경 이력 추적

## User Scenarios & Testing

### User Story 1 - 규칙 추가 및 활성화 (Priority: P1)

보안 담당자가 웹 UI에서 새로운 탐지 규칙을 추가하고 즉시 활성화하여 사기 탐지를 시작하는 기능

**Acceptance Scenarios**:

1. **Given** 규칙 관리 페이지에 접속했을 때, **When** "새 규칙 추가" 버튼을 클릭하면, **Then** 규칙 편집기 모달이 열림

2. **Given** 규칙 편집기에서, **When** 규칙명 "초고액 거래", 조건 "amount > 2000000", 심각도 "CRITICAL"을 입력하고 저장하면, **Then** 규칙이 DB에 저장되고 목록에 표시됨

3. **Given** 새 규칙이 저장된 후, **When** 5초 후 해당 조건의 거래가 발생하면, **Then** 새 규칙으로 알림이 생성됨

4. **Given** 규칙 목록에서, **When** 특정 규칙의 활성화 토글을 OFF로 변경하면, **Then** 해당 규칙이 비활성화되고 더 이상 알림이 생성되지 않음

5. **Given** 비활성화된 규칙을, **When** 다시 활성화하면, **Then** 즉시 탐지가 재개됨

---

### User Story 2 - 규칙 수정 (Priority: P1)

보안 담당자가 기존 규칙의 조건이나 심각도를 수정하여 탐지 기준을 조정하는 기능

**Acceptance Scenarios**:

1. **Given** 규칙 목록에서 "고액 거래" 규칙을 선택했을 때, **When** "수정" 버튼을 클릭하면, **Then** 규칙 편집기가 열리고 기존 값이 표시됨

2. **Given** 규칙 편집기에서, **When** 조건을 "amount > 1000000"에서 "amount > 1500000"으로 변경하고 저장하면, **Then** 규칙이 업데이트되고 5초 이내에 새 조건이 적용됨

3. **Given** 규칙을 수정한 후, **When** 100만원 거래가 발생하면, **Then** 알림이 생성되지 않음 (150만원 이상만 탐지)

4. **Given** 규칙을 수정한 후, **When** 200만원 거래가 발생하면, **Then** 새 조건으로 알림이 생성됨

---

### User Story 3 - 규칙 테스트 (Priority: P2)

보안 담당자가 샘플 거래 데이터로 규칙을 테스트하여 프로덕션 배포 전에 동작을 확인하는 기능

**Acceptance Scenarios**:

1. **Given** 규칙 편집기에서 새 규칙을 작성한 상태에서, **When** "테스트" 버튼을 클릭하면, **Then** 샘플 거래 입력 모달이 열림

2. **Given** 샘플 거래 입력 모달에서, **When** amount=2000000, countryCode=KR을 입력하고 "테스트 실행"을 클릭하면, **Then** 규칙 매칭 결과가 표시됨 (매칭 여부, 이유)

3. **Given** 규칙 "amount > 1500000"을 테스트할 때, **When** amount=1000000을 입력하면, **Then** "매칭 안됨" 결과가 표시됨

4. **Given** 규칙 "amount > 1500000"을 테스트할 때, **When** amount=2000000을 입력하면, **Then** "매칭됨 - 초고액 거래 (150만원 초과): 2,000,000원" 결과가 표시됨

---

## Requirements

### Functional Requirements

#### 규칙 CRUD
- **FR-001**: 웹 UI에서 새로운 규칙을 추가할 수 있어야 함
- **FR-002**: 기존 규칙을 수정할 수 있어야 함
- **FR-003**: 규칙을 삭제할 수 있어야 함 (소프트 삭제)
- **FR-004**: 규칙을 활성화/비활성화할 수 있어야 함

#### 규칙 정의
- **FR-005**: 규칙명, 설명, 조건(JSON), 심각도를 입력받아야 함
- **FR-006**: 조건은 간단한 비교 연산자(>, <, =, !=, IN) 지원
- **FR-007**: 지원 필드: amount, countryCode, userId
- **FR-008**: 규칙 구문 검증을 수행하여 잘못된 규칙 차단

#### 규칙 적용
- **FR-009**: 규칙 추가/수정 후 5초 이내에 fraud-detector에 반영
- **FR-010**: fraud-detector는 DB에서 활성 규칙을 로딩해야 함
- **FR-011**: 규칙 변경 시 Flink Job 재시작 없이 Hot-reload

#### 규칙 테스트
- **FR-012**: 샘플 거래로 규칙을 테스트할 수 있어야 함
- **FR-013**: 테스트 결과는 매칭 여부와 이유를 표시해야 함

### Key Entities

- **DetectionRule (DB)**:
  - `ruleId`: UUID
  - `ruleName`: string (고유)
  - `description`: string
  - `ruleType`: 'SIMPLE_RULE' | 'STATEFUL_RULE'
  - `severity`: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  - `isActive`: boolean
  - `conditionJson`: JSONB
  - `createdAt`: timestamp
  - `updatedAt`: timestamp

- **RuleCondition (JSON)**:
  ```json
  {
    "type": "simple",
    "field": "amount",
    "operator": ">",
    "value": 1500000
  }
  ```

## Success Criteria

- **SC-001**: 규칙 추가 후 5초 이내에 탐지 시작
- **SC-002**: 규칙 구문 검증 정확도 100%
- **SC-003**: 규칙 테스트 기능 정확도 100%
- **SC-004**: UI 응답 시간 <1초

## 시스템 경계

### In Scope
✅ 규칙 CRUD (웹 UI)
✅ 간단한 조건 규칙 (>, <, =, !=, IN)
✅ 규칙 활성화/비활성화
✅ 규칙 구문 검증
✅ 샘플 거래 테스트
✅ PostgreSQL 저장
✅ Hot-reload 메커니즘

### Out of Scope
❌ 복잡한 로직 (AND/OR 조합, 중첩)
❌ 시간 기반 윈도우 규칙 (UI에서 작성 불가)
❌ 규칙 버전 관리
❌ 규칙 변경 이력 추적
❌ 규칙 성능 통계
❌ A/B 테스트 기능

## 제약사항 및 가정

### Technical Constraints
- 간단한 비교 연산만 지원
- 복잡한 stateful 규칙은 코드로 작성 필요
- Flink Job 재시작이 필요할 수 있음 (Hot-reload 한계)

### Assumptions
- 규칙 수 <50개
- 규칙 변경 빈도: 주 1-2회

## Dependencies

### Prerequisite Features
- **003-alert-history**: PostgreSQL 인프라

## Database Schema

```sql
CREATE TABLE detection_rules (
  rule_id VARCHAR(36) PRIMARY KEY,
  rule_name VARCHAR(100) UNIQUE NOT NULL,
  description TEXT,
  rule_type VARCHAR(50),
  severity VARCHAR(20),
  is_active BOOLEAN DEFAULT true,
  condition_json JSONB NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rules_active ON detection_rules(is_active);
```

## API Design

```
GET /api/rules
POST /api/rules
  Body: { ruleName, description, severity, conditionJson }
PUT /api/rules/{ruleId}
DELETE /api/rules/{ruleId}
PATCH /api/rules/{ruleId}/toggle
POST /api/rules/{ruleId}/test
  Body: { sampleTransaction }
  Response: { matched: boolean, reason: string }
```

---

**Note**: 이 spec은 골격 버전입니다. `/speckit.specify` 명령으로 검증 및 개선 가능합니다.
