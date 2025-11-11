# Tasks: 알림 확인 및 처리 시스템 (Alert Management System)

**Input**: Design documents from `/specs/002-alert-management/`
**Prerequisites**: plan.md, spec.md, checklists/requirements.md

---

**⚠️ Constitution 준수 필수 사항**:

1. **테스트 우선 (Constitution V)**:
   - 단위 테스트 ≥70% 커버리지 필수
   - 통합 테스트는 모든 상태 전이 시나리오에 대해 필수
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

**Tests**: 테스트는 이 프로젝트에 필수입니다 (Constitution V에 ≥70% 커버리지 요구)

**Organization**: 작업은 각 User Story별로 독립적인 구현 및 테스트가 가능하도록 User Story 단위로 그룹화되었습니다.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[Story]**: 어떤 User Story에 속하는지 (예: US1, US2, US3)
- 설명에 정확한 파일 경로 포함

## 경로 규칙

이 프로젝트는 마이크로서비스 구조로 아래 경로를 사용합니다:

- `fraud-detector/src/main/scala/` (Scala/Flink)
- `alert-service/src/main/java/` (Spring Boot)
- `websocket-gateway/src/main/java/` (Spring Boot)
- `frontend-dashboard/src/` (React + TypeScript)

---

## Phase 1: Setup (공유 인프라)

**목적**: 프로젝트 초기화 및 문서 생성

- [X] T001 Phase 1 문서 생성: data-model.md 작성 (Alert 및 DetectionRule 엔터티 확장)
- [X] T002 [P] Phase 1 문서 생성: contracts/rest-api.md 작성 (REST API 엔드포인트 명세)
- [X] T003 [P] Phase 1 문서 생성: contracts/websocket-api.md 작성 (WebSocket 이벤트 스키마)
- [X] T004 기존 quickstart.md 업데이트 (알림 상태 관리 기능 사용법 추가)
- [X] T005 git 브랜치 생성 및 전환: 002-alert-management

---

## Phase 2: Foundational (블로킹 선행 작업)

**목적**: 모든 User Story 구현 전에 완료되어야 하는 핵심 인프라

**⚠️ 중요**: 이 Phase가 완료되어야 User Story 작업을 시작할 수 있습니다

- [X] T006 [P] fraud-detector: DetectionRule 모델에 severity 필드 추가 (fraud-detector/src/main/scala/com/realfds/detector/models/DetectionRule.scala)
- [X] T007 [P] fraud-detector: HighValueRule에 severity=HIGH 설정 추가 (fraud-detector/src/main/scala/com/realfds/detector/rules/HighValueRule.scala)
- [X] T008 [P] fraud-detector: ForeignCountryRule에 severity=MEDIUM 설정 추가 (fraud-detector/src/main/scala/com/realfds/detector/rules/ForeignCountryRule.scala)
- [X] T009 [P] fraud-detector: HighFrequencyRule에 severity=HIGH 설정 추가 (fraud-detector/src/main/scala/com/realfds/detector/rules/HighFrequencyRule.scala)
- [X] T010 [P] alert-service: Alert 모델에 status, assignedTo, actionNote, processedAt 필드 추가 (alert-service/src/main/java/com/realfds/alert/model/Alert.java)
- [X] T011 [P] alert-service: AlertStatus enum 생성 (UNREAD, IN_PROGRESS, COMPLETED) (alert-service/src/main/java/com/realfds/alert/model/AlertStatus.java)
- [X] T012 [P] alert-service: Severity enum 생성 (LOW, MEDIUM, HIGH, CRITICAL) (alert-service/src/main/java/com/realfds/alert/model/Severity.java)

**Checkpoint**: 기반 인프라 준비 완료 - User Story 구현을 병렬로 시작 가능

---

## Phase 3: User Story 1 - 알림 상태 관리 (Priority: P1) 🎯 MVP

**Goal**: 보안 담당자가 알림의 처리 상태를 관리하여 업무 진행 상황을 추적하고 중복 작업을 방지

**Independent Test**: 알림 상태를 변경하고 새로고침 후에도 상태가 유지되는지 확인. 다른 브라우저에서 접속 시 동일한 상태가 보이는지 검증.

**Acceptance Scenarios** (spec.md 참조):
1. 알림 상태가 "미확인"에서 "확인중"으로 변경되고 1초 이내에 UI에 반영
2. "확인중" → "완료" 상태 변경 시 처리 완료 시각 기록
3. 상태별 필터링 동작
4. 브라우저 새로고침 시 상태 유지
5. 다른 브라우저에 실시간 상태 동기화

### Tests for User Story 1 (MANDATORY per Constitution V) ⚠️

> **중요: 테스트를 먼저 작성하고, 실패하는지 확인한 후 구현 시작**
> **Constitution V 요구사항**: ≥70% 커버리지, Given-When-Then 구조 사용

- [X] T013 [P] [US1] alert-service: AlertService 상태 변경 로직 단위 테스트 작성 (alert-service/src/test/java/com/realfds/alert/service/AlertServiceTest.java)
  - Given: UNREAD 상태의 알림, When: IN_PROGRESS로 변경, Then: 상태 변경 성공 및 processedAt null 유지
  - Given: IN_PROGRESS 상태의 알림, When: COMPLETED로 변경, Then: 상태 변경 성공 및 processedAt 자동 설정
  - 한국어 주석으로 테스트 의도 설명
