# Research: 기술 선택 및 패턴 연구

**작성일**: 2025-11-06
**Phase**: Phase 0 - Outline & Research
**목적**: 실시간 금융 거래 탐지 시스템 구현을 위한 기술 스택, 아키텍처 패턴, 모범 사례 연구

---

## 1. 메시지 브로커 선택: Apache Kafka

### 결정
**Apache Kafka 3.6+**를 메시지 브로커로 사용

### 근거
1. **높은 처리량**: 초당 수백만 개의 메시지 처리 가능 (목표: 초당 10~100개 거래)
2. **내구성**: 로그 기반 저장으로 메시지 손실 방지 (1시간~24시간 보관)
3. **파티셔닝**: userId 기반 파티셔닝으로 상태 기반 처리 지원
4. **이벤트 시간 지원**: 이벤트 타임스탬프 기반 처리로 정확한 윈도우 연산
5. **생태계**: Flink, Spring Kafka와의 원활한 통합
6. **학습 가치**: 업계 표준 메시지 브로커, 포트폴리오 가치 높음

### 고려한 대안
- **RabbitMQ**: 메시지 큐에 강점이지만, 스트림 처리에는 Kafka가 더 적합
- **Redis Streams**: 단순하지만 내구성과 확장성이 Kafka보다 낮음
- **AWS Kinesis**: 클라우드 전용 서비스로 로컬 실행 불가 (Constitution 위반)

### 구현 세부사항
- **Kafka 버전**: 3.6.1 (안정 버전)
- **Zookeeper**: 3.8.3 (Kafka 의존성)
- **토픽 구성**:
  - `virtual-transactions`: 파티션 3개, 보관 기간 1시간, userId로 키 지정
  - `transaction-alerts`: 파티션 3개, 보관 기간 24시간, userId로 키 지정
- **복제 계수**: 1 (로컬 개발 환경, 단일 브로커)
- **메시지 형식**: JSON (직렬화 단순성, 디버깅 용이성)

---

## 2. 스트림 처리 엔진 선택: Apache Flink

### 결정
**Apache Flink 1.18+** (Scala 2.12 + Java 11)를 스트림 처리 엔진으로 사용

### 근거
1. **상태 기반 처리**: RocksDB State Backend로 빈번한 거래 탐지를 위한 상태 관리
2. **이벤트 시간 시맨틱**: 워터마크와 이벤트 시간 처리로 정확한 윈도우 연산
3. **낮은 지연 시간**: 실시간 처리에 최적화 (밀리초 단위)
4. **정확히 한 번 처리**: Exactly-once 시맨틱으로 데이터 무결성 보장
5. **Kafka 통합**: Flink Kafka Connector로 원활한 연동
6. **학습 가치**: 업계 표준 스트림 처리 프레임워크

### 고려한 대안
- **Kafka Streams**: 단순하지만 Flink보다 기능이 제한적 (State Backend 옵션 적음)
- **Apache Spark Streaming**: 마이크로 배치 처리로 실시간성이 Flink보다 낮음
- **Spring Cloud Stream**: 간단한 처리에는 적합하지만 복잡한 윈도우 처리에는 부족

### 구현 세부사항
- **Flink 버전**: 1.18.1 (LTS 버전)
- **언어**: Scala 2.12 (함수형 프로그래밍 패턴, Flink API와 자연스러운 통합)
- **State Backend**: RocksDB (디스크 기반, 대용량 상태 관리 가능)
- **체크포인트**: 60초마다 체크포인트 생성 (장애 복구)
- **워터마크**: 5초 지연 허용 (늦게 도착하는 이벤트 처리)
- **윈도우**: 1분 텀블링 윈도우 (빈번한 거래 탐지)

### Flink Job 구조
```scala
// FraudDetectionJob.scala
val transactions = env
  .addSource(new FlinkKafkaConsumer[Transaction]("virtual-transactions", ...))
  .assignTimestampsAndWatermarks(...)

// 고액 거래 탐지 (Stateless)
val highValueAlerts = transactions
  .filter(new HighValueRule(threshold = 1000000))
  .map(toAlert)

// 해외 거래 탐지 (Stateless)
val foreignAlerts = transactions
  .filter(new ForeignCountryRule(allowedCountry = "KR"))
  .map(toAlert)

// 빈번한 거래 탐지 (Stateful)
val highFrequencyAlerts = transactions
  .keyBy(_.userId)
  .window(TumblingEventTimeWindows.of(Time.minutes(1)))
  .process(new HighFrequencyRule(threshold = 5))

// 모든 알림을 Kafka로 전송
highValueAlerts.union(foreignAlerts).union(highFrequencyAlerts)
  .addSink(new FlinkKafkaProducer[Alert]("transaction-alerts", ...))
```

