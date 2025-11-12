# Tasks: 실시간 시스템 대시보드

**Feature**: 004-dashboard-realtime
**Input**: 설계 문서 from `/specs/004-dashboard-realtime/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

---

**⚠️ Constitution 준수 필수 사항**:

1. **테스트 우선 (Constitution V)**:
   - 단위 테스트 ≥70% 커버리지 필수
   - 통합 테스트는 모든 기능에 대해 필수
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

**Tests**: 테스트는 이 프로젝트에 필수입니다 (Constitution V: ≥70% 커버리지 요구)

**Organization**: 태스크는 사용자 스토리별로 그룹화되어 각 스토리를 독립적으로 구현하고 테스트할 수 있습니다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[Story]**: 이 태스크가 속한 사용자 스토리 (예: US1, US2, US3)
- 설명에 정확한 파일 경로 포함

## Path Conventions

- **Web app**: `alert-dashboard/backend/src/`, `alert-dashboard/frontend/src/`
- 아래 경로는 plan.md의 프로젝트 구조를 기준으로 함

---

## Phase 1: Setup (공유 인프라)

**목적**: 프로젝트 초기화 및 기본 구조 설정

- [X] T001 백엔드 의존성 추가 in alert-dashboard/backend/build.gradle.kts
  - Spring Boot Actuator, Spring WebSocket, Spring WebFlux (WebClient) 추가
  - 한국어 주석으로 각 의존성 목적 설명
- [X] T002 [P] 프론트엔드 의존성 추가 in frontend-dashboard/package.json
  - Recharts, WebSocket 타입 정의 추가
  - 한국어 주석으로 각 의존성 목적 설명
- [X] T003 [P] application.yml 기본 설정 in alert-dashboard/backend/src/main/resources/application.yml
  - 5초 스케줄링 간격, 1시간 데이터 보관 설정
  - 5개 서비스 URL 환경 변수 설정
  - 한국어 주석으로 각 설정 설명

---

## Phase 2: Foundational (필수 선행 작업)

**목적**: 모든 사용자 스토리 구현 전에 완료해야 하는 핵심 인프라

**⚠️ CRITICAL**: 이 단계가 완료되기 전까지 사용자 스토리 작업을 시작할 수 없습니다

- [X] T004 [P] ServiceHealth 엔티티 생성 in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/model/ServiceHealth.kt
  - serviceName, status, lastChecked, responseTime, memoryUsage, errorType, errorMessage 필드
  - 한국어 주석으로 각 필드 설명
  - data-model.md의 Validation Rules 구현
- [X] T005 [P] TransactionMetrics 엔티티 생성 in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/model/TransactionMetrics.kt
  - timestamp, tps, totalTransactions 필드
  - 한국어 주석으로 각 필드 설명
- [X] T006 [P] AlertMetrics 엔티티 생성 in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/model/AlertMetrics.kt
  - timestamp, alertsPerMinute, byRule 필드
  - 한국어 주석으로 각 필드 설명
  - byRule은 Map<String, Long> 타입 (HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY)
- [X] T007 [P] MetricsDataPoint 클래스 생성 in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/model/MetricsDataPoint.kt
  - 시계열 데이터 포인트 wrapper 클래스
  - services, transactionMetrics, alertMetrics 필드
  - 한국어 주석으로 각 필드 설명
- [X] T008 MetricsStore 구현 (Circular Buffer) in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/service/MetricsStore.kt
  - ConcurrentLinkedDeque 기반 circular buffer
  - 1시간(720개) 데이터 포인트 자동 관리
  - addDataPoint(), getAll(), getDataSince() 메서드
  - 한국어 주석으로 circular buffer 로직 설명
  - 함수 길이 ≤50줄 준수
- [X] T009 [P] WebSocket 설정 in alert-dashboard/backend/src/main/kotlin/io/realfds/alert/config/WebSocketConfig.kt
  - ws://localhost:8082/ws/metrics 엔드포인트 등록
  - CORS 설정 (로컬 개발용)
  - 한국어 주석으로 설정 설명
  - MetricsWebSocketHandler placeholder 생성 (Phase 6에서 완전 구현)
- [X] T010 [P] TypeScript 타입 정의 in frontend-dashboard/src/types/metrics.ts
  - ServiceHealth, TransactionMetrics, AlertMetrics 인터페이스
  - WebSocket 메시지 타입 (METRICS_UPDATE, BACKFILL_REQUEST, BACKFILL_RESPONSE, ERROR)
  - 한국어 주석으로 각 타입 설명

**Checkpoint**: ✅ 기반 완성 - 사용자 스토리 구현을 병렬로 시작할 수 있음

---

## Phase 3: User Story 1 - 서비스 상태 실시간 모니터링 (Priority: P1) 🎯 MVP

**Goal**: 5개 마이크로서비스의 Health Check 상태를 실시간으로 수집하고 대시보드에 표시

**Independent Test**: 각 서비스의 Health Check 엔드포인트를 호출하고 대시보드에 상태 카드가 올바르게 표시되는지 확인. 하나의 서비스를 중단시켰을 때 3초 이내에 빨간색 DOWN 상태로 전환되는지 테스트

### Tests for User Story 1 (MANDATORY per Constitution V) ⚠️

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**
> **Constitution V 요구사항**: ≥70% 커버리지, Given-When-Then 구조 사용

- [X] T011 [P] [US1] HealthCheckCollector 단위 테스트 in alert-dashboard/backend/src/test/java/com/realfds/dashboard/service/HealthCheckCollectorTest.java
  - Given-When-Then 구조 사용
  - 테스트 시나리오:
    - Given 모든 서비스 정상 작동, When Health Check 수집, Then 5개 서비스 모두 UP 상태
    - Given 한 서비스 중단, When Health Check 수집, Then 해당 서비스 DOWN, errorType 설정
    - Given 한 서비스 타임아웃, When Health Check 수집, Then 3초 이내 타임아웃 처리
  - 한국어 주석으로 테스트 의도 설명
- [X] T012 [P] [US1] ServiceHealthCard 컴포넌트 테스트 in alert-dashboard/frontend/src/tests/components/dashboard/ServiceHealthCard.test.tsx
  - Given UP 상태 서비스, When 렌더링, Then 녹색 카드 표시
  - Given DOWN 상태 서비스, When 렌더링, Then 빨간색 카드 표시
  - Given 상태 카드 클릭, When 클릭 이벤트, Then 상세 정보 모달 열림
  - 한국어 주석으로 테스트 의도 설명

### Implementation for User Story 1

- [X] T013 [US1] HealthCheckCollector 구현 in alert-dashboard/backend/src/main/java/com/realfds/dashboard/service/HealthCheckCollector.java
  - Spring WebClient로 5개 서비스 /actuator/health 호출 (3초 타임아웃)
  - 비동기 병렬 호출 (Mono.zip)
  - UP/DOWN 상태 판단 및 errorType 설정 (TIMEOUT, HTTP_ERROR, NETWORK_ERROR)
  - 한국어 주석으로 Health Check 로직 설명
  - 함수 길이 ≤50줄 준수
  - SLF4J 로깅 (INFO: 수집 시작/완료, WARN: 타임아웃, ERROR: 연결 실패)
- [X] T014 [P] [US1] ServiceHealthCard 컴포넌트 in alert-dashboard/frontend/src/components/dashboard/ServiceHealthCard.tsx
  - 서비스 이름, 상태(UP/DOWN), 마지막 확인 시간 표시
  - UP: 녹색, DOWN: 빨간색
  - 클릭 시 상세 정보 모달 열기 (onCardClick prop)
  - 한국어 주석으로 UI 로직 설명
- [X] T015 [P] [US1] ServiceDetailModal 컴포넌트 in alert-dashboard/frontend/src/components/dashboard/ServiceDetailModal.tsx
  - 메모리 사용량, 평균 응답 시간 표시
  - DOWN 상태 시 errorType, errorMessage 표시
  - 한국어 주석으로 모달 로직 설명
- [X] T016 [US1] MetricsScheduler에 Health Check 통합 in alert-dashboard/backend/src/main/java/com/realfds/dashboard/service/MetricsScheduler.java
  - @Scheduled(fixedRate = 5000) 메서드 생성
  - HealthCheckCollector 호출 후 MetricsStore에 저장
  - 한국어 주석으로 스케줄링 로직 설명
  - SLF4J 로깅 (INFO: 스케줄 실행, ERROR: 예외 발생)

**Checkpoint**: User Story 1 완전 기능 - 독립적으로 테스트 가능

---

## Phase 4: User Story 2 - 실시간 거래량 추이 확인 (Priority: P1)

**Goal**: 초당 거래 처리량(TPS)을 실시간 시계열 차트로 확인

**Independent Test**: 거래 생성기를 통해 초당 10개, 50개, 100개로 거래 생성 속도를 변경하면서 차트가 5초마다 새로운 데이터 포인트를 추가하고, 현재 TPS 수치가 정확하게 업데이트되는지 확인

### Tests for User Story 2 (MANDATORY per Constitution V) ⚠️

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T017 [P] [US2] KafkaMetricsCollector 단위 테스트 in alert-dashboard/backend/src/test/java/io/realfds/dashboard/service/KafkaMetricsCollectorTest.java
  - Given Kafka 토픽 offset 100, When 5초 후 offset 150, Then TPS = 10
  - Given Kafka 연결 실패, When TPS 수집, Then 이전 값 유지, ERROR 로깅
  - 한국어 주석으로 테스트 의도 설명
- [X] T018 [P] [US2] TpsChart 컴포넌트 테스트 in alert-dashboard/frontend/src/tests/components/dashboard/TpsChart.test.tsx
  - Given 1시간 TPS 데이터, When 렌더링, Then LineChart 표시
  - Given 차트 마우스 호버, When 데이터 포인트 가리킴, Then 툴팁 표시
  - 한국어 주석으로 테스트 의도 설명

### Implementation for User Story 2

- [X] T019 [US2] KafkaMetricsCollector 구현 in alert-dashboard/backend/src/main/java/io/realfds/dashboard/service/KafkaMetricsCollector.java
  - Kafka AdminClient로 virtual-transactions 토픽 offset 조회
  - 이전 offset과 비교하여 TPS 계산 (delta / 5초)
  - 한국어 주석으로 TPS 계산 로직 설명
  - 함수 길이 ≤50줄 준수
  - SLF4J 로깅 (INFO: TPS 수집, WARN: Kafka 연결 지연, ERROR: Kafka 연결 실패)
- [X] T020 [P] [US2] TpsChart 컴포넌트 in alert-dashboard/frontend/src/components/dashboard/TpsChart.tsx
  - Recharts LineChart 사용
  - X축: timestamp (최근 1시간), Y축: TPS (0-10000)
  - 툴팁: 시각과 정확한 TPS 값 표시
  - 자동 Y축 범위 조정
  - 한국어 주석으로 차트 로직 설명
- [X] T021 [P] [US2] TpsMetricsCard 컴포넌트 in alert-dashboard/frontend/src/components/dashboard/TpsMetricsCard.tsx
  - 현재 TPS, 평균 TPS, 최대 TPS 숫자 표시
  - 수치 업데이트 시 부드러운 애니메이션
  - 한국어 주석으로 카드 로직 설명
- [X] T022 [US2] MetricsScheduler에 TPS 수집 통합 in alert-dashboard/backend/src/main/java/io/realfds/dashboard/service/MetricsScheduler.java
  - @Scheduled 메서드에 KafkaMetricsCollector 호출 추가
  - TransactionMetrics를 MetricsStore에 저장
  - 한국어 주석으로 통합 로직 설명

**Checkpoint**: User Story 1과 2 모두 독립적으로 작동

---

## Phase 5: User Story 3 - 실시간 알림 발생률 추이 확인 (Priority: P2)

**Goal**: 분당 알림 발생 수를 실시간 차트로 확인하여 사기 탐지 패턴의 변화를 감지

**Independent Test**: 사기 탐지 규칙의 임계값을 조정하여 알림 발생 빈도를 변경하고, 차트가 규칙별로 알림 수를 구분하여 표시하는지 확인

### Tests for User Story 3 (MANDATORY per Constitution V) ⚠️

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T023 [P] [US3] AlertMetricsCollector 단위 테스트 in alert-dashboard/backend/src/test/java/io/realfds/dashboard/service/AlertMetricsCollectorTest.java
  - Given 3개 규칙 알림 발생, When 알림률 수집, Then 규칙별 분당 알림 수 계산
  - Given HIGH_VALUE 알림 20개, When 알림률 계산, Then byRule["HIGH_VALUE"] = 20
  - 한국어 주석으로 테스트 의도 설명
- [X] T024 [P] [US3] AlertRateChart 컴포넌트 테스트 in alert-dashboard/frontend/src/tests/components/dashboard/AlertRateChart.test.tsx
  - Given 3개 규칙 알림 데이터, When 렌더링, Then AreaChart 스택 형태 표시
  - Given 범례 클릭, When 특정 규칙 클릭, Then 해당 규칙 데이터 숨김/표시
  - Given 알림률 평균 2배 초과, When 렌더링, Then 그래프 주황색
  - 한국어 주석으로 테스트 의도 설명

### Implementation for User Story 3

- [X] T025 [US3] AlertMetricsCollector 구현 in alert-dashboard/backend/src/main/java/io/realfds/dashboard/service/AlertMetricsCollector.java
  - Kafka AdminClient로 transaction-alerts 토픽 메시지 수 조회
  - 규칙별(HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY) 알림 수 집계
  - 분당 알림 수 계산
  - 한국어 주석으로 알림률 계산 로직 설명
  - 함수 길이 ≤50줄 준수
  - SLF4J 로깅 (INFO: 알림률 수집, WARN: Kafka 연결 지연, ERROR: Kafka 연결 실패)
- [X] T026 [P] [US3] AlertRateChart 컴포넌트 in alert-dashboard/frontend/src/components/dashboard/AlertRateChart.tsx
  - Recharts AreaChart 사용, 스택 형태
  - 3개 규칙 다른 색상 (HIGH_VALUE: 빨강, FOREIGN_COUNTRY: 파랑, HIGH_FREQUENCY: 노랑)
  - 범례 클릭 시 해당 규칙 데이터 숨김/표시
  - 알림률 평균 2배 초과 시 그래프 영역 주황색
  - 한국어 주석으로 차트 로직 설명
- [X] T027 [P] [US3] AlertMetricsCard 컴포넌트 in alert-dashboard/frontend/src/components/dashboard/AlertMetricsCard.tsx
  - 현재 알림 발생률, 평균, 최대값 숫자 표시
  - 수치 업데이트 시 부드러운 애니메이션
  - 한국어 주석으로 카드 로직 설명
- [X] T028 [US3] MetricsScheduler에 알림률 수집 통합 in alert-dashboard/backend/src/main/java/io/realfds/dashboard/service/MetricsScheduler.java
  - @Scheduled 메서드에 AlertMetricsCollector 호출 추가
  - AlertMetrics를 MetricsStore에 저장
  - 한국어 주석으로 통합 로직 설명

**Checkpoint**: 모든 사용자 스토리가 독립적으로 기능

---

## Phase 6: WebSocket 실시간 브로드캐스트

**목적**: 5초마다 메트릭 데이터를 모든 클라이언트에 WebSocket으로 전송

### Tests for WebSocket (MANDATORY per Constitution V) ⚠️

- [X] T029 [P] MetricsWebSocketHandler 통합 테스트 in alert-dashboard/backend/src/test/java/io/realfds/dashboard/websocket/MetricsWebSocketHandlerTest.java
  - Given WebSocket 연결, When 5초마다 METRICS_UPDATE 전송, Then 모든 세션에 브로드캐스트
  - Given BACKFILL_REQUEST 수신, When lastReceivedTimestamp 제공, Then BACKFILL_RESPONSE 전송
  - Given 잘못된 메시지 형식, When 수신, Then ERROR 메시지 전송
  - 한국어 주석으로 테스트 의도 설명
- [X] T030 [P] useWebSocket 훅 테스트 in alert-dashboard/frontend/src/tests/hooks/useWebSocket.test.ts
  - Given WebSocket 연결, When METRICS_UPDATE 수신, Then 상태 업데이트
  - Given 연결 끊김, When 재연결, Then Exponential Backoff (1s, 2s, 4s...)
  - Given 재연결 성공, When BACKFILL_REQUEST 전송, Then 누락 데이터 복구
  - 한국어 주석으로 테스트 의도 설명

### Implementation for WebSocket

- [X] T031 MetricsMessage DTO 생성 in alert-dashboard/backend/src/main/java/io/realfds/dashboard/websocket/MetricsMessage.java
  - type (METRICS_UPDATE, BACKFILL_REQUEST, BACKFILL_RESPONSE, ERROR)
  - payload (MetricsDataPoint 또는 에러 정보)
  - timestamp
  - 한국어 주석으로 각 필드 설명
- [X] T032 MetricsWebSocketHandler 구현 in alert-dashboard/backend/src/main/java/io/realfds/dashboard/websocket/MetricsWebSocketHandler.java
  - afterConnectionEstablished(): 새 세션 등록
  - handleTextMessage(): BACKFILL_REQUEST 처리
  - broadcast(): 모든 세션에 METRICS_UPDATE 전송
  - Ping-pong heartbeat (30초마다)
  - 한국어 주석으로 WebSocket 로직 설명
  - 함수 길이 ≤50줄 준수
  - SLF4J 로깅 (INFO: 연결/해제, WARN: 메시지 파싱 실패, ERROR: 브로드캐스트 실패)
- [X] T033 MetricsScheduler에 브로드캐스트 통합 in alert-dashboard/backend/src/main/java/io/realfds/dashboard/service/MetricsScheduler.java
  - @Scheduled 메서드에서 MetricsStore 최신 데이터 조회
  - MetricsWebSocketHandler.broadcast() 호출
  - 한국어 주석으로 브로드캐스트 타이밍 설명
- [X] T034 useWebSocket 커스텀 훅 in alert-dashboard/frontend/src/hooks/useWebSocket.ts
  - WebSocket 연결 관리 (ws://localhost:8083/ws/metrics)
  - METRICS_UPDATE 수신 시 상태 업데이트
  - Exponential Backoff 재연결 (1s, 2s, 4s, 8s, 16s, 32s max)
  - 재연결 시 BACKFILL_REQUEST 전송
  - 한국어 주석으로 재연결 로직 설명
- [X] T035 [P] ConnectionStatus 배너 컴포넌트 in alert-dashboard/frontend/src/components/common/ConnectionStatus.tsx
  - 연결 끊김 시 "연결 끊김" 경고 배너 표시
  - 재연결 중 "재연결 중..." 표시
  - 한국어 주석으로 연결 상태 로직 설명

---

## Phase 7: Dashboard 레이아웃 및 통합

**목적**: 모든 컴포넌트를 통합하여 완전한 대시보드 UI 구성

- [X] T036 DashboardLayout 컴포넌트 in alert-dashboard/frontend/src/components/dashboard/DashboardLayout.tsx
  - useWebSocket 훅 호출
  - ConnectionStatus 배너
  - 상단: 5개 ServiceHealthCard (그리드 레이아웃)
  - 중간: TpsChart + TpsMetricsCard
  - 하단: AlertRateChart + AlertMetricsCard
  - 한국어 주석으로 레이아웃 구조 설명
- [X] T037 Dashboard 페이지 라우팅 in alert-dashboard/frontend/src/App.tsx
  - /dashboard 경로에 DashboardLayout 연결
  - 한국어 주석으로 라우팅 설명
- [X] T038 [P] REST API 엔드포인트 in alert-dashboard/backend/src/main/java/com/realfds/dashboard/controller/MetricsRestController.java
  - GET /api/v1/services: 5개 서비스 목록 반환
  - GET /api/v1/metrics/current: 현재 메트릭 스냅샷 반환 (optional)
  - 한국어 주석으로 엔드포인트 설명

---

## Phase 8: Polish & Cross-Cutting Concerns

**목적**: 여러 사용자 스토리에 영향을 주는 개선 사항

### Observability & Monitoring (Constitution V - MANDATORY)

- [X] T039 [P] Health Check 엔드포인트 검증 in alert-dashboard/backend/src/main/resources/application.yml
  - /actuator/health 엔드포인트 활성화 확인
  - WebSocket 연결 수, 브로드캐스트 수 포함
  - 한국어 주석으로 헬스 체크 설정 설명
- [X] T040 [P] 구조화된 로깅 검증
  - 모든 서비스에 SLF4J + Logback JSON 레이아웃 적용 확인
  - 서비스 생명주기 이벤트 로깅 (INFO 레벨)
  - 중요 비즈니스 이벤트 로깅 (메트릭 수집, WebSocket 연결)
  - 오류는 컨텍스트와 함께 로깅 (ERROR 레벨)
  - 로그 메시지는 한국어로 작성

### Edge Case 처리

- [X] T041 서비스 응답 지연 처리 in alert-dashboard/backend/src/main/java/com/realfds/dashboard/service/HealthCheckCollector.java
  - Health Check 응답 5초 이상 → DOWN 상태, errorType: TIMEOUT
  - 한국어 주석으로 타임아웃 로직 설명
- [X] T042 데이터 없음 처리 in alert-dashboard/frontend/src/components/dashboard/TpsChart.tsx
  - 시스템 초기 시작 시 "데이터 수집 중..." 메시지 표시
  - 한국어 주석으로 빈 상태 로직 설명
- [X] T043 급격한 값 변화 처리 in alert-dashboard/frontend/src/components/dashboard/TpsChart.tsx
  - TPS 10배 증가 시 Y축 자동 범위 조정
  - 한국어 주석으로 Y축 조정 로직 설명
- [X] T044 브라우저 탭 비활성화 처리 in alert-dashboard/frontend/src/hooks/useWebSocket.ts
  - 탭 전환 후 복귀 시 BACKFILL_REQUEST 전송하여 누락 데이터 백필
  - 한국어 주석으로 백필 로직 설명

### Documentation (Constitution VI - MANDATORY)

- [X] T045 [P] README.md 업데이트 in alert-dashboard/README.md
  - 실시간 대시보드 기능 설명 (한국어)
  - 빠른 시작 가이드 (docker-compose up)
  - 대시보드 접속 URL (http://localhost:8083/dashboard)
  - 환경 변수 문서화
  - 문제 해결 섹션
- [X] T046 [P] 모든 복잡한 로직에 한국어 주석 추가
  - Circular buffer 로직
  - Exponential backoff 재연결 로직
  - TPS 계산 로직
  - 알림률 집계 로직

### Quality & Testing (Constitution V - MANDATORY)

- [ ] T047 단위 테스트 커버리지 ≥70% 검증
  - 커버리지 리포트 생성 (Jacoco)
  - 누락된 테스트 추가
- [ ] T048 모든 통합 테스트 실행
  - Health Check 수집 → WebSocket 브로드캐스트 → UI 업데이트 검증
  - 재연결 및 백필 시나리오 검증
- [ ] T049 성능 테스트
  - 평균 메트릭 업데이트 지연 시간 <5초 검증
  - 차트 렌더링 시간 <50ms 검증
  - 1시간 지속 사용 중 성능 유지 검증
- [ ] T050 코드 품질 리뷰
  - 함수 길이 ≤50줄 검증
  - 파일 길이 ≤300줄 검증
  - 서술적인 변수/함수명 검증

### Constitution Compliance Check (MANDATORY)

- [ ] T051 모든 Constitution 원칙 준수 검증
  - I. 학습 우선: WebSocket 실시간 스트리밍 개념 명확히 시연 확인
  - II. 단순함: docker-compose up 동작 확인
  - III. 실시간 우선: WebSocket 사용, 5초 업데이트 확인
  - IV. 서비스 경계: RAD 서비스에만 통합, 3개 서비스 유지 확인
  - V. 품질 표준: 테스트 커버리지 ≥70%, 로깅, 오류 처리 확인
  - VI. 한국어 우선: 주석, 문서, 커밋 메시지 한국어 확인
- [ ] T052 MVP 인수 기준 검증 (from Constitution)
  - docker-compose up으로 모든 서비스 시작
  - 시스템이 5분 내에 완전히 작동
  - 30분 동안 충돌 없이 실행
  - 모든 헬스 체크 엔드포인트 200 OK 응답

### Final Polish

- [ ] T053 코드 정리 및 리팩토링
  - 중복 코드 제거
  - 코드 스타일 일관성 확인
- [ ] T054 quickstart.md 검증
  - 문서화된 단계가 실제로 작동하는지 검증
  - 모든 FR과 SC 체크리스트 확인
- [ ] T055 보안 리뷰 (기본 - 인증 불필요)
  - 데이터 검증 확인 (WebSocket 메시지 파싱)
  - 오류 메시지에 민감 정보 미포함 확인

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 의존성 없음 - 즉시 시작 가능
- **Foundational (Phase 2)**: Setup 완료 후 - 모든 사용자 스토리를 차단
- **User Stories (Phase 3-5)**: 모두 Foundational 완료에 의존
  - User Story 1, 2는 P1 우선순위로 병렬 구현 가능 (인력 충분 시)
  - User Story 3은 P2 우선순위로 US1, US2 완료 후 구현 권장
- **WebSocket (Phase 6)**: User Story 1, 2, 3 완료 후 (메트릭 수집 완료 필요)
- **Dashboard Layout (Phase 7)**: WebSocket 완료 후
- **Polish (Phase 8)**: 모든 기능 완료 후

### User Story Dependencies

- **User Story 1 (P1)**: Foundational 완료 후 시작 - 다른 스토리에 의존하지 않음
- **User Story 2 (P1)**: Foundational 완료 후 시작 - US1과 병렬 구현 가능
- **User Story 3 (P2)**: Foundational 완료 후 시작 - US1, US2와 독립적이지만 P2 우선순위

### Within Each User Story

- 테스트를 먼저 작성하고 실패 확인 후 구현
- 엔티티 → 서비스 → UI 컴포넌트
- 핵심 구현 → 통합 → 스토리 완료

### Parallel Opportunities

- Setup 태스크 중 [P] 마크는 병렬 실행 가능
- Foundational 태스크 중 [P] 마크는 병렬 실행 가능 (Phase 2 내)
- Foundational 완료 후, US1과 US2는 병렬 구현 가능 (팀 인력 충분 시)
- 각 스토리 내 테스트 중 [P] 마크는 병렬 실행 가능
- 각 스토리 내 엔티티/컴포넌트 중 [P] 마크는 병렬 실행 가능
- 서로 다른 사용자 스토리는 다른 팀원이 병렬 작업 가능

---

## Parallel Example: User Story 1

```bash
# User Story 1의 모든 테스트를 함께 실행:
Task: "HealthCheckCollector 단위 테스트 in alert-dashboard/backend/src/test/java/..."
Task: "ServiceHealthCard 컴포넌트 테스트 in alert-dashboard/frontend/src/tests/components/..."