- [X] T014 [P] [US1] alert-service: 상태별 필터링 로직 단위 테스트 작성 (alert-service/src/test/java/com/realfds/alert/service/AlertFilterTest.java)
  - Given: 다양한 상태의 알림 목록, When: status=UNREAD 필터 적용, Then: UNREAD 알림만 반환
  - 한국어 주석으로 필터링 로직 설명
- [X] T015 [P] [US1] alert-service: REST API 엔드포인트 통합 테스트 작성 (alert-service/src/test/java/com/realfds/alert/controller/AlertControllerIntegrationTest.java)
  - Given: 테스트 알림 생성, When: PATCH /api/alerts/{id}/status 호출, Then: 200 OK 및 상태 변경 확인
  - 한국어 주석으로 API 계약 설명

### Implementation for User Story 1

#### Backend: alert-service

- [X] T016 [US1] alert-service: AlertRepository에 상태 업데이트 메서드 추가 (alert-service/src/main/java/com/realfds/alert/repository/AlertRepository.java)
  - updateStatus(String alertId, AlertStatus status): 상태 업데이트
  - updateProcessedAt(String alertId, LocalDateTime processedAt): 처리 시각 기록
  - 최대 함수 길이 50줄 준수
  - 한국어 주석으로 메서드 설명
- [X] T017 [US1] alert-service: AlertService에 상태 관리 비즈니스 로직 추가 (alert-service/src/main/java/com/realfds/alert/service/AlertService.java)
  - changeStatus(String alertId, AlertStatus newStatus): 상태 전이 로직
  - COMPLETED 상태 시 processedAt 자동 설정
  - 상태 전이 검증 (UNREAD → IN_PROGRESS → COMPLETED, 역방향 가능)
  - 한국어 주석으로 비즈니스 규칙 설명
- [X] T018 [US1] alert-service: AlertService에 필터링 로직 추가 (alert-service/src/main/java/com/realfds/alert/service/AlertService.java)
  - filterByStatus(AlertStatus status): 상태별 필터링
  - 응답 시간 <100ms 목표 (100개 알림 기준)
  - 한국어 주석으로 필터링 로직 설명
- [X] T019 [US1] alert-service: AlertController에 PATCH /api/alerts/{alertId}/status 엔드포인트 추가 (alert-service/src/main/java/com/realfds/alert/controller/AlertController.java)
  - Request: { "status": "IN_PROGRESS" | "COMPLETED" }
  - Response: { "alertId", "status", "processedAt" }
  - 에러 처리: 404 Not Found, 400 Bad Request
  - 한국어 주석으로 API 설명
- [X] T020 [US1] alert-service: GET /api/alerts에 status 쿼리 파라미터 지원 추가 (alert-service/src/main/java/com/realfds/alert/controller/AlertController.java)
  - Query: ?status=UNREAD | IN_PROGRESS | COMPLETED
  - 기존 엔드포인트 확장
  - 한국어 주석으로 파라미터 설명
- [X] T021 [US1] alert-service: 상태 변경 시 Kafka로 이벤트 발행 (alert-service/src/main/java/com/realfds/alert/service/AlertService.java)
  - Topic: alert-status-changed
  - Event: { alertId, status, processedAt }
  - 한국어 주석으로 이벤트 스키마 설명

#### Backend: websocket-gateway

- [X] T022 [US1] websocket-gateway: alert-status-changed Kafka 이벤트 구독 (websocket-gateway/src/main/java/com/realfds/gateway/service/KafkaConsumerService.java)
  - Topic: alert-status-changed 구독
  - 이벤트 수신 시 WebSocket 브로드캐스트 트리거
  - 한국어 주석으로 구독 로직 설명
- [X] T023 [US1] websocket-gateway: ALERT_STATUS_CHANGED WebSocket 이벤트 브로드캐스트 추가 (websocket-gateway/src/main/java/com/realfds/gateway/handler/AlertWebSocketHandler.java)
  - Event: { type: "ALERT_STATUS_CHANGED", alertId, status, processedAt }
  - 모든 연결된 클라이언트에 브로드캐스트
  - 1초 이내 브로드캐스트 목표
  - 한국어 주석으로 이벤트 구조 설명

#### Frontend: frontend-dashboard

- [X] T024 [P] [US1] frontend: Alert 타입에 status, processedAt 필드 추가 (frontend-dashboard/src/types/alert.ts)
  - status: 'UNREAD' | 'IN_PROGRESS' | 'COMPLETED'
  - processedAt: string | null
  - TypeScript 타입 정의
  - 한국어 주석으로 타입 설명
- [X] T025 [P] [US1] frontend: AlertStatus enum 타입 정의 (frontend-dashboard/src/types/alertStatus.ts)
  - UNREAD, IN_PROGRESS, COMPLETED
  - 한국어 주석으로 상태 설명
- [X] T026 [US1] frontend: useAlertManagement 커스텀 hook 생성 (frontend-dashboard/src/hooks/useAlertManagement.ts)
  - changeAlertStatus(alertId, newStatus): 상태 변경 API 호출
  - 에러 처리 및 로딩 상태 관리
  - 한국어 주석으로 hook 사용법 설명
