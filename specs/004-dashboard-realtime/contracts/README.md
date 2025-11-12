# API Contracts Overview
# 004-dashboard-realtime API 계약서 개요

**Feature**: 실시간 시스템 대시보드
**Version**: 1.0.0
**Last Updated**: 2025-11-12

---

## 목차 (Table of Contents)

1. [개요 (Overview)](#개요-overview)
2. [파일 구조 (File Structure)](#파일-구조-file-structure)
3. [REST API 계약 (REST API Contract)](#rest-api-계약-rest-api-contract)
4. [WebSocket 계약 (WebSocket Contract)](#websocket-계약-websocket-contract)
5. [메시지 스키마 (Message Schema)](#메시지-스키마-message-schema)
6. [사용 방법 (How to Use)](#사용-방법-how-to-use)
7. [검증 도구 (Validation Tools)](#검증-도구-validation-tools)
8. [변경 이력 (Change Log)](#변경-이력-change-log)

---

## 개요 (Overview)

본 디렉토리는 실시간 시스템 대시보드 기능(004-dashboard-realtime)의 모든 API 계약서를 포함합니다. 각 계약서는 백엔드와 프론트엔드 간 통신 규격을 명확히 정의하며, 구현 시 기준 문서로 사용됩니다.

**계약서의 역할**:
- **설계 단계**: 백엔드와 프론트엔드 개발자 간 합의 문서
- **구현 단계**: 코드 생성 및 검증의 기준
- **테스트 단계**: API 테스트 케이스 작성 근거
- **유지보수 단계**: API 변경 시 영향 범위 파악

**주요 특징**:
- OpenAPI 3.0 표준 사용 (REST API)
- JSON Schema 사용 (WebSocket 메시지)
- 한국어 설명 포함 (Constitution VI)
- 실제 예시 포함 (Examples)

---

## 파일 구조 (File Structure)

```
contracts/
├── README.md                      (이 파일) - 전체 개요
├── rest-api.yaml                  OpenAPI 3.0 명세 (REST 엔드포인트)
├── websocket-protocol.md          WebSocket 프로토콜 상세 명세
└── websocket-messages.json        JSON Schema (WebSocket 메시지)
```

### 각 파일 설명

| 파일명 | 형식 | 목적 | 주요 내용 |
|--------|------|------|-----------|
| `rest-api.yaml` | OpenAPI 3.0 YAML | REST API 엔드포인트 정의 | 헬스 체크, 서비스 목록, 초기 메트릭 조회 |
| `websocket-protocol.md` | Markdown | WebSocket 프로토콜 상세 명세 | 연결 절차, 메시지 타입, 재연결 전략, 에러 처리 |
| `websocket-messages.json` | JSON Schema | WebSocket 메시지 형식 정의 | METRICS_UPDATE, BACKFILL_REQUEST, BACKFILL_RESPONSE, ERROR |
| `README.md` | Markdown | 계약서 전체 개요 및 사용 가이드 | 이 문서 |

---

## REST API 계약 (REST API Contract)

### 파일: `rest-api.yaml`

**형식**: OpenAPI 3.0
**도구**: Swagger Editor, Postman, Insomnia

### 엔드포인트 목록

#### 1. `GET /actuator/health`

**목적**: 대시보드 서비스 자체의 헬스 체크
**응답**: Spring Boot Actuator 표준 형식

**예시**:
```bash
curl http://localhost:8083/actuator/health
```

**응답 (200 OK)**:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 189474095104,
        "threshold": 10485760,
        "exists": true
      }
    },
    "memory": {
      "status": "UP",
      "details": {
        "total": 4294967296,
        "used": 2147483648,
        "free": 2147483648
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

---

#### 2. `GET /api/v1/services`

**목적**: 모니터링 대상 5개 서비스 목록 조회
**응답**: 서비스 이름, URL, 설명 등 정적 정보

**예시**:
```bash
curl http://localhost:8083/api/v1/services
```

**응답 (200 OK)**:
```json
{
  "services": [
    {
      "serviceName": "transaction-generator",
      "displayName": "거래 생성기",
      "description": "가상 거래 데이터를 생성하여 Kafka로 전송",
      "healthCheckUrl": "http://transaction-generator:8080/actuator/health",
      "port": 8080
    },
    {
      "serviceName": "fraud-detector",
      "displayName": "사기 탐지기",
      "description": "거래 데이터를 분석하여 사기 패턴 탐지",
      "healthCheckUrl": "http://fraud-detector:8081/actuator/health",
      "port": 8081
    }
    // ... 나머지 3개 서비스
  ],
  "totalCount": 5
}
```

---

#### 3. `GET /api/v1/metrics/current` (선택적)

**목적**: 초기 로딩 시 현재 메트릭 스냅샷 조회
**주의**: WebSocket 연결 후 첫 METRICS_UPDATE 메시지로 대체 가능

**예시**:
```bash
curl http://localhost:8083/api/v1/metrics/current
```

**응답 (200 OK)**:
```json
{
  "timestamp": "2025-11-12T10:30:05Z",
  "services": [
    {
      "serviceName": "transaction-generator",
      "status": "UP",
      "lastChecked": "2025-11-12T10:30:05Z",
      "responseTime": 45,
      "memoryUsage": 128,
      "errorType": null,
      "errorMessage": null
    }
    // ... 나머지 4개 서비스
  ],
  "tps": {
    "current": 87,
    "average": 65,
    "max": 150
  },
  "alerts": {
    "current": 12,
    "average": 8,
    "max": 25,
    "byRule": {
      "HIGH_VALUE": 5,
      "FOREIGN_COUNTRY": 4,
      "HIGH_FREQUENCY": 3
    }
  }
}
```

---

### 사용 방법

1. **Swagger UI로 문서 보기**:
   ```bash
   npx @redocly/openapi-cli preview-docs rest-api.yaml
   ```
   - 브라우저에서 `http://localhost:8080` 접속

2. **Postman으로 임포트**:
   - Postman 실행 → Import → File → `rest-api.yaml` 선택
   - 자동으로 모든 엔드포인트와 예시 생성

3. **코드 생성 (OpenAPI Generator)**:
   ```bash
   # TypeScript 클라이언트 생성
   npx @openapitools/openapi-generator-cli generate \
     -i rest-api.yaml \
     -g typescript-axios \
     -o ./src/api/generated

   # Java Spring 서버 스텁 생성
   npx @openapitools/openapi-generator-cli generate \
     -i rest-api.yaml \
     -g spring \
     -o ./src/main/java/generated
   ```

---

## WebSocket 계약 (WebSocket Contract)

### 파일: `websocket-protocol.md`

**형식**: Markdown
**도구**: 텍스트 에디터, Markdown Viewer

### 주요 섹션

1. **연결 엔드포인트**: `ws://localhost:8083/ws/metrics`
2. **핸드셰이크**: HTTP Upgrade 요청 및 응답 형식
3. **메시지 타입**:
   - `METRICS_UPDATE` (서버 → 클라이언트, 5초마다)
   - `BACKFILL_REQUEST` (클라이언트 → 서버, 재연결 시)
   - `BACKFILL_RESPONSE` (서버 → 클라이언트, 누락 데이터)
   - `ERROR` (서버 → 클라이언트, 오류 발생 시)
4. **연결 수명 주기**:
   - 초기 연결
   - 정상 작동
   - 연결 끊김
   - 재연결 (Exponential Backoff)
   - 연결 종료
5. **Heartbeat / Ping-Pong**: 30초마다 Ping, 자동 Pong 응답
6. **에러 처리**: 에러 코드, 처리 방법, 복구 전략

### 연결 시퀀스 다이어그램

```
클라이언트                           서버
   |                                  |
   |------ HTTP Upgrade Request ----->|
   |       (GET /ws/metrics)          |
   |                                  |
   |<----- 101 Switching Protocols ---|
   |                                  |
   |===== WebSocket 연결 수립 =======|
   |                                  |
   |<---- METRICS_UPDATE (5초마다) ---|
   |<---- METRICS_UPDATE -------------|
   |<---- METRICS_UPDATE -------------|
   |                                  |
   |--- (연결 끊김) ------------------|
   |                                  |
   |------ 재연결 시도 -------------->|
   |===== WebSocket 연결 재수립 =====|
   |                                  |
   |------ BACKFILL_REQUEST --------->|
   |    (lastReceivedTimestamp)       |
   |                                  |
   |<----- BACKFILL_RESPONSE ---------|
   |    (누락된 데이터)               |
   |                                  |
   |<---- METRICS_UPDATE (정상 재개) -|
```

---

## 메시지 스키마 (Message Schema)

### 파일: `websocket-messages.json`

**형식**: JSON Schema (Draft 7)
**도구**: JSON Schema Validator, AJV

### 정의된 메시지 타입

#### 1. MetricsUpdateMessage

**방향**: 서버 → 클라이언트
**주기**: 5초마다

**스키마 경로**: `#/$defs/MetricsUpdateMessage`

**필수 필드**:
- `type`: "METRICS_UPDATE" (상수)
- `timestamp`: ISO-8601 날짜 문자열
- `data`: 객체
  - `services`: 배열 (5개, ServiceHealth)
  - `tps`: 객체 (TpsAggregated)
  - `alerts`: 객체 (AlertsAggregated)

**예시**:
```json
{
  "type": "METRICS_UPDATE",
  "timestamp": "2025-11-12T10:30:05Z",
  "data": {
    "services": [ /* 5개 서비스 */ ],
    "tps": { "current": 87, "average": 65, "max": 150, "history": [...] },
    "alerts": { "current": 12, "average": 8, "max": 25, "byRule": {...}, "history": [...] }
  }
}
```

---

#### 2. BackfillRequestMessage

**방향**: 클라이언트 → 서버
**시점**: 재연결 직후

**스키마 경로**: `#/$defs/BackfillRequestMessage`

**필수 필드**:
- `type`: "BACKFILL_REQUEST" (상수)
- `lastReceivedTimestamp`: ISO-8601 날짜 문자열

**예시**:
```json
{
  "type": "BACKFILL_REQUEST",
  "lastReceivedTimestamp": "2025-11-12T10:25:00Z"
}
```

---

#### 3. BackfillResponseMessage

**방향**: 서버 → 클라이언트
**시점**: BACKFILL_REQUEST 수신 후

**스키마 경로**: `#/$defs/BackfillResponseMessage`

**필수 필드**:
- `type`: "BACKFILL_RESPONSE" (상수)
- `data`: 배열 (최대 720개, 시간 순서)

**예시**:
```json
{
  "type": "BACKFILL_RESPONSE",
  "data": [
    {
      "timestamp": "2025-11-12T10:25:05Z",
      "tps": 65,
      "totalTransactions": 234000,
      "alertsPerMinute": 8,
      "byRule": { "HIGH_VALUE": 3, "FOREIGN_COUNTRY": 3, "HIGH_FREQUENCY": 2 }
    }
    // ... 누락된 데이터 포인트들
  ]
}
```

---

#### 4. ErrorMessage

**방향**: 서버 → 클라이언트
**시점**: 오류 발생 시

**스키마 경로**: `#/$defs/ErrorMessage`

**필수 필드**:
- `type`: "ERROR" (상수)
- `timestamp`: ISO-8601 날짜 문자열
- `error`: 객체 (ErrorDetails)
  - `code`: 에러 코드 (enum)
  - `message`: 에러 메시지 (한글)
  - `details`: 상세 정보 (선택적)

**에러 코드**:
- `INVALID_MESSAGE_FORMAT`: 메시지 형식 오류
- `BACKFILL_TOO_OLD`: 백필 요청 시각이 1시간 이상 이전
- `INTERNAL_SERVER_ERROR`: 서버 내부 오류
- `SERVICE_UNAVAILABLE`: 서비스 일시 중단

**예시**:
```json
{
  "type": "ERROR",
  "timestamp": "2025-11-12T10:30:05Z",
  "error": {
    "code": "BACKFILL_TOO_OLD",
    "message": "백필 요청 시각이 1시간 이상 이전입니다. 페이지를 새로고침하세요."
  }
}
```

---

## 사용 방법 (How to Use)

### 1. 계약서 읽기 및 이해

**백엔드 개발자**:
1. `rest-api.yaml` 읽기 → Spring Boot 컨트롤러 구조 파악
2. `websocket-protocol.md` 읽기 → WebSocket 핸들러 설계
3. `websocket-messages.json` 읽기 → DTO 클래스 정의

**프론트엔드 개발자**:
1. `rest-api.yaml` 읽기 → HTTP 클라이언트 (Axios) 설정
2. `websocket-protocol.md` 읽기 → WebSocket 훅(useWebSocket) 구현
3. `websocket-messages.json` 읽기 → TypeScript 인터페이스 정의

---

### 2. 코드 생성 (자동화)

#### TypeScript 타입 생성 (WebSocket 메시지)

```bash
# json-schema-to-typescript 설치
npm install -g json-schema-to-typescript

# TypeScript 인터페이스 생성
json2ts websocket-messages.json > ../src/types/websocket.ts
```

**생성된 TypeScript 타입 예시**:
```typescript
export interface MetricsUpdateMessage {
  type: "METRICS_UPDATE";
  timestamp: string;
  data: {
    services: ServiceHealth[];
    tps: TpsAggregated;
    alerts: AlertsAggregated;
  };
}

export interface ServiceHealth {
  serviceName: "transaction-generator" | "fraud-detector" | "alert-service" | "websocket-gateway" | "frontend-dashboard";
  status: "UP" | "DOWN";
  lastChecked: string;
  responseTime: number | null;
  memoryUsage: number | null;
  errorType: "TIMEOUT" | "HTTP_ERROR" | "NETWORK_ERROR" | null;
  errorMessage: string | null;
}

// ... 나머지 타입들
```

---

#### Java DTO 클래스 생성

```bash
# jsonschema2pojo 사용
mvn exec:java -Dexec.mainClass="org.jsonschema2pojo.cli.Main" \
  -Dexec.args="--source websocket-messages.json \
               --target ../src/main/java/com/realfds/dashboard/dto \
               --package com.realfds.dashboard.dto \
               --target-language JAVA"
```

**생성된 Java 클래스 예시**:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsUpdateMessage {
    @JsonProperty("type")
    private String type; // "METRICS_UPDATE"

    @JsonProperty("timestamp")
    private String timestamp; // ISO-8601

    @JsonProperty("data")
    private MetricsData data;
}

@Data
public class MetricsData {
    @JsonProperty("services")
    private List<ServiceHealth> services; // 5개

    @JsonProperty("tps")
    private TpsAggregated tps;

    @JsonProperty("alerts")
    private AlertsAggregated alerts;
}

// ... 나머지 클래스들
```

---

### 3. 테스트 케이스 작성

#### REST API 테스트 (Spring Boot)

```java
@SpringBootTest
@AutoConfigureMockMvc
class RestApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components.diskSpace.status").value("UP"))
            .andExpect(jsonPath("$.components.memory.status").value("UP"));
    }

    @Test
    void testGetServiceList() throws Exception {
        mockMvc.perform(get("/api/v1/services"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(5))
            .andExpect(jsonPath("$.services[0].serviceName").value("transaction-generator"))
            .andExpect(jsonPath("$.services[0].port").value(8080));
    }
}
```

---

#### WebSocket 메시지 검증 (JSON Schema)

```javascript
// Node.js + AJV (Another JSON Schema Validator)
const Ajv = require('ajv');
const schema = require('./websocket-messages.json');

const ajv = new Ajv();
const validate = ajv.compile(schema);

// 테스트: METRICS_UPDATE 메시지 검증
const metricsUpdateMessage = {
  type: 'METRICS_UPDATE',
  timestamp: '2025-11-12T10:30:05Z',
  data: {
    services: [ /* 5개 서비스 */ ],
    tps: { current: 87, average: 65, max: 150, history: [] },
    alerts: { current: 12, average: 8, max: 25, byRule: {}, history: [] }
  }
};

const valid = validate(metricsUpdateMessage);
if (!valid) {
  console.error('검증 실패:', validate.errors);
} else {
  console.log('검증 성공!');
}
```

---

#### WebSocket 통합 테스트 (React + Jest)

```typescript
import { renderHook, waitFor } from '@testing-library/react';
import { useWebSocket } from './useWebSocket';

test('WebSocket 연결 및 METRICS_UPDATE 수신', async () => {
  const { result } = renderHook(() => useWebSocket('ws://localhost:8083/ws/metrics'));

  // 연결 대기
  await waitFor(() => expect(result.current.status).toBe('connected'), { timeout: 5000 });

  // 첫 메시지 수신 대기
  await waitFor(() => expect(result.current.lastMessage).not.toBeNull(), { timeout: 10000 });

  // 메시지 타입 검증
  expect(result.current.lastMessage.type).toBe('METRICS_UPDATE');
  expect(result.current.lastMessage.data.services).toHaveLength(5);
  expect(result.current.lastMessage.data.tps.current).toBeGreaterThanOrEqual(0);
});
```

---

### 4. 문서 배포

#### Swagger UI 호스팅

```bash
# Docker로 Swagger UI 실행
docker run -p 8080:8080 \
  -e SWAGGER_JSON=/api/rest-api.yaml \
  -v $(pwd)/contracts:/api \
  swaggerapi/swagger-ui
```

브라우저에서 `http://localhost:8080` 접속하여 REST API 문서 확인

---

#### Markdown 문서 배포 (GitHub Pages)

```bash
# MkDocs 설치
pip install mkdocs mkdocs-material

# mkdocs.yml 설정
cat > mkdocs.yml <<EOF
site_name: Dashboard Realtime API Docs
theme:
  name: material
nav:
  - Home: README.md
  - REST API: rest-api.yaml
  - WebSocket Protocol: websocket-protocol.md
  - WebSocket Messages: websocket-messages.json
EOF

# 로컬 서버 실행
mkdocs serve

# GitHub Pages 배포
mkdocs gh-deploy
```

---

## 검증 도구 (Validation Tools)

### REST API 검증

**1. Swagger Validator**:
```bash
npx @redocly/openapi-cli lint rest-api.yaml
```

**2. Spectral (OpenAPI Linter)**:
```bash
npm install -g @stoplight/spectral-cli
spectral lint rest-api.yaml
```

---

### WebSocket 메시지 검증

**1. AJV (Another JSON Schema Validator)**:
```javascript
const Ajv = require('ajv');
const addFormats = require('ajv-formats');

const ajv = new Ajv();
addFormats(ajv);

const schema = require('./websocket-messages.json');
const validate = ajv.compile(schema);

const message = { /* 테스트 메시지 */ };
const valid = validate(message);

if (!valid) {
  console.error('검증 실패:', validate.errors);
}
```

**2. Online JSON Schema Validator**:
- https://www.jsonschemavalidator.net/
- 왼쪽에 `websocket-messages.json` 붙여넣기
- 오른쪽에 테스트 메시지 JSON 붙여넣기
- 자동으로 검증 결과 표시

---

### WebSocket 프로토콜 테스트

**1. wscat (Command Line WebSocket Client)**:
```bash
npm install -g wscat

# WebSocket 연결
wscat -c ws://localhost:8083/ws/metrics

# 백필 요청 전송
> {"type":"BACKFILL_REQUEST","lastReceivedTimestamp":"2025-11-12T10:25:00Z"}

# 응답 수신 확인
< {"type":"BACKFILL_RESPONSE","data":[...]}
```

**2. Postman WebSocket**:
- Postman 실행 → New → WebSocket Request
- URL: `ws://localhost:8083/ws/metrics`
- Connect 클릭
- 메시지 자동 수신 확인

---

## 변경 이력 (Change Log)

### Version 1.0.0 (2025-11-12)

**초기 릴리스**:
- REST API 계약 작성 (`rest-api.yaml`)
  - `GET /actuator/health`: 대시보드 서비스 헬스 체크
  - `GET /api/v1/services`: 모니터링 대상 서비스 목록
  - `GET /api/v1/metrics/current`: 현재 메트릭 스냅샷 (선택적)

- WebSocket 프로토콜 명세 작성 (`websocket-protocol.md`)
  - 연결 엔드포인트: `ws://localhost:8083/ws/metrics`
  - 메시지 타입: METRICS_UPDATE, BACKFILL_REQUEST, BACKFILL_RESPONSE, ERROR
  - Exponential Backoff 재연결 전략
  - Ping-Pong Heartbeat (30초 주기)

- WebSocket 메시지 JSON Schema 작성 (`websocket-messages.json`)
  - 4개 메시지 타입 정의
  - 모든 필드 타입 검증 규칙
  - 실제 예시 포함

- README 작성 (이 파일)
  - 계약서 개요 및 파일 구조
  - 각 파일 사용 방법
  - 코드 생성 및 테스트 가이드
  - 검증 도구 소개

---

## 추가 리소스 (Additional Resources)

### 관련 문서
- [Feature Specification](../spec.md): 기능 명세서
- [Research Document](../research.md): 기술 결정 연구
- [Data Model](../data-model.md): 데이터 구조 정의

### 외부 참고 자료
- [OpenAPI Specification](https://spec.openapis.org/oas/v3.0.0)
- [JSON Schema Specification](https://json-schema.org/draft-07/schema)
- [WebSocket Protocol (RFC 6455)](https://datatracker.ietf.org/doc/html/rfc6455)
- [Spring Boot Actuator Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

## 문의 및 지원 (Contact & Support)

**이슈 보고**:
- GitHub Issues: https://github.com/realfds/realfds/issues
- 이메일: realfds@example.com

**기여 방법**:
1. 계약서 개선 제안: Pull Request 작성
2. 버그 발견: Issue 생성 (라벨: `contracts`, `bug`)
3. 예시 추가: 각 파일의 `examples` 섹션에 추가

---

**문서 완료**: 이 README는 모든 API 계약서의 진입점이며, 구현 및 테스트 단계에서 지속적으로 참고해야 합니다.
