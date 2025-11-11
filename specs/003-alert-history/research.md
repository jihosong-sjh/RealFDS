# Research: Alert History (과거 알림 조회)

**Feature**: 003-alert-history
**Date**: 2025-11-11
**Status**: Completed

## Overview

이 문서는 Alert History 기능 구현을 위한 기술 조사 및 결정사항을 담고 있습니다. 모든 NEEDS CLARIFICATION 항목을 해결하고, 기술 선택의 근거를 제시합니다.

---

## 1. 데이터베이스 선택: PostgreSQL

### Decision
PostgreSQL 15 이상을 사용하여 알림 데이터를 영속화합니다.

### Rationale
- **관계형 데이터 모델**: Alert 엔티티는 구조화된 스키마를 가지며, 복잡한 쿼리(다중 필터, 페이지네이션)가 필요합니다
- **트랜잭션 지원**: ACID 보장으로 데이터 일관성 확보
- **인덱스 성능**: B-Tree 인덱스를 통해 날짜 범위 검색, 필터링에서 우수한 성능 제공
- **R2DBC 호환성**: Spring WebFlux의 비동기 특성과 호환되는 R2DBC 드라이버 제공
- **Flyway 지원**: 데이터베이스 마이그레이션 도구와의 완벽한 통합

### Alternatives Considered
- **MongoDB (NoSQL)**: Constitution IV 위반 (NoSQL 데이터베이스 금지). 관계형 쿼리가 필요한 요구사항에 부적합
- **In-memory (Redis)**: 시스템 재시작 시 데이터 유실 위험, 대용량 데이터 보관 시 메모리 부족
- **Elasticsearch**: 과도한 복잡성, 전체 텍스트 검색이 현재 범위에서 제외되어 있음

### Risk Mitigation
- Docker Compose로 PostgreSQL 컨테이너를 자동 실행하여 단일 명령어 배포 유지
- 개발 환경에서 데이터베이스 초기화 스크립트 제공

---

## 2. 비동기 데이터베이스 액세스: Spring Data R2DBC

### Decision
Spring Data R2DBC를 사용하여 비동기 방식으로 PostgreSQL에 액세스합니다.

### Rationale
- **WebFlux 호환성**: Spring WebFlux의 비동기 특성과 일관된 프로그래밍 모델
- **백프레셔(Backpressure) 지원**: Reactor 기반의 리액티브 스트림으로 시스템 부하 제어
- **Repository 추상화**: Spring Data의 표준 Repository 패턴 사용 가능
- **성능**: 블로킹 I/O를 피하여 높은 동시성 처리 가능
- **Constitution III 준수**: 실시간 우선 원칙에 따라 비동기 처리 필수

### Alternatives Considered
- **Spring Data JDBC**: 동기 방식으로 WebFlux와 호환되지 않음 (블로킹 작업)
- **Plain JDBC**: Constitution I 위반 (명시적 패턴 선호). R2DBC의 리액티브 추상화가 더 명확함
- **jOOQ**: 추가 학습 곡선이 있고, Spring Data R2DBC만으로 충분한 기능 제공

### Implementation Notes
```kotlin
// AlertRepository 예시
interface AlertRepository : R2dbcRepository<Alert, UUID> {
    fun findByRuleName(ruleName: String, pageable: Pageable): Flux<Alert>
    fun findByAlertTimestampBetween(start: Instant, end: Instant, pageable: Pageable): Flux<Alert>
}
```

---

## 3. 데이터베이스 마이그레이션: Flyway

### Decision
Flyway를 사용하여 데이터베이스 스키마를 버전 관리하고 마이그레이션을 자동화합니다.

### Rationale
- **자동화**: 애플리케이션 시작 시 자동으로 스키마 마이그레이션 실행
- **버전 관리**: 스키마 변경 이력을 명확하게 추적
- **단순함**: SQL 파일 기반으로 학습 곡선이 낮음
- **Constitution II 준수**: 수동 스키마 설정 불필요 (단순함 우선)
- **Spring Boot 통합**: Spring Boot Starter를 통해 쉽게 설정 가능