- [X] T027 [US1] frontend: AlertItem 컴포넌트에 상태 뱃지 추가 (frontend-dashboard/src/components/AlertItem.tsx)
  - UNREAD: 회색, IN_PROGRESS: 파란색, COMPLETED: 초록색
  - 상태 텍스트 표시 (미확인/확인중/완료)
  - 한국어 주석으로 컴포넌트 설명
- [X] T028 [US1] frontend: AlertDetailModal 컴포넌트 생성 (frontend-dashboard/src/components/AlertDetailModal.tsx)
  - 알림 상세 정보 표시
  - 상태 변경 버튼 (확인중으로 변경, 완료 처리)
  - 모달 로딩 시간 <200ms 목표
  - 한국어 주석으로 UI 설명
- [X] T029 [US1] frontend: AlertFilterPanel 컴포넌트 생성 - 상태 필터 (frontend-dashboard/src/components/AlertFilterPanel.tsx)
  - 상태별 필터 드롭다운 (전체/미확인/확인중/완료)
  - 필터 적용 시 API 호출 및 목록 업데이트
  - 한국어 주석으로 필터 로직 설명
- [X] T030 [US1] frontend: WebSocket 이벤트 리스너에 ALERT_STATUS_CHANGED 처리 추가 (frontend-dashboard/src/hooks/useWebSocket.ts)
  - 이벤트 수신 시 알림 목록 자동 업데이트
  - 1초 이내 UI 반영 목표
  - 한국어 주석으로 동기화 로직 설명

#### Logging & Observability

- [X] T031 [US1] alert-service: 상태 변경 이벤트 구조화 로깅 추가 (alert-service/src/main/java/com/realfds/alert/service/AlertService.java)
  - INFO 레벨: 상태 변경 성공 (alertId, oldStatus, newStatus, processedAt 포함)
  - ERROR 레벨: 상태 변경 실패 (alertId, 오류 원인 포함)
  - 로그 메시지는 한국어로 작성
- [X] T032 [US1] websocket-gateway: 상태 변경 브로드캐스트 로깅 추가 (websocket-gateway/src/main/java/com/realfds/gateway/service/BroadcastService.java)
  - INFO 레벨: 브로드캐스트 성공 (eventType, alertId, 클라이언트 수 포함)
  - ERROR 레벨: 브로드캐스트 실패 (오류 원인 포함)
  - 로그 메시지는 한국어로 작성

**Checkpoint**: 이 시점에서 User Story 1은 완전히 기능하며 독립적으로 테스트 가능해야 합니다

---

## Phase 4: User Story 2 - 담당자 할당 및 조치 기록 (Priority: P1)

**Goal**: 보안 담당자가 알림에 담당자를 할당하고 조치 내용을 기록하여 업무 분담과 이력 관리를 수행

**Independent Test**: 알림에 담당자를 할당하고 조치 내용을 입력한 후, 해당 알림을 다시 조회하여 정보가 정확히 저장되었는지 확인.

**Acceptance Scenarios** (spec.md 참조):
1. 담당자 할당 및 목록에 표시
2. 담당자 미할당 시 "미할당" 표시
3. 조치 내용 입력 및 "완료" 상태 변경
4. 조치 완료된 알림 재조회 시 담당자 및 조치 내용 표시
5. 담당자별 알림 필터링

### Tests for User Story 2 (MANDATORY per Constitution V) ⚠️

> **중요: 테스트를 먼저 작성하고, 실패하는지 확인한 후 구현 시작**

- [X] T033 [P] [US2] alert-service: 담당자 할당 로직 단위 테스트 작성 (alert-service/src/test/java/com/realfds/alert/service/AlertAssignmentTest.java)
  - Given: 알림 생성, When: 담당자 할당, Then: assignedTo 필드 저장 및 최대 100자 검증
  - Given: 담당자 미할당, When: 알림 조회, Then: assignedTo null 반환
  - 한국어 주석으로 테스트 의도 설명
- [X] T034 [P] [US2] alert-service: 조치 내용 기록 로직 단위 테스트 작성 (alert-service/src/test/java/com/realfds/alert/service/AlertActionTest.java)
  - Given: 알림 생성, When: 조치 내용 입력 (최대 2000자), Then: actionNote 필드 저장
  - Given: 조치 내용 입력, When: 완료 처리, Then: status=COMPLETED 및 processedAt 자동 설정
  - 한국어 주석으로 비즈니스 규칙 설명
- [X] T035 [P] [US2] alert-service: 담당자별 필터링 통합 테스트 작성 (alert-service/src/test/java/com/realfds/alert/service/AlertFilterByAssigneeTest.java)
  - Given: 다양한 담당자의 알림 목록, When: assignedTo 필터 적용, Then: 해당 담당자 알림만 반환
  - 한국어 주석으로 필터링 로직 설명

### Implementation for User Story 2

#### Backend: alert-service

- [ ] T036 [US2] alert-service: AlertRepository에 담당자 할당 메서드 추가 (alert-service/src/main/java/com/realfds/alert/repository/AlertRepository.java)
  - assignTo(String alertId, String assignedTo): 담당자 할당
  - 최대 100자 검증
  - 한국어 주석으로 메서드 설명
