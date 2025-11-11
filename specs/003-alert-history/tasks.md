# Tasks: Alert History (과거 알림 조회)

**Feature**: 003-alert-history
**Input**: Design documents from `/specs/003-alert-history/`
**Prerequisites**: plan.md (✅), spec.md (✅), research.md (✅), data-model.md (✅), contracts/ (✅)

---

**⚠️ Constitution 준수 필수 사항**:

1. **테스트 우선 (Constitution V)**:
   - 단위 테스트 ≥70% 커버리지 필수
   - 통합 테스트는 모든 검색 조건에 대해 필수
   - Given-When-Then 구조 사용
   - 테스트는 구현 **전에** 작성하고 실패 확인 후 구현

2. **관찰 가능성 (Constitution V)**:
   - 모든 서비스에 구조화된 로깅 (SLF4J + JSON) 필수
   - 헬스 체크 엔드포인트 (`/actuator/health`) 필수
   - 중요 비즈니스 이벤트 로깅 필수

3. **한국어 우선 (Constitution VI)**:
   - 모든 코드 주석은 한국어
   - 커밋 메시지는 Conventional Commits + 한국어
   - 문서는 한국어로 작성

4. **품질 표준 (Constitution V)**:
   - 최대 함수 길이: 50줄
   - 최대 파일 길이: 300줄
   - 서술적인 변수/함수 이름 사용

---

**Tests**: Tests are MANDATORY for this project (Constitution V requires ≥70% coverage)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 프로젝트 초기화 및 기본 구조 설정

- [X] T001 Create database migration directory structure in alert-dashboard/backend/src/main/resources/db/migration/
- [X] T002 [P] Add PostgreSQL R2DBC dependencies to alert-dashboard/backend/build.gradle.kts (Spring Data R2DBC, PostgreSQL R2DBC Driver)
- [X] T003 [P] Add Flyway dependency to alert-dashboard/backend/build.gradle.kts
- [X] T004 [P] Configure R2DBC connection in alert-dashboard/backend/src/main/resources/application.yml
- [X] T005 [P] Configure Flyway settings in alert-dashboard/backend/src/main/resources/application.yml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 모든 User Story 구현을 위한 핵심 인프라 (이 단계 완료 전까지 User Story 작업 불가)

**⚠️ CRITICAL**: 이 단계가 완료되어야만 User Story 작업을 시작할 수 있습니다

### 데이터베이스 스키마 및 마이그레이션

- [X] T006 Create Flyway migration V1__create_alerts_table.sql in alert-dashboard/backend/src/main/resources/db/migration/
  - alerts 테이블 생성 (data-model.md의 스키마 참조)
  - 인덱스 생성 (idx_alert_timestamp, idx_rule_name, idx_user_id, idx_status)
  - 제약 조건 설정 (CHECK constraints, NOT NULL)
  - 한국어 주석으로 테이블 및 컬럼 설명
- [X] T007 Create Flyway migration V2__insert_sample_alerts.sql in alert-dashboard/backend/src/main/resources/db/migration/
  - 개발/테스트용 샘플 알림 데이터 3개 삽입
  - data-model.md의 Sample Data 참조

### 도메인 엔티티 및 Enum

- [X] T008 [P] Create AlertStatus enum in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/domain/AlertStatus.kt
  - UNREAD, IN_PROGRESS, COMPLETED 상태 정의
  - 한국어 주석으로 각 상태 설명
- [X] T009 [P] Create Severity enum in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/domain/Severity.kt
  - HIGH, MEDIUM, LOW 심각도 정의
  - 한국어 주석으로 각 심각도 설명
- [X] T010 Create Alert entity in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/domain/Alert.kt
  - @Table("alerts") 어노테이션 추가
  - data-model.md의 모든 필드 포함
  - 한국어 주석으로 각 필드 설명
  - 서술적인 변수명 사용

### 환경 설정 및 Docker Compose

- [X] T011 Add PostgreSQL service to docker-compose.yml
  - PostgreSQL 15-alpine 이미지 사용
  - 환경 변수 설정 (POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD)
  - 포트 매핑 (5432:5432)
  - 볼륨 설정 (postgres_data)
  - healthcheck 설정 (pg_isready)
  - 한국어 주석으로 설정 설명
