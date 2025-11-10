# Feature Specification: 실시간 시스템 대시보드 (Realtime System Dashboard)

**Feature Branch**: `004-dashboard-realtime`
**Created**: 2025-11-10
**Status**: Draft
**Prerequisites**: 001-realtime-fds (MVP 완료)

## 비전 및 목적

### 무엇을 만드는가
시스템의 건강도와 성능 지표를 실시간으로 모니터링하는 대시보드. 거래량(TPS), 알림 발생률, 5개 서비스의 상태를 시계열 차트와 상태 카드로 시각화하여 시스템 이상 징후를 빠르게 발견.

### 왜 만드는가
- **시스템 가시성**: 실시간 거래량과 알림 발생률을 한눈에 파악
- **장애 조기 발견**: 서비스 다운 또는 성능 저하를 즉시 감지
- **용량 계획**: 트래픽 패턴 분석으로 리소스 증설 계획 수립
- **운영 효율성**: 대시보드 하나로 전체 시스템 상태 모니터링

### 성공 지표
- 메트릭 업데이트 주기 **5초 이내**
- 차트 렌더링 **60 FPS** 유지 (부드러운 애니메이션)
- 서비스 상태 변경 감지 **3초 이내**
- 대시보드 초기 로딩 **2초 이내**

## 사용자 및 페르소나

### 주요 사용자: 시스템 운영자 (DevOps Engineer)
- **이름**: 이운영 (28세, 2년 경력)
- **목표**:
  - 시스템 건강도를 실시간으로 모니터링
  - 서비스 장애를 조기에 발견하여 빠르게 대응
  - 트래픽 패턴을 파악하여 리소스 최적화
- **문제점**:
  - 여러 로그를 확인해야 하는 번거로움
  - 서비스 다운을 늦게 발견하는 경우 발생
  - 시스템 전체 상태를 한눈에 보기 어려움

### 부차적 사용자: 금융 보안 담당자
- **목표**: 알림 발생 추이를 보고 사기 패턴 변화 감지
- **니즈**: 실시간 거래량과 알림 발생률의 상관관계 확인

## User Scenarios & Testing

### User Story 1 - 서비스 상태 모니터링 (Priority: P1)

운영자가 5개 마이크로서비스의 상태를 실시간으로 확인하여 장애를 조기에 발견하는 기능

**Acceptance Scenarios**:

1. **Given** 대시보드에 접속했을 때, **When** 페이지가 로딩되면, **Then** 5개 서비스(transaction-generator, fraud-detector, alert-service, websocket-gateway, frontend-dashboard)의 상태가 표시됨

2. **Given** 모든 서비스가 정상일 때, **When** 상태 카드를 확인하면, **Then** 각 서비스가 녹색으로 표시되고 "UP" 상태가 보임

3. **Given** alert-service가 다운되었을 때, **When** 3초 후 대시보드를 확인하면, **Then** 해당 서비스가 빨간색으로 표시되고 "DOWN" 상태가 보임

4. **Given** 서비스 상태 카드를 클릭했을 때, **When** 클릭하면, **Then** 해당 서비스의 상세 정보 모달이 열림 (메모리 사용량, 응답 시간 등)

---

### User Story 2 - 실시간 거래량 모니터링 (Priority: P1)

운영자가 초당 거래 처리량(TPS)을 실시간 차트로 확인하여 트래픽 패턴을 파악하는 기능

**Acceptance Scenarios**:

1. **Given** 대시보드에 접속했을 때, **When** 거래량 차트를 확인하면, **Then** 최근 1시간의 TPS가 시계열 그래프로 표시됨

2. **Given** 거래가 지속적으로 발생할 때, **When** 5초마다 차트를 확인하면, **Then** 새로운 데이터 포인트가 부드럽게 추가됨

3. **Given** 거래량이 급증했을 때, **When** 차트를 확인하면, **Then** 그래프가 상승하고 현재 TPS 수치가 강조 표시됨

4. **Given** 차트에 마우스를 올렸을 때, **When** 특정 시점을 가리키면, **Then** 해당 시각의 정확한 TPS 값이 툴팁으로 표시됨

---

### User Story 3 - 알림 발생률 모니터링 (Priority: P2)

운영자가 분당 알림 발생 수를 차트로 확인하여 사기 패턴 변화를 감지하는 기능

**Acceptance Scenarios**:

1. **Given** 대시보드에 접속했을 때, **When** 알림 발생률 차트를 확인하면, **Then** 최근 1시간의 분당 알림 수가 시계열 그래프로 표시됨