### Alternatives Considered
- **Liquibase**: 더 많은 기능을 제공하지만 복잡성이 높음. Flyway로 충분한 기능 제공
- **수동 SQL 스크립트**: 버전 관리 부족, 실행 순서 관리 어려움, 에러 발생 가능성 높음
- **JPA/Hibernate `ddl-auto`**: 프로덕션 환경에서 위험, 명시적 스키마 제어 불가

### Migration Strategy
```sql
-- V1__create_alerts_table.sql
CREATE TABLE alerts (
    alert_id UUID PRIMARY KEY,
    schema_version VARCHAR(10) NOT NULL,
    transaction_id UUID NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    reason TEXT NOT NULL,
    severity VARCHAR(10) NOT NULL,
    alert_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    assigned_to VARCHAR(50),
    action_note TEXT,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alert_timestamp ON alerts(alert_timestamp DESC);
CREATE INDEX idx_rule_name ON alerts(rule_name);
CREATE INDEX idx_user_id ON alerts(user_id);
CREATE INDEX idx_status ON alerts(status);
```

---

## 4. 검색 성능 최적화: 인덱스 전략

### Decision
검색 조건에 해당하는 필드에 B-Tree 인덱스를 생성합니다.

### Rationale
- **성능 요구사항**: 10,000개 알림 중 검색 시 500ms 이내 응답 필요 (FR-013)
- **검색 패턴**: 날짜 범위, 규칙명, 사용자 ID, 상태로 자주 검색
- **인덱스 선택성**: 위 필드들은 선택성(selectivity)이 높아 인덱스 효과가 큼

### Index Design
| 필드 | 인덱스 타입 | 이유 |
|-----|----------|-----|
| `alert_timestamp` | B-Tree (DESC) | 날짜 범위 검색, 기본 정렬 (최신 순) |
| `rule_name` | B-Tree | 규칙명 필터링 (약 10개 규칙) |
| `user_id` | B-Tree | 사용자별 필터링 (user-1 ~ user-10) |
| `status` | B-Tree | 상태 필터링 (UNREAD, IN_PROGRESS, COMPLETED) |

### Composite Index 고려사항
- **현재 제외**: 단일 인덱스로 충분한 성능 제공 (10,000개 규모)
- **향후 확장**: 100,000개 이상으로 확장 시 복합 인덱스 고려
  - 예: `(alert_timestamp, rule_name)` - 날짜 범위 + 규칙명 조합 검색 시

### Performance Testing Plan
- 10,000개 알림 데이터 생성
- 다양한 검색 조건으로 응답 시간 측정
- `EXPLAIN ANALYZE` 로 쿼리 플랜 검증

---

## 5. 페이지네이션: Spring Data Pageable

### Decision
Spring Data의 `Pageable` 인터페이스를 사용하여 페이지네이션을 구현합니다.

### Rationale
- **표준 패턴**: Spring Data의 표준 페이지네이션 추상화
- **명시적**: 페이지 번호, 크기, 정렬 조건을 명확하게 정의
- **R2DBC 지원**: R2DBC Repository와 완벽하게 통합
- **Constitution I 준수**: 명시적 패턴 사용

### Implementation
```kotlin
// Service Layer
fun searchAlerts(criteria: AlertSearchCriteria, page: Int, size: Int): Mono<PagedAlertResult> {
    val pageable = PageRequest.of(page, size, Sort.by("alertTimestamp").descending())
    return alertRepository.findByCriteria(criteria, pageable)
        .collectList()
        .zipWith(alertRepository.countByCriteria(criteria))
        .map { (content, total) ->
            PagedAlertResult(
                content = content,
                totalElements = total,
                totalPages = (total + size - 1) / size,
                currentPage = page,
                pageSize = size
            )
        }
}
```

### Performance Consideration
- **LIMIT/OFFSET**: PostgreSQL의 `LIMIT`와 `OFFSET`을 사용
- **Deep Pagination 이슈**: 매우 큰 페이지 번호(예: 1000페이지)에서 성능 저하 가능
  - **현재 범위**: 최대 100,000개 알림 (2,000페이지) 까지만 고려
  - **향후 개선**: Keyset Pagination (Cursor-based) 고려 (범위 외)

