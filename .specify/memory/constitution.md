<!--
Sync Impact Report:
- Version change: 1.0.0 (initial) → 1.0.0 (first formalization in SpecKit format)
- Modified principles: Comprehensive migration from freeform document to structured constitution
- Added sections: Formalized Core Principles (6), Technical Standards, Architecture Standards, Quality Gates, Performance Requirements
- Removed sections: None (consolidated from detailed sections into principle-based structure)
- Templates requiring updates:
  ✅ constitution.md (this file - updated)
  ⚠ plan-template.md (requires Constitution Check gates alignment)
  ⚠ spec-template.md (requires FR validation against principles)
  ⚠ tasks-template.md (requires task categorization alignment)
- Follow-up TODOs:
  - Validate Constitution Check gates in plan-template.md against new principles
  - Ensure spec-template.md includes Korean language requirements
  - Update tasks-template.md to reflect TDD and observability requirements
-->

# RealFDS (실시간 금융 거래 탐지 시스템) Constitution

## Core Principles

### I. 학습 우선 접근 (Learning-First Approach)

**NON-NEGOTIABLE**: 이 프로젝트는 프로덕션 수준의 복잡성보다 실시간 스트리밍 기술 학습을 최우선으로 합니다.

**강제 사항**:
- 스트리밍 개념을 명확하게 보여주는 기술을 선택해야 함
- 암시적인 마법(magic)보다 명시적인 패턴을 사용해야 함
- 포괄적인 로깅을 포함해야 함 (학습 및 디버깅 목적)
- "무엇을"과 "왜"를 모두 문서화해야 함
- 코드는 포트폴리오 목적으로 가독성 있고 주석이 잘 달려야 함

**금지 사항**:
- ❌ 인증/인가 시스템 추가 (범위 외)
- ❌ 복잡한 보안 기능 구현 (학습용 프로젝트)
- ❌ 학습 가치 없는 프로덕션 복잡성

**근거**: 이 프로젝트의 목표는 실시간 스트리밍 아키텍처를 이해하고 시연하는 것입니다. 프로덕션 등급 기능은 이 목표에서 벗어납니다.

---

### II. 완벽함보다 단순함 (Simplicity Over Perfection)

**NON-NEGOTIABLE**: 실행하기 어려운 복잡한 시스템보다 실시간 기능을 명확하게 보여주는 작동하는 데모가 우선합니다.

**강제 사항**:
- 단일 명령어 배포 (`docker-compose up`) 필수
- 외부 의존성 최소화
- 합리적인 기본값을 가진 환경 변수 사용
- Windows, macOS, Linux에서 모두 작동해야 함

**금지 사항**:
- ❌ 수동 설정 파일 편집 요구
- ❌ 클라우드 전용 서비스 사용 (로컬 실행 가능해야 함)
- ❌ 예외적인 경우(edge case)를 위한 과도한 엔지니어링
- ❌ 최신 유행 기술 (검증된 기술 우선)

**근거**: 포트폴리오 프로젝트는 쉽게 실행하고 시연할 수 있어야 합니다. 복잡성은 학습 가치를 방해합니다.

---

### III. 실시간 우선 (Real-time First)

**NON-NEGOTIABLE**: 모든 아키텍처 결정은 실시간 데이터 흐름과 낮은 지연 시간을 최우선으로 해야 합니다.

**강제 사항**:
- 서비스 간 이벤트 기반 통신 필수
- 빈도 탐지를 위한 상태 기반(stateful) 스트림 처리 사용
- 브라우저 통신에 WebSocket 사용 (폴링 금지)
- 시간 기반 집계를 위한 적절한 윈도우(windowing) 구현
- 이벤트 시간(event-time) 시맨틱 사용 (처리 시간 아님)

**금지 사항**:
- ❌ 실시간 업데이트를 위한 REST API 폴링
- ❌ 탐지 규칙의 배치(batch) 처리
- ❌ 동기식 블로킹 작업 (최소화 필수)

**성능 목표**:
- 평균 종단 간 지연 시간: <3초 (목표), <5초 (허용)
- p95 종단 간 지연 시간: <5초 (목표), <8초 (허용)

**근거**: 프로젝트의 핵심 가치는 실시간 스트리밍 능력을 시연하는 것입니다. 동기식 또는 배치 처리는 이 목표를 훼손합니다.