# User Story 1의 병렬 구현 가능 태스크:
Task: "ServiceHealthCard 컴포넌트 in alert-dashboard/frontend/src/components/..."
Task: "ServiceDetailModal 컴포넌트 in alert-dashboard/frontend/src/components/..."
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2만 우선)

1. Phase 1: Setup 완료
2. Phase 2: Foundational 완료 (CRITICAL - 모든 스토리 차단)
3. Phase 3: User Story 1 완료
4. Phase 4: User Story 2 완료
5. Phase 6: WebSocket 브로드캐스트 (US1, US2 메트릭만)
6. Phase 7: Dashboard Layout (US1, US2 컴포넌트만)
7. **STOP and VALIDATE**: US1, US2 독립 테스트
8. Deploy/Demo MVP (서비스 상태 + TPS 차트)

### Incremental Delivery

1. Setup + Foundational → 기반 준비
2. User Story 1 추가 → 독립 테스트 → Deploy/Demo (서비스 상태 모니터링)
3. User Story 2 추가 → 독립 테스트 → Deploy/Demo (서비스 상태 + TPS)
4. User Story 3 추가 → 독립 테스트 → Deploy/Demo (전체 대시보드)
5. 각 스토리가 이전 스토리를 깨뜨리지 않고 가치 추가