---

## 6. 동적 쿼리 생성: Criteria API

### Decision
Spring Data R2DBC의 `@Query` 어노테이션과 동적 쿼리 생성을 조합하여 다중 검색 조건을 처리합니다.

### Rationale
- **유연성**: 사용자가 선택한 필터 조건만 WHERE 절에 포함
- **타입 안정성**: 컴파일 타임에 타입 체크 가능
- **가독성**: SQL 쿼리를 명시적으로 작성하여 이해하기 쉬움

### Implementation Approach
```kotlin
// Custom Repository Implementation
class CustomAlertRepositoryImpl(
    private val databaseClient: R2dbcEntityTemplate
) : CustomAlertRepository {

    override fun findByCriteria(criteria: AlertSearchCriteria, pageable: Pageable): Flux<Alert> {
        var sql = "SELECT * FROM alerts WHERE 1=1"
        val params = mutableMapOf<String, Any>()

        criteria.startDate?.let {
            sql += " AND alert_timestamp >= :startDate"
            params["startDate"] = it
        }
        criteria.endDate?.let {
            sql += " AND alert_timestamp <= :endDate"
            params["endDate"] = it
        }
        criteria.ruleName?.let {
            sql += " AND rule_name = :ruleName"
            params["ruleName"] = it
        }
        criteria.userId?.let {
            sql += " AND user_id = :userId"
            params["userId"] = it
        }
        criteria.status?.let {
            sql += " AND status = :status"
            params["status"] = it.name
        }

        sql += " ORDER BY alert_timestamp DESC LIMIT :limit OFFSET :offset"
        params["limit"] = pageable.pageSize
        params["offset"] = pageable.offset

        return databaseClient.sql(sql)
            .bindValues(params)
            .map { row, metadata -> /* map to Alert */ }
            .all()
    }
}
```

### Alternatives Considered
- **QueryDSL**: 더 강력한 타입 안정성을 제공하지만 R2DBC 지원이 제한적이고 복잡성 증가
- **JOOQ**: Constitution I 위반 (추가 학습 곡선). 현재 요구사항에 과도한 복잡성
- **JPA Criteria API**: R2DBC와 호환되지 않음 (JPA는 동기 방식)

---

## 7. UI 구현: React + TypeScript

### Decision
React 18과 TypeScript 5를 사용하여 알림 이력 조회 페이지를 구현합니다.

### Rationale
- **Constitution 준수**: constitution.md에서 요구하는 기술 스택 (React 18+ + TypeScript 5+)
- **기존 프로젝트 일관성**: RAD (Real-time Alert Dashboard)가 이미 React + TypeScript로 구현되어 있음
- **타입 안정성**: TypeScript로 컴파일 타임 에러 감소
- **컴포넌트 재사용**: 기존 대시보드 컴포넌트 재사용 가능

### Component Structure
```text
frontend/src/
├── pages/
│   └── AlertHistory.tsx          # 메인 페이지
├── components/
│   ├── AlertHistoryTable.tsx     # 알림 목록 테이블
│   ├── AlertHistoryFilters.tsx   # 검색 필터 UI
│   ├── Pagination.tsx            # 페이지네이션 컨트롤
│   └── DateRangePicker.tsx       # 날짜 범위 선택기
├── services/
│   └── alertHistoryService.ts    # API 호출 로직
└── types/
    └── alert.ts                  # Alert 타입 정의
```

### UI Library Selection
- **Material-UI (MUI)**: 프로페셔널한 UI, DatePicker 컴포넌트 기본 제공
- **TanStack Table (React Table v8)**: 강력한 테이블 라이브러리, 페이지네이션/정렬 기본 지원
- **React Query (TanStack Query)**: 서버 상태 관리, 캐싱, 자동 리페칭

