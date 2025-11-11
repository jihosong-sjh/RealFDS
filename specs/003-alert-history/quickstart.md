# Quickstart Guide: Alert History (과거 알림 조회)

**Feature**: 003-alert-history
**Date**: 2025-11-11
**Target Audience**: 개발자

## Overview

이 가이드는 Alert History 기능을 개발 환경에서 빠르게 시작하는 방법을 안내합니다. 10분 이내에 로컬 환경을 설정하고 첫 API 호출을 할 수 있습니다.

---

## Prerequisites

시작하기 전에 다음 도구들이 설치되어 있는지 확인하세요:

- **Docker Desktop**: 20.10 이상
- **Docker Compose**: 2.0 이상
- **Git**: 2.30 이상
- **JDK**: 17 이상 (백엔드 개발 시)
- **Node.js**: 18 이상 (프론트엔드 개발 시)
- **curl** 또는 **Postman** (API 테스트용)

---

## Quick Start (5분)

### 1. 프로젝트 클론

```bash
git clone https://github.com/your-org/RealFDS.git
cd RealFDS
```

### 2. 브랜치 체크아웃

```bash
git checkout 003-alert-history
```

### 3. Docker Compose 실행

```bash
docker-compose up -d
```

이 명령은 다음 서비스들을 시작합니다:
- **PostgreSQL**: 포트 5432
- **Kafka**: 포트 9092
- **Zookeeper**: 포트 2181
- **Alert Dashboard (Backend)**: 포트 8080
- **Alert Dashboard (Frontend)**: 포트 3000

### 4. 서비스 상태 확인

```bash
# 모든 컨테이너가 실행 중인지 확인
docker-compose ps

# PostgreSQL이 준비되었는지 확인
docker-compose logs postgres | grep "database system is ready"

# Alert Dashboard가 준비되었는지 확인
curl http://localhost:8080/actuator/health
```

**예상 응답**:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### 5. 첫 API 호출

```bash
# 최근 7일간의 알림 조회
curl -X GET "http://localhost:8080/api/alerts/history?page=0&size=10"
```

**예상 응답**:
```json
{
  "content": [
    {
      "alertId": "550e8400-e29b-41d4-a716-446655440001",
      "ruleName": "HIGH_AMOUNT",
      "userId": "user-5",
      "amount": 1500000,
      "alertTimestamp": "2025-11-11T12:00:00Z",
      "status": "UNREAD"
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 10,
  "hasNext": false,
  "hasPrevious": false
}
```

### 6. 웹 UI 접속

브라우저에서 http://localhost:3000/alerts/history 를 열어 알림 이력 페이지를 확인합니다.

---

## Development Setup (로컬 개발)

### Backend (Kotlin + Spring Boot)

#### 1. 환경 설정

```bash
cd alert-dashboard/backend
```

#### 2. application-local.yml 생성

```yaml
# src/main/resources/application-local.yml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/realfds
    username: realfds_user
    password: realfds_password
  flyway:
    url: jdbc:postgresql://localhost:5432/realfds
    user: realfds_user
    password: realfds_password
    baseline-on-migrate: true
    locations: classpath:db/migration

logging:
  level:
    io.realfds: DEBUG
    org.springframework.r2dbc: DEBUG
```

#### 3. Gradle로 빌드 및 실행

```bash
# 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 로컬에서 실행 (PostgreSQL은 Docker로 실행 필요)
./gradlew bootRun --args='--spring.profiles.active=local'
```

#### 4. 주요 패키지 구조

```text
src/main/kotlin/io/realfds/alert/
├── domain/
│   ├── Alert.kt                  # 엔티티
│   ├── AlertStatus.kt            # Enum
│   └── Severity.kt               # Enum
├── repository/
│   ├── AlertRepository.kt        # R2DBC Repository
│   └── CustomAlertRepository.kt  # 동적 쿼리
├── service/
│   └── AlertHistoryService.kt    # 비즈니스 로직
├── controller/
│   └── AlertHistoryController.kt # REST API
└── dto/
    ├── AlertSearchCriteria.kt    # 검색 조건
    └── PagedAlertResult.kt       # 응답 DTO

src/main/resources/
└── db/migration/
    ├── V1__create_alerts_table.sql
    └── V2__insert_sample_alerts.sql
```

---

### Frontend (React + TypeScript)

#### 1. 환경 설정

```bash
cd alert-dashboard/frontend
npm install
```

#### 2. .env.local 생성

```env
REACT_APP_API_BASE_URL=http://localhost:8080/api
```

#### 3. 개발 서버 실행

```bash
npm start
```

브라우저가 자동으로 열리며 http://localhost:3000 로 접속됩니다.

#### 4. 주요 컴포넌트 구조

