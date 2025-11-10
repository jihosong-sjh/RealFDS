# 시스템 아키텍처: RealFDS

**작성일**: 2025-11-06
**목적**: RealFDS의 전체 시스템 아키텍처, 데이터 흐름, 각 서비스의 역할 및 책임 설명

---

## 1. 시스템 개요

RealFDS는 실시간 금융 거래 탐지를 위한 이벤트 기반 마이크로서비스 아키텍처 시스템입니다. Apache Kafka를 중심으로 5개의 독립적인 서비스가 비동기적으로 통신하며, Apache Flink를 활용한 실시간 스트림 처리로 의심스러운 패턴을 즉시 탐지합니다.

### 핵심 원칙

- **실시간 우선**: 모든 데이터 처리는 스트리밍 방식으로 즉시 처리
- **서비스 독립성**: 각 서비스는 독립적으로 배포 및 확장 가능
- **이벤트 기반 통신**: Kafka를 통한 비동기 메시지 전달
- **단순한 배포**: Docker Compose로 단일 명령어 실행

---

## 2. 시스템 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           RealFDS Architecture                          │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────┐
│ transaction-        │  ① 거래 데이터 생성 (Python)
│ generator           │
│                     │
│ - 1초마다 거래 생성  │
│ - Faker로 랜덤 데이터│
│ - Kafka Producer    │
└──────────┬──────────┘
           │ Kafka Topic: virtual-transactions
           ↓
┌─────────────────────────────────────────────┐
│           Apache Kafka (Broker)             │
│                                             │
│  Topics:                                    │
│  - virtual-transactions (1시간 보관)         │
│  - transaction-alerts (24시간 보관)          │
└─────────────────────────────────────────────┘
           │
           │ ② Flink가 거래 소비
           ↓
┌─────────────────────┐
│ fraud-detector      │  ③ 실시간 패턴 탐지 (Apache Flink)
│ (Flink Job)         │
│                     │
│ Rules:              │
│ - HighValueRule     │  → 100만원 초과 거래
│ - ForeignCountryRule│  → KR 외 국가 거래
│ - HighFrequencyRule │  → 1분 내 5회 초과 거래
│                     │
│ State Backend:      │
│ - RocksDB           │  (빈번한 거래 추적)
└──────────┬──────────┘
           │ Kafka Topic: transaction-alerts
           ↓
┌─────────────────────────────────────────────┐
│           Apache Kafka (Broker)             │
└─────────────────────────────────────────────┘
           │
           │ ④ alert-service가 알림 소비
           ↓
┌─────────────────────┐
│ alert-service       │  ⑤ 알림 저장 및 관리 (Spring WebFlux)
│ (Spring Boot)       │
│                     │
│ - Kafka Consumer    │
│ - 인메모리 저장소    │  (최근 100개만 유지)
│ - REST API          │
│   GET /api/alerts   │
│   GET /actuator/    │
│       health        │
└──────────┬──────────┘
           │ HTTP Polling (1초마다)
           ↓
┌─────────────────────┐
│ websocket-gateway   │  ⑥ WebSocket 연결 관리 (Spring WebFlux)
│ (Spring Boot)       │
│                     │
│ - WebClient         │  → alert-service 호출
│ - WebSocket Handler │
│ - Broadcast Service │  → 모든 클라이언트에 알림 푸시
└──────────┬──────────┘
           │ WebSocket: ws://localhost:8082/ws/alerts
           ↓
┌─────────────────────┐
│ frontend-dashboard  │  ⑦ 웹 UI (React + TypeScript)
│ (React + Vite)      │
│                     │
│ - WebSocket Client  │
│ - Real-time Alerts  │
│ - Connection Status │
│                     │
│ URL: http://        │
│   localhost:8083    │
└─────────────────────┘
```

---

## 3. 데이터 흐름 (Data Flow)

### 정상 흐름 (Happy Path)

```
1️⃣ 거래 생성
   transaction-generator
   → Transaction 객체 생성 (Python dataclass)
   → JSON 직렬화
   → Kafka Producer
   → Topic: virtual-transactions

2️⃣ 실시간 탐지
   fraud-detector (Flink)
   → Kafka Consumer (virtual-transactions)
   → 역직렬화 (JSON → Transaction)
   → 3가지 규칙 병렬 적용
      - HighValueRule: amount > 1,000,000 필터
      - ForeignCountryRule: countryCode != "KR" 필터
      - HighFrequencyRule: 1분 윈도우 내 거래 수 집계
   → 조건 충족 시 Alert 생성
   → JSON 직렬화
   → Kafka Producer
   → Topic: transaction-alerts