- [ ] T037 [US2] alert-service: AlertRepository에 조치 내용 기록 메서드 추가 (alert-service/src/main/java/com/realfds/alert/repository/AlertRepository.java)
  - updateActionNote(String alertId, String actionNote): 조치 내용 저장
  - 최대 2000자 검증
  - 한국어 주석으로 메서드 설명
- [ ] T038 [US2] alert-service: AlertService에 담당자 할당 비즈니스 로직 추가 (alert-service/src/main/java/com/realfds/alert/service/AlertService.java)
  - assignAlert(String alertId, String assignedTo): 담당자 할당
  - 유효성 검증 (최대 100자)
  - 한국어 주석으로 비즈니스 규칙 설명
- [ ] T039 [US2] alert-service: AlertService에 조치 기록 비즈니스 로직 추가 (alert-service/src/main/java/com/realfds/alert/service/AlertService.java)
  - recordAction(String alertId, String actionNote, boolean complete): 조치 내용 기록
  - complete=true 시 status=COMPLETED 및 processedAt 자동 설정
  - 유효성 검증 (최대 2000자)
  - 한국어 주석으로 비즈니스 규칙 설명
- [ ] T040 [US2] alert-service: AlertService에 담당자별 필터링 로직 추가 (alert-service/src/main/java/com/realfds/alert/service/AlertService.java)
  - filterByAssignee(String assignedTo): 담당자별 필터링
  - 응답 시간 <100ms 목표
  - 한국어 주석으로 필터링 로직 설명
- [ ] T041 [US2] alert-service: AlertController에 PATCH /api/alerts/{alertId}/assign 엔드포인트 추가 (alert-service/src/main/java/com/realfds/alert/controller/AlertController.java)
  - Request: { "assignedTo": "김보안" }
  - Response: { "alertId", "assignedTo" }
  - 에러 처리: 404 Not Found, 400 Bad Request (100자 초과)
  - 한국어 주석으로 API 설명
- [ ] T042 [US2] alert-service: AlertController에 POST /api/alerts/{alertId}/action 엔드포인트 추가 (alert-service/src/main/java/com/realfds/alert/controller/AlertController.java)
  - Request: { "actionNote": "고객 확인 완료", "status": "COMPLETED" (optional) }
  - Response: { "alertId", "actionNote", "status", "processedAt" }
  - 에러 처리: 404 Not Found, 400 Bad Request (2000자 초과)
  - 한국어 주석으로 API 설명
- [ ] T043 [US2] alert-service: GET /api/alerts에 assignedTo 쿼리 파라미터 지원 추가 (alert-service/src/main/java/com/realfds/alert/controller/AlertController.java)
  - Query: ?assignedTo=김보안
  - 기존 엔드포인트 확장
  - 한국어 주석으로 파라미터 설명
- [ ] T044 [US2] alert-service: 담당자 할당 및 조치 기록 시 Kafka 이벤트 발행 (alert-service/src/main/java/com/realfds/alert/service/AlertService.java)
  - Topic: alert-status-changed (기존 이벤트 확장)
  - Event에 assignedTo, actionNote 필드 추가
  - 한국어 주석으로 이벤트 스키마 설명

#### Backend: websocket-gateway

- [X] T045 [US2] websocket-gateway: ALERT_STATUS_CHANGED 이벤트에 assignedTo, actionNote 필드 추가 (websocket-gateway/src/main/java/com/realfds/gateway/handler/AlertWebSocketHandler.java)
  - Event: { type: "ALERT_STATUS_CHANGED", alertId, status, assignedTo, actionNote, processedAt }
  - 기존 브로드캐스트 로직 확장
  - 한국어 주석으로 확장된 스키마 설명

#### Frontend: frontend-dashboard

- [X] T046 [P] [US2] frontend: Alert 타입에 assignedTo, actionNote 필드 추가 (frontend-dashboard/src/types/alert.ts)
  - assignedTo: string | null
  - actionNote: string | null
  - TypeScript 타입 정의
  - 한국어 주석으로 타입 설명
- [X] T047 [US2] frontend: useAlertManagement hook에 담당자 할당 함수 추가 (frontend-dashboard/src/hooks/useAlertManagement.ts)
  - assignAlert(alertId, assignedTo): 담당자 할당 API 호출
  - 최대 100자 클라이언트 검증
  - 한국어 주석으로 함수 설명
- [X] T048 [US2] frontend: useAlertManagement hook에 조치 기록 함수 추가 (frontend-dashboard/src/hooks/useAlertManagement.ts)
  - recordAction(alertId, actionNote, complete): 조치 기록 API 호출
  - 최대 2000자 클라이언트 검증
  - 한국어 주석으로 함수 설명
- [X] T049 [US2] frontend: AlertItem 컴포넌트에 담당자 표시 추가 (frontend-dashboard/src/components/AlertItem.tsx)
  - assignedTo 표시 (미할당 시 "미할당" 표시)
  - 담당자 아이콘 및 이름 표시
  - 한국어 주석으로 UI 설명