- [X] T012 Update alert-dashboard service in docker-compose.yml
  - R2DBC 환경 변수 추가 (SPRING_R2DBC_URL, USERNAME, PASSWORD)
  - Flyway 환경 변수 추가 (SPRING_FLYWAY_URL, USER, PASSWORD)
  - depends_on 설정 (postgres service_healthy 조건)
  - 한국어 주석으로 설정 설명

**Checkpoint**: 기반 인프라 준비 완료 - User Story 구현 시작 가능

---

## Phase 3: User Story 1 - 기본 알림 이력 조회 (Priority: P1) 🎯 MVP

**Goal**: 과거에 발생한 모든 알림을 조회하여 패턴 분석 및 오탐 검토 지원. 시스템 재시작 후에도 모든 알림 데이터 보존.

**Independent Test**: 100개의 알림을 생성하고 시스템을 재시작한 후 모든 알림이 조회 가능한지 확인. 단독으로 테스트 가능하며 알림 영속화라는 명확한 가치를 제공.

### DTO 및 검색 조건 (User Story 1)

- [ ] T013 [P] [US1] Create AlertSearchCriteria DTO in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/dto/AlertSearchCriteria.kt
  - startDate, endDate, page, size 필드 포함 (규칙명, 사용자ID, 상태는 US3에서 추가)
  - 검증 로직 (init 블록에서 page ≥ 0, size 1~100, startDate ≤ endDate 등)
  - 기본값 설정 (page=0, size=50)
  - 한국어 주석으로 각 필드 및 검증 규칙 설명
- [ ] T014 [P] [US1] Create PagedAlertResult DTO in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/dto/PagedAlertResult.kt
  - content, totalElements, totalPages, currentPage, pageSize, hasNext, hasPrevious 필드 포함
  - 한국어 주석으로 각 필드 설명

### 테스트 (User Story 1 - MANDATORY per Constitution V) ⚠️

> **CRITICAL: 테스트를 먼저 작성하고, FAIL 확인 후 구현을 시작하세요**

- [ ] T015 [P] [US1] Unit test for Alert entity validation in alert-dashboard/backend/src/test/kotlin/io/realfds/alert/domain/AlertTest.kt
  - Given-When-Then 구조 사용
  - 유효한 Alert 생성 테스트
  - 필드 검증 테스트 (amount > 0, reason 길이 등)
  - 한국어 주석으로 테스트 의도 설명
- [ ] T016 [P] [US1] Unit test for AlertSearchCriteria validation in alert-dashboard/backend/src/test/kotlin/io/realfds/alert/dto/AlertSearchCriteriaTest.kt
  - Given-When-Then 구조 사용
  - 날짜 범위 검증 테스트 (startDate ≤ endDate)
  - 페이지 번호/크기 검증 테스트
  - 한국어 주석으로 테스트 의도 설명

### Repository (User Story 1)

- [ ] T017 [US1] Create AlertRepository interface in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/repository/AlertRepository.kt
  - R2dbcRepository<Alert, UUID> 확장
  - 기본 CRUD 메서드 상속
  - 한국어 주석으로 Repository 목적 설명
- [ ] T018 [US1] Create CustomAlertRepository interface in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/repository/CustomAlertRepository.kt
  - 동적 쿼리 메서드 정의 (findByCriteria, countByCriteria)
  - 한국어 주석으로 메서드 설명
- [ ] T019 [US1] Implement CustomAlertRepositoryImpl in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/repository/CustomAlertRepositoryImpl.kt
  - R2dbcEntityTemplate 사용하여 동적 쿼리 생성
  - 날짜 범위 필터링 구현 (startDate, endDate)
  - 페이지네이션 구현 (LIMIT, OFFSET)
  - 정렬 구현 (ORDER BY alert_timestamp DESC)
  - 함수 길이 ≤50줄 준수
  - 한국어 주석으로 쿼리 로직 설명

### 테스트 (User Story 1 - Repository Layer) ⚠️

- [ ] T020 [US1] Integration test for AlertRepository basic operations in alert-dashboard/backend/src/test/kotlin/io/realfds/alert/repository/AlertRepositoryTest.kt
  - Testcontainers (PostgreSQL) 사용
  - Given-When-Then 구조 사용
  - Alert 저장 및 조회 테스트
  - 시스템 재시작 시뮬레이션 테스트 (영속성 확인)
  - 한국어 주석으로 테스트 시나리오 설명
