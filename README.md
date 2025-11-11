# RealFDS: 실시간 금융 거래 탐지 시스템

실시간 금융 거래 탐지 시스템(RealFDS)은 Apache Kafka와 Apache Flink를 활용하여 금융 거래를 실시간으로 모니터링하고 의심스러운 패턴을 즉시 탐지하는 마이크로서비스 아키텍처 기반 시스템입니다.

## 프로젝트 개요

RealFDS는 학습 및 포트폴리오 목적으로 개발된 실시간 스트리밍 데이터 처리 시스템입니다. 거래 데이터가 생성되어 Kafka를 통해 전송되고, Flink에서 실시간으로 분석하여 의심스러운 패턴을 탐지한 후, WebSocket을 통해 웹 브라우저에 즉시 알림을 전달합니다.

### 주요 기능

- **실시간 거래 데이터 생성**: 가상의 금융 거래를 자동으로 생성
- **실시간 탐지 엔진**: 3가지 패턴을 실시간으로 탐지
  - 고액 거래 (100만원 초과)
  - 해외 거래 (KR 외 국가)
  - 빈번한 거래 (1분 내 5회 초과)
- **실시간 알림**: WebSocket을 통한 즉각적인 브라우저 알림
- **알림 상태 관리**: 알림 처리 상태 추적 (미확인/확인중/완료)
- **담당자 할당 및 조치 기록**: 알림 담당자 할당 및 처리 내역 기록
- **심각도별 우선순위**: 탐지 규칙별 심각도 자동 할당 및 색상 구분 (낮음/보통/높음/긴급)
- **대시보드**: 실시간 알림 모니터링 웹 UI

### 시스템 구성 요소

RealFDS는 5개의 독립적인 마이크로서비스로 구성됩니다:

1. **transaction-generator** (Python): 가상 거래 데이터 생성
2. **fraud-detector** (Apache Flink): 실시간 패턴 탐지 엔진
3. **alert-service** (Spring WebFlux): 알림 저장 및 관리
4. **websocket-gateway** (Spring WebFlux): WebSocket 연결 관리
5. **frontend-dashboard** (React): 실시간 알림 대시보드

### 기술 스택

- **메시지 브로커**: Apache Kafka 3.6+
- **스트림 처리**: Apache Flink 1.18+ (Scala)
- **백엔드**: Spring Boot 3.2+ (WebFlux, Reactive)
- **프론트엔드**: React 18+ with TypeScript + Vite
- **컨테이너**: Docker + Docker Compose

## 빠른 시작

### 사전 요구사항

- Docker Desktop 설치 (최신 버전)
- 최소 8GB RAM, 4 Core CPU
- 사용 가능한 포트: 2181, 9092, 8081, 8082, 8083

### 실행 방법

1. 저장소 클론

```bash
git clone <repository-url>
cd RealFDS
```

2. 환경 변수 설정 (선택사항)

```bash
cp .env.example .env
# 필요시 .env 파일에서 환경 변수 수정
```

3. 전체 시스템 실행

```bash
docker-compose up -d
```

4. 시스템 준비 대기 (약 3-5분 소요)

```bash
# 모든 서비스가 시작될 때까지 대기
docker-compose logs -f
```

5. 웹 브라우저에서 대시보드 접속

```
http://localhost:8083
```

실시간으로 생성되는 거래와 탐지된 알림을 확인할 수 있습니다.

### 시스템 종료

```bash
docker-compose down
```

전체 데이터 삭제 (볼륨 포함):

```bash
docker-compose down -v
```

## 환경 변수

주요 환경 변수는 `.env.example` 파일을 참고하세요.

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Kafka 브로커 주소 |
| `TRANSACTION_GENERATION_INTERVAL_MS` | `1000` | 거래 생성 주기 (밀리초) |
| `HIGH_VALUE_THRESHOLD` | `1000000` | 고액 거래 임계값 (원) |
| `ALERT_SERVICE_URL` | `http://alert-service:8081` | alert-service 주소 |