### Alternatives Considered
- **Vue.js**: Constitution 위반 (React 18+ 요구)
- **Angular**: Constitution 위반, 학습 곡선 높음
- **Plain JavaScript**: TypeScript의 타입 안정성 상실

---

## 8. API 설계: RESTful Endpoints

### Decision
RESTful API 패턴을 사용하여 알림 이력 조회 엔드포인트를 설계합니다.

### Rationale
- **표준 패턴**: REST는 널리 사용되는 API 설계 패턴
- **HTTP 시맨틱**: GET 메서드로 조회, 쿼리 파라미터로 필터/페이지네이션 전달
- **캐싱 가능**: HTTP 캐싱 헤더 활용 가능
- **단순함**: WebSocket이나 GraphQL보다 간단한 조회 작업에 적합

### Endpoint Specification
```
GET /api/alerts/history
Query Parameters:
  - startDate: ISO 8601 (optional, default: 7 days ago)
  - endDate: ISO 8601 (optional, default: now)
  - ruleName: String (optional)
  - userId: String (optional)
  - status: Enum(UNREAD, IN_PROGRESS, COMPLETED) (optional)
  - page: Int (optional, default: 0)
  - size: Int (optional, default: 50, max: 100)

Response: 200 OK
{
  "content": [Alert],
  "totalElements": Int,
  "totalPages": Int,
  "currentPage": Int,
  "pageSize": Int
}

Error Responses:
  - 400 Bad Request: 잘못된 날짜 형식, 페이지 번호 음수 등
  - 500 Internal Server Error: 데이터베이스 연결 오류 등
```

### Alternatives Considered
- **GraphQL**: 과도한 복잡성, 현재 단순한 조회 작업에 불필요
- **WebSocket**: 실시간 스트리밍이 아닌 일회성 조회에 부적합
- **gRPC**: 브라우저 호환성 문제, 추가 프록시 필요

---

## 9. 에러 처리 전략

### Decision
계층별 에러 처리와 구조화된 로깅을 구현합니다.

### Rationale
- **Constitution V 준수**: 예외를 조용히 무시하지 않음, 컨텍스트와 함께 오류 로깅
- **디버깅 용이성**: 로그를 통해 문제 원인을 빠르게 파악 가능
- **사용자 경험**: 사용자에게 명확한 에러 메시지 제공

### Error Handling Strategy
```kotlin
// Controller Level
@RestController
class AlertHistoryController(
    private val alertHistoryService: AlertHistoryService
) {
    @GetMapping("/api/alerts/history")
    fun searchAlerts(@Valid criteria: AlertSearchCriteria): Mono<ResponseEntity<PagedAlertResult>> {
        return alertHistoryService.searchAlerts(criteria)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { error ->
                logger.error("Failed to search alerts", error)
                when (error) {
                    is InvalidDateRangeException ->
                        Mono.just(ResponseEntity.badRequest().body(ErrorResponse(error.message)))
                    is DatabaseConnectionException ->
                        Mono.just(ResponseEntity.status(503).body(ErrorResponse("서비스를 일시적으로 사용할 수 없습니다")))
                    else ->
                        Mono.just(ResponseEntity.internalServerError().body(ErrorResponse("알 수 없는 오류가 발생했습니다")))
                }
            }
    }
}
```

### Logging Strategy
- **SLF4J + Logback**: 구조화된 JSON 로깅
- **로그 레벨**:
  - `INFO`: 알림 조회 요청 시작/완료
  - `WARN`: 잘못된 검색 조건, 빈 결과
  - `ERROR`: 데이터베이스 연결 오류, 예외 발생
- **로그 포맷**: `[timestamp] [level] [AlertHistoryService] - message {contextKey=value}`

---

## 10. 테스트 전략

### Decision
3-tier 테스트 전략: 단위 테스트 (70%+), 통합 테스트, 성능 테스트

### Rationale
- **Constitution V 준수**: 단위 테스트 ≥70% 커버리지, 통합 테스트 필수
- **품질 보장**: 리팩토링 시 안전망 제공
- **성능 검증**: 500ms 응답 시간 요구사항 검증

### Test Types

