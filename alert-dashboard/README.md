# Alert Dashboard

실시간 알림 대시보드 서비스 - RealFDS 시스템의 실시간 모니터링 및 알림 관리를 위한 웹 애플리케이션

## 목차

- [개요](#개요)
- [주요 기능](#주요-기능)
- [아키텍처](#아키텍처)
- [빠른 시작](#빠른-시작)
- [환경 변수](#환경-변수)
- [API 문서](#api-문서)
- [개발 가이드](#개발-가이드)
- [문제 해결](#문제-해결)

## 개요

Alert Dashboard는 RealFDS 사기 탐지 시스템의 실시간 모니터링 및 알림 관리를 위한 웹 애플리케이션입니다. Spring Boot 기반 백엔드와 React 기반 프론트엔드로 구성되어 있으며, 다음 기능들을 제공합니다:

- 📊 **실시간 시스템 대시보드**: 5개 마이크로서비스의 상태, TPS, 알림 발생률을 실시간으로 모니터링
- 📜 **알림 이력 조회**: 과거 알림 데이터를 날짜, 규칙, 위험도별로 필터링 및 검색
- 📈 **실시간 차트**: WebSocket을 통한 5초 주기 메트릭 업데이트

## 주요 기능

### 1. 실시간 시스템 대시보드 (Feature 004)

5개 마이크로서비스의 Health Check 상태와 성능 지표를 실시간으로 시각화합니다.

**핵심 기능:**
- ✅ **서비스 상태 모니터링**: 5개 서비스(transaction-generator, fraud-detector, alert-service, websocket-gateway, alert-dashboard)의 UP/DOWN 상태 실시간 표시
- 📊 **TPS 차트**: 초당 거래 처리량을 시계열 차트로 표시 (최근 1시간)
- 🚨 **알림 발생률 차트**: 분당 알림 발생 수를 규칙별(HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY)로 스택 차트 표시
- 🔄 **자동 재연결**: WebSocket 연결 끊김 시 Exponential Backoff 전략으로 자동 재연결
- 📦 **백필 지원**: 재연결 또는 탭 활성화 시 누락된 데이터 자동 복구

**접속 URL:**
```
http://localhost:8084/dashboard
```

**기술 스택:**
- **백엔드**: Spring Boot 3.2+, Spring WebSocket, Spring WebClient
- **프론트엔드**: React 18+, TypeScript 5+, Recharts 2.x
- **실시간 통신**: WebSocket (5초 주기 브로드캐스트)
- **데이터 저장**: In-memory Circular Buffer (최근 1시간 데이터)

### 2. 알림 이력 조회 (Feature 003)

과거 발생한 알림 데이터를 다양한 조건으로 조회하고 분석합니다.

**핵심 기능:**
- 📅 **날짜 범위 필터**: 시작일/종료일 기반 알림 검색
- 🏷️ **규칙 필터**: HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY 규칙별 필터링
- ⚠️ **위험도 필터**: LOW, MEDIUM, HIGH, CRITICAL 위험도별 필터링
- 📄 **페이지네이션**: 50개 단위 페이지 네비게이션 (최대 100개)
- 🔍 **정렬**: 발생 시각, 위험도, 규칙 기준 정렬
- 📊 **상세 정보**: 알림 상세 정보 모달 표시

**API 엔드포인트:**
```
GET /api/v1/alerts?startDate={startDate}&endDate={endDate}&rule={rule}&severity={severity}&page={page}&size={size}
```

## 아키텍처

```
┌────────────────────────────────────────────────────────────────┐
│                    Alert Dashboard (RAD)                       │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────┐         ┌─────────────────────────┐  │
│  │   Backend (8084)    │         │   Frontend (React)      │  │
│  │                     │         │                         │  │
│  │  - Spring Boot      │◄────────┤  - TypeScript           │  │
│  │  - WebSocket        │ WebSocket│  - Recharts            │  │
│  │  - WebClient        │         │  - useWebSocket Hook   │  │
│  │  - R2DBC            │         │                         │  │
│  │  - Kafka Consumer   │         │                         │  │
│  └─────────────────────┘         └─────────────────────────┘  │
│          │                                                      │
└──────────┼──────────────────────────────────────────────────────┘
           │
           ├─────────► PostgreSQL (알림 이력 저장)
           ├─────────► Kafka (transaction-alerts 토픽 구독)
           └─────────► 5개 마이크로서비스 Health Check
                       (transaction-generator, fraud-detector,
                        alert-service, websocket-gateway, alert-dashboard)
```

## 빠른 시작

### 전제 조건

- Docker & Docker Compose
- JDK 17+
- Node.js 18+
- PostgreSQL 15+ (docker-compose 자동 실행)
- Kafka 3.6+ (docker-compose 자동 실행)

### 1. Docker Compose로 전체 시스템 실행

```bash
# 프로젝트 루트 디렉터리에서
docker-compose up -d
```

이 명령어는 다음 서비스들을 모두 실행합니다:
- PostgreSQL
- Kafka + Zookeeper
- Transaction Generator (TGS)
- Fraud Detector (RDE)
- Alert Service (ALS)
- WebSocket Gateway (WSG)
- **Alert Dashboard (RAD)** ← 이 서비스

### 2. 로컬 개발 모드 실행

#### 백엔드 실행

```bash
cd alert-dashboard/backend

# 의존성 설치 및 빌드
./gradlew clean build

# 개발 모드 실행 (dev 프로파일)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

백엔드 서버: `http://localhost:8084`

#### 프론트엔드 실행

```bash
cd alert-dashboard/frontend

# 의존성 설치
npm install

# 개발 서버 실행
npm start
```

프론트엔드 개발 서버: `http://localhost:3000`

### 3. 대시보드 접속

```
http://localhost:8084/dashboard
```

## 환경 변수

### 백엔드 환경 변수

`alert-dashboard/backend/src/main/resources/application.yml` 파일 또는 환경 변수로 설정:

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `SERVER_PORT` | `8084` | 서버 포트 |
| `SPRING_R2DBC_URL` | `r2dbc:postgresql://localhost:5432/realfds` | PostgreSQL R2DBC URL |
| `SPRING_R2DBC_USERNAME` | `realfds_user` | DB 사용자명 |
| `SPRING_R2DBC_PASSWORD` | `realfds_password` | DB 비밀번호 |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka 브로커 URL |
| `METRICS_COLLECTION_INTERVAL_MS` | `5000` | 메트릭 수집 간격 (밀리초) |
| `METRICS_COLLECTION_TIMEOUT_MS` | `3000` | Health Check 타임아웃 (밀리초) |
| `METRICS_RETENTION_HOURS` | `1` | 메트릭 데이터 보관 시간 |
| `SERVICE_URL_TGS` | `http://transaction-generator:8080` | Transaction Generator URL |
| `SERVICE_URL_FDE` | `http://fraud-detector:8081` | Fraud Detector URL |
| `SERVICE_URL_ALS` | `http://alert-service:8082` | Alert Service URL |
| `SERVICE_URL_WSG` | `http://websocket-gateway:8083` | WebSocket Gateway URL |
| `SERVICE_URL_RAD` | `http://alert-dashboard:8084` | Alert Dashboard URL (자기 자신) |

### 프론트엔드 환경 변수

`alert-dashboard/frontend/.env` 파일:

```env
REACT_APP_API_BASE_URL=http://localhost:8084
REACT_APP_WS_URL=ws://localhost:8084/ws/metrics
```

## API 문서

### Health Check

```
GET /actuator/health
```

**응답 예시:**
```json
{
  "status": "UP",
  "components": {
    "webSocket": {
      "status": "UP",
      "details": {
        "activeConnections": 3,
        "maxConnections": 5,
        "metricsDataPoints": 120,
        "maxDataPoints": 720
      }
    },
    "db": {
      "status": "UP"
    }
  }
}
```

### Alert History API

#### 알림 목록 조회

```
GET /api/v1/alerts?startDate=2025-01-01T00:00:00Z&endDate=2025-01-31T23:59:59Z&page=0&size=50
```

**쿼리 파라미터:**
- `startDate`: 시작 날짜 (ISO-8601, optional)
- `endDate`: 종료 날짜 (ISO-8601, optional)
- `rule`: 규칙 필터 (HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY, optional)
- `severity`: 위험도 필터 (LOW, MEDIUM, HIGH, CRITICAL, optional)
- `page`: 페이지 번호 (0부터 시작, 기본값: 0)
- `size`: 페이지 크기 (1-100, 기본값: 50)

**응답 예시:**
```json
{
  "alerts": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "ruleName": "HIGH_VALUE",
      "severity": "HIGH",
      "message": "고액 거래 탐지: ₩15,000,000",
      "transactionId": "txn-001",
      "userId": "user-123",
      "occurredAt": "2025-01-15T10:30:00Z",
      "metadata": {}
    }
  ],
  "pagination": {
    "page": 0,
    "size": 50,
    "totalElements": 1234,
    "totalPages": 25
  }
}
```

### WebSocket API

#### 연결

```
ws://localhost:8084/ws/metrics
```

#### 메시지 타입

**METRICS_UPDATE** (Server → Client, 5초마다):
```json
{
  "type": "METRICS_UPDATE",
  "timestamp": "2025-01-15T10:30:05Z",
  "payload": {
    "timestamp": "2025-01-15T10:30:05Z",
    "tps": 87,
    "totalTransactions": 5040000,
    "alertsPerMinute": 12,
    "byRule": {
      "HIGH_VALUE": 5,
      "FOREIGN_COUNTRY": 4,
      "HIGH_FREQUENCY": 3
    }
  }
}
```

**BACKFILL_REQUEST** (Client → Server):
```json
{
  "type": "BACKFILL_REQUEST",
  "timestamp": "2025-01-15T10:35:00Z",
  "lastReceivedTimestamp": "2025-01-15T10:25:00Z"
}
```

**BACKFILL_RESPONSE** (Server → Client):
```json
{
  "type": "BACKFILL_RESPONSE",
  "timestamp": "2025-01-15T10:35:00Z",
  "payload": [
    {
      "timestamp": "2025-01-15T10:25:05Z",
      "tps": 65,
      "totalTransactions": 4950000,
      "alertsPerMinute": 8,
      "byRule": {
        "HIGH_VALUE": 3,
        "FOREIGN_COUNTRY": 3,
        "HIGH_FREQUENCY": 2
      }
    }
  ]
}
```

## 개발 가이드

### 백엔드 개발

#### 테스트 실행

```bash
cd alert-dashboard/backend

# 모든 테스트 실행
./gradlew test

# 커버리지 리포트 생성 (Jacoco)
./gradlew jacocoTestReport

# 리포트 확인
open build/reports/jacoco/test/html/index.html
```

#### 코드 스타일

- **함수 길이**: 최대 50줄 (Constitution V)
- **파일 길이**: 최대 300줄 (Constitution V)
- **주석**: 한국어 (Constitution VI)
- **커밋 메시지**: Conventional Commits + 한국어 (Constitution VI)

### 프론트엔드 개발

#### 테스트 실행

```bash
cd alert-dashboard/frontend

# 모든 테스트 실행
npm test

# 커버리지 리포트 생성
npm test -- --coverage

# 리포트 확인
open coverage/lcov-report/index.html
```

#### 빌드

```bash
# 프로덕션 빌드
npm run build

# 빌드 결과물
ls -la build/
```

## 문제 해결

### 1. WebSocket 연결 실패

**증상**: 대시보드에 "연결 끊김" 배너 표시

**해결책**:
```bash
# 백엔드 서버 상태 확인
curl http://localhost:8084/actuator/health

# 로그 확인
docker logs alert-dashboard

# WebSocket 엔드포인트 확인
wscat -c ws://localhost:8084/ws/metrics
```

### 2. 서비스 DOWN 상태

**증상**: 대시보드에 서비스가 DOWN으로 표시

**해결책**:
```bash
# 해당 서비스 Health Check 직접 확인
curl http://transaction-generator:8080/actuator/health
curl http://fraud-detector:8081/actuator/health
curl http://alert-service:8082/actuator/health
curl http://websocket-gateway:8083/actuator/health

# 서비스 로그 확인
docker logs <service-name>

# 서비스 재시작
docker-compose restart <service-name>
```

### 3. 차트 데이터 없음

**증상**: 차트에 "데이터 수집 중..." 메시지만 표시

**원인 및 해결책**:
- **Kafka 연결 실패**: Kafka 브로커 상태 확인
  ```bash
  docker logs kafka
  docker-compose ps kafka
  ```
- **메트릭 수집 스케줄러 미작동**: 백엔드 로그 확인
  ```bash
  docker logs alert-dashboard | grep "Health Check 수집"
  ```
- **시스템 시작 직후**: 최소 5초 대기 후 첫 번째 메트릭 수집

### 4. 데이터베이스 마이그레이션 실패

**증상**: 백엔드 시작 시 Flyway 마이그레이션 오류

**해결책**:
```bash
# PostgreSQL 연결 확인
docker exec -it postgres psql -U realfds_user -d realfds

# 마이그레이션 히스토리 확인
SELECT * FROM flyway_schema_history;

# 마이그레이션 재실행 (개발 환경)
./gradlew flywayClean flywayMigrate
```

### 5. 메모리 부족

**증상**: 대시보드가 느려지거나 응답하지 않음

**해결책**:
```bash
# JVM 메모리 사용량 확인
curl http://localhost:8084/actuator/metrics/jvm.memory.used

# Docker 컨테이너 메모리 제한 확인
docker stats alert-dashboard

# 메트릭 데이터 포인트 수 확인 (최대 720개)
curl http://localhost:8084/actuator/health | jq '.components.webSocket.details.metricsDataPoints'
```

### 6. 연결 타임아웃

**증상**: Health Check 수집 시 서비스가 DOWN으로 표시 (errorType: TIMEOUT)

**해결책**:
- **타임아웃 설정 증가**: `application.yml`에서 `METRICS_COLLECTION_TIMEOUT_MS` 값을 3000ms에서 5000ms로 증가
- **네트워크 지연 확인**: 서비스 간 네트워크 연결 상태 확인
- **서비스 성능 개선**: 해당 서비스의 응답 시간 최적화

## 라이선스

이 프로젝트는 학습 목적으로 제작되었습니다.

## 기여

기여를 환영합니다! 이슈나 풀 리퀘스트를 통해 참여해주세요.

## 관련 문서

- [Feature Specification (004-dashboard-realtime)](../specs/004-dashboard-realtime/spec.md)
- [Implementation Plan](../specs/004-dashboard-realtime/plan.md)
- [API Contracts](../specs/004-dashboard-realtime/contracts/)
- [Quickstart Guide](../specs/004-dashboard-realtime/quickstart.md)
- [Constitution](.specify/memory/constitution.md)