2. **Given** 알림이 지속적으로 발생할 때, **When** 5초마다 차트를 확인하면, **Then** 새로운 데이터가 실시간으로 업데이트됨

3. **Given** 알림 발생률이 급증했을 때, **When** 차트를 확인하면, **Then** 그래프 색상이 주황색으로 변경되어 경고 표시

4. **Given** 규칙별 알림 비율을 확인하고 싶을 때, **When** 차트 범례를 클릭하면, **Then** 특정 규칙의 데이터만 표시/숨김 처리됨

## Requirements

### Functional Requirements

#### 서비스 상태
- **FR-001**: 5개 서비스의 Health Check 상태를 실시간으로 수집해야 함
- **FR-002**: 각 서비스는 UP(녹색) 또는 DOWN(빨간색)으로 표시되어야 함
- **FR-003**: 서비스 상태는 5초마다 업데이트되어야 함
- **FR-004**: 서비스 다운 발생 시 3초 이내에 UI에 반영되어야 함

#### 거래량 메트릭
- **FR-005**: 초당 거래 처리량(TPS)을 실시간으로 수집해야 함
- **FR-006**: 최근 1시간의 TPS를 시계열 그래프로 표시해야 함
- **FR-007**: 현재 TPS, 평균 TPS, 최대 TPS를 숫자로 표시해야 함
- **FR-008**: 메트릭은 5초마다 업데이트되어야 함

#### 알림 발생률 메트릭
- **FR-009**: 분당 알림 발생 수를 실시간으로 수집해야 함
- **FR-010**: 최근 1시간의 알림 발생률을 시계열 그래프로 표시해야 함
- **FR-011**: 규칙별(HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY) 분리 표시 가능
- **FR-012**: 현재 알림 발생률, 평균, 최대값을 숫자로 표시해야 함

#### UI/UX
- **FR-013**: 차트는 60 FPS로 부드럽게 렌더링되어야 함
- **FR-014**: 메트릭 업데이트 시 깜빡임 없이 자연스럽게 전환되어야 함
- **FR-015**: 서비스 상태 카드 클릭 시 상세 정보 모달이 열려야 함

### Key Entities

- **ServiceHealth**:
  - `serviceName`: string
  - `status`: 'UP' | 'DOWN'
  - `lastChecked`: timestamp
  - `responseTime`: number (ms)
  - `memoryUsage`: number (MB)

- **TransactionMetrics**:
  - `timestamp`: Date
  - `tps`: number (초당 거래 수)
  - `totalTransactions`: number

- **AlertMetrics**:
  - `timestamp`: Date
  - `alertsPerMinute`: number
  - `byRule`: { ruleName: string, count: number }[]

## Success Criteria

- **SC-001**: 메트릭 업데이트 주기 평균 4초, 최대 5초
- **SC-002**: 차트 렌더링 60 FPS 유지
- **SC-003**: 서비스 다운 감지 시간 평균 2초, 최대 3초
- **SC-004**: 대시보드 초기 로딩 2초 이내
- **SC-005**: 1시간 동안 대시보드 사용 시 메모리 누수 없음

## 시스템 경계

### In Scope
✅ 5개 서비스 Health Check
✅ 실시간 TPS 모니터링
✅ 실시간 알림 발생률 모니터링
✅ 시계열 차트 (Chart.js)
✅ WebSocket 실시간 데이터 푸시
✅ 서비스 상세 정보 모달

### Out of Scope
❌ 메트릭 데이터 영속 저장 (DB)
❌ 과거 메트릭 조회 (1시간 이전)
❌ 알림 설정 (임계값 초과 시 알림)
❌ 메트릭 내보내기 (CSV, JSON)
❌ 커스텀 대시보드 설정
❌ 여러 대시보드 레이아웃

## 제약사항 및 가정

### Technical Constraints
- 메트릭은 메모리에만 저장 (최근 1시간)
- Spring Boot Actuator 사용
- Chart.js 라이브러리

### Assumptions
- 서비스 Health Check 응답 시간 <100ms
- 동시 대시보드 사용자 5명 이하
- 메트릭 데이터 크기 <10MB (1시간)

## Dependencies

### Prerequisite Features
- **001-realtime-fds**: 기본 인프라

## API Design

```
GET /actuator/health (각 서비스)
GET /actuator/metrics/transactions.generated
GET /actuator/metrics/alerts.generated
WebSocket /ws/metrics
```

---

**Note**: 이 spec은 골격 버전입니다. `/speckit.specify` 명령으로 검증 및 개선 가능합니다.