---

### IV. 마이크로서비스 경계 (Microservice Boundaries)

**NON-NEGOTIABLE**: 시스템은 각각 단일 책임을 가진 정확히 3개의 독립적인 서비스로 구성됩니다.

**서비스 정의**:

1. **거래 생성 서비스 (TGS - Transaction Generator Service)**
   - 책임: 모의 거래 데이터 생성
   - 통신: Kafka에만 발행(publish)
   - 메모리 제한: <256MB

2. **실시간 탐지 엔진 (RDE - Real-time Detection Engine)** ⭐ 핵심 서비스
   - 책임: 탐지 규칙 적용 및 의심스러운 패턴 식별
   - 통신: Kafka에서 소비(consume), Kafka로 발행(publish)
   - 메모리 제한: <2GB (상태 관리용)

3. **실시간 알림 대시보드 (RAD - Real-time Alert Dashboard)**
   - 책임: 사용자에게 실시간으로 알림 표시
   - 통신: Kafka에서 소비(consume), WebSocket을 통해 푸시(push)
   - 메모리 제한: <512MB

**금지 사항**:
- ❌ 추가 서비스 생성 (3개 이외)
- ❌ 서비스 간 HTTP/REST 직접 호출
- ❌ 공유 데이터베이스 (서비스 간)
- ❌ 동기식 서비스 간 통신

**강제 사항**:
- 각 서비스는 독립적으로 배포 가능해야 함
- 서비스 간 통신은 Kafka만 사용
- 각 서비스는 헬스 체크 엔드포인트 노출 (`/actuator/health`)

**근거**: 명확한 서비스 경계는 독립적인 개발, 테스트, 배포를 가능하게 하며 마이크로서비스 아키텍처 학습 목표와 일치합니다.

---

### V. 테스트 및 품질 표준 (Test & Quality Standards)

**강제 사항**:
- 단위 테스트: 비즈니스 로직(탐지 규칙)에 대해 ≥70% 커버리지
- 통합 테스트: 3가지 탐지 규칙 모두에 대한 종단 간 테스트 필수
- 성능 테스트: 5초 미만 지연 시간 요구사항 검증
- Given-When-Then 구조를 사용한 명확한 테스트 작성

**코드 품질**:
- 서술적인 변수 및 함수 이름 사용
- 최대 함수 길이: 50줄
- 최대 파일 길이: 300줄
- Conventional Commits 형식의 커밋 메시지 (한국어)

**오류 처리**:
- 예외를 조용히 무시하지 않음
- 컨텍스트(거래 ID, userId 등)와 함께 오류 로깅
- 구조화된 로깅 (SLF4J + JSON 레이아웃)
- Kafka 연결에 서킷 브레이커 구현

**근거**: 높은 품질 표준은 코드를 유지보수 가능하게 만들고 포트폴리오 가치를 높입니다.

---

### VI. 한국어 우선 (Korean-First Documentation)

**NON-NEGOTIABLE**: 모든 참여자의 명확성과 일관성을 보장하기 위해 프로젝트 관련 문서, 주석, 커밋 메시지는 한국어로 작성합니다.

**강제 사항**:
- 모든 코드 주석은 한국어로 작성
- 모든 커밋 메시지는 Conventional Commits 형식 + 한국어
- 모든 공식 문서 (`README.md`, `constitution.md` 등)는 한국어
- 변수/함수명은 영어 사용 가능 (기술적 명확성)
- 필요시 영어 코드에 한국어 주석으로 설명 추가

**금지 사항**:
- ❌ 한 문장/주석 블록 내에서 한국어와 영어 혼용
- ❌ 신중한 검토 없는 기계 번역 사용

**근거**: 일관된 언어 사용은 프로젝트 이해도를 높이고 협업을 촉진하며, 한국어 우선은 주요 참여자의 모국어를 존중합니다.

---

## Architecture Standards

### 이벤트 기반 아키텍처 (Event-Driven Architecture)

**원칙**: 모든 서비스 간 통신은 비동기 이벤트 스트림을 통해 이루어집니다.

**이벤트 설계 규칙**:
- 이벤트는 불변(immutable)
- 이벤트는 필요한 모든 컨텍스트 포함 (외부 키 조회 없음)
- 타임스탬프 형식: ISO 8601
- 직렬화: JSON (단순성을 위해)
- 모든 이벤트에 `schemaVersion` 포함