- [ ] T021 [US1] Integration test for CustomAlertRepository date range search in alert-dashboard/backend/src/test/kotlin/io/realfds/alert/repository/CustomAlertRepositoryTest.kt
  - Testcontainers (PostgreSQL) 사용
  - Given-When-Then 구조 사용
  - 날짜 범위 검색 테스트 (1주일 전 ~ 오늘)
  - 페이지네이션 테스트 (첫 페이지, 두 번째 페이지)
  - 빈 결과 테스트
  - 한국어 주석으로 테스트 시나리오 설명

### Service (User Story 1)

- [ ] T022 [US1] Create AlertHistoryService in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/service/AlertHistoryService.kt
  - searchAlerts 메서드 구현 (AlertSearchCriteria 받아서 PagedAlertResult 반환)
  - 기본 날짜 범위 설정 (최근 7일)
  - 구조화된 로깅 추가 (검색 시작, 완료, 결과 개수)
  - 에러 처리 및 로깅 (DatabaseConnectionException 등)
  - 함수 길이 ≤50줄 준수
  - 한국어 주석으로 비즈니스 로직 설명
  - 로그 메시지는 한국어로 작성

### 테스트 (User Story 1 - Service Layer) ⚠️

- [ ] T023 [US1] Unit test for AlertHistoryService search logic in alert-dashboard/backend/src/test/kotlin/io/realfds/alert/service/AlertHistoryServiceTest.kt
  - Mockito로 Repository 모킹
  - Reactor Test (StepVerifier) 사용
  - Given-When-Then 구조 사용
  - 정상 검색 테스트
  - 빈 결과 테스트
  - 기본 날짜 범위 설정 테스트
  - 한국어 주석으로 테스트 의도 설명

### Controller (User Story 1)

- [ ] T024 [US1] Create AlertHistoryController in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/controller/AlertHistoryController.kt
  - GET /api/alerts/history 엔드포인트 구현
  - @Valid로 AlertSearchCriteria 검증
  - ResponseEntity로 응답 반환
  - 에러 처리 (InvalidDateRangeException → 400, DatabaseConnectionException → 503)
  - 구조화된 로깅 추가 (요청 로깅, 응답 로깅)
  - 함수 길이 ≤50줄 준수
  - 한국어 주석으로 엔드포인트 설명
  - 로그 메시지는 한국어로 작성

### 테스트 (User Story 1 - Controller Layer) ⚠️

- [ ] T025 [US1] Integration test for AlertHistoryController GET /api/alerts/history in alert-dashboard/backend/src/test/kotlin/io/realfds/alert/controller/AlertHistoryControllerTest.kt
  - @SpringBootTest + Testcontainers (PostgreSQL)
  - WebTestClient 사용
  - Given-When-Then 구조 사용
  - 정상 응답 테스트 (200 OK)
  - 페이지네이션 테스트 (page=0, size=50)
  - 빈 결과 테스트 (content=[], totalElements=0)
  - 잘못된 날짜 범위 테스트 (400 Bad Request)
  - 한국어 주석으로 API 테스트 시나리오 설명

### 알림 저장 통합 (User Story 1)

- [ ] T026 [US1] Update AlertService to save alerts to PostgreSQL in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/service/AlertService.kt
  - Kafka에서 AlertEvent 수신 시 AlertRepository.save() 호출
  - 저장 성공/실패 로깅
  - 저장 실패 시 재시도 로직 (최대 3회)
  - 실시간 WebSocket 전송은 유지 (기존 기능)
  - 한국어 주석으로 통합 로직 설명
  - 로그 메시지는 한국어로 작성

### 테스트 (User Story 1 - Alert Persistence) ⚠️

- [ ] T027 [US1] Integration test for alert persistence in alert-dashboard/backend/src/test/kotlin/io/realfds/alert/service/AlertServicePersistenceTest.kt
  - @SpringBootTest + Testcontainers (PostgreSQL + Kafka)
  - Given-When-Then 구조 사용
  - Kafka로 AlertEvent 전송 → 데이터베이스 저장 확인
  - 시스템 재시작 시뮬레이션 → 저장된 알림 조회 확인
  - 한국어 주석으로 종단 간 테스트 시나리오 설명

**Checkpoint**: User Story 1 완료 - 기본 알림 이력 조회 및 영속화 기능 작동. 이 단계에서 MVP 배포 가능.

---