#### 1. 단위 테스트 (Unit Tests)
- **대상**: Service, Repository (Custom Query), Validator
- **도구**: JUnit 5, Mockito, Reactor Test
- **커버리지 목표**: ≥70%

```kotlin
@Test
fun `should filter alerts by date range`() {
    // Given
    val criteria = AlertSearchCriteria(
        startDate = Instant.parse("2025-11-01T00:00:00Z"),
        endDate = Instant.parse("2025-11-10T23:59:59Z")
    )
    val alerts = listOf(
        createAlert(timestamp = Instant.parse("2025-11-05T12:00:00Z")),
        createAlert(timestamp = Instant.parse("2025-11-15T12:00:00Z"))
    )
    `when`(alertRepository.findByCriteria(criteria, pageable)).thenReturn(Flux.fromIterable(alerts))

    // When
    val result = alertHistoryService.searchAlerts(criteria, 0, 50).block()

    // Then
    assertThat(result.content).hasSize(1)
    assertThat(result.content[0].alertTimestamp).isEqualTo(Instant.parse("2025-11-05T12:00:00Z"))
}
```

#### 2. 통합 테스트 (Integration Tests)
- **대상**: API Endpoint + Database
- **도구**: Spring Boot Test, Testcontainers (PostgreSQL), WebTestClient
- **목표**: 전체 요청-응답 흐름 검증

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AlertHistoryIntegrationTest {

    @Container
    val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine")

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `should return paginated alerts with filters`() {
        // Given
        insertTestAlerts(100)

        // When
        val response = webTestClient.get()
            .uri("/api/alerts/history?ruleName=HIGH_AMOUNT&page=0&size=50")
            .exchange()

        // Then
        response.expectStatus().isOk
            .expectBody()
            .jsonPath("$.content").isArray
            .jsonPath("$.totalElements").isEqualTo(50)
            .jsonPath("$.totalPages").isEqualTo(1)
    }
}
```

#### 3. 성능 테스트 (Performance Tests)
- **대상**: 검색 응답 시간
- **도구**: JMeter 또는 K6
- **목표**: 10,000개 알림에서 500ms 이내 응답

```javascript
// k6 script
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  vus: 10, // 10명의 동시 사용자
  duration: '30s',
};

export default function() {
  let response = http.get('http://localhost:8080/api/alerts/history?page=0&size=50');
  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });
}
```

---

## 11. Docker Compose 통합

### Decision
PostgreSQL 컨테이너를 docker-compose.yml에 추가하고, alert-dashboard 서비스와 연결합니다.

### Rationale
- **Constitution II 준수**: 단일 명령어 배포 (`docker-compose up`)
- **로컬 개발 편의성**: 개발자가 PostgreSQL을 별도로 설치할 필요 없음
- **환경 일관성**: 모든 개발자가 동일한 PostgreSQL 버전 사용

### Docker Compose Configuration
```yaml
services:
  postgres:
    image: postgres:15-alpine
    container_name: realfds-postgres
    environment:
      POSTGRES_DB: realfds
      POSTGRES_USER: realfds_user
      POSTGRES_PASSWORD: realfds_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - realfds-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U realfds_user -d realfds"]
      interval: 10s
      timeout: 5s
      retries: 5

  alert-dashboard:
    # ... existing configuration ...
    environment:
      # ... existing environment variables ...
      SPRING_R2DBC_URL: r2dbc:postgresql://postgres:5432/realfds
      SPRING_R2DBC_USERNAME: realfds_user
      SPRING_R2DBC_PASSWORD: realfds_password
      SPRING_FLYWAY_URL: jdbc:postgresql://postgres:5432/realfds
      SPRING_FLYWAY_USER: realfds_user
      SPRING_FLYWAY_PASSWORD: realfds_password
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - realfds-network

volumes:
  postgres_data:

networks:
  realfds-network:
    driver: bridge
