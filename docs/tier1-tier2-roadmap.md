# Tier 1 & Tier 2 구현 로드맵

**생성일**: 2025-11-10
**목적**: MVP 완성 후 향후 개선 사항 (Tier 1, Tier 2)의 Spec Driven Development 계획

---

## 📋 개요

이 문서는 RealFDS MVP 완성 후 다음 단계로 추가할 기능들의 우선순위와 구현 계획을 정의합니다.

### 우선순위 결정 기준

- **비즈니스 가치**: 사용자에게 제공하는 실질적 가치
- **기술적 의존성**: 다른 기능의 선행 요구사항 여부
- **구현 복잡도**: 개발 난이도 및 소요 시간
- **Quick Win**: 낮은 비용으로 큰 효과

---

## 🎯 Phase 1: Tier 1 완성 (1-2주)

### 우선순위 요약

1. **알림 확인/처리** (Week 1) - 사용자 워크플로우 완성
2. **알림 우선순위** (Week 1) - Quick Win
3. **과거 알림 조회** (Week 2) - 모든 이후 기능의 기반

### Week 1: Alert Management (002-alert-management)

**Feature Branch**: `002-alert-management`
**목표**: 알림 확인/처리 + 알림 우선순위

#### 주요 기능
- 알림 상태 관리 (미확인/확인중/완료)
- 담당자 할당 및 조치 내용 기록
- 알림 우선순위(심각도) 설정
- 심각도별 색상 코딩 및 정렬

#### 기술 스택
- Backend: Alert 모델 확장 (status, assignedTo, actionNote, processedAt)
- Backend: DetectionRule에 severity 추가
- Frontend: 상태 뱃지, 조치 입력 모달, 우선순위 색상

#### 데이터 모델 변경
```typescript
interface Alert {
  // 기존 필드...
  status: 'UNREAD' | 'IN_PROGRESS' | 'COMPLETED';
  assignedTo?: string;
  actionNote?: string;
  processedAt?: Date;
}

interface DetectionRule {
  // 기존 필드...
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
}
```

#### API 설계
```
PATCH /api/alerts/{alertId}/status
  Body: { status: 'COMPLETED' }

PATCH /api/alerts/{alertId}/assign
  Body: { assignedTo: '김보안' }

POST /api/alerts/{alertId}/action
  Body: { actionNote: '고객에게 연락하여 확인 완료' }
```

#### 성공 지표
- 알림 상태 변경이 1초 이내에 UI에 반영
- 심각도별 색상 코딩이 명확히 구분
- 조치 내용 저장 및 조회 가능

#### 주요 태스크 (예상 40h)
```
Phase 1: 데이터 모델 확장 (4h)
- Alert 모델 확장 (status, assignedTo, actionNote, processedAt)
- DetectionRule에 severity 추가
- 각 규칙에 적절한 severity 설정

Phase 2: Backend API 구현 (12h)
- AlertRepository에 상태 업데이트 메서드
- AlertService 비즈니스 로직
- AlertController REST API
- 단위 테스트 (≥70% 커버리지)

Phase 3: Frontend UI 구현 (16h)
- AlertItem에 상태 뱃지 추가
- 우선순위별 색상 코딩
- AlertDetailModal 구현
- 상태 변경 API 연동
- 단위 테스트

Phase 4: 통합 및 검증 (8h)
- E2E 테스트
- 문서 업데이트 (contracts/rest-api.md)
```

---

### Week 2: Alert History (003-alert-history)

**Feature Branch**: `003-alert-history`
**목표**: 과거 알림 조회 (PostgreSQL 도입)

#### 주요 기능
- 모든 알림을 PostgreSQL에 영속 저장
- 날짜 범위로 알림 검색
- 규칙명, 사용자 ID, 상태별 필터링
- 페이지네이션 (한 페이지 50개)

#### 기술 스택
- PostgreSQL 15 컨테이너 추가
- Spring Data R2DBC (Reactive)
- Flyway 마이그레이션
- Frontend: 검색 패널, 페이지네이션

#### 데이터 모델
```sql
CREATE TABLE alerts (
  alert_id VARCHAR(36) PRIMARY KEY,
  schema_version VARCHAR(10),
  transaction_id VARCHAR(36),
  user_id VARCHAR(50),
  amount DECIMAL(15,2),
  rule_name VARCHAR(50),
  reason TEXT,
  severity VARCHAR(20),
  alert_timestamp TIMESTAMP,
  status VARCHAR(20),
  assigned_to VARCHAR(100),
  action_note TEXT,
  processed_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_timestamp ON alerts(alert_timestamp);
CREATE INDEX idx_rule_name ON alerts(rule_name);
CREATE INDEX idx_user_id ON alerts(user_id);
CREATE INDEX idx_status ON alerts(status);
```