## Phase 4: User Story 2 - 날짜 범위 검색 (Priority: P2)

**Goal**: 특정 기간 동안 발생한 알림만 조회하여 시간대별 패턴 분석 지원.

**Independent Test**: 1주일 전부터 오늘까지 매일 10개씩 알림 생성. 특정 날짜 범위(예: 3일 전 ~ 1일 전)로 검색하여 해당 기간의 알림만 조회되는지 확인. Story 1과 독립적으로 테스트 가능.

### Frontend: DateRangePicker Component (User Story 2)

- [ ] T028 [P] [US2] Create DateRangePicker component in alert-dashboard/frontend/src/components/DateRangePicker.tsx
  - Material-UI DatePicker 사용
  - startDate, endDate state 관리
  - onChange 콜백 prop
  - 유효성 검사 (startDate ≤ endDate)
  - 한국어 라벨 및 에러 메시지
  - TypeScript 타입 정의
- [ ] T029 [P] [US2] Unit test for DateRangePicker component in alert-dashboard/frontend/src/components/DateRangePicker.test.tsx
  - React Testing Library 사용
  - 날짜 선택 시 onChange 호출 확인
  - 잘못된 날짜 범위 입력 시 에러 메시지 표시 확인

### Frontend: Alert History Page Update (User Story 2)

- [ ] T030 [US2] Update AlertHistoryPage to include DateRangePicker in alert-dashboard/frontend/src/pages/AlertHistoryPage.tsx
  - DateRangePicker 컴포넌트 추가
  - 날짜 범위 state 관리
  - 검색 버튼 클릭 시 API 호출
  - 한국어 UI 텍스트
  - TypeScript 타입 정의
- [ ] T031 [US2] Update alertHistoryService to support date range parameters in alert-dashboard/frontend/src/services/alertHistoryService.ts
  - startDate, endDate 쿼리 파라미터 추가
  - ISO 8601 형식으로 변환
  - TypeScript 타입 정의

### 테스트 (User Story 2 - Frontend) ⚠️

- [ ] T032 [US2] Integration test for date range search in alert-dashboard/frontend/src/pages/AlertHistoryPage.test.tsx
  - React Testing Library 사용
  - Given-When-Then 구조 사용
  - 날짜 범위 선택 → 검색 → 결과 표시 확인
  - Mock Service Worker (MSW)로 API 모킹

### 테스트 (User Story 2 - End-to-End) ⚠️

- [ ] T033 [US2] End-to-end test for date range search in alert-dashboard/backend/src/test/kotlin/io/realfds/alert/e2e/DateRangeSearchE2ETest.kt
  - @SpringBootTest + Testcontainers (PostgreSQL)
  - Given-When-Then 구조 사용
  - 1주일 분량의 샘플 데이터 생성 (매일 10개씩)
  - 날짜 범위 검색 API 호출 (3일 전 ~ 1일 전)
  - 해당 기간의 알림만 반환되는지 확인
  - 한국어 주석으로 테스트 시나리오 설명

**Checkpoint**: User Story 2 완료 - 날짜 범위 검색 기능 작동. User Story 1과 독립적으로 테스트 가능.

---

## Phase 5: User Story 3 - 다중 조건 필터링 (Priority: P3)

**Goal**: 규칙명, 사용자 ID, 알림 상태 등 여러 조건을 조합하여 원하는 알림만 필터링.

**Independent Test**: 다양한 규칙(HIGH_AMOUNT, RAPID_TRANSACTION), 상태(UNREAD, COMPLETED), 사용자로 알림 생성. 규칙명="HIGH_AMOUNT" AND 상태="UNREAD" 조건으로 검색하여 정확히 매칭되는 알림만 조회되는지 확인. Story 1, 2와 독립적으로 테스트 가능.

### Backend: Multi-Filter Support (User Story 3)

- [ ] T034 [US3] Update AlertSearchCriteria to include ruleName, userId, status filters in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/dto/AlertSearchCriteria.kt
  - ruleName, userId, status 필드 추가 (모두 nullable)
  - 검증 로직 추가 (ruleName은 Enum 값, userId는 패턴 검증)
  - 한국어 주석으로 필터 설명