```text
src/
├── pages/
│   └── AlertHistoryPage.tsx      # 메인 페이지
├── components/
│   ├── AlertHistoryTable.tsx     # 알림 목록 테이블
│   ├── AlertHistoryFilters.tsx   # 검색 필터
│   ├── Pagination.tsx            # 페이지네이션
│   └── DateRangePicker.tsx       # 날짜 범위 선택기
├── services/
│   └── alertHistoryService.ts    # API 호출
├── types/
│   └── alert.ts                  # TypeScript 타입
└── hooks/
    └── useAlertHistory.ts        # React Query 훅
```

---

## Database Management

### PostgreSQL 접속

```bash
# Docker 컨테이너 내부에서 psql 실행
docker-compose exec postgres psql -U realfds_user -d realfds
```

### 유용한 SQL 쿼리

```sql
-- 전체 알림 개수 확인
SELECT COUNT(*) FROM alerts;

-- 상태별 알림 개수
SELECT status, COUNT(*) as count
FROM alerts
GROUP BY status;

-- 최근 10개 알림 조회
SELECT alert_id, rule_name, user_id, amount, alert_timestamp, status
FROM alerts
ORDER BY alert_timestamp DESC
LIMIT 10;

-- 특정 사용자의 알림 조회
SELECT *
FROM alerts
WHERE user_id = 'user-5'
ORDER BY alert_timestamp DESC;

-- 인덱스 확인
\di alerts*
```

### 데이터베이스 초기화

```bash
# 데이터베이스 리셋 (주의: 모든 데이터 삭제)
docker-compose down -v
docker-compose up -d

# Flyway 마이그레이션 재실행
docker-compose restart alert-dashboard
```

---

## API Testing

### cURL 예제

#### 1. 기본 조회 (최근 7일)

```bash
curl -X GET "http://localhost:8080/api/alerts/history"
```

#### 2. 날짜 범위 검색

```bash
curl -X GET "http://localhost:8080/api/alerts/history?startDate=2025-11-01T00:00:00Z&endDate=2025-11-10T23:59:59Z"
```

#### 3. 규칙명 필터링

```bash
curl -X GET "http://localhost:8080/api/alerts/history?ruleName=HIGH_AMOUNT"
```

#### 4. 상태 필터링

```bash
curl -X GET "http://localhost:8080/api/alerts/history?status=UNREAD"
```

#### 5. 복합 검색

```bash
curl -X GET "http://localhost:8080/api/alerts/history?startDate=2025-11-01T00:00:00Z&ruleName=HIGH_AMOUNT&status=UNREAD&page=0&size=20"
```

#### 6. 페이지네이션

```bash
# 첫 페이지 (0~49번)
curl -X GET "http://localhost:8080/api/alerts/history?page=0&size=50"

# 두 번째 페이지 (50~99번)
curl -X GET "http://localhost:8080/api/alerts/history?page=1&size=50"
```

### Postman Collection

Postman Collection 파일은 `/specs/003-alert-history/contracts/postman-collection.json` 에서 다운로드할 수 있습니다.

**Import 방법**:
1. Postman 열기
2. File > Import
3. `postman-collection.json` 파일 선택
4. Collection 이름: "Alert History API"
5. Environment 변수 설정:
   - `baseUrl`: `http://localhost:8080/api`

---

## Testing

### 단위 테스트 실행

```bash
cd alert-dashboard/backend
./gradlew test

# 커버리지 리포트 생성
./gradlew jacocoTestReport

# 리포트 확인
open build/reports/jacoco/test/html/index.html
```

### 통합 테스트 실행

```bash
# Testcontainers를 사용한 통합 테스트
./gradlew integrationTest
```

### 성능 테스트 (K6)

```bash
# K6 설치 (macOS)
brew install k6

# 성능 테스트 실행
k6 run tests/performance/alert-history-load-test.js
```

**테스트 시나리오**:
- 동시 사용자: 10명
- 테스트 기간: 30초
- 목표 응답 시간: <500ms

---

## Common Tasks

### 1. 샘플 데이터 생성

```kotlin
// scripts/GenerateSampleAlerts.kt
fun main() {
    val alerts = (1..1000).map { i ->
        Alert(
            transactionId = UUID.randomUUID(),
            userId = "user-${(i % 10) + 1}",
            amount = (100_000L..2_000_000L).random(),
            currency = "KRW",
            countryCode = listOf("KR", "US", "JP", "CN").random(),
            ruleName = listOf("HIGH_AMOUNT", "FOREIGN_COUNTRY", "RAPID_TRANSACTION").random(),
            reason = "테스트 알림 #$i",
            severity = listOf(Severity.HIGH, Severity.MEDIUM, Severity.LOW).random(),
            alertTimestamp = Instant.now().minus(Duration.ofHours(i.toLong())),
            status = listOf(AlertStatus.UNREAD, AlertStatus.IN_PROGRESS, AlertStatus.COMPLETED).random()
        )
    }

    // alertRepository.saveAll(alerts)
}
```