#### API 설계
```
GET /api/alerts?startDate={}&endDate={}&ruleName={}&userId={}&status={}&page={}&size={}
Response: {
  content: Alert[],
  totalElements: number,
  totalPages: number,
  currentPage: number
}
```

#### 성공 지표
- 10,000개 알림에서 검색 속도 <500ms
- 시스템 재시작 후에도 알림 보존
- 페이지네이션으로 대량 데이터 효율적 처리

#### 주요 태스크 (예상 40h)
```
Phase 1: 인프라 및 DB 설정 (8h)
- PostgreSQL 컨테이너 추가
- R2DBC 의존성 추가
- Flyway 마이그레이션 스크립트
- DB 초기화 확인

Phase 2: Repository 구현 (12h)
- AlertRepository를 R2DBC로 변경
- 검색 메서드 구현
- 페이지네이션 지원
- 단위 테스트 (TestContainers)

Phase 3: Service 및 Controller (8h)
- AlertService 검색 로직
- AlertController 쿼리 파라미터
- 인메모리 캐시 유지 (최근 100개)
- 통합 테스트

Phase 4: Frontend 검색 UI (8h)
- AlertSearchPanel 컴포넌트
- Pagination 컴포넌트
- 검색 API 연동
- 단위 테스트

Phase 5: 통합 및 검증 (4h)
- E2E 테스트 (검색 시나리오)
- 성능 테스트 (10,000개 알림)
- 문서 업데이트
```

---

## 🎯 Phase 2: Tier 2 완성 (2-3주)

### 우선순위 요약

4. **실시간 대시보드** (Week 3) - 시스템 상태 가시성
5. **알림 통계** (Week 4) - 패턴 분석
6. **동적 규칙 관리** (Week 5) - 시스템 유연성 향상

### Week 3: Dashboard Realtime (004-dashboard-realtime)

**Feature Branch**: `004-dashboard-realtime`
**목표**: 실시간 대시보드 (시스템 상태 가시성)

#### 주요 기능
- 실시간 거래량 표시 (초당 TPS)
- 알림 발생률 표시 (분당 알림 수)
- 서비스 상태 표시 (5개 서비스 Health Check)
- 시계열 그래프 (최근 1시간)

#### 기술 스택
- Spring Boot Actuator metrics
- Chart.js 또는 Recharts
- WebSocket으로 실시간 메트릭 푸시

#### API 설계
```
GET /actuator/metrics/transactions.generated
GET /actuator/metrics/alerts.generated
WebSocket /ws/metrics
```

#### UI 설계
- DashboardPage 컴포넌트
- ServiceStatusCard (Health Check)
- TransactionChart (TPS)
- AlertChart (알림 발생률)

#### 성공 지표
- 메트릭 업데이트 주기 5초 이내
- 차트 렌더링 부드러움 (60 FPS)

#### 주요 태스크 (예상 32h)
```
Phase 1: Backend 메트릭 수집 (8h)
- transaction-generator에 Prometheus metrics
- fraud-detector 카운터 메트릭
- alert-service Actuator 메트릭
- websocket-gateway 메트릭 집계
- WebSocket /ws/metrics 엔드포인트

Phase 2: Frontend 대시보드 (16h)
- Chart.js 라이브러리 추가
- DashboardPage 구현
- ServiceStatusCard 구현
- TransactionChart 구현
- AlertChart 구현
- useMetricsWebSocket hook
- 단위 테스트

Phase 3: 통합 및 검증 (8h)
- E2E 테스트
- 문서 업데이트
```

---

### Week 4: Alert Analytics (005-alert-analytics)

**Feature Branch**: `005-alert-analytics`
**목표**: 알림 통계 (패턴 분석)

**의존성**: 003-alert-history (PostgreSQL 필요)

#### 주요 기능
- 시간대별 알림 추이 (24시간)
- 규칙별 알림 비율 (파이 차트)
- 일/주/월 단위 통계
- CSV 내보내기

#### API 설계
```
GET /api/analytics/hourly?date={}
  Response: { hour: number, count: number }[]

GET /api/analytics/by-rule?startDate={}&endDate={}
  Response: { ruleName: string, count: number, percentage: number }[]

GET /api/analytics/export?format=csv&startDate={}&endDate={}
  Response: CSV file download
```

#### UI 설계
- AnalyticsPage 컴포넌트
- 탭 구조 (시간대별/규칙별/내보내기)
- 차트 + 테이블 조합