**Kafka 토픽 전략**:

| 토픽 이름 | 목적 | 생산자 | 소비자 | 키 | 보관 기간 |
|---|---|---|---|---|---|
| `virtual-transactions` | 원시 거래 스트림 | TGS | RDE | userId | 1시간 |
| `transaction-alerts` | 탐지된 의심스러운 거래 | RDE | RAD | userId | 24시간 |

**제약 조건**:
- ❌ 추가 Kafka 토픽 생성 금지
- ❌ Kafka를 요청-응답 패턴으로 사용 금지
- ✅ userId로 파티셔닝 (상태 기반 처리용)

---

### 상태 기반 스트림 처리 (Stateful Stream Processing)

**원칙**: 빈도 기반 탐지는 시간 윈도우에 걸쳐 상태를 유지해야 합니다.

**상태 관리 요구사항**:
- 1분짜리 텀블링 윈도우(tumbling window) 사용
- userId별로 상태 유지 (키 기반 스트림)
- 서비스 재시작 시에도 상태 유지 (영속성 필수)
- 이벤트 시간(event-time) 시맨틱 사용
- 워터마크 처리: 5초 지연 허용

**구현 옵션**:
- **권장**: Apache Flink 1.18+ (RocksDB 상태 백엔드)
- **대안**: Kafka Streams 3.6+ (내장 상태 저장소)
- **결정**: README에 선택과 이유 문서화 필수

---

## Technical Standards

### 기술 스택

**필수 기술**:
- **메시지 브로커**: Apache Kafka 3.6+
- **스트림 처리**: Apache Flink 1.18+ OR Kafka Streams 3.6+
- **백엔드 (TGS, RAD)**: Spring Boot 3.2+
- **프론트엔드 (RAD UI)**: React 18+ + TypeScript 5+
- **컨테이너화**: Docker + Docker Compose

**금지 기술**:
- ❌ Kubernetes (로컬 개발에 너무 복잡함)
- ❌ 서비스 메시 (Istio, Linkerd)
- ❌ 클라우드 전용 서비스 (AWS Kinesis, Azure Event Hubs)
- ❌ NoSQL 데이터베이스 (영속성 계층 불필요)
- ❌ API 게이트웨이 (직접 서비스 접근으로 충분)

---

### 설정 관리

**원칙**: 모든 환경별 값은 외부화해야 합니다.

**제약 조건**:
- ❌ 소스 코드에 값 하드코딩 금지
- ❌ 수동 파일 편집 요구 금지
- ✅ 코드에 합리적인 기본값 제공
- ✅ 환경 변수를 통해 재정의 가능
- ✅ 모든 환경 변수를 README에 문서화

**필수 환경 변수 예시**:
- `KAFKA_BOOTSTRAP_SERVERS` (기본값: kafka:9092)
- `TRANSACTION_GENERATION_INTERVAL_MS` (기본값: 100)
- `HIGH_VALUE_THRESHOLD` (기본값: 1000000)

---

### 로깅 표준

**로그 레벨 사용**:
- `ERROR`: 즉각적인 조치가 필요한 시스템 장애
- `WARN`: 복구 가능한 오류 또는 예상치 못한 상황
- `INFO`: 중요한 비즈니스 이벤트 (서비스 시작, 알림 생성)
- `DEBUG`: 상세한 진단 정보 (개별 거래 처리)
- `TRACE`: 매우 상세한 진단 정보 (Kafka 메타데이터)

**로그 형식**:
```
[timestamp] [level] [service] [thread] [class] - message
```

**필수 로그 항목**:
- 서비스 생명주기 이벤트
- 알림 생성 (ruleType, userId, transactionId 포함)
- 오류 (컨텍스트와 함께)
- 연결 상태 변경

---

### 데이터 모델

**VirtualTransactionEvent** (v1.0):
```json
{
  "schemaVersion": "1.0",
  "transactionId": "uuid-v4",
  "userId": "string (user-1 to user-10)",
  "amount": "long (1000-1500000)",
  "currency": "string (KRW)",
  "countryCode": "string (KR|US|JP|CN)",
  "timestamp": "string (ISO 8601)"
}
```

