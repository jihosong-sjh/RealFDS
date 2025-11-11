# Alert History (과거 알림 조회)

**Feature**: 003-alert-history
**Status**: ✅ Completed
**Created**: 2025-11-11

---

## 개요

Alert History 기능은 RealFDS 시스템에서 발생한 모든 알림을 PostgreSQL 데이터베이스에 영구 저장하고, 다양한 검색 조건으로 과거 알림을 조회할 수 있는 기능입니다. 이를 통해 보안 담당자는 시스템 재시작 후에도 모든 알림 데이터를 유지하고, 패턴 분석 및 오탐(false positive) 검토를 수행할 수 있습니다.

### 핵심 가치

- ✅ **데이터 영속성**: 시스템 재시작 후에도 모든 알림 데이터 보존
- ⚡ **빠른 검색**: 10,000개 알림 중 500ms 이내 검색 응답
- 🔍 **다양한 필터링**: 날짜 범위, 규칙명, 사용자 ID, 상태별 검색
- 📊 **패턴 분석 지원**: 과거 알림 데이터를 통한 사기 패턴 분석
- 🎯 **페이지네이션**: 대량 데이터 효율적 조회

---

## 빠른 시작

### 1. 시스템 시작

```bash
# 프로젝트 루트에서 실행
docker-compose up -d
```

이 명령은 다음 서비스들을 시작합니다:
- PostgreSQL (포트 5432)
- Alert Dashboard Backend (포트 8080)
- Alert Dashboard Frontend (포트 3000)
- Kafka, Zookeeper 등 기타 서비스

### 2. 상태 확인

```bash
# 헬스 체크
curl http://localhost:8080/actuator/health

# PostgreSQL 연결 확인
docker-compose exec postgres psql -U realfds_user -d realfds -c "SELECT COUNT(*) FROM alerts;"
```

### 3. 웹 UI 접속

브라우저에서 http://localhost:3000/alerts/history 를 열어 알림 이력 페이지를 확인합니다.

### 4. API 호출 예제

```bash
# 최근 7일간의 알림 조회
curl -X GET "http://localhost:8080/api/alerts/history?page=0&size=50"

# 날짜 범위 검색
curl -X GET "http://localhost:8080/api/alerts/history?startDate=2025-11-01T00:00:00Z&endDate=2025-11-11T23:59:59Z"

# 규칙명으로 필터링
curl -X GET "http://localhost:8080/api/alerts/history?ruleName=HIGH_AMOUNT"

# 복합 검색
curl -X GET "http://localhost:8080/api/alerts/history?ruleName=HIGH_AMOUNT&status=UNREAD&page=0&size=20"
```

---

## 주요 기능

### 1. 알림 영속화 (User Story 1 - P1)

- **목적**: 모든 알림을 PostgreSQL에 영구 저장
- **가치**: 시스템 재시작 후에도 모든 알림 데이터 보존
- **구현**:
  - Kafka에서 수신한 알림을 자동으로 PostgreSQL에 저장
  - Flyway를 통한 자동 데이터베이스 마이그레이션
  - 저장 실패 시 재시도 로직 (최대 3회)

**테스트 방법**:
```bash
# 시스템 재시작 후에도 알림이 유지되는지 확인
docker-compose restart alert-dashboard
curl http://localhost:8080/api/alerts/history
```

### 2. 날짜 범위 검색 (User Story 2 - P2)

- **목적**: 특정 기간 동안 발생한 알림만 조회
- **가치**: 시간대별 패턴 분석 및 주간/월간 리포트 작성 지원
- **구현**:
  - ISO 8601 형식의 startDate, endDate 파라미터 지원
  - 날짜 인덱스를 통한 빠른 검색
  - 기본값: 최근 7일

**사용 예**:
- 지난주 알림 조회
- 특정 날짜의 알림 조회
- 월말 결산 리포트 작성

### 3. 다중 조건 필터링 (User Story 3 - P3)

- **목적**: 규칙명, 사용자 ID, 상태 등 여러 조건 조합 검색
- **가치**: 특정 패턴의 알림만 빠르게 필터링
- **구현**:
  - 규칙명 필터 (HIGH_AMOUNT, FOREIGN_COUNTRY, RAPID_TRANSACTION)
  - 사용자 ID 필터
  - 상태 필터 (UNREAD, IN_PROGRESS, COMPLETED)
  - 동적 쿼리 생성 (선택된 필터만 WHERE 절에 포함)