#### 성공 지표
- 집계 쿼리 <1초
- CSV 내보내기 10,000개 <3초
- 차트 시각화 명확성

#### 주요 태스크 (예상 32h)
```
Phase 1: 집계 쿼리 구현 (8h)
- PostgreSQL 집계 쿼리 (시간대별)
- 규칙별 집계 쿼리
- AnalyticsRepository
- AnalyticsService
- 단위 테스트

Phase 2: API 구현 (8h)
- AnalyticsController
- CSV 생성 로직
- 통합 테스트

Phase 3: Frontend 구현 (12h)
- AnalyticsPage
- 차트 컴포넌트 (시간대별, 규칙별)
- CSV 다운로드 버튼
- 단위 테스트

Phase 4: 통합 및 검증 (4h)
- E2E 테스트
- 문서 업데이트
```

---

### Week 5: Dynamic Rules (006-dynamic-rules)

**Feature Branch**: `006-dynamic-rules`
**목표**: 동적 규칙 관리 (재시작 없이 규칙 수정)

#### 주요 기능
- 웹 UI에서 탐지 규칙 추가/수정/삭제
- 규칙 목록 조회 및 활성화/비활성화
- 규칙 구문 검증
- 규칙 테스트 (샘플 거래로 검증)

#### 기술 스택
- PostgreSQL rules 테이블
- fraud-detector: Rule Engine 구조 개선
- 규칙 표현: JSON 형식

#### 데이터 모델
```sql
CREATE TABLE detection_rules (
  rule_id VARCHAR(36) PRIMARY KEY,
  rule_name VARCHAR(100) UNIQUE,
  description TEXT,
  rule_type VARCHAR(50),
  severity VARCHAR(20),
  is_active BOOLEAN DEFAULT true,
  condition_json JSONB,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

예시 condition_json:
```json
{
  "type": "simple",
  "field": "amount",
  "operator": ">",
  "value": 1000000
}
```

#### API 설계
```
GET /api/rules
POST /api/rules
  Body: { ruleName, description, severity, conditionJson }
PUT /api/rules/{ruleId}
DELETE /api/rules/{ruleId}
POST /api/rules/{ruleId}/test
  Body: { sampleTransaction }
  Response: { matched: boolean, reason: string }
```

#### 제약사항
- 간단한 조건식만 지원 (복잡한 로직은 코드 수정 필요)
- Flink Job 재시작이 필요할 수 있음 (Hot-reload 한계)

#### 성공 지표
- 규칙 추가 후 5초 이내에 탐지 시작
- 규칙 구문 검증 정확도 100%
- 규칙 테스트 기능 작동

#### 주요 태스크 (예상 40h)
```
Phase 1: 규칙 저장소 구현 (8h)
- PostgreSQL rules 테이블
- RuleRepository
- RuleService
- 규칙 구문 검증 로직
- 단위 테스트

Phase 2: Rule Engine 개선 (12h)
- fraud-detector DynamicRuleEvaluator
- DB에서 규칙 로딩
- 규칙 변경 감지 및 Hot-reload
- 단위 테스트

Phase 3: API 구현 (8h)
- RuleController (CRUD)
- 규칙 테스트 엔드포인트
- 통합 테스트

Phase 4: Frontend 규칙 관리 UI (8h)
- RulesPage 컴포넌트
- RuleEditor 컴포넌트
- RuleTestModal 컴포넌트
- CRUD API 연동
- 단위 테스트

Phase 5: 통합 및 검증 (4h)
- E2E 테스트 (규칙 추가 → 즉시 탐지)
- 문서 업데이트 (dynamic-rules.md)
```

---

## 🔄 Spec Driven Development 워크플로우

### 각 Feature의 표준 프로세스

```bash
# 1️⃣ 새 Feature Branch 생성
git checkout -b 002-alert-management
mkdir -p specs/002-alert-management/contracts

# 2️⃣ Spec 작성
# specs/002-alert-management/spec.md 작성
/speckit.specify  # Spec 검증 및 개선

# 3️⃣ Plan 생성
/speckit.plan  # 기술 설계 및 아키텍처 결정

# 4️⃣ Tasks 생성
/speckit.tasks  # 구현 작업 분해

# 5️⃣ 구현 시작
/speckit.implement  # 작업 실행