**AlertEvent** (v1.0):
```json
{
  "schemaVersion": "1.0",
  "alertId": "uuid-v4",
  "originalTransaction": { ... },
  "ruleType": "string (SIMPLE_RULE|STATEFUL_RULE)",
  "ruleName": "string (HIGH_VALUE|FOREIGN_COUNTRY|HIGH_FREQUENCY)",
  "reason": "string (한국어 설명)",
  "severity": "string (HIGH|MEDIUM|LOW)",
  "alertTimestamp": "string (ISO 8601)"
}
```

**스키마 규칙**:
- ❌ 버전 관리 없이 스키마 변경 금지
- ❌ null 값 사용 금지 (Optional 사용 또는 필드 생략)
- ✅ 모든 이벤트에 schemaVersion 포함
- ✅ 수신 이벤트 검증 필수

---

## Quality Gates

### 완료의 정의 (Definition of Done)

**코드**:
- [ ] 모든 설계 원칙을 따름
- [ ] 단위 테스트 ≥70% 커버리지 통과
- [ ] 통합 테스트 통과
- [ ] 컴파일러 경고 없음
- [ ] 코드 리뷰 완료 (팀 프로젝트인 경우)

**문서화**:
- [ ] README 업데이트 (해당하는 경우)
- [ ] 복잡한 로직에 한국어 주석
- [ ] API 변경 사항 문서화

**테스팅**:
- [ ] 수동 테스트 완료
- [ ] 성능 요구사항 검증 (<5초 지연)
- [ ] 오류 시나리오 테스트

**통합**:
- [ ] docker-compose에서 성공적으로 실행
- [ ] 로그가 명확하고 유익함
- [ ] 헬스 체크 엔드포인트 응답 (200 OK)

---

### MVP 인수 기준

**기능 요구사항**:
- [ ] `docker-compose up`이 모든 서비스 시작
- [ ] 시스템이 5분 내에 완전히 작동
- [ ] TGS가 초당 약 10개 거래 생성
- [ ] RDE가 3가지 규칙 모두 정확히 탐지
- [ ] RAD가 5초 미만 지연으로 실시간 알림 표시
- [ ] 웹 UI가 연결 상태 명확히 표시
- [ ] 시스템이 30분 동안 충돌 없이 실행

**비기능 요구사항**:
- [ ] 평균 종단 간 지연 시간: <3초
- [ ] p95 종단 간 지연 시간: <5초
- [ ] 정상 작동 중 ERROR 로그 없음
- [ ] 모든 헬스 체크 엔드포인트 200 OK
- [ ] README 빠른 시작 가이드 완성

**데모 준비성**:
- [ ] 비기술자 청중에게 시연 가능
- [ ] 웹 UI에 명확한 시각적 피드백
- [ ] 3가지 알림 유형 모두 눈에 띄게 발생
- [ ] 시스템 깔끔하게 중지/재시작 가능

---

## Performance Requirements

### 지연 시간

**종단 간 (거래 생성 → 알림 표시)**:
- 목표: 평균 3초, p95 5초
- 허용: 평균 5초, p95 8초
- 불가: p95 > 10초

**컴포넌트 수준**:

| 컴포넌트 | 목표 | 허용 | 불가 |
|---|---|---|---|
| TGS → Kafka | <10ms | <50ms | >100ms |
| RDE 처리 (상태 없음) | <50ms | <100ms | >200ms |
| RDE 처리 (상태 있음) | <200ms | <500ms | >1000ms |
| RAD → WebSocket | <100ms | <500ms | >1000ms |

### 처리량

- **거래 생성률**: 초당 10개 (목표), 초당 100개 (최대)
- **알림 탐지율**: 거래의 10-20% 예상
- **RDE 용량**: 초당 최소 10,000개 거래 처리

### 리소스 제약

**로컬 개발 환경**:
- 총 메모리: <4GB (모든 컨테이너 합계)
- CPU: <2 코어 (정상 부하)
- 디스크 I/O: 최소화

**개별 서비스 제한**:
- TGS: <256MB
- RDE: <2GB
- RAD: <512MB
- Kafka: <1GB
- Zookeeper: <256MB

---

## Development Workflow

### 프로젝트 구조