**사용 예**:
- 특정 규칙으로 발생한 미확인 알림만 조회
- 특정 사용자의 완료된 알림만 조회
- 여러 조건을 조합한 정밀 검색

### 4. 페이지네이션

- **목적**: 대량 알림 데이터 효율적 조회
- **구현**:
  - 페이지당 기본 50개 (최대 100개)
  - 전체 개수, 전체 페이지, 현재 페이지 정보 제공
  - 이전/다음 페이지 존재 여부 표시

---

## 기술 스택

### Backend
- **언어**: Kotlin 1.9+
- **프레임워크**: Spring Boot 3.2+ (WebFlux)
- **데이터베이스**: PostgreSQL 15+
- **ORM**: Spring Data R2DBC (비동기)
- **마이그레이션**: Flyway
- **테스트**: JUnit 5, Mockito, Reactor Test, Testcontainers

### Frontend
- **언어**: TypeScript 5+
- **프레임워크**: React 18+ with Vite
- **상태 관리**: React Query (TanStack Query)
- **테스트**: Vitest, React Testing Library

### 성능 목표
- 10,000개 알림 중 검색 시 **500ms 이내** 응답
- 동시 사용자 50명까지 지원
- 최대 100,000개 알림 처리

---

## 환경 변수

| 변수명 | 기본값 | 설명 |
|-------|-------|-----|
| `SPRING_R2DBC_URL` | `r2dbc:postgresql://postgres:5432/realfds` | R2DBC 연결 URL |
| `SPRING_R2DBC_USERNAME` | `realfds_user` | 데이터베이스 사용자명 |
| `SPRING_R2DBC_PASSWORD` | `realfds_password` | 데이터베이스 비밀번호 |
| `SPRING_FLYWAY_URL` | `jdbc:postgresql://postgres:5432/realfds` | Flyway 마이그레이션 URL |
| `ALERT_HISTORY_DEFAULT_PAGE_SIZE` | `50` | 기본 페이지 크기 |
| `ALERT_HISTORY_MAX_PAGE_SIZE` | `100` | 최대 페이지 크기 |
| `ALERT_HISTORY_DEFAULT_DATE_RANGE_DAYS` | `7` | 기본 날짜 범위 (일) |

### 환경 변수 변경 방법

Docker Compose 사용 시 `docker-compose.yml` 파일 수정:

```yaml
services:
  alert-dashboard:
    environment:
      SPRING_R2DBC_URL: r2dbc:postgresql://custom-host:5432/custom-db
      ALERT_HISTORY_DEFAULT_PAGE_SIZE: 100
```

---

## 문제 해결

### PostgreSQL 연결 오류

**증상**:
```
ERROR: Connection refused: postgres:5432
```

**해결 방법**:
```bash
# PostgreSQL 상태 확인
docker-compose ps postgres

# PostgreSQL 로그 확인
docker-compose logs postgres

# PostgreSQL 재시작
docker-compose restart postgres
```

### Flyway 마이그레이션 실패

**증상**:
```
ERROR: Flyway migration failed
```

**해결 방법**:
```bash
# Flyway 히스토리 확인
docker-compose exec postgres psql -U realfds_user -d realfds -c "SELECT * FROM flyway_schema_history;"

# 실패한 마이그레이션 삭제 (개발 환경 전용)
docker-compose exec postgres psql -U realfds_user -d realfds -c "DELETE FROM flyway_schema_history WHERE success = false;"

# 서비스 재시작
docker-compose restart alert-dashboard
```

### 검색 결과가 비어있음

**증상**:
```json
{
  "content": [],
  "totalElements": 0
}
```

**해결 방법**:
```bash
# 데이터베이스에 데이터가 있는지 확인
docker-compose exec postgres psql -U realfds_user -d realfds -c "SELECT COUNT(*) FROM alerts;"

# 샘플 데이터 확인
docker-compose exec postgres psql -U realfds_user -d realfds -c "SELECT * FROM alerts LIMIT 5;"
```

### 응답 시간이 느림 (>500ms)