- [ ] T035 [US3] Update CustomAlertRepositoryImpl to support multi-filter search in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/repository/CustomAlertRepositoryImpl.kt
  - ruleName 필터링 추가 (WHERE rule_name = :ruleName)
  - userId 필터링 추가 (WHERE user_id = :userId)
  - status 필터링 추가 (WHERE status = :status)
  - 동적 쿼리 생성 (선택된 필터만 WHERE 절에 포함)
  - 한국어 주석으로 필터 로직 설명

### 테스트 (User Story 3 - Backend Multi-Filter) ⚠️

- [ ] T036 [P] [US3] Integration test for ruleName filter in alert-dashboard/backend/src/test/kotlin/io/realfds/alert/repository/RuleNameFilterTest.kt
  - Testcontainers (PostgreSQL) 사용
  - Given-When-Then 구조 사용
  - 다양한 규칙의 알림 생성 (HIGH_AMOUNT, FOREIGN_COUNTRY, RAPID_TRANSACTION)
  - ruleName="HIGH_AMOUNT" 검색 → 해당 규칙의 알림만 반환 확인
  - 한국어 주석으로 테스트 시나리오 설명
- [ ] T037 [P] [US3] Integration test for userId filter in alert-dashboard/backend/src/test/kotlin/io/realfds/alert/repository/UserIdFilterTest.kt
  - Testcontainers (PostgreSQL) 사용
  - Given-When-Then 구조 사용
  - 여러 사용자의 알림 생성 (user-1 ~ user-10)
  - userId="user-5" 검색 → 해당 사용자의 알림만 반환 확인
  - 한국어 주석으로 테스트 시나리오 설명
- [ ] T038 [P] [US3] Integration test for status filter in alert-dashboard/backend/src/test/kotlin/io/realfds/alert/repository/StatusFilterTest.kt
  - Testcontainers (PostgreSQL) 사용
  - Given-When-Then 구조 사용
  - 다양한 상태의 알림 생성 (UNREAD, IN_PROGRESS, COMPLETED)
  - status="UNREAD" 검색 → 미확인 알림만 반환 확인
  - 한국어 주석으로 테스트 시나리오 설명
- [ ] T039 [US3] Integration test for combined filters in alert-dashboard/backend/src/test/kotlin/io/realfds/alert/repository/CombinedFiltersTest.kt
  - Testcontainers (PostgreSQL) 사용
  - Given-When-Then 구조 사용
  - 다양한 조합의 알림 생성
  - 다중 필터 검색 (ruleName="HIGH_AMOUNT" AND status="UNREAD")
  - 모든 조건을 만족하는 알림만 반환 확인
  - 한국어 주석으로 테스트 시나리오 설명

### Frontend: Filter Components (User Story 3)

- [ ] T040 [P] [US3] Create AlertHistoryFilters component in alert-dashboard/frontend/src/components/AlertHistoryFilters.tsx
  - 규칙명 드롭다운 (HIGH_AMOUNT, FOREIGN_COUNTRY, RAPID_TRANSACTION)
  - 사용자 ID 입력 필드
  - 상태 드롭다운 (UNREAD, IN_PROGRESS, COMPLETED)
  - 검색 버튼
  - 초기화 버튼
  - 한국어 라벨
  - TypeScript 타입 정의
- [ ] T041 [US3] Update AlertHistoryPage to include AlertHistoryFilters in alert-dashboard/frontend/src/pages/AlertHistoryPage.tsx
  - AlertHistoryFilters 컴포넌트 추가
  - 필터 state 관리
  - 검색 버튼 클릭 시 API 호출 (모든 필터 포함)
  - 한국어 UI 텍스트
  - TypeScript 타입 정의
- [ ] T042 [US3] Update alertHistoryService to support all filter parameters in alert-dashboard/frontend/src/services/alertHistoryService.ts
  - ruleName, userId, status 쿼리 파라미터 추가
  - TypeScript 타입 정의

### 테스트 (User Story 3 - Frontend Multi-Filter) ⚠️

- [ ] T043 [US3] Unit test for AlertHistoryFilters component in alert-dashboard/frontend/src/components/AlertHistoryFilters.test.tsx
  - React Testing Library 사용
  - 필터 선택 시 onChange 호출 확인
  - 초기화 버튼 클릭 시 필터 리셋 확인
- [ ] T044 [US3] Integration test for multi-filter search in alert-dashboard/frontend/src/pages/AlertHistoryPage.test.tsx
  - React Testing Library 사용
  - Given-When-Then 구조 사용
  - 여러 필터 선택 → 검색 → 결과 표시 확인
  - Mock Service Worker (MSW)로 API 모킹

