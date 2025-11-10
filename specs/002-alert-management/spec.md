# Feature Specification: 알림 확인 및 처리 시스템 (Alert Management System)

**Feature Branch**: `002-alert-management`
**Created**: 2025-11-10
**Status**: ✅ Ready for Planning
**Prerequisites**: 001-realtime-fds (MVP 완료)
**Validation**: [requirements.md](./checklists/requirements.md)

## 비전 및 목적

### 무엇을 만드는가
보안 담당자가 탐지된 알림을 확인하고, 담당자를 할당하며, 조치 내용을 기록하여 알림의 전체 라이프사이클을 관리하는 시스템. 동시에 알림의 심각도(우선순위)를 시각적으로 구분하여 중요한 알림을 빠르게 식별할 수 있도록 지원.

### 왜 만드는가
- **업무 효율성**: 알림 상태 관리로 중복 작업 방지 및 진행 상황 추적
- **책임 명확화**: 담당자 할당으로 업무 분담 및 책임 소재 명확화
- **이력 관리**: 조치 내용 기록으로 향후 유사 사례 대응 시 참고 가능
- **우선순위 관리**: 심각도별 색상 코딩으로 긴급 알림 우선 처리

### 성공 지표
- 알림 상태 변경이 **1초 이내**에 UI에 실시간 반영
- 담당자 할당 및 조치 내용 기록이 **100% 정확**하게 저장
- 심각도별 색상이 **즉시 구분** 가능 (사용자 테스트)
- 조치 완료된 알림과 미완료 알림의 명확한 시각적 구분

## 사용자 및 페르소나

