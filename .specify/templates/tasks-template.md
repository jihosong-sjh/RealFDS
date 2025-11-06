---

description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

---

**⚠️ Constitution 준수 필수 사항**:

1. **테스트 우선 (Constitution V)**:
   - 단위 테스트 ≥70% 커버리지 필수
   - 통합 테스트는 모든 탐지 규칙에 대해 필수
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

## Path Conventions

- **Single project**: `src/`, `tests/` at repository root
- **Web app**: `backend/src/`, `frontend/src/`
- **Mobile**: `api/src/`, `ios/src/` or `android/src/`
- Paths shown below assume single project - adjust based on plan.md structure

<!-- 
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.
  
  The /speckit.tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md (with their priorities P1, P2, P3...)
  - Feature requirements from plan.md
  - Entities from data-model.md
  - Endpoints from contracts/
  
  Tasks MUST be organized by user story so each story can be:
  - Implemented independently
  - Tested independently
  - Delivered as an MVP increment
  
  DO NOT keep these sample tasks in the generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Create project structure per implementation plan
- [ ] T002 Initialize [language] project with [framework] dependencies
- [ ] T003 [P] Configure linting and formatting tools

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

Examples of foundational tasks (adjust based on your project):

- [ ] T004 Setup database schema and migrations framework
- [ ] T005 [P] Implement authentication/authorization framework
- [ ] T006 [P] Setup API routing and middleware structure
- [ ] T007 Create base models/entities that all stories depend on
- [ ] T008 Configure error handling and logging infrastructure
- [ ] T009 Setup environment configuration management

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 1 (MANDATORY per Constitution V) ⚠️

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**
> **Constitution V 요구사항**: ≥70% 커버리지, Given-When-Then 구조 사용

- [ ] T010 [P] [US1] Contract test for [endpoint] in tests/contract/test_[name].py
  - Given-When-Then 구조 사용
  - 한국어 주석으로 테스트 의도 설명
- [ ] T011 [P] [US1] Integration test for [user journey] in tests/integration/test_[name].py
  - 종단 간 시나리오 검증
  - 한국어 주석으로 시나리오 설명

### Implementation for User Story 1

- [ ] T012 [P] [US1] Create [Entity1] model in src/models/[entity1].py
  - 한국어 주석으로 필드 설명
  - 서술적인 변수명 사용
- [ ] T013 [P] [US1] Create [Entity2] model in src/models/[entity2].py
  - 한국어 주석으로 필드 설명
  - 서술적인 변수명 사용
- [ ] T014 [US1] Implement [Service] in src/services/[service].py (depends on T012, T013)
  - 함수 길이 ≤50줄 준수
  - 한국어 주석으로 비즈니스 로직 설명
- [ ] T015 [US1] Implement [endpoint/feature] in src/[location]/[file].py
  - 한국어 주석으로 기능 설명
- [ ] T016 [US1] Add validation and error handling
  - 컨텍스트와 함께 오류 로깅 (Constitution V)
  - 예외를 조용히 무시하지 않음
- [ ] T017 [US1] Add structured logging (SLF4J + JSON) for user story 1 operations
  - 중요 비즈니스 이벤트 로깅 (INFO 레벨)
  - 오류는 컨텍스트와 함께 로깅 (ERROR 레벨)
  - 로그 메시지는 한국어로 작성

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 2 (MANDATORY per Constitution V) ⚠️

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T018 [P] [US2] Contract test for [endpoint] in tests/contract/test_[name].py
  - Given-When-Then 구조 사용
- [ ] T019 [P] [US2] Integration test for [user journey] in tests/integration/test_[name].py
  - 종단 간 시나리오 검증

### Implementation for User Story 2

- [ ] T020 [P] [US2] Create [Entity] model in src/models/[entity].py
- [ ] T021 [US2] Implement [Service] in src/services/[service].py
- [ ] T022 [US2] Implement [endpoint/feature] in src/[location]/[file].py
- [ ] T023 [US2] Integrate with User Story 1 components (if needed)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 3 (MANDATORY per Constitution V) ⚠️

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T024 [P] [US3] Contract test for [endpoint] in tests/contract/test_[name].py
  - Given-When-Then 구조 사용
- [ ] T025 [P] [US3] Integration test for [user journey] in tests/integration/test_[name].py
  - 종단 간 시나리오 검증

### Implementation for User Story 3