- [X] T050 [US2] frontend: AlertDetailModal에 담당자 할당 UI 추가 (frontend-dashboard/src/components/AlertDetailModal.tsx)
  - 담당자 입력 필드 (텍스트 인풋, 최대 100자)
  - 할당 버튼
  - 한국어 주석으로 폼 로직 설명
- [X] T051 [US2] frontend: AlertDetailModal에 조치 내용 입력 UI 추가 (frontend-dashboard/src/components/AlertDetailModal.tsx)
  - 조치 내용 텍스트 영역 (최대 2000자)
  - 완료 처리 버튼
  - 입력 권장 메시지 표시
  - 한국어 주석으로 폼 로직 설명
- [X] T052 [US2] frontend: AlertFilterPanel에 담당자 필터 추가 (frontend-dashboard/src/components/AlertFilterPanel.tsx)
  - 담당자별 필터 드롭다운 (전체/특정 담당자)
  - 필터 적용 시 API 호출 및 목록 업데이트
  - 한국어 주석으로 필터 로직 설명
- [X] T053 [US2] frontend: WebSocket 이벤트 리스너에 assignedTo, actionNote 필드 처리 추가 (frontend-dashboard/src/hooks/useWebSocket.ts)
  - 이벤트 수신 시 담당자 및 조치 내용 자동 업데이트
  - 한국어 주석으로 동기화 로직 설명

#### Logging & Observability

- [X] T054 [US2] alert-service: 담당자 할당 및 조치 기록 이벤트 구조화 로깅 추가 (alert-service/src/main/java/com/realfds/alert/service/AlertService.java)
  - INFO 레벨: 담당자 할당 성공 (alertId, assignedTo 포함)
  - INFO 레벨: 조치 기록 성공 (alertId, actionNote 길이, status 포함)
  - ERROR 레벨: 할당/기록 실패 (alertId, 오류 원인 포함)
  - 로그 메시지는 한국어로 작성

**Checkpoint**: 이 시점에서 User Story 1과 2 모두 독립적으로 동작해야 합니다

---

## Phase 5: User Story 3 - 알림 우선순위 (심각도) 표시 (Priority: P2)

**Goal**: 시스템이 탐지 규칙별로 설정된 심각도를 알림에 자동으로 할당하고, UI에서 색상으로 구분하여 표시

**Independent Test**: 각 탐지 규칙으로 생성된 알림의 색상이 설정된 심각도에 맞게 표시되는지 확인.

**Acceptance Scenarios** (spec.md 참조):
1. 고액 거래 알림: 주황색 + "HIGH" 뱃지
2. 해외 거래 알림: 노란색 + "MEDIUM" 뱃지
3. 빈번한 거래 알림: 주황색 + "HIGH" 뱃지
4. 심각도별 정렬 (CRITICAL → HIGH → MEDIUM → LOW)
5. 심각도별 필터링

### Tests for User Story 3 (MANDATORY per Constitution V) ⚠️

> **중요: 테스트를 먼저 작성하고, 실패하는지 확인한 후 구현 시작**

- [X] T055 [P] [US3] fraud-detector: 규칙별 severity 설정 단위 테스트 작성 (fraud-detector/src/test/scala/com/realfds/detector/rules/SeverityAssignmentTest.scala)
  - Given: HighValueRule 실행, When: 알림 생성, Then: severity=HIGH 확인
  - Given: ForeignCountryRule 실행, When: 알림 생성, Then: severity=MEDIUM 확인
  - Given: HighFrequencyRule 실행, When: 알림 생성, Then: severity=HIGH 확인
  - 한국어 주석으로 테스트 의도 설명
- [X] T056 [P] [US3] alert-service: 심각도별 필터링 단위 테스트 작성 (alert-service/src/test/java/com/realfds/alert/service/AlertFilterBySeverityTest.java)
  - Given: 다양한 심각도의 알림 목록, When: severity=HIGH 필터 적용, Then: HIGH 알림만 반환
  - 한국어 주석으로 필터링 로직 설명
- [X] T057 [P] [US3] alert-service: 심각도별 정렬 단위 테스트 작성 (alert-service/src/test/java/com/realfds/alert/service/AlertSortBySeverityTest.java)
  - Given: 다양한 심각도의 알림 목록, When: 심각도별 정렬, Then: CRITICAL → HIGH → MEDIUM → LOW 순서 확인
  - 한국어 주석으로 정렬 로직 설명

### Implementation for User Story 3

#### Backend: fraud-detector (이미 Phase 2에서 완료됨)

- ✅ T006-T009: fraud-detector에 severity 설정이 이미 Phase 2에서 완료되었으므로 추가 작업 불필요

#### Backend: alert-service

- [ ] T058 [US3] alert-service: AlertService에 심각도별 필터링 로직 추가 (alert-service/src/main/java/com/realfds/alert/service/AlertService.java)
  - filterBySeverity(Severity severity): 심각도별 필터링
  - 응답 시간 <100ms 목표
  - 한국어 주석으로 필터링 로직 설명
- [ ] T059 [US3] alert-service: AlertService에 심각도별 정렬 로직 추가 (alert-service/src/main/java/com/realfds/alert/service/AlertService.java)
  - sortBySeverity(): CRITICAL → HIGH → MEDIUM → LOW 순서 정렬
  - Comparator 구현
  - 한국어 주석으로 정렬 로직 설명