**해결 방법**:
```sql
-- 인덱스 확인
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'alerts';

-- 쿼리 플랜 확인
EXPLAIN ANALYZE
SELECT * FROM alerts
WHERE alert_timestamp BETWEEN '2025-11-01' AND '2025-11-11'
ORDER BY alert_timestamp DESC
LIMIT 50 OFFSET 0;
```

인덱스가 사용되고 있는지 확인하세요 (`Index Scan` 또는 `Bitmap Index Scan`이 표시되어야 함).

---

## 프로젝트 구조

```
specs/003-alert-history/
├── README.md (this file)          # 기능 개요 및 빠른 시작 가이드
├── spec.md                        # 상세 요구사항 명세
├── plan.md                        # 구현 계획
├── research.md                    # 기술 결정사항
├── data-model.md                  # 데이터베이스 스키마
├── quickstart.md                  # 개발자용 상세 가이드
├── tasks.md                       # 구현 태스크 목록
└── contracts/
    └── alert-history-api.yaml     # OpenAPI 스펙

alert-dashboard/
├── backend/
│   ├── src/main/kotlin/io/realfds/alert/
│   │   ├── domain/               # 엔티티 (Alert, AlertStatus, Severity)
│   │   ├── repository/           # Repository (AlertRepository, CustomAlertRepository)
│   │   ├── service/              # 비즈니스 로직 (AlertHistoryService)
│   │   ├── controller/           # REST API (AlertHistoryController)
│   │   └── dto/                  # DTO (AlertSearchCriteria, PagedAlertResult)
│   └── src/main/resources/
│       └── db/migration/         # Flyway 마이그레이션
│           ├── V1__create_alerts_table.sql
│           └── V2__insert_sample_alerts.sql
└── frontend/
    └── src/
        ├── pages/                # AlertHistoryPage
        ├── components/           # DateRangePicker, AlertHistoryFilters, etc.
        ├── services/             # alertHistoryService
        └── types/                # TypeScript 타입 정의
```

---

## 데이터베이스 스키마

### alerts 테이블

| 컬럼명 | 타입 | 제약 조건 | 설명 |
|--------|------|----------|------|
| alert_id | UUID | PRIMARY KEY | 알림 고유 식별자 |
| schema_version | INTEGER | NOT NULL | 스키마 버전 |
| transaction_id | UUID | NOT NULL | 연관된 거래 ID |
| user_id | VARCHAR(50) | NOT NULL | 사용자 ID |
| amount | BIGINT | NOT NULL | 거래 금액 |
| currency | VARCHAR(3) | NOT NULL | 통화 코드 |
| country_code | VARCHAR(2) | NOT NULL | 국가 코드 |
| rule_name | VARCHAR(100) | NOT NULL | 탐지 규칙명 |
| reason | TEXT | NOT NULL | 탐지 사유 |
| severity | VARCHAR(20) | NOT NULL | 심각도 (HIGH/MEDIUM/LOW) |
| alert_timestamp | TIMESTAMPTZ | NOT NULL | 알림 발생 시각 |
| status | VARCHAR(20) | NOT NULL DEFAULT 'UNREAD' | 상태 |
| assigned_to | VARCHAR(100) | NULL | 담당자 |
| action_note | TEXT | NULL | 조치 내역 |
| processed_at | TIMESTAMPTZ | NULL | 처리 완료 시각 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW() | 생성 시각 |

### 인덱스

- `idx_alert_timestamp`: alert_timestamp DESC (날짜 범위 검색 최적화)
- `idx_rule_name`: rule_name (규칙명 필터링 최적화)
- `idx_user_id`: user_id (사용자 ID 필터링 최적화)
- `idx_status`: status (상태 필터링 최적화)

---

## API 엔드포인트

### GET /api/alerts/history

과거 알림을 검색합니다.

**쿼리 파라미터**:
- `startDate` (optional): 검색 시작 날짜 (ISO 8601 형식)
- `endDate` (optional): 검색 종료 날짜 (ISO 8601 형식)
- `ruleName` (optional): 규칙명 필터
- `userId` (optional): 사용자 ID 필터
- `status` (optional): 상태 필터 (UNREAD, IN_PROGRESS, COMPLETED)
- `page` (optional): 페이지 번호 (기본값: 0)
- `size` (optional): 페이지 크기 (기본값: 50, 최대: 100)