---

## 3. 백엔드 프레임워크 선택: Spring Boot + Spring WebFlux

### 결정
**Spring Boot 3.2+ with Spring WebFlux**를 alert-service와 websocket-gateway에 사용

### 근거
1. **Reactive Programming**: WebFlux로 비동기 논블로킹 I/O, 높은 동시성
2. **Kafka 통합**: Spring Kafka로 간편한 Consumer/Producer 구현
3. **WebSocket 지원**: Spring WebSocket으로 실시간 양방향 통신
4. **경량화**: WebFlux는 Tomcat보다 적은 리소스 사용 (목표: <512MB per service)
5. **생태계**: Spring Actuator (헬스 체크), Logback (구조화된 로깅)
6. **학습 가치**: Reactive programming 패턴 학습

### 고려한 대안
- **Spring MVC**: 동기식 블로킹 I/O로 실시간 처리에 적합하지 않음
- **Vert.x**: 성능은 좋지만 생태계와 학습 자료가 Spring보다 부족
- **Quarkus**: 네이티브 컴파일 지원하지만 설정이 복잡하고 Docker 환경에서는 이점 적음

### 구현 세부사항
- **Spring Boot 버전**: 3.2.0
- **Java 버전**: 17 (LTS)
- **주요 의존성**:
  - `spring-boot-starter-webflux`: Reactive web
  - `spring-kafka`: Kafka 통합
  - `spring-boot-starter-actuator`: 헬스 체크
  - `reactor-core`: Reactive Streams
- **로깅**: Logback + JSON 레이아웃 (구조화된 로깅)
- **에러 처리**: Spring Retry + Circuit Breaker (Kafka 연결 장애 대응)

### alert-service 아키텍처
```
Kafka (alerts 토픽)
    ↓
AlertConsumer (@KafkaListener)
    ↓
AlertService (비즈니스 로직)
    ↓
AlertRepository (인메모리, ConcurrentLinkedDeque, 최근 100개)
    ↓
AlertController (REST API - GET /api/alerts)
```

### websocket-gateway 아키텍처
```
AlertWebSocketHandler (WebSocket 연결 관리)
    ↓
BroadcastService (연결된 모든 클라이언트에 브로드캐스트)
    ↑
AlertStreamService (alert-service에서 주기적으로 새 알림 폴링)
```

**주의**: alert-service와 websocket-gateway 간 통신은 REST API 폴링 사용 (1초마다)
- **이유**: 단순성 우선 (Constitution 원칙)
- **대안**: SSE (Server-Sent Events) 또는 Reactive WebClient로 스트리밍 (추후 개선 고려)

---

## 4. 프론트엔드 프레임워크 선택: React + TypeScript + Vite

### 결정
**React 18+ with TypeScript 5+ and Vite 5+**를 frontend-dashboard에 사용

### 근거
1. **컴포넌트 기반**: 재사용 가능한 UI 컴포넌트 (AlertList, AlertItem, ConnectionStatus)
2. **타입 안전성**: TypeScript로 컴파일 타임 타입 체크
3. **빠른 개발**: Vite의 빠른 HMR (Hot Module Replacement)
4. **WebSocket 지원**: 네이티브 WebSocket API 사용 가능
5. **생태계**: React Hooks, React Testing Library
6. **학습 가치**: 현대적인 프론트엔드 개발 스택

### 고려한 대안
- **Vue.js**: 간단하지만 생태계가 React보다 작음
- **Angular**: 너무 복잡하고 학습 곡선이 가파름 (단순성 위반)
- **Svelte**: 혁신적이지만 생태계와 학습 자료 부족

### 구현 세부사항
- **React 버전**: 18.2.0
- **TypeScript 버전**: 5.0+
- **Vite 버전**: 5.0+
- **주요 라이브러리**:
  - `react`, `react-dom`: 핵심
  - `@types/react`, `@types/react-dom`: TypeScript 타입
  - `vitest`, `@testing-library/react`: 테스트
- **상태 관리**: useState + useEffect (간단한 로컬 상태, Redux 불필요)
- **스타일링**: CSS Modules 또는 Tailwind CSS (추후 결정)