- [ ] T060 [US3] alert-service: GET /api/alerts에 severity 쿼리 파라미터 지원 추가 (alert-service/src/main/java/com/realfds/alert/controller/AlertController.java)
  - Query: ?severity=LOW | MEDIUM | HIGH | CRITICAL
  - 기존 엔드포인트 확장
  - 한국어 주석으로 파라미터 설명
- [ ] T061 [US3] alert-service: GET /api/alerts에 sortBy=severity 쿼리 파라미터 지원 추가 (alert-service/src/main/java/com/realfds/alert/controller/AlertController.java)
  - Query: ?sortBy=severity
  - 기존 엔드포인트 확장
  - 한국어 주석으로 파라미터 설명

#### Frontend: frontend-dashboard

- [ ] T062 [P] [US3] frontend: Severity enum 타입 정의 (frontend-dashboard/src/types/severity.ts)
  - LOW, MEDIUM, HIGH, CRITICAL
  - 한국어 주석으로 심각도 설명
- [ ] T063 [P] [US3] frontend: SeverityBadge 컴포넌트 생성 (frontend-dashboard/src/components/SeverityBadge.tsx)
  - CRITICAL: 빨간색, HIGH: 주황색, MEDIUM: 노란색, LOW: 파란색
  - 심각도 텍스트 표시 (낮음/보통/높음/긴급)
  - 한국어 주석으로 컴포넌트 설명
- [ ] T064 [US3] frontend: AlertItem 컴포넌트에 SeverityBadge 추가 (frontend-dashboard/src/components/AlertItem.tsx)
  - severity 필드를 SeverityBadge에 전달
  - 색상 코딩으로 긴급 알림 시각적 구분
  - 한국어 주석으로 UI 설명
- [ ] T065 [US3] frontend: AlertItem 컴포넌트에 심각도별 배경색 추가 (frontend-dashboard/src/components/AlertItem.tsx)
  - CRITICAL: 빨간색 배경 (연한색), HIGH: 주황색 배경, MEDIUM: 노란색 배경, LOW: 파란색 배경
  - 시각적 구분 강화
  - 한국어 주석으로 스타일 설명
- [ ] T066 [US3] frontend: AlertFilterPanel에 심각도 필터 추가 (frontend-dashboard/src/components/AlertFilterPanel.tsx)
  - 심각도별 필터 드롭다운 (전체/낮음/보통/높음/긴급)
  - 필터 적용 시 API 호출 및 목록 업데이트
  - 한국어 주석으로 필터 로직 설명
- [ ] T067 [US3] frontend: AlertList에 심각도별 정렬 기능 추가 (frontend-dashboard/src/components/AlertList.tsx)
  - 정렬 버튼 (심각도순)
  - 정렬 적용 시 API 호출 및 목록 업데이트
  - 한국어 주석으로 정렬 로직 설명

#### Logging & Observability

- [ ] T068 [US3] alert-service: 심각도별 필터링 및 정렬 로깅 추가 (alert-service/src/main/java/com/realfds/alert/service/AlertService.java)
  - DEBUG 레벨: 필터링/정렬 요청 (severity, 결과 개수 포함)
  - 로그 메시지는 한국어로 작성

**Checkpoint**: 모든 User Story가 이제 독립적으로 기능합니다

---

## Phase 6: Polish & Cross-Cutting Concerns

**목적**: 여러 User Story에 영향을 주는 개선 사항

### Observability & Monitoring (Constitution V - MANDATORY)

- [ ] T069 [P] fraud-detector: 헬스 체크 엔드포인트 확인 및 severity 필드 반영 (fraud-detector/src/main/scala/com/realfds/detector/health/HealthCheck.scala)
  - /actuator/health가 severity 설정 반영 여부 포함
  - 한국어 주석으로 헬스 체크 설명
- [ ] T070 [P] alert-service: 헬스 체크 엔드포인트에 상태 관리 통계 추가 (alert-service/src/main/java/com/realfds/alert/health/AlertHealthIndicator.java)
  - /actuator/health에 알림 개수(상태별, 심각도별) 포함
  - 한국어 주석으로 헬스 체크 설명
- [ ] T071 [P] websocket-gateway: 헬스 체크 엔드포인트에 WebSocket 연결 수 추가 (websocket-gateway/src/main/java/com/realfds/gateway/health/WebSocketHealthIndicator.java)
  - /actuator/health에 활성 WebSocket 연결 수 포함
  - 한국어 주석으로 헬스 체크 설명
- [ ] T072 alert-service: Kafka 연결에 서킷 브레이커 검증 (alert-service/src/main/java/com/realfds/alert/config/KafkaConfig.java)
  - 지수적 백오프 (1s, 2s, 4s, 8s, 최대 30s) 동작 확인
  - 연결 실패 로깅 확인
  - 한국어 주석으로 서킷 브레이커 로직 설명
- [ ] T073 [P] alert-service: Micrometer 메트릭 추가 (alert-service/src/main/java/com/realfds/alert/service/AlertService.java)
  - alerts_status_changed_total (counter)
  - alerts_assigned_total (counter)
  - alert_status_change_latency (histogram)
  - 한국어 주석으로 메트릭 설명