- [ ] T026 [P] [US3] Create [Entity] model in src/models/[entity].py
- [ ] T027 [US3] Implement [Service] in src/services/[service].py
- [ ] T028 [US3] Implement [endpoint/feature] in src/[location]/[file].py

**Checkpoint**: All user stories should now be independently functional

---

[Add more user story phases as needed, following the same pattern]

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

### Observability & Monitoring (Constitution V - MANDATORY)

- [ ] TXXX [P] Implement health check endpoints for all services (`/actuator/health`)
  - TGS: 이벤트 생성률 및 Kafka 연결 상태 포함
  - RDE: 거래 처리율, 알림 생성률, 상태 저장소 크기 포함
  - RAD: WebSocket 연결 수, 알림 브로드캐스트 수 포함
- [ ] TXXX [P] Add structured logging (SLF4J + JSON) to all services
  - 서비스 생명주기 이벤트 로깅
  - 중요 비즈니스 이벤트 로깅
  - 로그 메시지는 한국어로 작성
- [ ] TXXX Implement circuit breaker for Kafka connections
  - 지수적 백오프 (1s, 2s, 4s, 8s, 최대 30s)
  - 연결 실패 로깅
- [ ] TXXX [P] Add metrics collection (Micrometer)
  - TGS: transactions_generated_total, generation_latency
  - RDE: transactions_processed_total, alerts_generated_total, processing_latency
  - RAD: alerts_consumed_total, websocket_messages_sent_total, websocket_connections_active

### Documentation (Constitution VI - MANDATORY)

- [ ] TXXX [P] Write/update README.md in Korean
  - 빠른 시작 가이드
  - 환경 변수 문서화
  - 문제 해결 섹션
- [ ] TXXX [P] Add Korean comments to all complex logic
  - 비즈니스 로직 설명
  - 알고리즘 설명
  - 설정 파라미터 설명
- [ ] TXXX Update service-level README.md files
  - 서비스 목적 및 책임
  - 입력/출력 설명
  - 로컬 실행 방법

### Quality & Testing (Constitution V - MANDATORY)

- [ ] TXXX Verify ≥70% unit test coverage
  - 커버리지 리포트 생성
  - 누락된 테스트 추가
- [ ] TXXX Run all integration tests
  - 3가지 탐지 규칙 모두 검증
  - 종단 간 시나리오 검증
- [ ] TXXX Performance testing
  - 평균 종단 간 지연 시간 <5초 검증
  - p95 지연 시간 <8초 검증
- [ ] TXXX Code quality review
  - 함수 길이 ≤50줄 검증
  - 파일 길이 ≤300줄 검증
  - 서술적인 변수/함수명 검증

### Constitution Compliance Check (MANDATORY)

- [ ] TXXX Verify all Constitution principles are followed
  - I. 학습 우선: 실시간 스트리밍 개념 명확히 시연
  - II. 단순함: `docker-compose up` 동작 확인
  - III. 실시간 우선: 이벤트 기반 통신, WebSocket 사용 확인
  - IV. 서비스 경계: 정확히 3개 서비스, 독립 배포 가능 확인
  - V. 품질 표준: 테스트 커버리지, 로깅, 오류 처리 확인
  - VI. 한국어 우선: 주석, 문서, 커밋 메시지 확인
- [ ] TXXX Verify MVP acceptance criteria (from Constitution)
  - docker-compose up으로 모든 서비스 시작
  - 시스템이 5분 내에 완전히 작동
  - 30분 동안 충돌 없이 실행
  - 헬스 체크 엔드포인트 200 OK 응답

### Final Polish

- [ ] TXXX Code cleanup and refactoring
  - 중복 코드 제거
  - 코드 스타일 일관성 확인
- [ ] TXXX Run quickstart.md validation
  - 문서화된 단계가 실제로 작동하는지 검증
- [ ] TXXX Security review (basic - no auth required)
  - 데이터 검증 확인
  - 오류 메시지에 민감 정보 미포함 확인

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but should be independently testable

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (if tests requested):
Task: "Contract test for [endpoint] in tests/contract/test_[name].py"
Task: "Integration test for [user journey] in tests/integration/test_[name].py"

# Launch all models for User Story 1 together:
Task: "Create [Entity1] model in src/models/[entity1].py"
Task: "Create [Entity2] model in src/models/[entity2].py"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