3️⃣ 알림 저장
   alert-service
   → Kafka Consumer (transaction-alerts)
   → 역직렬화 (JSON → Alert)
   → ConcurrentLinkedDeque에 추가 (최신 순)
   → 101개 이상 시 가장 오래된 알림 제거
   → INFO 로그 기록

4️⃣ WebSocket 브로드캐스트
   websocket-gateway
   → @Scheduled(1초마다)
   → WebClient로 GET http://alert-service:8081/api/alerts
   → 마지막 조회 이후 새로운 알림만 필터링
   → BroadcastService.broadcast(alert)
   → 모든 활성 WebSocket 세션에 JSON 전송

5️⃣ 브라우저 표시
   frontend-dashboard
   → WebSocket.onmessage 이벤트
   → JSON 파싱 (Alert 타입)
   → React State 업데이트
   → AlertList 컴포넌트 리렌더링
   → 화면에 알림 표시 (fade-in 애니메이션)
```

### 종단 간 지연 시간 (End-to-End Latency)

```
Transaction.timestamp → Alert 브라우저 표시

목표: 평균 3초, p95 5초
실제: 측정 스크립트로 검증 (scripts/measure-latency.sh)

구간별 지연:
- 거래 생성 → Kafka 발행: <100ms
- Kafka → Flink 소비: <200ms
- Flink 처리 → Alert 생성: <500ms
- Alert → Kafka 발행: <100ms
- Kafka → alert-service 소비: <200ms
- alert-service 저장 → websocket-gateway 폴링: <1000ms (최악)
- WebSocket 전송 → 브라우저 수신: <100ms
- 브라우저 렌더링: <50ms

총 합계: ~2250ms (평균), ~3000ms (p95)
```

---

## 4. 각 서비스의 역할 및 책임

### 4.1 transaction-generator

**책임**: 가상 금융 거래 데이터 생성 및 Kafka 발행

**기술 스택**: Python 3.11+, confluent-kafka, Faker

**주요 기능**:
- 1초마다 무작위 Transaction 객체 생성
- 금액: 정규 분포 (평균 30만원, 표준편차 20만원)
- 사용자: user-1 ~ user-10 균등 분포
- 국가: 80% KR, 10% US, 5% JP, 5% CN
- Kafka Producer로 `virtual-transactions` 토픽에 발행

**환경 변수**:
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka 브로커 주소
- `TRANSACTION_GENERATION_INTERVAL_MS`: 거래 생성 주기 (기본: 1000ms)

**메모리 제한**: 256MB

---

### 4.2 fraud-detector

**책임**: 실시간 스트림 처리 및 의심스러운 패턴 탐지

**기술 스택**: Apache Flink 1.18+, Scala 2.12, RocksDB State Backend

**주요 기능**:
- Kafka Source: `virtual-transactions` 토픽 구독
- 3가지 탐지 규칙 병렬 실행
  - **HighValueRule**: 고액 거래 탐지 (100만원 초과)
  - **ForeignCountryRule**: 해외 거래 탐지 (KR 외 국가)
  - **HighFrequencyRule**: 빈번한 거래 탐지 (1분 내 5회 초과)
- 이벤트 시간(event-time) 기반 처리
- 워터마크: 5초 지연 허용 (out-of-order 이벤트 처리)
- 체크포인트: 60초마다 (상태 복구용)
- Kafka Sink: `transaction-alerts` 토픽으로 Alert 발행

**상태 관리**:
- RocksDB State Backend (HighFrequencyRule용)
- KeyedState: userId별 1분 윈도우 내 거래 수 추적
- 1분 이상 지난 상태는 자동 삭제

**메모리 제한**: 2GB

---

### 4.3 alert-service

**책임**: 알림 데이터 저장 및 REST API 제공

**기술 스택**: Spring Boot 3.2+, Spring WebFlux, Spring Kafka

**주요 기능**:
- Kafka Consumer: `transaction-alerts` 토픽 구독
- 인메모리 저장소 (ConcurrentLinkedDeque)
  - 최근 100개 알림만 유지
  - 101개 이상 시 가장 오래된 알림 자동 제거
- REST API
  - `GET /api/alerts`: 최근 알림 목록 조회 (Reactive)
  - `GET /actuator/health`: 헬스 체크
- JSON 역직렬화 (Jackson)

**환경 변수**:
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka 브로커 주소
- `SERVER_PORT`: 서버 포트 (기본: 8081)

**메모리 제한**: 512MB

---

### 4.4 websocket-gateway

**책임**: WebSocket 연결 관리 및 실시간 알림 브로드캐스트

**기술 스택**: Spring Boot 3.2+, Spring WebFlux, Spring WebSocket

**주요 기능**:
- WebSocket 엔드포인트: `ws://localhost:8082/ws/alerts`
- AlertStreamService
  - `@Scheduled(fixedDelay = 1000)`: 1초마다 alert-service 폴링
  - WebClient로 `GET http://alert-service:8081/api/alerts` 호출
  - 마지막 알림 ID 추적하여 새로운 알림만 필터링