### 2. 로그 확인

```bash
# Alert Dashboard 로그
docker-compose logs -f alert-dashboard

# PostgreSQL 로그
docker-compose logs -f postgres

# 특정 키워드 검색
docker-compose logs alert-dashboard | grep "ERROR"
```

### 3. 데이터베이스 백업

```bash
# 백업
docker-compose exec postgres pg_dump -U realfds_user realfds > backup.sql

# 복원
docker-compose exec -T postgres psql -U realfds_user -d realfds < backup.sql
```

---

## Troubleshooting

### 문제: PostgreSQL 연결 오류

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

---

### 문제: Flyway 마이그레이션 실패

**증상**:
```
ERROR: Flyway migration failed
```

**해결 방법**:
```bash
# Flyway 히스토리 확인
docker-compose exec postgres psql -U realfds_user -d realfds -c "SELECT * FROM flyway_schema_history;"

# 수동 마이그레이션 실패 해결 (주의: 개발 환경 전용)
docker-compose exec postgres psql -U realfds_user -d realfds -c "DELETE FROM flyway_schema_history WHERE success = false;"

# 서비스 재시작
docker-compose restart alert-dashboard
```

---

### 문제: 검색 결과가 비어있음

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

# 샘플 데이터가 없으면 마이그레이션 실행 확인
docker-compose exec postgres psql -U realfds_user -d realfds -c "SELECT version FROM flyway_schema_history;"

# V2 마이그레이션이 실행되지 않았다면 수동 실행
docker-compose exec postgres psql -U realfds_user -d realfds < alert-dashboard/backend/src/main/resources/db/migration/V2__insert_sample_alerts.sql
```

---

### 문제: 응답 시간이 느림 (>500ms)

**증상**:
알림 검색 시 응답 시간이 500ms를 초과합니다.

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

-- 인덱스가 사용되고 있는지 확인
-- 'Index Scan' 또는 'Bitmap Index Scan'이 표시되어야 함
```

---

## Environment Variables

| 변수명 | 기본값 | 설명 |
|-------|-------|-----|
| `SPRING_R2DBC_URL` | `r2dbc:postgresql://postgres:5432/realfds` | R2DBC URL |
| `SPRING_R2DBC_USERNAME` | `realfds_user` | DB 사용자명 |
| `SPRING_R2DBC_PASSWORD` | `realfds_password` | DB 비밀번호 |
| `SPRING_FLYWAY_URL` | `jdbc:postgresql://postgres:5432/realfds` | Flyway URL |
| `ALERT_HISTORY_DEFAULT_PAGE_SIZE` | `50` | 기본 페이지 크기 |
| `ALERT_HISTORY_MAX_PAGE_SIZE` | `100` | 최대 페이지 크기 |
| `ALERT_HISTORY_DEFAULT_DATE_RANGE_DAYS` | `7` | 기본 날짜 범위 (일) |

**재정의 방법 (Docker Compose)**:
```yaml
services:
  alert-dashboard:
    environment:
      SPRING_R2DBC_URL: r2dbc:postgresql://custom-host:5432/custom-db
      ALERT_HISTORY_DEFAULT_PAGE_SIZE: 100
```

---

## Next Steps

구현을 시작하려면:

1. **spec.md** 읽기: 전체 요구사항 이해
2. **research.md** 읽기: 기술 결정사항 이해
3. **data-model.md** 읽기: 데이터베이스 스키마 이해
4. **contracts/alert-history-api.yaml** 읽기: API 계약 이해
5. **/speckit.tasks** 실행: 구현 태스크 생성 및 실행

---

## Resources

- [Spring Data R2DBC Reference](https://docs.spring.io/spring-data/r2dbc/reference/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/15/)
- [Flyway Documentation](https://documentation.red-gate.com/fd)
- [React Query Documentation](https://tanstack.com/query/latest)
- [OpenAPI Specification](https://spec.openapis.org/oas/v3.0.3)

---

## Support

문제가 발생하면:
1. **로그 확인**: `docker-compose logs -f alert-dashboard`
2. **헬스 체크**: `curl http://localhost:8080/actuator/health`
3. **데이터베이스 상태**: `docker-compose ps postgres`
4. **GitHub Issues**: [프로젝트 이슈 페이지](https://github.com/your-org/RealFDS/issues)

---

**Quickstart Status**: ✅ Ready
**Last Updated**: 2025-11-11