- [ ] T074 [P] websocket-gateway: Micrometer 메트릭 추가 (websocket-gateway/src/main/java/com/realfds/gateway/handler/AlertWebSocketHandler.java)
  - websocket_broadcast_total (counter)
  - websocket_broadcast_latency (histogram)
  - 한국어 주석으로 메트릭 설명

### Documentation (Constitution VI - MANDATORY)

- [ ] T075 [P] 프로젝트 루트 README.md 업데이트 (README.md)
  - 알림 상태 관리 기능 설명 추가
  - 담당자 할당 및 조치 기록 기능 설명 추가
  - 심각도별 우선순위 기능 설명 추가
  - 한국어로 작성
- [ ] T076 [P] alert-service: README.md 생성 또는 업데이트 (alert-service/README.md)
  - 서비스 목적 및 책임 설명
  - 새로운 API 엔드포인트 문서화
  - 상태 관리 로직 설명
  - 한국어로 작성
- [ ] T077 [P] websocket-gateway: README.md 업데이트 (websocket-gateway/README.md)
  - ALERT_STATUS_CHANGED 이벤트 스키마 문서화
  - 한국어로 작성
- [ ] T078 [P] frontend-dashboard: README.md 업데이트 (frontend-dashboard/README.md)
  - 새로운 컴포넌트 설명 (AlertDetailModal, AlertFilterPanel, SeverityBadge)
  - 상태 관리 hook 사용법 설명
  - 한국어로 작성

### Quality & Testing (Constitution V - MANDATORY)

- [ ] T079 ≥70% 단위 테스트 커버리지 검증
  - alert-service: Jacoco 커버리지 리포트 생성
  - fraud-detector: scoverage 커버리지 리포트 생성
  - 누락된 테스트 추가
- [ ] T080 통합 테스트 실행
  - User Story 1: 상태 변경 E2E 시나리오
  - User Story 2: 담당자 할당 및 조치 기록 E2E 시나리오
  - User Story 3: 심각도별 색상 구분 E2E 시나리오
  - 종단 간 시나리오 검증
- [ ] T081 성능 테스트
  - 알림 상태 변경 지연 시간 측정 (목표: 평균 0.5초, 최대 1초)
  - 필터링 응답 시간 측정 (목표: <100ms)
  - 모달 로딩 시간 측정 (목표: <200ms)
  - WebSocket 브로드캐스트 지연 시간 측정 (목표: <1초)
- [ ] T082 코드 품질 검토
  - 함수 길이 ≤50줄 검증
  - 파일 길이 ≤300줄 검증
  - 서술적인 변수/함수명 검증
  - 한국어 주석 완전성 검증

### Constitution Compliance Check (MANDATORY)

- [ ] T083 Constitution 원칙 준수 검증
  - I. 학습 우선: 실시간 상태 동기화로 WebSocket 개념 강화
  - II. 단순함: `docker-compose up` 동작 확인 (추가 컨테이너 없음)
  - III. 실시간 우선: 이벤트 기반 통신 및 WebSocket 사용 확인
  - IV. 서비스 경계: 5개 서비스 유지, 독립 배포 가능 확인 (001에서 정당화됨)
  - V. 품질 표준: 테스트 커버리지 ≥70%, 구조화된 로깅, 오류 처리 확인
  - VI. 한국어 우선: 주석, 문서, 커밋 메시지 한국어 확인
- [ ] T084 MVP acceptance criteria 검증 (from Constitution)
  - docker-compose up으로 모든 서비스 시작 확인
  - 시스템이 5분 내에 완전히 작동 확인
  - 30분 동안 충돌 없이 실행 확인
  - 모든 헬스 체크 엔드포인트 200 OK 응답 확인

### Final Polish

- [ ] T085 코드 정리 및 리팩토링
  - 중복 코드 제거
  - 코드 스타일 일관성 확인
  - 사용되지 않는 import 제거
- [ ] T086 quickstart.md 검증
  - 문서화된 단계가 실제로 작동하는지 검증
  - 스크린샷 추가 (알림 상태 뱃지, 심각도 색상)
  - 한국어로 작성
- [ ] T087 보안 검토 (기본 - 인증 불필요)
  - 데이터 검증 확인 (assignedTo 100자, actionNote 2000자 제한)
  - 오류 메시지에 민감 정보 미포함 확인
  - XSS 방지 확인 (React는 기본적으로 안전)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 의존성 없음 - 즉시 시작 가능
- **Foundational (Phase 2)**: Setup 완료 후 - 모든 User Story를 블로킹
- **User Stories (Phase 3-5)**: Foundational phase 완료 후
  - User Story들은 병렬 진행 가능 (팀 역량에 따라)
  - 또는 우선순위 순서로 순차 진행 (P1 → P1 → P2)
- **Polish (Phase 6)**: 원하는 모든 User Story 완료 후

### User Story Dependencies

- **User Story 1 (P1)**: Foundational (Phase 2) 완료 후 시작 가능 - 다른 스토리와 독립적
- **User Story 2 (P1)**: Foundational (Phase 2) 완료 후 시작 가능 - User Story 1과 통합되지만 독립적으로 테스트 가능
- **User Story 3 (P2)**: Foundational (Phase 2) 완료 후 시작 가능 - User Story 1, 2와 통합되지만 독립적으로 테스트 가능