### 테스트 (User Story 3 - End-to-End Multi-Filter) ⚠️

- [ ] T045 [US3] End-to-end test for multi-filter search in alert-dashboard/backend/src/test/kotlin/io/realfds/alert/e2e/MultiFilterSearchE2ETest.kt
  - @SpringBootTest + Testcontainers (PostgreSQL)
  - Given-When-Then 구조 사용
  - 다양한 조합의 샘플 데이터 생성 (규칙, 상태, 사용자 조합)
  - 다중 필터 검색 API 호출 (ruleName + status + userId)
  - 모든 조건을 만족하는 알림만 반환되는지 확인
  - 한국어 주석으로 테스트 시나리오 설명

**Checkpoint**: User Story 3 완료 - 다중 조건 필터링 기능 작동. 모든 User Story가 독립적으로 작동 가능.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 여러 User Story에 영향을 주는 개선 사항

### Observability & Monitoring (Constitution V - MANDATORY)

- [ ] T046 [P] Implement health check endpoint for alert-dashboard in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/health/AlertDashboardHealthIndicator.kt
  - PostgreSQL 연결 상태 확인
  - R2DBC 커넥션 풀 상태 확인
  - 한국어 주석으로 헬스 체크 로직 설명
- [ ] T047 [P] Add structured logging (SLF4J + JSON) to all services
  - Logback JSON encoder 설정 (alert-dashboard/backend/src/main/resources/logback.xml)
  - 서비스 생명주기 이벤트 로깅
  - 중요 비즈니스 이벤트 로깅 (알림 저장, 검색)
  - 로그 메시지는 한국어로 작성
- [ ] T048 [P] Add metrics collection (Micrometer) in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/metrics/AlertHistoryMetrics.kt
  - alert.history.search.count (검색 횟수)
  - alert.history.search.duration (검색 응답 시간)
  - alert.persistence.success.count (저장 성공 횟수)
  - alert.persistence.failure.count (저장 실패 횟수)

### Documentation (Constitution VI - MANDATORY)

- [ ] T049 [P] Write README.md for alert-history feature in specs/003-alert-history/README.md
  - 기능 개요 (한국어)
  - 빠른 시작 가이드
  - 환경 변수 문서화
  - 문제 해결 섹션
- [ ] T050 [P] Add Korean comments to all complex logic
  - 동적 쿼리 생성 로직 주석
  - 필터링 로직 주석
  - 에러 처리 로직 주석
- [ ] T051 [P] Update main README.md to include alert-history feature
  - 기능 목록에 Alert History 추가
  - 관련 문서 링크 추가

### Quality & Testing (Constitution V - MANDATORY)

- [ ] T052 Verify ≥70% unit test coverage
  - Gradle JaCoCo 리포트 생성 (./gradlew jacocoTestReport)
  - 커버리지 확인
  - 누락된 테스트 추가
- [ ] T053 Run all integration tests
  - 모든 검색 조건 (날짜, 규칙, 사용자, 상태) 검증
  - 종단 간 시나리오 검증 (Kafka → DB → API → Frontend)
  - Testcontainers로 실제 PostgreSQL 사용
- [ ] T054 Performance testing with K6
  - 10,000개 알림 데이터 생성
  - 검색 응답 시간 <500ms 검증
  - 동시 사용자 10명 시뮬레이션
  - K6 스크립트 작성 (tests/performance/alert-history-load-test.js)
- [ ] T055 Code quality review
  - 함수 길이 ≤50줄 검증
  - 파일 길이 ≤300줄 검증
  - 서술적인 변수/함수명 검증
  - 중복 코드 제거

### Constitution Compliance Check (MANDATORY)

- [ ] T056 Verify all Constitution principles are followed
  - I. 학습 우선: PostgreSQL R2DBC 사용으로 비동기 DB 액세스 학습
  - II. 단순함: docker-compose up으로 PostgreSQL 자동 실행 확인
  - III. 실시간 우선: R2DBC 비동기 처리 확인, 실시간 알림과 영속화 병행
  - IV. 서비스 경계: alert-service 내부 확장 (별도 서비스 추가 안 함)
  - V. 품질 표준: 테스트 커버리지 ≥70%, 로깅, 헬스 체크 확인
  - VI. 한국어 우선: 주석, 문서, 로그 메시지 한국어 사용 확인