### 주요 사용자: 금융 보안 담당자 (Security Analyst)
**기존 페르소나 참조**: [001-realtime-fds/spec.md](../001-realtime-fds/spec.md#사용자-및-페르소나)

**추가 니즈**:
- 알림을 확인했음을 표시하여 중복 조사 방지
- 다른 담당자에게 알림 할당하여 업무 분담
- 조치한 내용을 기록하여 감사(audit) 추적 가능
- 긴급한 알림을 시각적으로 빠르게 식별

## User Scenarios & Testing

### User Story 1 - 알림 상태 관리 (Priority: P1)

보안 담당자가 알림의 처리 상태를 관리하여 업무 진행 상황을 추적하고 중복 작업을 방지하는 기능

**Why this priority**: 알림 관리의 가장 기본적인 기능이며, 담당자 할당과 조치 기록의 기반이 됨.

**Independent Test**: 알림 상태를 변경하고 새로고침 후에도 상태가 유지되는지 확인. 다른 브라우저에서 접속 시 동일한 상태가 보이는지 검증.

**Acceptance Scenarios**:

1. **Given** 새로운 알림이 발생하여 목록에 표시될 때, **When** 알림을 확인하면, **Then** 알림 상태가 "미확인"에서 "확인중"으로 변경되고 1초 이내에 UI에 반영됨

2. **Given** 알림이 "확인중" 상태일 때, **When** 조치를 완료하고 "완료" 버튼을 클릭하면, **Then** 알림 상태가 "완료"로 변경되고 처리 완료 시각이 기록됨

3. **Given** 알림 목록에 여러 상태의 알림이 있을 때, **When** 상태별 필터를 적용하면, **Then** 선택한 상태의 알림만 표시됨 (미확인/확인중/완료)

4. **Given** 알림 상태를 변경한 후, **When** 브라우저를 새로고침하면, **Then** 변경된 상태가 그대로 유지됨

5. **Given** 한 브라우저에서 알림 상태를 변경할 때, **When** 다른 브라우저에서 동일한 알림을 조회하면, **Then** 1초 이내에 변경된 상태가 실시간으로 반영됨

---

### User Story 2 - 담당자 할당 및 조치 기록 (Priority: P1)

보안 담당자가 알림에 담당자를 할당하고 조치 내용을 기록하여 업무 분담과 이력 관리를 수행하는 기능

**Why this priority**: 팀 단위 협업을 위한 핵심 기능이며, 조치 내역의 감사 추적을 가능하게 함.

**Independent Test**: 알림에 담당자를 할당하고 조치 내용을 입력한 후, 해당 알림을 다시 조회하여 정보가 정확히 저장되었는지 확인.

**Acceptance Scenarios**:

1. **Given** 알림을 클릭하여 상세 모달이 열릴 때, **When** 담당자 입력 필드에 이름을 입력하고 저장하면, **Then** 담당자가 할당되고 알림 목록에 담당자 이름이 표시됨

2. **Given** 알림에 담당자가 할당되지 않은 상태일 때, **When** 알림 목록을 확인하면, **Then** "미할당" 표시가 보임

3. **Given** 알림 상세 모달이 열린 상태에서, **When** 조치 내용을 텍스트 필드에 입력하고 "완료" 버튼을 클릭하면, **Then** 조치 내용이 저장되고 알림 상태가 "완료"로 변경됨

4. **Given** 조치가 완료된 알림을 다시 열 때, **When** 상세 모달을 확인하면, **Then** 이전에 입력한 담당자와 조치 내용이 그대로 표시됨

5. **Given** 담당자별로 알림을 필터링할 때, **When** 특정 담당자를 선택하면, **Then** 해당 담당자에게 할당된 알림만 표시됨

---

### User Story 3 - 알림 우선순위 (심각도) 표시 (Priority: P2)

시스템이 탐지 규칙별로 설정된 심각도를 알림에 자동으로 할당하고, UI에서 색상으로 구분하여 표시하는 기능

**Why this priority**: 빠른 구현으로 큰 UX 개선 효과를 제공하는 Quick Win. 담당자가 긴급 알림을 즉시 식별 가능.

**Independent Test**: 각 탐지 규칙으로 생성된 알림의 색상이 설정된 심각도에 맞게 표시되는지 확인.

**Acceptance Scenarios**:

1. **Given** 고액 거래(HIGH_VALUE) 알림이 생성될 때, **When** 알림 목록을 확인하면, **Then** 알림이 주황색으로 표시되고 "HIGH" 뱃지가 보임

2. **Given** 해외 거래(FOREIGN_COUNTRY) 알림이 생성될 때, **When** 알림 목록을 확인하면, **Then** 알림이 노란색으로 표시되고 "MEDIUM" 뱃지가 보임

3. **Given** 빈번한 거래(HIGH_FREQUENCY) 알림이 생성될 때, **When** 알림 목록을 확인하면, **Then** 알림이 주황색으로 표시되고 "HIGH" 뱃지가 보임

4. **Given** 알림 목록에 다양한 심각도의 알림이 있을 때, **When** 심각도별 정렬을 선택하면, **Then** CRITICAL → HIGH → MEDIUM → LOW 순서로 정렬됨

5. **Given** 심각도별 필터를 적용할 때, **When** "HIGH" 필터를 선택하면, **Then** HIGH 심각도의 알림만 표시됨

---

### Edge Cases

- **동시 상태 변경**: 두 담당자가 동시에 같은 알림의 상태를 변경하면? → 마지막 변경이 적용되며, WebSocket으로 모든 클라이언트에 즉시 동기화
- **담당자 이름 중복**: 동일한 이름의 담당자가 여러 명이면? → 현재 버전에서는 이름만 저장 (향후 사용자 ID 시스템 필요)
- **조치 내용 길이**: 매우 긴 조치 내용(1000자 이상)을 입력하면? → 최대 2000자로 제한, UI에서 경고 표시
- **알림 삭제**: 완료된 알림을 삭제할 수 있는가? → 현재 버전에서는 삭제 불가 (감사 추적 목적)
- **상태 되돌리기**: "완료"된 알림을 다시 "확인중"으로 변경할 수 있는가? → 가능, 단 이력은 기록되지 않음 (향후 개선)

## Requirements

### Functional Requirements

#### 알림 상태 관리
- **FR-001**: 각 알림은 "미확인(UNREAD)", "확인중(IN_PROGRESS)", "완료(COMPLETED)" 중 하나의 상태를 가져야 함
- **FR-002**: 사용자는 알림을 클릭하여 상태를 변경할 수 있어야 함
- **FR-003**: 알림 상태 변경은 1초 이내에 UI에 반영되어야 함
- **FR-004**: 알림 상태는 시스템 재시작 후에도 유지되어야 함 (인메모리 저장소)
- **FR-005**: 상태별 필터링 기능을 제공해야 함

#### 담당자 할당
- **FR-006**: 각 알림에 담당자 이름을 할당할 수 있어야 함
- **FR-007**: 담당자가 할당되지 않은 알림은 "미할당"으로 표시되어야 함
- **FR-008**: 담당자명은 최대 100자까지 입력 가능해야 함
- **FR-009**: 담당자별 알림 필터링 기능을 제공해야 함

#### 조치 내용 기록
- **FR-010**: 각 알림에 조치 내용을 텍스트로 기록할 수 있어야 함
- **FR-011**: 조치 내용은 최대 2000자까지 입력 가능해야 함
- **FR-012**: 알림을 "완료" 상태로 변경 시 처리 완료 시각이 자동 기록되어야 함
- **FR-013**: 조치 내용은 알림 상세 모달에서 확인 가능해야 함

#### 알림 우선순위 (심각도)
- **FR-014**: 각 탐지 규칙은 심각도(LOW/MEDIUM/HIGH/CRITICAL)를 가져야 함
- **FR-015**: 고액 거래 규칙은 HIGH 심각도로 설정되어야 함
- **FR-016**: 해외 거래 규칙은 MEDIUM 심각도로 설정되어야 함
- **FR-017**: 빈번한 거래 규칙은 HIGH 심각도로 설정되어야 함
- **FR-018**: 알림은 심각도에 따라 다른 색상으로 표시되어야 함:
  - CRITICAL: 빨간색
  - HIGH: 주황색
  - MEDIUM: 노란색
  - LOW: 파란색
- **FR-019**: 심각도별 정렬 기능을 제공해야 함
- **FR-020**: 심각도별 필터링 기능을 제공해야 함

### Key Entities

- **Alert** (확장):
  - 기존 필드: alertId, originalTransaction, ruleName, reason, severity, alertTimestamp
  - **새 필드**:
    - `status`: 'UNREAD' | 'IN_PROGRESS' | 'COMPLETED'
    - `assignedTo`: string | null (담당자 이름)
    - `actionNote`: string | null (조치 내용, 최대 2000자)
    - `processedAt`: timestamp | null (완료 처리 시각)

- **DetectionRule** (확장):
  - 기존 필드: ruleName, ruleType, description
  - **새 필드**:
    - `severity`: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

## Success Criteria

### Measurable Outcomes

- **SC-001**: 알림 상태 변경이 평균 0.5초, 최대 1초 이내에 UI에 반영되어야 함
- **SC-002**: 담당자 할당 및 조치 내용이 100% 정확하게 저장되고 조회되어야 함
- **SC-003**: 심각도별 색상이 명확히 구분되어야 함 (사용자 테스트에서 90% 이상 정확도)
- **SC-004**: 상태 변경이 모든 연결된 클라이언트(브라우저)에 실시간 동기화되어야 함
- **SC-005**: 100개의 알림 중 특정 상태/담당자/심각도로 필터링 시 응답 시간 <100ms
- **SC-006**: 알림 상세 모달 로딩 시간 <200ms

## 시스템 경계

### In Scope (포함)
✅ 알림 상태 관리 (미확인/확인중/완료)
✅ 담당자 할당 기능
✅ 조치 내용 기록 (최대 2000자)
✅ 알림 우선순위(심각도) 자동 할당
✅ 심각도별 색상 코딩
✅ 상태/담당자/심각도별 필터링 및 정렬
✅ 실시간 상태 동기화 (WebSocket)
✅ 인메모리 저장소 (최근 100개 알림만)

### Out of Scope (제외)
❌ 데이터베이스 영속성 (003-alert-history에서 구현)
❌ 사용자 인증 및 권한 관리
❌ 알림 이력 추적 (누가 언제 상태를 변경했는지)
❌ 담당자 ID 시스템 (현재는 이름만 저장)
❌ 알림 삭제 기능
❌ 일괄 상태 변경 (여러 알림 동시 처리)
❌ 알림 우선순위 동적 변경 (규칙 레벨에서만 설정)
❌ 조치 내용 수정 이력
❌ 파일 첨부 기능

## 제약사항 및 가정

### Technical Constraints
- **인메모리 저장소**: 최근 100개 알림만 상태 관리 (시스템 재시작 시 초기화)
- **WebSocket 기반**: 상태 변경은 WebSocket을 통해 실시간 브로드캐스트
- **단일 세션**: 사용자 인증이 없으므로 누가 변경했는지 추적 불가

### Environmental Constraints
- 기존 MVP(001-realtime-fds) 인프라를 그대로 사용
- 추가 컨테이너 없음 (메모리 제약 유지)

### Assumptions
- 담당자 이름은 한글 또는 영문으로 입력 가능
- 동일한 이름의 담당자가 있을 수 있음 (현재 버전에서는 구분 불가)
- 조치 내용은 평문 텍스트 (마크다운 등 미지원)
- 알림은 삭제되지 않고 영구 보관 (최근 100개 내에서)

## 향후 개선 사항 (현재 범위 외)

현재 버전에서는 구현하지 않지만, 향후 추가를 고려할 수 있는 기능:

1. **알림 이력 추적**: 누가 언제 상태를 변경했는지 타임라인 기록
2. **담당자 자동 할당**: 라운드 로빈 또는 부하 기반 자동 배정
3. **알림 에스컬레이션**: 일정 시간 내 미처리 시 자동으로 상위 담당자에게 할당
4. **조치 템플릿**: 자주 사용하는 조치 내용을 템플릿으로 저장
5. **파일 첨부**: 조치 증빙 자료 첨부 기능
6. **알림 댓글**: 여러 담당자가 협업할 수 있도록 댓글 기능
7. **일괄 처리**: 여러 알림을 한 번에 상태 변경
8. **SLA 추적**: 알림 발생부터 완료까지 소요 시간 측정
9. **담당자 통계**: 담당자별 처리 건수 및 평균 처리 시간
10. **모바일 푸시 알림**: 새 알림 할당 시 모바일로 알림

## 위험 및 열린 질문

### Known Risks

| 위험 | 영향 | 확률 | 완화 방안 |
|------|------|------|-----------|
| 동시 상태 변경 충돌 | 마지막 변경만 반영됨 | 중 | WebSocket으로 즉시 동기화하여 충돌 최소화 |
| 인메모리 한계로 오래된 알림 소실 | 100개 초과 시 알림 상태 유실 | 높음 | 003-alert-history(DB)를 빠르게 구현하여 해결 |
| 담당자 이름 중복 | 동명이인 구분 불가 | 중 | 향후 사용자 인증 시스템 도입 시 해결 |
| 조치 내용 미입력 | 완료 처리되었으나 조치 내용 없음 | 중 | UI에서 조치 내용 필수 입력으로 설정 (선택적) |

### Design Decisions

Design decisions have been made for areas that could have multiple interpretations:

1. **조치 내용 필수 여부**: 선택 사항으로 두되, UI에서 입력 권장 메시지 표시
   - **이유**: 일부 알림은 조치 내용이 간단할 수 있어 유연성 필요

2. **담당자 할당 방식**: 현재 버전에서는 수동 할당만 지원
   - **이유**: 자동 할당은 향후 개선 사항 (#2)으로 연기

3. **심각도 결정**: 규칙 추가 시 담당자가 수동으로 심각도 설정
   - **이유**: 현재 버전에서는 3개 규칙만 존재하며, 006-dynamic-rules에서 UI 기반 설정 지원 예정

4. **알림 만료**: 자동 만료 처리 미지원
   - **이유**: 현재 feature는 활성 알림 관리에 집중, 003-alert-history에서 아카이빙 정책 고려

## Dependencies

### Prerequisite Features
- **001-realtime-fds**: MVP 완료 (Alert 모델, WebSocket 인프라 필요)

### Dependent Features
- **003-alert-history**: 이 feature의 Alert 모델 확장을 사용하여 DB 스키마 설계
- **005-alert-analytics**: 알림 상태 데이터를 활용한 통계 분석

## API Design (간략)

### REST API
```
PATCH /api/alerts/{alertId}/status
  Request: { "status": "COMPLETED" }
  Response: { "alertId": "...", "status": "COMPLETED", "processedAt": "..." }

PATCH /api/alerts/{alertId}/assign
  Request: { "assignedTo": "김보안" }
  Response: { "alertId": "...", "assignedTo": "김보안" }

POST /api/alerts/{alertId}/action
  Request: { "actionNote": "고객 확인 완료", "status": "COMPLETED" }
  Response: { "alertId": "...", "actionNote": "...", "status": "COMPLETED", "processedAt": "..." }

GET /api/alerts?status={}&assignedTo={}&severity={}
  Response: { "alerts": [...] }
```

### WebSocket Events
```
// 서버 → 클라이언트 (알림 상태 변경 브로드캐스트)
{
  "type": "ALERT_STATUS_CHANGED",
  "alertId": "...",
  "status": "COMPLETED",
  "assignedTo": "김보안",
  "actionNote": "...",
  "processedAt": "2025-11-10T10:30:00Z"
}
```

## UI Components (간략)

- **AlertItem** (개선): 상태 뱃지, 담당자 표시, 심각도 색상 추가
- **AlertDetailModal** (신규): 알림 상세 정보, 상태 변경, 담당자 할당, 조치 입력
- **AlertFilterPanel** (신규): 상태/담당자/심각도 필터 및 정렬
- **SeverityBadge** (신규): 심각도 뱃지 컴포넌트

---

**Specification Status**: ✅ Validated and Ready for Planning

This specification has been validated against quality criteria. See [requirements.md](./checklists/requirements.md) for validation details.

**Next Steps**:
- `/speckit.clarify` - For further refinement (optional)
- `/speckit.plan` - To create implementation plan