### Parallel Team Strategy

여러 개발자가 있는 경우:

1. 팀이 Setup + Foundational 함께 완료
2. Foundational 완료 후:
   - Developer A: User Story 1 (서비스 상태)
   - Developer B: User Story 2 (TPS 차트)
   - Developer C: WebSocket 인프라 준비
3. Developer C: User Story 3 (알림률 차트)
4. 모든 팀원: WebSocket 통합 및 Dashboard Layout
5. 스토리가 독립적으로 완료되고 통합됨

---

## Task Summary

**Total Tasks**: 55 tasks
- Phase 1 (Setup): 3 tasks
- Phase 2 (Foundational): 7 tasks
- Phase 3 (User Story 1): 6 tasks (2 tests + 4 implementation)
- Phase 4 (User Story 2): 6 tasks (2 tests + 4 implementation)
- Phase 5 (User Story 3): 6 tasks (2 tests + 4 implementation)
- Phase 6 (WebSocket): 7 tasks (2 tests + 5 implementation)
- Phase 7 (Dashboard Layout): 3 tasks
- Phase 8 (Polish): 17 tasks

**Test Tasks**: 8 tasks (Constitution V: ≥70% 커버리지 필수)
**Parallel Opportunities**: 25 tasks marked with [P]

**Independent Test Criteria**:
- **US1**: 서비스 Health Check 수집 → 상태 카드 표시 → 서비스 중단 시 3초 이내 DOWN 상태 전환
- **US2**: TPS 수집 → 시계열 차트 표시 → 5초마다 차트 업데이트 → 마우스 호버 툴팁
- **US3**: 알림률 수집 → 규칙별 스택 차트 표시 → 범례 클릭 필터링 → 알림률 2배 초과 시 주황색

**Suggested MVP Scope**: User Story 1 + User Story 2 (서비스 상태 + TPS 모니터링)

---

## Notes

- [P] 태스크 = 다른 파일, 의존성 없음
- [Story] 라벨은 특정 사용자 스토리에 태스크 매핑 (추적 용이)
- 각 사용자 스토리는 독립적으로 완료 및 테스트 가능
- 테스트는 구현 전에 작성하고 실패 확인
- 각 태스크 또는 논리적 그룹 후 커밋
- 체크포인트에서 스토리를 독립적으로 검증
- 피할 것: 모호한 태스크, 같은 파일 충돌, 스토리 독립성을 깨는 교차 의존성