- BroadcastService
  - 모든 활성 WebSocket 세션에 알림 브로드캐스트
  - 세션 전송 실패 시 해당 세션 제거 (자동 정리)
- 헬스 체크: `GET /actuator/health`

**환경 변수**:
- `ALERT_SERVICE_URL`: alert-service 주소 (기본: http://alert-service:8081)
- `SERVER_PORT`: 서버 포트 (기본: 8082)

**메모리 제한**: 512MB

---

### 4.5 frontend-dashboard

**책임**: 실시간 알림 대시보드 웹 UI

**기술 스택**: React 18+, TypeScript 5+, Vite 5+, WebSocket API

**주요 기능**:
- `useWebSocket` hook
  - WebSocket 연결 수립 (`ws://localhost:8082/ws/alerts`)
  - onopen: connectionStatus를 'connected'로 업데이트
  - onmessage: Alert 수신 및 React State 업데이트 (최근 100개)
  - onclose: 5초 후 자동 재연결 시도
- React 컴포넌트
  - `Header`: 제목 표시
  - `ConnectionStatus`: 연결 상태 표시 (연결됨/끊김/연결 중)
  - `AlertList`: 알림 목록 스크롤 영역 (최대 높이 600px)
  - `AlertItem`: 개별 알림 카드 (심각도별 색상, 애니메이션)
- 반응형 디자인 (미디어 쿼리)

**환경 변수**:
- `VITE_WEBSOCKET_URL`: WebSocket 서버 주소 (기본: ws://localhost:8082/ws/alerts)

**메모리 제한**: 256MB (Nginx 정적 파일 서빙)

---

## 5. 기술 스택 상세

### 메시지 브로커: Apache Kafka

**역할**: 서비스 간 비동기 이벤트 전달

**토픽 구성**:
| 토픽명 | 생산자 | 소비자 | 보관 기간 | 파티션 수 |
|--------|--------|--------|----------|----------|
| `virtual-transactions` | transaction-generator | fraud-detector | 1시간 | 1 |
| `transaction-alerts` | fraud-detector | alert-service | 24시간 | 1 |

**설정**:
- Zookeeper: 포트 2181
- Kafka Broker: 포트 9092
- 메모리 제한: Kafka 1GB, Zookeeper 256MB

---

### 스트림 처리 엔진: Apache Flink

**역할**: 실시간 데이터 처리 및 상태 관리

**클러스터 구성**:
- JobManager: 1개 (포트 8081)
- TaskManager: 1개 (4개 슬롯)

**주요 개념**:
- **DataStream API**: 스트림 처리 추상화
- **Event Time Processing**: 이벤트 발생 시각 기준 처리
- **Watermarks**: 5초 지연 허용 (늦게 도착한 이벤트 처리)
- **Checkpointing**: 60초마다 상태 스냅샷 저장 (장애 복구용)
- **State Backend**: RocksDB (디스크 기반, 대용량 상태 지원)

**윈도우 처리**:
- HighFrequencyRule: 1분 텀블링 윈도우 (TumblingEventTimeWindows)
- keyBy userId: 사용자별 독립적인 윈도우 상태 관리

---

### 백엔드 프레임워크: Spring Boot 3.2+ (WebFlux)

**역할**: Reactive 백엔드 서비스 (alert-service, websocket-gateway)

**주요 특징**:
- **Non-blocking I/O**: Project Reactor 기반 비동기 처리
- **WebFlux**: Reactive 웹 스택 (Netty 기반)
- **Spring Kafka**: Kafka 통합 (Consumer, Producer)
- **Spring WebSocket**: WebSocket 연결 관리
- **Actuator**: 헬스 체크 및 메트릭

**로깅**:
- Logback + JSON 레이아웃 (구조화된 로깅)
- INFO 레벨 이상만 출력

---

### 프론트엔드: React 18+ + TypeScript + Vite

**역할**: 단일 페이지 애플리케이션 (SPA)

**주요 라이브러리**:
- React 18: UI 컴포넌트 라이브러리
- TypeScript 5: 타입 안정성
- Vite 5: 빠른 빌드 도구
- WebSocket API: 실시간 통신

**빌드 및 배포**:
- Vite로 프로덕션 빌드 (npm run build)
- Nginx로 정적 파일 서빙 (포트 8083)
- SPA 라우팅 지원 (try_files)

---

## 6. 네트워크 및 포트

### 서비스별 포트

| 서비스 | 프로토콜 | 포트 | 외부 노출 | 용도 |
|--------|----------|------|-----------|------|
| Zookeeper | TCP | 2181 | ❌ | Kafka 메타데이터 |
| Kafka | TCP | 9092 | ❌ | 메시지 브로커 |
| Flink JobManager | HTTP | 8081 | ❌ | Flink Web UI (선택적) |
| alert-service | HTTP | 8081 | ❌ | REST API (내부용) |
| websocket-gateway | HTTP/WS | 8082 | ✅ | WebSocket 연결 |
| frontend-dashboard | HTTP | 8083 | ✅ | 웹 UI |

### Docker 네트워크

- 네트워크 이름: `realfds-network`
- 드라이버: bridge
- 모든 서비스가 동일 네트워크에 연결
- 서비스 간 통신: Docker DNS로 서비스명 사용 (예: `kafka:9092`)

---

## 7. 보안 고려사항 (현재 구현)

**현재 범위** (학습용 프로젝트):
- ❌ 인증/인가 없음
- ❌ TLS/SSL 암호화 없음
- ✅ 입력 검증 (Transaction, Alert 필드)
- ✅ 오류 메시지에 민감 정보 미포함
- ✅ 환경 변수로 설정 외부화

**프로덕션 환경 권장사항** (Out of Scope):
- Kafka: SASL/SSL 인증 및 암호화
- Spring Boot: Spring Security + OAuth2
- WebSocket: 토큰 기반 인증
- 프론트엔드: CORS 정책 강화

---

## 8. 확장성 (Scalability)

### 수평 확장 (Horizontal Scaling)

**현재 구성** (로컬 개발):
- 모든 서비스: 1개 인스턴스

**확장 시나리오** (프로덕션):
1. **transaction-generator**: 여러 인스턴스로 처리량 증가
   - Kafka 파티션 수 증가 (예: 4개)
   - 각 인스턴스가 독립적으로 거래 생성
2. **fraud-detector**: Flink TaskManager 증가
   - TaskManager 3개 → 12개 슬롯
   - 병렬 처리 증가
3. **alert-service**: 여러 인스턴스 + 로드 밸런서
   - Consumer Group으로 파티션 분산 처리
4. **websocket-gateway**: 여러 인스턴스 + Redis Pub/Sub
   - Redis로 인스턴스 간 알림 동기화
5. **frontend-dashboard**: CDN으로 정적 파일 제공

### 상태 관리

- **Stateless 서비스**: transaction-generator, alert-service, websocket-gateway
- **Stateful 서비스**: fraud-detector (Flink RocksDB State Backend)
  - 체크포인트로 상태 복구 가능
  - 장애 발생 시 마지막 체크포인트부터 재시작

---

## 9. 모니터링 및 관찰 가능성 (Observability)

### 로깅

**모든 서비스**:
- 구조화된 JSON 로깅 (Logback/Python logging)
- 필드: timestamp, level, logger, message, context (transactionId, userId, alertId)
- INFO 레벨 이상만 출력

**주요 로그 이벤트**:
- transaction-generator: 거래 생성 (1초마다)
- fraud-detector: 알림 생성 (탐지 시)
- alert-service: 알림 수신 및 저장
- websocket-gateway: WebSocket 연결/해제, 브로드캐스트

### 헬스 체크

**엔드포인트**:
- alert-service: `GET http://localhost:8081/actuator/health`
- websocket-gateway: `GET http://localhost:8082/actuator/health`

**응답 형식**:
```json
{
  "status": "UP"
}
```

### 메트릭 (추후 확장 고려)

**Flink Metrics** (현재 Out of Scope):
- 처리량 (records/sec)
- 지연 시간 (latency)
- 체크포인트 성공률

**Spring Actuator Metrics** (현재 Out of Scope):
- JVM 메모리 사용량
- HTTP 요청 수
- WebSocket 활성 연결 수

---

## 10. 장애 처리 (Fault Tolerance)

### Kafka 연결 실패

**alert-service, transaction-generator**:
- Circuit Breaker 패턴 (Spring Retry)
- 지수적 백오프 (1s, 2s, 4s, 8s, 최대 30s)
- 최대 3회 재시도 후 실패 로그

### WebSocket 연결 끊김

**frontend-dashboard**:
- onclose 이벤트 감지
- ConnectionStatus를 "disconnected"로 업데이트
- 5초 후 자동 재연결 시도
- 최대 재연결 시도 횟수: 무제한

### Flink 장애 복구

**fraud-detector**:
- 체크포인트: 60초마다 상태 스냅샷 저장
- 장애 발생 시 마지막 체크포인트부터 재시작
- 상태 복구 완료 후 처리 계속

### 메시지 손실 방지

**Kafka 보관 정책**:
- virtual-transactions: 1시간 보관
- transaction-alerts: 24시간 보관
- Consumer Offset 관리로 재처리 가능

---

## 11. 성능 최적화

### Kafka

- 배치 전송 (linger.ms: 10ms)
- 압축: gzip
- 파티션 수: 1 (로컬 개발), 4+ (프로덕션)

### Flink

- 병렬 처리: TaskManager 슬롯 4개
- 상태 백엔드: RocksDB (디스크 기반, 메모리 절약)
- 체크포인트 간격: 60초 (트레이드오프: 복구 시간 vs 성능)

### Spring Boot

- WebFlux: Non-blocking I/O로 스레드 효율성
- Connection Pooling: WebClient (alert-service 호출용)

### React

- Virtual DOM: 효율적인 UI 업데이트
- 최근 100개 알림만 유지: 메모리 절약
- CSS 애니메이션: GPU 가속

---

## 12. 개발 환경 vs 프로덕션 환경

| 구성 요소 | 개발 환경 (현재) | 프로덕션 환경 (권장) |
|-----------|-----------------|---------------------|
| **배포** | Docker Compose | Kubernetes (Helm) |
| **Kafka** | 단일 브로커, 파티션 1 | 3+ 브로커, 파티션 4+ |
| **Flink** | JobManager 1, TaskManager 1 | JobManager 2 (HA), TaskManager 3+ |
| **로드 밸런서** | 없음 | Nginx/HAProxy |
| **인증** | 없음 | OAuth2 + JWT |
| **암호화** | 없음 | TLS 1.3 |
| **모니터링** | 로그만 | Prometheus + Grafana |
| **알림** | 로그만 | PagerDuty/Slack |

---

## 13. 데이터 모델 요약

### Transaction (거래)

```json
{
  "schemaVersion": "1.0",
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-3",
  "amount": 1250000,
  "currency": "KRW",
  "countryCode": "KR",
  "timestamp": "2025-11-06T10:30:45.123Z"
}
```

### Alert (알림)

```json
{
  "schemaVersion": "1.0",
  "alertId": "660e9511-f39c-52e5-b827-557766551111",
  "originalTransaction": { /* Transaction 객체 */ },
  "ruleType": "SIMPLE_RULE",
  "ruleName": "HIGH_VALUE",
  "reason": "고액 거래 (100만원 초과): 1,250,000원",
  "severity": "HIGH",
  "alertTimestamp": "2025-11-06T10:30:47.456Z"
}
```

---

## 14. 참고 문서

- [데이터 모델 상세](../specs/001-realtime-fds/data-model.md)
- [API 계약서](../specs/001-realtime-fds/contracts/)
- [개발 가이드](development.md)
- [문제 해결 가이드](troubleshooting.md)
- [빠른 시작](../specs/001-realtime-fds/quickstart.md)

---

**아키텍처 문서 완료**: 시스템 전체 구조, 데이터 흐름, 기술 스택, 확장성, 성능 최적화 방안 설명