```
realtime-fds/
├── README.md
├── docker-compose.yml
├── .gitignore
├── constitution.md
│
├── transaction-generator/
│   ├── Dockerfile
│   ├── build.gradle
│   ├── src/
│   └── README.md
│
├── detection-engine/
│   ├── Dockerfile
│   ├── build.gradle (또는 pom.xml)
│   ├── src/
│   └── README.md
│
└── alert-dashboard/
    ├── backend/
    │   ├── Dockerfile
    │   ├── build.gradle
    │   └── src/
    └── frontend/
        ├── Dockerfile
        ├── package.json
        └── src/
```

### Git 워크플로우

**브랜치 전략**:
- `main`: 프로덕션 준비 코드
- `develop`: 통합 브랜치
- `feature/*`: 기능 브랜치

**커밋 메시지 형식** (Conventional Commits + 한국어):
```
<타입>(<스코프>): <제목>

<본문>

<꼬리말>
```

**타입**: feat, fix, docs, style, refactor, test, chore

**예시**:
```
feat(rde): 고액 거래 탐지 규칙 구현

거래 금액이 1,000,000원을 초과할 때 알림을 발생시키는
HighValueTransactionRule을 추가합니다.

Closes #12
```

---

## Anti-Patterns (금지 패턴)

### 아키텍처 안티패턴

- ❌ 서비스 간 공유 데이터베이스
- ❌ 서비스 간 동기식 HTTP 호출
- ❌ Kafka로 요청-응답 패턴
- ❌ 불필요한 추상화 계층

### 코드 안티패턴

- ❌ 지연을 위한 `Thread.sleep()` 사용
- ❌ 예외를 조용히 무시
- ❌ 설정 값 하드코딩
- ❌ `System.out.println()`로 로깅

### 성능 안티패턴

- ❌ 메시지마다 새 Kafka 프로듀서 생성
- ❌ WebSocket 핸들러에서 동기 처리

---

## Governance

### 헌법 우선순위

이 Constitution은 모든 다른 개발 관행보다 우선합니다. 모든 구현 결정은 이 원칙들을 따라야 합니다.

### 수정 절차

Constitution 수정은 다음 절차를 따라야 합니다:

1. 팀과 **논의** (팀 프로젝트인 경우)
2. 커밋 메시지에 **문서화**
3. 명확한 근거로 **정당화**
4. **버전 관리** (날짜 및 버전 번호 업데이트)

### 버전 관리

**버전 번호 형식**: MAJOR.MINOR.PATCH

- **MAJOR**: 하위 호환 불가능한 원칙 제거/재정의
- **MINOR**: 새 원칙/섹션 추가 또는 실질적 확장
- **PATCH**: 명확화, 표현, 오타 수정, 비의미적 개선

### 준수 검증

- 모든 PR/리뷰는 준수 여부 검증 필수
- 복잡성은 정당화되어야 함
- Constitution Check는 plan.md에서 필수 게이트

### 예외 처리

Constitution 위반이 필요한 경우:
- plan.md의 "Complexity Tracking" 섹션에 문서화
- 왜 필요한지 설명
- 더 간단한 대안이 왜 부족한지 설명

### 개정 로그

| 날짜 | 버전 | 변경 사항 | 사유 |
|---|---|---|---|
| 2025-11-06 | 1.0.0 | 최초 Constitution 작성 (SpecKit 형식) | 프로젝트 거버넌스 공식화 |

---

## 최종 참고 사항

이 Constitution은 학습과 실험의 유연성을 유지하면서 개발을 안내하기 위해 만들어졌습니다.

**의심스러울 때 우선순위**:
1. 복잡성보다 **단순성**
2. 기교보다 **가독성**
3. 완벽한 아키텍처보다 **작동하는 소프트웨어**
4. 프로덕션 준비성보다 **학습**

**목표**: 프로덕션 등급 소프트웨어가 아니라, 실시간 스트리밍 기능 시연 및 포트폴리오 가치 구축

**참조**:
- "무엇을"과 "왜": spec.md
- "어떻게"와 "제약 조건": constitution.md (이 문서)
- "구체적 단계": plan.md, tasks.md

---

**헌법 상태**: ✅ 활성
**Version**: 1.0.0 | **Ratified**: 2025-11-06 | **Last Amended**: 2025-11-06
**작성자**: 프로젝트 팀
**검토 주기**: 개발 중 필요에 따라 업데이트