### WebSocket 통신 패턴
```typescript
// useWebSocket.ts hook
const useWebSocket = (url: string) => {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [status, setStatus] = useState<'connected' | 'disconnected'>('disconnected');

  useEffect(() => {
    const ws = new WebSocket(url);

    ws.onopen = () => setStatus('connected');
    ws.onclose = () => {
      setStatus('disconnected');
      // 재연결 로직 (5초 후)
      setTimeout(() => /* 재연결 */, 5000);
    };
    ws.onmessage = (event) => {
      const alert = JSON.parse(event.data);
      setAlerts((prev) => [alert, ...prev].slice(0, 100)); // 최근 100개만 유지
    };

    return () => ws.close();
  }, [url]);

  return { alerts, status };
};
```

---

## 5. 데이터 생성기 선택: Python

### 결정
**Python 3.11+**를 transaction-generator에 사용

### 근거
1. **빠른 프로토타이핑**: 간결한 문법으로 데이터 생성 로직 구현
2. **Kafka 라이브러리**: `kafka-python` 또는 `confluent-kafka-python`로 간편한 Kafka 통합
3. **난수 생성**: `random` 모듈로 거래 데이터 생성
4. **경량화**: 메모리 사용량 적음 (목표: <256MB)
5. **학습 가치**: 다중 언어 마이크로서비스 환경 시연

### 고려한 대안
- **Java**: 더 복잡하고 메모리 사용량 높음
- **Node.js**: 가능하지만 Python이 데이터 생성에 더 적합

### 구현 세부사항
- **Python 버전**: 3.11
- **주요 라이브러리**:
  - `confluent-kafka`: Kafka Producer (librdkafka 기반, 고성능)
  - `faker`: 가상 데이터 생성 (사용자 이름 등)
  - `python-dotenv`: 환경 변수 로드
- **생성 주기**: 100ms마다 1개 거래 (초당 10개, 환경 변수로 조정 가능)
- **거래 분포**:
  - 금액: 1,000원 ~ 1,500,000원 (정규 분포, 평균 300,000원)
  - 국가: 80% KR, 10% US, 5% JP, 5% CN
  - 사용자: user-1 ~ user-10 (균등 분포)

```python
# generator.py
import random
from datetime import datetime, timezone

def generate_transaction():
    return {
        "transactionId": str(uuid.uuid4()),
        "userId": f"user-{random.randint(1, 10)}",
        "amount": int(random.gauss(300000, 200000)),  # 평균 30만원, 표준편차 20만원
        "currency": "KRW",
        "countryCode": random.choices(["KR", "US", "JP", "CN"], weights=[80, 10, 5, 5])[0],
        "timestamp": datetime.now(timezone.utc).isoformat()
    }
```

---

## 6. 컨테이너화 전략: Docker + Docker Compose

### 결정
**Docker**로 각 서비스를 컨테이너화하고, **Docker Compose**로 오케스트레이션

### 근거
1. **환경 일관성**: Windows, macOS, Linux에서 동일하게 실행
2. **단일 명령어 배포**: `docker-compose up`으로 전체 시스템 실행 (Constitution 원칙)
3. **의존성 관리**: Kafka, Zookeeper를 포함한 모든 구성 요소 통합 관리
4. **네트워킹**: Docker 네트워크로 서비스 간 통신
5. **리소스 제한**: 각 컨테이너의 메모리/CPU 제한 설정

### Docker Compose 구조
```yaml
# docker-compose.yml
version: '3.8'

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    mem_limit: 256m

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    mem_limit: 1g

  transaction-generator:
    build: ./transaction-generator
    depends_on:
      - kafka
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      GENERATION_INTERVAL_MS: 100
    mem_limit: 256m

  fraud-detector:
    build: ./fraud-detector
    depends_on:
      - kafka
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      HIGH_VALUE_THRESHOLD: 1000000
    mem_limit: 2g

  alert-service:
    build: ./alert-service
    depends_on:
      - kafka
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    ports:
      - "8081:8081"
    mem_limit: 512m

  websocket-gateway:
    build: ./websocket-gateway
    depends_on:
      - alert-service
    environment:
      ALERT_SERVICE_URL: http://alert-service:8081
    ports:
      - "8082:8082"
    mem_limit: 512m

  frontend-dashboard:
    build: ./frontend-dashboard
    depends_on:
      - websocket-gateway
    environment:
      WEBSOCKET_URL: ws://localhost:8082/ws/alerts
    ports:
      - "8083:8083"
    mem_limit: 256m

networks:
  default:
    name: realfds-network
```

### Dockerfile 전략
- **Multi-stage build**: 빌드와 실행 단계 분리로 이미지 크기 최소화
- **베이스 이미지**:
  - Python: `python:3.11-slim`
  - Flink: `flink:1.18-scala_2.12-java11`
  - Spring: `eclipse-temurin:17-jre-alpine`
  - React: `node:20-alpine` (빌드) + `nginx:alpine` (실행)