```

### Health Check Strategy
- PostgreSQL 컨테이너가 완전히 시작될 때까지 alert-dashboard 대기
- `pg_isready` 명령어로 PostgreSQL 상태 확인
- 최대 5번 재시도, 10초 간격

---

## 12. 기존 시스템 통합

### Decision
alert-service 내부에 알림 저장 로직을 추가하여, 알림 생성 시 PostgreSQL에 자동으로 저장합니다.

### Rationale
- **Constitution IV 준수**: 마이크로서비스 경계 존중 (RAD 서비스 내부 확장)
- **단순함**: 별도의 마이크로서비스 추가 없이 기존 서비스 확장
- **데이터 일관성**: 알림 생성과 저장이 동일한 서비스 내에서 처리

### Integration Points
```kotlin
// AlertService.kt (alert-dashboard/backend)
@Service
class AlertService(
    private val alertRepository: AlertRepository,
    private val kafkaAlertConsumer: KafkaAlertConsumer
) {

    fun processAlert(alertEvent: AlertEvent): Mono<Void> {
        return alertRepository.save(alertEvent.toAlert())
            .doOnSuccess { logger.info("Alert saved: alertId=${it.alertId}") }
            .doOnError { logger.error("Failed to save alert", it) }
            .then()
    }
}
```

### Data Flow
```
Kafka Topic (transaction-alerts)
    → RAD Backend (AlertService)
        → AlertRepository.save()
            → PostgreSQL (alerts table)
        → WebSocket (실시간 알림)
            → Frontend
```

### Backward Compatibility
- 기존 실시간 알림 기능은 유지 (WebSocket)
- PostgreSQL 저장은 추가 기능으로, 실시간 알림에 영향 없음
- 데이터베이스 저장 실패 시에도 실시간 알림은 정상 동작

---

## 13. 환경 변수 및 설정

### Decision
모든 데이터베이스 연결 정보를 환경 변수로 외부화하고, 합리적인 기본값을 제공합니다.

### Rationale
- **Constitution 준수**: 설정 관리 원칙 (소스 코드에 하드코딩 금지)
- **보안**: 민감한 정보(비밀번호)를 코드에 포함하지 않음
- **유연성**: 환경별로 다른 설정 사용 가능 (개발/테스트/프로덕션)

### Environment Variables
| 변수명 | 기본값 | 설명 |
|-------|-------|-----|
| `SPRING_R2DBC_URL` | `r2dbc:postgresql://postgres:5432/realfds` | R2DBC 데이터베이스 URL |
| `SPRING_R2DBC_USERNAME` | `realfds_user` | 데이터베이스 사용자명 |
| `SPRING_R2DBC_PASSWORD` | `realfds_password` | 데이터베이스 비밀번호 |
| `SPRING_FLYWAY_URL` | `jdbc:postgresql://postgres:5432/realfds` | Flyway 마이그레이션 URL |
| `SPRING_FLYWAY_USER` | `realfds_user` | Flyway 사용자명 |
| `SPRING_FLYWAY_PASSWORD` | `realfds_password` | Flyway 비밀번호 |
| `ALERT_HISTORY_DEFAULT_PAGE_SIZE` | `50` | 기본 페이지 크기 |
| `ALERT_HISTORY_MAX_PAGE_SIZE` | `100` | 최대 페이지 크기 |
| `ALERT_HISTORY_DEFAULT_DATE_RANGE_DAYS` | `7` | 기본 날짜 범위 (일) |

### Application Configuration (application.yml)
```yaml
spring:
  r2dbc:
    url: ${SPRING_R2DBC_URL:r2dbc:postgresql://postgres:5432/realfds}
    username: ${SPRING_R2DBC_USERNAME:realfds_user}
    password: ${SPRING_R2DBC_PASSWORD:realfds_password}
  flyway:
    url: ${SPRING_FLYWAY_URL:jdbc:postgresql://postgres:5432/realfds}
    user: ${SPRING_FLYWAY_USER:realfds_user}
    password: ${SPRING_FLYWAY_PASSWORD:realfds_password}
    baseline-on-migrate: true
    locations: classpath:db/migration

alert-history:
  default-page-size: ${ALERT_HISTORY_DEFAULT_PAGE_SIZE:50}
  max-page-size: ${ALERT_HISTORY_MAX_PAGE_SIZE:100}
  default-date-range-days: ${ALERT_HISTORY_DEFAULT_DATE_RANGE_DAYS:7}
```

