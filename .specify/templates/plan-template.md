# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: [e.g., Python 3.11, Swift 5.9, Rust 1.75 or NEEDS CLARIFICATION]  
**Primary Dependencies**: [e.g., FastAPI, UIKit, LLVM or NEEDS CLARIFICATION]  
**Storage**: [if applicable, e.g., PostgreSQL, CoreData, files or N/A]  
**Testing**: [e.g., pytest, XCTest, cargo test or NEEDS CLARIFICATION]  
**Target Platform**: [e.g., Linux server, iOS 15+, WASM or NEEDS CLARIFICATION]
**Project Type**: [single/web/mobile - determines source structure]  
**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps or NEEDS CLARIFICATION]  
**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory, offline-capable or NEEDS CLARIFICATION]  
**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or NEEDS CLARIFICATION]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**목적**: 모든 구현 결정이 프로젝트 Constitution의 핵심 원칙을 준수하는지 검증합니다.

### I. 학습 우선 접근 (Learning-First Approach)

- [ ] 기능이 실시간 스트리밍 개념을 명확하게 보여줍니까?
- [ ] 구현이 명시적 패턴을 사용합니까 (암시적 마법 금지)?
- [ ] 포괄적인 로깅이 계획되어 있습니까?
- [ ] "무엇을"과 "왜"가 모두 문서화됩니까?
- [ ] ❌ 인증/인가 시스템을 추가하지 않습니까?
- [ ] ❌ 복잡한 보안 기능을 구현하지 않습니까?

**위반 시**: Complexity Tracking 섹션에 정당화 필요

---

### II. 완벽함보다 단순함 (Simplicity Over Perfection)

- [ ] 단일 명령어 배포 (`docker-compose up`)가 가능합니까?
- [ ] 외부 의존성이 최소화되어 있습니까?
- [ ] 합리적인 기본값을 가진 환경 변수를 사용합니까?
- [ ] Windows, macOS, Linux에서 모두 작동합니까?
- [ ] ❌ 수동 설정 파일 편집을 요구하지 않습니까?
- [ ] ❌ 클라우드 전용 서비스를 사용하지 않습니까?

**위반 시**: Complexity Tracking 섹션에 정당화 필요

---

### III. 실시간 우선 (Real-time First)

- [ ] 서비스 간 이벤트 기반 통신을 사용합니까?
- [ ] 상태 기반(stateful) 스트림 처리가 필요한 경우 적절히 구현됩니까?
- [ ] 브라우저 통신에 WebSocket을 사용합니까 (폴링 금지)?
- [ ] 시간 기반 집계에 적절한 윈도우(windowing)를 사용합니까?
- [ ] 이벤트 시간(event-time) 시맨틱을 사용합니까?
- [ ] ❌ REST API 폴링을 사용하지 않습니까?
- [ ] ❌ 배치(batch) 처리를 사용하지 않습니까?

**성능 목표 충족**:
- [ ] 평균 종단 간 지연 시간 <5초 (목표: <3초)
- [ ] p95 종단 간 지연 시간 <8초 (목표: <5초)

**위반 시**: Complexity Tracking 섹션에 정당화 필요

---

### IV. 마이크로서비스 경계 (Microservice Boundaries)

- [ ] 정확히 3개의 서비스만 사용합니까? (TGS, RDE, RAD)
- [ ] 각 서비스가 단일 책임을 가집니까?
- [ ] 각 서비스가 독립적으로 배포 가능합니까?
- [ ] 서비스 간 통신이 Kafka만 사용합니까?
- [ ] 각 서비스가 헬스 체크 엔드포인트를 노출합니까?
- [ ] ❌ 추가 서비스를 생성하지 않습니까?
- [ ] ❌ 서비스 간 HTTP/REST 직접 호출을 사용하지 않습니까?
- [ ] ❌ 공유 데이터베이스를 사용하지 않습니까?

**메모리 제한 준수**:
- [ ] TGS: <256MB
- [ ] RDE: <2GB
- [ ] RAD: <512MB

**위반 시**: Complexity Tracking 섹션에 정당화 필수 (중대한 위반)

---

### V. 테스트 및 품질 표준 (Test & Quality Standards)

- [ ] 단위 테스트 커버리지 ≥70% 계획되어 있습니까?
- [ ] 3가지 탐지 규칙 모두에 대한 통합 테스트가 계획되어 있습니까?
- [ ] 성능 테스트 (5초 미만 지연) 계획되어 있습니까?
- [ ] Given-When-Then 구조 사용이 계획되어 있습니까?
- [ ] 서술적인 변수/함수 이름을 사용합니까?
- [ ] 최대 함수 길이 50줄, 파일 길이 300줄을 준수합니까?
- [ ] Conventional Commits 형식 (한국어)을 사용합니까?
- [ ] 구조화된 로깅 (SLF4J + JSON)이 계획되어 있습니까?

**오류 처리**:
- [ ] 예외를 조용히 무시하지 않습니까?
- [ ] 컨텍스트와 함께 오류를 로깅합니까?
- [ ] Kafka 연결에 서킷 브레이커를 구현합니까?

**위반 시**: Complexity Tracking 섹션에 정당화 필요

---

### VI. 한국어 우선 (Korean-First Documentation)

- [ ] 모든 코드 주석을 한국어로 작성합니까?
- [ ] 커밋 메시지를 Conventional Commits + 한국어로 작성합니까?
- [ ] 모든 공식 문서를 한국어로 작성합니까?
- [ ] 변수/함수명은 영어 사용, 필요시 한국어 주석을 추가합니까?
- [ ] ❌ 한 문장/주석 내에서 한국어와 영어를 혼용하지 않습니까?

**위반 시**: Complexity Tracking 섹션에 정당화 필요

---

### 기술 스택 준수

**필수 기술**:
- [ ] Apache Kafka 3.6+
- [ ] Apache Flink 1.18+ OR Kafka Streams 3.6+
- [ ] Spring Boot 3.2+ (TGS, RAD)
- [ ] React 18+ + TypeScript 5+ (RAD UI)
- [ ] Docker + Docker Compose

**금지 기술** (사용 시 반드시 정당화 필요):
- [ ] ❌ Kubernetes 사용하지 않음
- [ ] ❌ 서비스 메시 사용하지 않음
- [ ] ❌ 클라우드 전용 서비스 사용하지 않음
- [ ] ❌ NoSQL 데이터베이스 사용하지 않음
- [ ] ❌ API 게이트웨이 사용하지 않음

---

### Constitution Check 결과

**통과**: [ ] 모든 항목이 준수됨
**조건부 통과**: [ ] 일부 위반 있으나 정당화됨 (Complexity Tracking 참조)
**실패**: [ ] 정당화되지 않은 위반 존재 → 설계 재검토 필요

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
# [REMOVE IF UNUSED] Option 1: Single project (DEFAULT)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# [REMOVE IF UNUSED] Option 2: Web application (when "frontend" + "backend" detected)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [REMOVE IF UNUSED] Option 3: Mobile + API (when "iOS/Android" detected)
api/
└── [same as backend above]

ios/ or android/
└── [platform-specific structure: feature modules, UI flows, platform tests]
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