---

## 7. 테스트 전략

### 단위 테스트
- **transaction-generator**: pytest로 거래 생성 로직 테스트
- **fraud-detector**: ScalaTest + Flink Testing Harness로 각 규칙 테스트
- **alert-service**: JUnit 5 + Mockito로 서비스 계층 테스트
- **websocket-gateway**: JUnit 5 + WebTestClient로 WebSocket 핸들러 테스트
- **frontend-dashboard**: Vitest + React Testing Library로 컴포넌트 테스트

### 통합 테스트
- **TestContainers**: Kafka 컨테이너를 사용한 통합 테스트
- **E2E 테스트**: `scripts/test-e2e.sh`로 전체 시스템 검증
  1. `docker-compose up -d`로 시스템 시작
  2. 5분 대기 (시스템 준비)
  3. WebSocket 연결 테스트
  4. 알림 수신 테스트 (3가지 규칙)
  5. 지연 시간 측정 (<5초)

### 성능 테스트
- **지연 시간 측정**: `scripts/measure-latency.sh`
  - 거래 생성 시각과 알림 수신 시각 차이 계산
  - 평균, p95, p99 지연 시간 출력
- **부하 테스트**: 거래 생성 주기를 10ms로 단축 (초당 100개)

---

## 8. 로깅 및 모니터링 전략

### 구조화된 로깅
- **형식**: JSON (파싱 용이, 로그 분석 도구 통합)
- **필드**:
  - `timestamp`: ISO 8601 형식
  - `level`: ERROR, WARN, INFO, DEBUG, TRACE
  - `service`: 서비스 이름 (transaction-generator, fraud-detector 등)
  - `message`: 로그 메시지
  - `context`: transactionId, userId, ruleName 등

### 로그 레벨 전략
- **ERROR**: Kafka 연결 실패, 예외 발생
- **WARN**: 재연결 시도, 잘못된 데이터 형식
- **INFO**: 서비스 시작/종료, 알림 생성, 중요한 비즈니스 이벤트
- **DEBUG**: 개별 거래 처리, WebSocket 연결/해제
- **TRACE**: Kafka 메타데이터, 상세한 디버깅 정보

### 헬스 체크
- **엔드포인트**: `/actuator/health` (Spring Actuator)
- **상태**: UP (정상), DOWN (장애)
- **체크 항목**:
  - Kafka 연결 상태
  - 메모리 사용량
  - 디스크 사용량

---

## 9. 개발 단계별 계획

### 1단계: 인프라 및 데이터 파이프라인 (우선순위 높음)
**목표**: 거래 데이터가 Kafka를 통해 흐르는 기본 파이프라인 구축

**작업**:
1. Docker Compose 파일 작성 (Kafka, Zookeeper)
2. transaction-generator 구현 (Python)
   - Kafka Producer 설정
   - 거래 데이터 생성 로직
   - Dockerfile 작성
3. 통합 테스트: 거래 데이터가 Kafka 토픽에 발행되는지 확인

**검증 기준**:
- `docker-compose up`으로 Kafka와 generator 실행
- `kafka-console-consumer`로 메시지 확인
- 초당 10개 거래 발생 확인

---

### 2단계: 실시간 탐지 엔진 (핵심 기능)
**목표**: 3가지 탐지 규칙을 Flink로 구현하고 알림 생성

**작업**:
1. fraud-detector 프로젝트 생성 (Gradle + Scala)
2. Flink Job 기본 구조 작성
3. 탐지 규칙 구현:
   - HighValueRule (고액 거래)
   - ForeignCountryRule (해외 거래)
   - HighFrequencyRule (빈번한 거래, 상태 기반)
4. 알림을 Kafka `transaction-alerts` 토픽으로 발행
5. 단위 테스트 (각 규칙별)
6. Dockerfile 작성

**검증 기준**:
- 고액 거래 (>100만원) 알림 생성 확인
- 해외 거래 (KR 외) 알림 생성 확인
- 1분 내 5회 초과 거래 알림 생성 확인
- `kafka-console-consumer`로 알림 토픽 확인

---

### 3단계: 백엔드 서비스 (알림 저장 및 WebSocket)
**목표**: 알림을 저장하고 WebSocket을 통해 프론트엔드로 전달

**작업**:
1. alert-service 구현 (Spring Boot + WebFlux)
   - Kafka Consumer (alerts 토픽 구독)
   - 인메모리 저장소 (최근 100개)
   - REST API (GET /api/alerts)
   - 헬스 체크 (/actuator/health)
   - Dockerfile 작성