- [ ] T057 Verify MVP acceptance criteria
  - docker-compose up으로 모든 서비스 (PostgreSQL 포함) 시작
  - 시스템이 5분 내에 완전히 작동 (Flyway 마이그레이션 포함)
  - 30분 동안 충돌 없이 실행
  - 헬스 체크 엔드포인트 200 OK 응답

### Frontend: Alert History Table & Pagination (Cross-Cutting)

- [ ] T058 [P] Create AlertHistoryTable component in alert-dashboard/frontend/src/components/AlertHistoryTable.tsx
  - TanStack Table (React Table v8) 사용
  - 알림 목록 표시 (alertId, userId, amount, ruleName, severity, alertTimestamp, status)
  - 정렬 기능 (alertTimestamp 기본 내림차순)
  - 한국어 컬럼 헤더
  - TypeScript 타입 정의
- [ ] T059 [P] Create Pagination component in alert-dashboard/frontend/src/components/Pagination.tsx
  - Material-UI Pagination 사용
  - 현재 페이지, 전체 페이지 표시
  - 이전/다음 버튼
  - 페이지 번호 클릭 이벤트
  - 한국어 라벨
  - TypeScript 타입 정의
- [ ] T060 Update AlertHistoryPage to integrate table and pagination in alert-dashboard/frontend/src/pages/AlertHistoryPage.tsx
  - AlertHistoryTable 컴포넌트 추가
  - Pagination 컴포넌트 추가
  - React Query (TanStack Query) 사용하여 서버 상태 관리
  - 로딩 상태 표시
  - 에러 상태 표시
  - 한국어 UI 텍스트
  - TypeScript 타입 정의

### Logging & Observability

- [ ] T061 Add comprehensive logging to AlertHistoryService
  - 검색 시작 로깅 (INFO): "알림 이력 검색 시작: criteria={...}"
  - 검색 완료 로깅 (INFO): "{count}개의 알림을 {duration}ms에 조회했습니다"
  - 빈 결과 로깅 (WARN): "검색 조건에 맞는 알림이 없습니다: {criteria}"
  - 데이터베이스 오류 로깅 (ERROR): "알림 검색 실패: {error}"
  - 로그 메시지는 한국어로 작성
- [ ] T062 Add comprehensive logging to AlertService persistence
  - 저장 시작 로깅 (INFO): "알림 저장 시작: alertId={alertId}"
  - 저장 성공 로깅 (INFO): "알림 저장 성공: alertId={alertId}"
  - 저장 실패 로깅 (ERROR): "알림 저장 실패: alertId={alertId}, error={error}"
  - 재시도 로깅 (WARN): "알림 저장 재시도 {attempt}/3: alertId={alertId}"
  - 로그 메시지는 한국어로 작성

### Final Polish

- [ ] T063 Code cleanup and refactoring
  - 중복 코드 제거 (DRY 원칙)
  - 코드 스타일 일관성 확인 (Kotlin Coding Conventions)
  - 불필요한 import 제거
- [ ] T064 Run quickstart.md validation
  - quickstart.md의 모든 단계를 실제로 실행하여 작동 확인
  - 오류가 있으면 문서 업데이트
- [ ] T065 Security review (basic - no auth required)
  - SQL Injection 방지 확인 (R2DBC 파라미터 바인딩 사용)
  - 데이터 검증 확인 (AlertSearchCriteria 검증 로직)
  - 오류 메시지에 민감 정보 미포함 확인
  - 한국어 오류 메시지 사용 (스택 트레이스 미노출)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 의존성 없음 - 즉시 시작 가능
- **Foundational (Phase 2)**: Setup 완료 후 - 모든 User Story를 차단
- **User Stories (Phase 3, 4, 5)**: Foundational 완료 후
  - User Story 1 (P1): Foundational 완료 후 즉시 시작 가능
  - User Story 2 (P2): Foundational 완료 후 시작 가능 (US1과 병렬 가능, 프론트엔드는 US1 완료 후 권장)
  - User Story 3 (P3): Foundational 완료 후 시작 가능 (US1, US2와 병렬 가능)
- **Polish (Phase 6)**: 모든 원하는 User Story 완료 후

### User Story Dependencies