# 6️⃣ 완료 후 메인 브랜치 병합
git checkout main
git merge 002-alert-management
```

### Spec 작성 체크리스트

각 feature의 spec.md는 다음을 포함해야 합니다:

- [ ] **비전 및 목적**: 무엇을 만들고 왜 만드는가
- [ ] **사용자 스토리**: User Story 1-5개, Given-When-Then 형식
- [ ] **주요 엔터티**: 데이터 모델 정의
- [ ] **성공 지표**: 측정 가능한 목표
- [ ] **제약사항**: 기술적 한계 및 범위
- [ ] **의존성**: 선행 feature 명시

---

## 📊 전체 타임라인

| Week | Feature | Branch | 핵심 작업 | 예상 시간 |
|------|---------|--------|----------|----------|
| **1** | Alert Management | 002-alert-management | 상태 관리 + 우선순위 | 40h |
| **2** | Alert History | 003-alert-history | PostgreSQL + 검색 | 40h |
| **3** | Dashboard Realtime | 004-dashboard-realtime | 메트릭 + 차트 | 32h |
| **4** | Alert Analytics | 005-alert-analytics | 통계 + CSV | 32h |
| **5** | Dynamic Rules | 006-dynamic-rules | 규칙 엔진 + UI | 40h |

**총 예상 시간**: 184시간 (약 5주, 주당 36-40시간)

---

## 🎯 마일스톤

### Milestone 1: Tier 1 완성 (Week 2 종료)
- ✅ 알림 확인/처리 기능
- ✅ 알림 우선순위
- ✅ 과거 알림 조회 (PostgreSQL)
- **검증**: 보안 담당자가 알림을 관리하고 과거 이력을 조회할 수 있음

### Milestone 2: Tier 2 완성 (Week 5 종료)
- ✅ 실시간 대시보드
- ✅ 알림 통계
- ✅ 동적 규칙 관리
- **검증**: 시스템 상태 모니터링 및 패턴 분석, 규칙 동적 관리 가능

---

## 📁 Feature Branch 구조

각 주차별로 독립적인 feature spec:

```text
specs/
├── 001-realtime-fds/          # ✅ 완료 (MVP)
│   ├── spec.md
│   ├── plan.md
│   ├── tasks.md
│   ├── data-model.md
│   ├── quickstart.md
│   └── contracts/
│
├── 002-alert-management/      # Week 1
│   ├── spec.md
│   ├── plan.md
│   ├── tasks.md
│   └── contracts/
│       └── rest-api.md
│
├── 003-alert-history/         # Week 2
│   ├── spec.md
│   ├── plan.md
│   ├── tasks.md
│   ├── data-model.md         # PostgreSQL 스키마
│   └── contracts/
│       └── search-api.md
│
├── 004-dashboard-realtime/    # Week 3
├── 005-alert-analytics/       # Week 4
└── 006-dynamic-rules/         # Week 5
```

---

## ✅ 다음 액션

### 즉시 시작 가능한 작업

1. **002-alert-management spec.md 작성**
   - 템플릿: `specs/001-realtime-fds/spec.md` 참고
   - 사용자 스토리 정의
   - 데이터 모델 설계

2. **전체 feature 구조 생성**
   ```bash
   mkdir -p specs/{002-alert-management,003-alert-history,004-dashboard-realtime,005-alert-analytics,006-dynamic-rules}/contracts
   ```

3. **Constitution 업데이트 검토**
   - 새로운 기능이 기존 원칙을 준수하는지 확인
   - 필요시 Constitution 업데이트

---

## 📚 참고 문서

- [001-realtime-fds/spec.md](../specs/001-realtime-fds/spec.md) - MVP 스펙 참고
- [001-realtime-fds/plan.md](../specs/001-realtime-fds/plan.md) - 계획 수립 방법
- [001-realtime-fds/tasks.md](../specs/001-realtime-fds/tasks.md) - 작업 분해 방법
- [architecture.md](./architecture.md) - 시스템 아키텍처

---

## 💡 핵심 인사이트

1. **과거 알림 조회(Week 2)**가 Tier 2의 모든 기능의 기반
   - 통계와 분석은 영속 데이터가 필요
   - 반드시 Week 2에 완료 필요

2. **알림 우선순위(Week 1)**는 Quick Win
   - 낮은 비용으로 큰 UX 개선
   - 기존 코드 수정 최소화

3. **동적 규칙 관리(Week 5)**는 가장 복잡
   - Rule Engine 재설계 필요
   - Hot-reload 메커니즘 복잡
   - 충분한 시간 확보 필요

4. **각 feature는 독립적으로 가치 제공**
   - Week 1 완료 후 바로 사용 가능
   - 순차적으로 가치 누적
   - 언제든 중단/재개 가능

---

**문서 버전**: 1.0
**최종 업데이트**: 2025-11-10
**작성자**: Claude Code with User