2. websocket-gateway 구현 (Spring Boot + WebFlux)
   - WebSocket 핸들러 (연결 관리)
   - BroadcastService (클라이언트 브로드캐스트)
   - AlertStreamService (alert-service 폴링)
   - Dockerfile 작성
3. 통합 테스트 (alert-service + websocket-gateway)

**검증 기준**:
- alert-service가 알림 토픽에서 메시지 수신 확인
- REST API로 알림 조회 가능 (`curl localhost:8081/api/alerts`)
- WebSocket 연결 후 알림 수신 확인 (`wscat -c ws://localhost:8082/ws/alerts`)

---

### 4단계: 프론트엔드 대시보드 (사용자 인터페이스)
**목표**: 웹 브라우저에서 실시간 알림 표시

**작업**:
1. React 프로젝트 생성 (Vite + TypeScript)
2. 컴포넌트 구현:
   - AlertList (알림 목록)
   - AlertItem (개별 알림)
   - ConnectionStatus (연결 상태)
3. useWebSocket hook (WebSocket 연결 관리)
4. 스타일링 (CSS 또는 Tailwind)
5. 단위 테스트 (Vitest)
6. Dockerfile 작성 (Nginx로 정적 파일 서빙)

**검증 기준**:
- 브라우저에서 `localhost:8083` 접속
- 실시간 알림이 1초 이내에 화면에 표시
- 연결 상태가 명확히 표시 ("연결됨" / "연결 끊김")
- 최근 100개 알림만 표시

---

### 5단계: 통합 및 최적화 (전체 시스템 검증)
**목표**: 전체 시스템을 통합하고 성능/안정성 검증

**작업**:
1. docker-compose.yml 완성 (5개 서비스 + Kafka + Zookeeper)
2. E2E 테스트 스크립트 작성 (`scripts/test-e2e.sh`)
3. 지연 시간 측정 스크립트 작성 (`scripts/measure-latency.sh`)
4. README 작성 (빠른 시작, 아키텍처 설명)
5. 문서 작성 (architecture.md, development.md, troubleshooting.md)
6. 성능 최적화:
   - 메모리 사용량 모니터링
   - 지연 시간 최적화 (<5초)
   - 안정성 테스트 (30분 실행)

**검증 기준**:
- `docker-compose up`으로 전체 시스템 실행 (5분 이내)
- 종단 간 지연 시간 <5초 (평균 3초 목표)
- 30분 동안 크래시 없이 실행
- README를 따라 10분 내에 시스템 실행 가능

---

## 10. 열린 질문 및 결정 사항

### 해결된 질문
✅ **Flink vs Kafka Streams**: Flink 선택 (상태 관리, 이벤트 시간 처리 우수)
✅ **Spring MVC vs WebFlux**: WebFlux 선택 (실시간 처리, 낮은 리소스 사용)
✅ **Python vs Java (generator)**: Python 선택 (단순성, 경량화)
✅ **서비스 개수**: 5개 사용 (사용자 명시 요구사항, Complexity Tracking에서 정당화)

### 추후 고려 사항
🔄 **alert-service와 websocket-gateway 간 통신**: 현재는 REST 폴링, 추후 SSE 또는 Reactive Stream으로 개선 고려
🔄 **프론트엔드 스타일링**: CSS Modules vs Tailwind CSS (1단계에서 결정)
🔄 **로그 수집**: 현재는 stdout, 추후 ELK Stack 또는 Loki 연동 고려 (Out of Scope)
🔄 **메트릭**: 현재는 헬스 체크만, 추후 Prometheus + Grafana 연동 고려 (Out of Scope)

---

## 11. 참고 자료

### 공식 문서
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Apache Flink Documentation](https://nightlies.apache.org/flink/flink-docs-release-1.18/)
- [Spring WebFlux Reference](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [React Documentation](https://react.dev/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

### 모범 사례
- [Kafka Best Practices](https://kafka.apache.org/documentation/#bestpractices)
- [Flink Best Practices](https://nightlies.apache.org/flink/flink-docs-master/docs/ops/production_ready/)
- [Spring Boot Production Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [React Performance Optimization](https://react.dev/learn/render-and-commit)

### 유사 프로젝트 (참고용)
- [Flink Training: Fraud Detection](https://github.com/apache/flink-training)
- [Spring Kafka Examples](https://github.com/spring-projects/spring-kafka/tree/main/samples)
- [React WebSocket Examples](https://github.com/topics/react-websocket)

---

**연구 완료**: Phase 0 완료, Phase 1 (Design & Contracts) 진행 준비 완료