---

## 14. Observability & Monitoring

### Decision
Spring Boot Actuator와 구조화된 로깅을 사용하여 시스템 상태를 모니터링합니다.

### Rationale
- **Constitution I 준수**: 포괄적인 로깅 포함 (학습 및 디버깅 목적)
- **헬스 체크**: 데이터베이스 연결 상태 확인
- **메트릭 수집**: 조회 횟수, 응답 시간, 에러율 추적

### Actuator Endpoints
- `/actuator/health`: 전체 시스템 헬스 체크
- `/actuator/health/readiness`: Kubernetes 준비성 프로브 (향후)
- `/actuator/health/liveness`: Kubernetes 활성 프로브 (향후)
- `/actuator/metrics`: Prometheus 형식 메트릭

### Custom Metrics
```kotlin
@Component
class AlertHistoryMetrics(
    private val meterRegistry: MeterRegistry
) {
    private val searchCounter = meterRegistry.counter("alert.history.search.count")
    private val searchTimer = meterRegistry.timer("alert.history.search.duration")

    fun recordSearch(duration: Duration) {
        searchCounter.increment()
        searchTimer.record(duration)
    }
}
```

### Logging Guidelines
- **알림 조회 시작**: `INFO` - "Searching alerts: criteria={...}"
- **알림 조회 완료**: `INFO` - "Found {count} alerts in {duration}ms"
- **검색 결과 없음**: `WARN` - "No alerts found for criteria: {criteria}"
- **데이터베이스 오류**: `ERROR` - "Failed to search alerts: {error}"

---

## Risks & Mitigation

| 위험 | 영향 | 완화 전략 |
|-----|-----|---------|
| PostgreSQL 초기화 지연으로 alert-dashboard 시작 실패 | HIGH | depends_on + healthcheck 설정으로 PostgreSQL 준비 대기 |
| R2DBC 학습 곡선 (팀원이 익숙하지 않음) | MEDIUM | Spring Data JPA와 유사한 Repository 패턴 사용, 예제 코드 제공 |
| 대량 데이터 조회 시 성능 저하 | MEDIUM | 인덱스 최적화, 페이지네이션, 성능 테스트로 검증 |
| Flyway 마이그레이션 실패 시 서비스 시작 불가 | HIGH | `baseline-on-migrate: true` 설정, 마이그레이션 스크립트 검증 |
| 동시 접속 사용자 증가 시 데이터베이스 커넥션 부족 | LOW | R2DBC 커넥션 풀 크기 조정 (현재 범위: 최대 50명) |

---

## Open Questions & Future Work

### Resolved Questions
- ✅ 데이터베이스 선택: PostgreSQL 15
- ✅ 비동기 액세스 방법: Spring Data R2DBC
- ✅ 마이그레이션 도구: Flyway
- ✅ 페이지네이션 구현: Spring Data Pageable
- ✅ 검색 성능 최적화: B-Tree 인덱스

### Future Work (Out of Scope)
- 알림 데이터 아카이빙/삭제 정책 (별도 feature)
- CSV 내보내기 기능 (005-alert-analytics에서 처리)
- 전체 텍스트 검색 (현재 범위 제외)
- Keyset Pagination (Cursor-based) 도입 (100,000개 이상 규모 시)
- 복합 인덱스 최적화 (100,000개 이상 규모 시)

---

## References

- [Spring Data R2DBC Documentation](https://docs.spring.io/spring-data/r2dbc/reference/)
- [PostgreSQL Indexing Best Practices](https://www.postgresql.org/docs/15/indexes-types.html)
- [Flyway Documentation](https://documentation.red-gate.com/fd)
- [Reactor Core Reference Guide](https://projectreactor.io/docs/core/release/reference/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

**Research Status**: ✅ Completed
**Author**: Project Team
**Last Updated**: 2025-11-11