**응답 예**:
```json
{
  "content": [
    {
      "alertId": "550e8400-e29b-41d4-a716-446655440001",
      "schemaVersion": 1,
      "transactionId": "660e8400-e29b-41d4-a716-446655440002",
      "userId": "user-5",
      "amount": 1500000,
      "currency": "KRW",
      "countryCode": "KR",
      "ruleName": "HIGH_AMOUNT",
      "reason": "금액이 임계값을 초과했습니다",
      "severity": "HIGH",
      "alertTimestamp": "2025-11-11T12:00:00Z",
      "status": "UNREAD",
      "assignedTo": null,
      "actionNote": null,
      "processedAt": null,
      "createdAt": "2025-11-11T12:00:01Z"
    }
  ],
  "totalElements": 150,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 50,
  "hasNext": true,
  "hasPrevious": false
}
```

상세 API 스펙은 [contracts/alert-history-api.yaml](./contracts/alert-history-api.yaml)를 참고하세요.

---

## 테스트

### 단위 테스트 실행

```bash
cd alert-dashboard/backend
./gradlew test
```

### 통합 테스트 실행

```bash
# Testcontainers를 사용한 통합 테스트
./gradlew integrationTest
```

### 커버리지 리포트 생성

```bash
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

### 성능 테스트 (K6)

```bash
# K6 설치 (macOS)
brew install k6

# 성능 테스트 실행
k6 run tests/performance/alert-history-load-test.js
```

**성능 목표**:
- 동시 사용자: 10명
- 응답 시간: <500ms (p95)
- 처리량: 초당 20개 요청

---

## 개발 가이드

### 로컬 개발 환경 설정

상세한 개발 환경 설정 방법은 [quickstart.md](./quickstart.md)를 참고하세요.

### 새로운 필터 추가

1. `AlertSearchCriteria` DTO에 필드 추가
2. `CustomAlertRepositoryImpl`에 필터 로직 구현
3. 단위 테스트 및 통합 테스트 작성
4. API 문서 업데이트

### 코드 스타일

- **한국어 주석**: 모든 복잡한 로직에 한국어 주석 추가 (Constitution VI)
- **함수 길이**: 최대 50줄 (Constitution V)
- **파일 길이**: 최대 300줄 (Constitution V)
- **서술적인 이름**: 변수, 함수, 클래스명은 명확하게 작성

---

## Constitution 준수

이 기능은 RealFDS Constitution의 모든 원칙을 준수합니다:

- ✅ **I. 학습 우선**: PostgreSQL R2DBC를 통한 비동기 DB 액세스 학습
- ✅ **II. 단순함**: docker-compose up으로 PostgreSQL 자동 실행
- ✅ **III. 실시간 우선**: R2DBC 비동기 처리, 500ms 이내 응답
- ✅ **IV. 서비스 경계**: alert-service 내부 확장 (새 서비스 추가 안 함)
- ✅ **V. 품질 표준**: 테스트 커버리지 ≥70%, 구조화된 로깅, 헬스 체크
- ✅ **VI. 한국어 우선**: 모든 주석, 문서, 로그 메시지 한국어 작성

---

## 관련 문서

- [spec.md](./spec.md) - 상세 요구사항 명세
- [plan.md](./plan.md) - 구현 계획
- [research.md](./research.md) - 기술 결정사항 (14개 항목)
- [data-model.md](./data-model.md) - 데이터베이스 스키마 및 쿼리 패턴
- [quickstart.md](./quickstart.md) - 개발자용 상세 가이드
- [contracts/alert-history-api.yaml](./contracts/alert-history-api.yaml) - OpenAPI 스펙

---

## 지원

문제가 발생하면:

1. **로그 확인**: `docker-compose logs -f alert-dashboard`
2. **헬스 체크**: `curl http://localhost:8080/actuator/health`
3. **데이터베이스 상태**: `docker-compose ps postgres`
4. **GitHub Issues**: 프로젝트 이슈 페이지에 문의

---

**Feature Status**: ✅ Completed
**Last Updated**: 2025-11-11
**Version**: 1.0