## 시스템 검증

### 헬스 체크

각 서비스의 상태 확인:

```bash
# alert-service
curl http://localhost:8081/actuator/health

# websocket-gateway
curl http://localhost:8082/actuator/health

# frontend-dashboard
curl http://localhost:8083
```

### 로그 확인

특정 서비스 로그 확인:

```bash
# 모든 서비스 로그
docker-compose logs -f

# 특정 서비스만 확인
docker-compose logs -f transaction-generator
docker-compose logs -f fraud-detector
docker-compose logs -f alert-service
```

### Kafka 토픽 확인

거래 데이터 토픽:

```bash
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic virtual-transactions \
  --from-beginning \
  --max-messages 5
```

알림 데이터 토픽:

```bash
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic transaction-alerts \
  --from-beginning \
  --max-messages 5
```

## 아키텍처

전체 시스템 아키텍처 및 데이터 흐름은 [docs/architecture.md](docs/architecture.md)를 참고하세요.

### 데이터 흐름 요약

```
거래 생성 → Kafka → Flink 탐지 → Kafka → alert-service → websocket-gateway → 브라우저
```

1. `transaction-generator`가 가상 거래 생성 → `virtual-transactions` 토픽
2. `fraud-detector`가 거래 소비 및 패턴 탐지 → `transaction-alerts` 토픽
3. `alert-service`가 알림 수신 및 저장 (최근 100개)
4. `websocket-gateway`가 alert-service를 폴링하여 WebSocket으로 푸시
5. `frontend-dashboard`가 WebSocket으로 실시간 알림 수신 및 표시

## 개발 가이드

로컬 개발 환경 설정 및 새로운 탐지 규칙 추가 방법은 [docs/development.md](docs/development.md)를 참고하세요.

## 문제 해결

일반적인 문제 및 해결 방법은 [docs/troubleshooting.md](docs/troubleshooting.md)를 참고하세요.

### 주요 문제 해결

#### 포트 충돌

특정 포트가 이미 사용 중인 경우:

```bash
# Windows
netstat -ano | findstr :8083

# macOS/Linux
lsof -i :8083
```

해당 프로세스를 종료하거나 `.env` 파일에서 포트 변경

#### 메모리 부족

Docker Desktop의 메모리 할당을 최소 8GB로 증가:

- Docker Desktop → Settings → Resources → Memory

#### Kafka 연결 실패

Kafka가 완전히 시작될 때까지 대기 (약 2-3분 소요):

```bash
docker-compose logs kafka | grep "started (kafka.server.KafkaServer)"
```

## 성능 목표

- **종단 간 지연 시간**: 평균 3초, p95 5초
- **처리량**: 초당 10개 거래 (목표), 최대 100개
- **알림 표시 지연**: 1초 이내
- **시스템 시작 시간**: 5분 이내
- **안정성**: 30분 이상 무중단 실행

## 테스트

### E2E 테스트 실행

```bash
bash scripts/test-e2e.sh
```

### 지연 시간 측정

```bash
bash scripts/measure-latency.sh
```

## 라이선스

이 프로젝트는 학습 및 포트폴리오 목적으로 개발되었습니다.

## 문서

- [아키텍처 설명](docs/architecture.md)
- [개발 가이드](docs/development.md)
- [문제 해결 가이드](docs/troubleshooting.md)
- [빠른 시작 가이드 - 실시간 FDS](specs/001-realtime-fds/quickstart.md)
- [빠른 시작 가이드 - 알림 관리](specs/002-alert-management/quickstart.md)

## 각 서비스 상세 문서

- [transaction-generator](transaction-generator/README.md)
- [fraud-detector](fraud-detector/README.md)
- [alert-service](alert-service/README.md)
- [websocket-gateway](websocket-gateway/README.md)
- [frontend-dashboard](frontend-dashboard/README.md)