- **User Story 1 (P1)**: Foundational 완료 후 시작 - 다른 Story에 의존하지 않음
- **User Story 2 (P2)**: Foundational 완료 후 시작 - US1과 독립적이지만 UI 통합 시 US1 권장
- **User Story 3 (P3)**: Foundational 완료 후 시작 - US1, US2와 독립적이지만 UI 통합 시 US1, US2 권장

### Within Each User Story

- 테스트를 먼저 작성하고 FAIL 확인 후 구현 시작
- DTO/Entity → Repository → Service → Controller 순서
- 핵심 구현 → 통합 → Story 완료
- 다음 우선순위 Story로 이동 전에 현재 Story 완료

### Parallel Opportunities

- Setup 단계의 모든 [P] 태스크는 병렬 실행 가능
- Foundational 단계의 모든 [P] 태스크는 병렬 실행 가능 (Phase 2 내에서)
- Foundational 완료 후, 모든 User Story는 병렬 시작 가능 (팀 인력 충분 시)
- 각 User Story 내에서 [P] 태스크는 병렬 실행 가능
- 서로 다른 User Story는 다른 팀원이 병렬로 작업 가능

---

## Parallel Example: User Story 1

```bash
# User Story 1의 모든 DTO를 함께 실행:
Task: "Create AlertSearchCriteria DTO in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/dto/AlertSearchCriteria.kt"
Task: "Create PagedAlertResult DTO in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/dto/PagedAlertResult.kt"

# User Story 1의 모든 단위 테스트를 함께 실행:
Task: "Unit test for Alert entity validation"
Task: "Unit test for AlertSearchCriteria validation"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 완료: Setup
2. Phase 2 완료: Foundational (중요 - 모든 Story 차단)
3. Phase 3 완료: User Story 1
4. **정지 및 검증**: User Story 1을 독립적으로 테스트
5. 준비되면 배포/데모

### Incremental Delivery

1. Setup + Foundational 완료 → 기반 준비
2. User Story 1 추가 → 독립 테스트 → 배포/데모 (MVP!)
3. User Story 2 추가 → 독립 테스트 → 배포/데모
4. User Story 3 추가 → 독립 테스트 → 배포/데모
5. 각 Story는 이전 Story를 망가뜨리지 않고 가치를 추가

### Parallel Team Strategy

여러 개발자가 있을 경우:

1. 팀이 함께 Setup + Foundational 완료
2. Foundational 완료 후:
   - 개발자 A: User Story 1 (Backend)
   - 개발자 B: User Story 2 (Frontend - US1 완료 후 통합)
   - 개발자 C: User Story 3 (Backend)
3. Story 완료 후 독립적으로 통합

---

## Summary

- **총 태스크 수**: 65개
- **User Story 1 (P1)**: 15개 태스크 (T013-T027) - MVP 범위
- **User Story 2 (P2)**: 6개 태스크 (T028-T033) - 날짜 범위 검색
- **User Story 3 (P3)**: 12개 태스크 (T034-T045) - 다중 필터링
- **Setup & Foundational**: 12개 태스크 (T001-T012)
- **Polish & Cross-Cutting**: 20개 태스크 (T046-T065)
- **병렬 실행 가능**: 약 30개 태스크 [P] 태그
- **테스트 태스크**: 약 20개 (Constitution V 요구사항 준수)

### MVP Scope 권장사항

**MVP는 User Story 1만 포함**:
- 기본 알림 이력 조회 및 영속화
- 페이지네이션
- 시스템 재시작 후 데이터 보존
- 총 27개 태스크 (Setup + Foundational + US1)

User Story 2, 3은 MVP 이후 점진적으로 추가 가능합니다.

---

## Notes

- [P] 태스크 = 서로 다른 파일, 의존성 없음
- [Story] 라벨은 태스크를 특정 User Story에 매핑하여 추적 가능
- 각 User Story는 독립적으로 완료 및 테스트 가능
- 구현 전에 테스트가 실패하는지 확인
- 각 태스크 또는 논리적 그룹 후 커밋
- 체크포인트에서 정지하여 Story를 독립적으로 검증
- 피해야 할 것: 모호한 태스크, 같은 파일 충돌, Story 독립성을 깨는 교차 의존성

---

**Tasks Status**: ✅ Ready for Implementation
**Generated**: 2025-11-11
**Based on**: spec.md, plan.md, data-model.md, research.md, alert-history-api.yaml