### Within Each User Story

- 테스트를 먼저 작성하고 실패 확인 후 구현
- 모델 → 서비스 → 엔드포인트 순서
- 핵심 구현 → 통합 순서
- 스토리 완료 후 다음 우선순위로 이동

### Parallel Opportunities

- Setup 작업 중 [P] 표시된 모든 작업 병렬 실행 가능
- Foundational phase 내 [P] 표시된 모든 작업 병렬 실행 가능
- Foundational phase 완료 후 모든 User Story 병렬 시작 가능 (팀 역량에 따라)
- 각 User Story 내 [P] 표시된 테스트 병렬 실행 가능
- 각 User Story 내 [P] 표시된 모델 병렬 실행 가능
- 서로 다른 User Story는 다른 팀 멤버가 병렬로 작업 가능

**주요 병렬 기회**:
- T006-T012: Foundational phase (7개 작업 병렬 가능)
- T013-T015: User Story 1 테스트 (3개 테스트 병렬 가능)
- T024-T025: User Story 1 frontend 타입 (2개 타입 병렬 가능)
- T033-T035: User Story 2 테스트 (3개 테스트 병렬 가능)
- T055-T057: User Story 3 테스트 (3개 테스트 병렬 가능)
- T069-T074: Observability 작업 (대부분 병렬 가능)
- T075-T078: Documentation 작업 (4개 문서 병렬 가능)

---

## Parallel Example: User Story 1

```bash
# User Story 1 테스트 모두 병렬 실행:
Task T013: "alert-service: AlertService 상태 변경 로직 단위 테스트"
Task T014: "alert-service: 상태별 필터링 로직 단위 테스트"
Task T015: "alert-service: REST API 엔드포인트 통합 테스트"

# User Story 1 frontend 타입 정의 병렬 실행:
Task T024: "frontend: Alert 타입에 status, processedAt 필드 추가"
Task T025: "frontend: AlertStatus enum 타입 정의"
```

---

## Implementation Strategy

### MVP First (User Story 1만)

1. Phase 1 완료: Setup (문서 생성)
2. Phase 2 완료: Foundational (중요 - 모든 스토리 블로킹)
3. Phase 3 완료: User Story 1 (알림 상태 관리)
4. **멈추고 검증**: User Story 1 독립적으로 테스트
5. 준비되면 배포/데모

### Incremental Delivery

1. Setup + Foundational 완료 → 기반 준비
2. User Story 1 추가 → 독립적으로 테스트 → 배포/데모 (MVP!)
3. User Story 2 추가 → 독립적으로 테스트 → 배포/데모
4. User Story 3 추가 → 독립적으로 테스트 → 배포/데모
5. 각 스토리는 이전 스토리를 깨뜨리지 않고 가치를 추가

### Parallel Team Strategy

여러 개발자가 있는 경우:

1. 팀이 함께 Setup + Foundational 완료
2. Foundational 완료 후:
   - 개발자 A: User Story 1 (알림 상태 관리)
   - 개발자 B: User Story 2 (담당자 할당 및 조치 기록)
   - 개발자 C: User Story 3 (심각도 표시)
3. 스토리들이 독립적으로 완료되고 통합

---

## Task Summary

**총 작업 수**: 87개

**Phase별 작업 수**:
- Phase 1 (Setup): 5개
- Phase 2 (Foundational): 7개
- Phase 3 (User Story 1): 20개 (테스트 3개 + 구현 17개)
- Phase 4 (User Story 2): 22개 (테스트 3개 + 구현 19개)
- Phase 5 (User Story 3): 14개 (테스트 3개 + 구현 11개)
- Phase 6 (Polish): 19개

**User Story별 작업 수**:
- User Story 1 (P1): 20개
- User Story 2 (P1): 22개
- User Story 3 (P2): 14개

**병렬 실행 가능 작업**: 약 30개 ([P] 표시)

**독립 테스트 기준**:
- User Story 1: 상태 변경 후 새로고침 시 상태 유지, 다른 브라우저에 실시간 동기화
- User Story 2: 담당자 할당 및 조치 내용 저장 후 재조회 시 정보 표시
- User Story 3: 각 규칙의 심각도에 맞는 색상 표시

**제안 MVP 범위**: User Story 1 (알림 상태 관리)
- 가장 기본적인 기능
- 나머지 User Story의 기반
- 즉시 가치 제공 (상태 추적으로 중복 작업 방지)

---

## Notes

- [P] 작업 = 다른 파일, 의존성 없음
- [Story] 레이블은 작업을 특정 User Story에 매핑하여 추적성 확보
- 각 User Story는 독립적으로 완료 및 테스트 가능
- 구현 전 테스트 실패 확인
- 각 작업 또는 논리적 그룹 후 커밋
- 각 체크포인트에서 멈춰 스토리를 독립적으로 검증
- 피해야 할 것: 모호한 작업, 동일 파일 충돌, 독립성을 깨뜨리는 스토리 간 의존성

---

**Format Validation**: ✅ 모든 작업이 체크리스트 형식을 따릅니다 (체크박스, ID, 레이블, 파일 경로)

**다음 단계**: `/speckit.implement` 명령으로 작업 실행
