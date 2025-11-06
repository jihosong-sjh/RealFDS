# Quick Start: 실시간 금융 거래 탐지 시스템

**작성일**: 2025-11-06
**Phase**: Phase 1 - Design & Contracts
**목적**: 개발자가 시스템을 빠르게 실행하고 검증하는 가이드

---

## 1. 사전 요구사항

### 1.1 소프트웨어 요구사항
- **Docker**: 24.0+ ([설치 가이드](https://docs.docker.com/get-docker/))
- **Docker Compose**: 2.20+ (Docker Desktop에 포함)
- **Git**: 2.40+ (선택적, 소스 코드 클론용)

### 1.2 하드웨어 요구사항
- **메모리**: 최소 8GB RAM (권장: 16GB)
- **CPU**: 4 core 이상
- **디스크**: 10GB 여유 공간

### 1.3 네트워크 요구사항
- **포트**: 8081, 8082, 8083, 9092 (Kafka) 사용 가능해야 함
- **인터넷**: Docker 이미지 다운로드용 (최초 1회)

---

## 2. 빠른 시작 (5분)

### 2.1 프로젝트 클론 (또는 압축 해제)

```bash
# Git으로 클론
git clone https://github.com/your-org/RealFDS.git
cd RealFDS

# 또는 압축 파일 해제 후
cd RealFDS
```

### 2.2 환경 변수 설정 (선택적)

```bash
# .env 파일 생성 (기본값 사용 시 생략 가능)
cp .env.example .env

# 필요 시 값 수정
# KAFKA_BOOTSTRAP_SERVERS=kafka:9092
# TRANSACTION_GENERATION_INTERVAL_MS=100
# HIGH_VALUE_THRESHOLD=1000000
```

### 2.3 전체 시스템 실행

```bash
docker-compose up
```

**예상 시간**: 5분 (최초 실행 시 이미지 다운로드 포함)

**출력 예시**:
```
✅ Network realfds-network        Created
✅ Container realfds-zookeeper-1  Started
✅ Container realfds-kafka-1       Started
✅ Container realfds-transaction-generator-1  Started
✅ Container realfds-fraud-detector-1          Started
✅ Container realfds-alert-service-1           Started
✅ Container realfds-websocket-gateway-1       Started
✅ Container realfds-frontend-dashboard-1      Started
```

### 2.4 시스템 준비 확인

모든 서비스가 시작되면 다음 로그가 표시됩니다:

```
transaction-generator-1  | INFO: 서비스 시작됨, 거래 생성 시작...
fraud-detector-1         | INFO: Flink Job 실행 중, 탐지 시작...
alert-service-1          | INFO: Kafka Consumer 시작됨
websocket-gateway-1      | INFO: WebSocket Gateway 시작됨
frontend-dashboard-1     | INFO: Frontend Dashboard 시작됨, http://localhost:8083
```

### 2.5 웹 대시보드 접속

브라우저에서 다음 URL을 엽니다:
```
http://localhost:8083
```

**확인 사항**:
- ✅ "실시간 연결됨" 상태 표시
- ✅ 1분 이내에 알림 표시 시작
- ✅ 고액 거래, 해외 거래, 빈번한 거래 알림 확인

---

## 3. 검증 테스트

### 3.1 헬스 체크

```bash
# alert-service
curl http://localhost:8081/actuator/health
# 예상 응답: {"status":"UP"}

# websocket-gateway
curl http://localhost:8082/actuator/health
# 예상 응답: {"status":"UP"}
```

### 3.2 Kafka 토픽 확인

```bash
# 토픽 목록
docker exec -it realfds-kafka-1 kafka-topics --list --bootstrap-server kafka:9092
# 예상 출력: virtual-transactions, transaction-alerts

# 거래 메시지 확인
docker exec -it realfds-kafka-1 kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic virtual-transactions \
  --from-beginning \
  --max-messages 5
```

### 3.3 알림 REST API 확인

```bash
curl http://localhost:8081/api/alerts | jq .
```

**예상 응답**:
```json
[
  {
    "alertId": "...",
    "transactionId": "...",
    "userId": "user-3",
    "amount": 1250000,
    "ruleName": "HIGH_VALUE",
    "reason": "고액 거래 (100만원 초과): 1,250,000원",
    "severity": "HIGH",
    ...
  }
]
```

### 3.4 WebSocket 연결 테스트 (wscat)

```bash
# wscat 설치 (최초 1회)
npm install -g wscat

# WebSocket 연결
wscat -c ws://localhost:8082/ws/alerts

# 연결 확인 메시지 수신
< {"type":"connection","data":{"status":"connected",...}}

# 알림 수신 대기 (10초 이내)
< {"type":"alert","data":{...}}
```

---

## 4. 시스템 종료

### 4.1 정상 종료

```bash
# Ctrl+C 또는
docker-compose down
```

### 4.2 데이터 포함 완전 제거

```bash
docker-compose down -v  # 볼륨 포함 제거
```

---

## 5. 개발 모드 실행

### 5.1 특정 서비스만 실행

```bash
# Kafka + transaction-generator만 실행
docker-compose up zookeeper kafka transaction-generator

# 백엔드만 실행 (프론트엔드 별도 개발)
docker-compose up zookeeper kafka transaction-generator fraud-detector alert-service websocket-gateway
```

### 5.2 로그 확인

```bash
# 전체 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f fraud-detector

# 최근 100줄만
docker-compose logs --tail=100 fraud-detector
```

### 5.3 서비스 재시작

```bash
# 특정 서비스만 재시작
docker-compose restart fraud-detector

# 전체 재시작
docker-compose restart
```

---

## 6. 문제 해결

### 6.1 포트 충돌

**증상**:
```
Error: bind: address already in use
```

**해결**:
```bash
# 사용 중인 포트 확인
# Windows
netstat -ano | findstr :8081

# macOS/Linux
lsof -i :8081

# 프로세스 종료 후 재시도
```

### 6.2 메모리 부족

**증상**:
```
Error: OOMKilled
```

**해결**:
```bash
# Docker Desktop 설정에서 메모리 할당 증가 (8GB → 12GB)
# 또는 docker-compose.yml에서 메모리 제한 감소
```

### 6.3 Kafka 연결 실패

**증상**:
```
ERROR: Failed to connect to Kafka broker
```

**해결**:
```bash
# Kafka 컨테이너 상태 확인
docker ps | grep kafka

# Kafka 로그 확인
docker-compose logs kafka

# Kafka 재시작
docker-compose restart kafka

# 30초 대기 후 다른 서비스 재시작
docker-compose restart transaction-generator fraud-detector
```

### 6.4 프론트엔드 화면 안 뜸

**증상**:
```
http://localhost:8083 접속 불가
```

**해결**:
```bash
# frontend-dashboard 컨테이너 상태 확인
docker ps | grep frontend-dashboard

# 로그 확인
docker-compose logs frontend-dashboard

# 브라우저 캐시 삭제 후 재시도
# 또는 시크릿 모드에서 접속
```

---

## 7. 고급 설정

### 7.1 거래 생성 주기 변경

```yaml
# docker-compose.yml
services:
  transaction-generator:
    environment:
      - TRANSACTION_GENERATION_INTERVAL_MS=50  # 기본값: 100ms (초당 10개)
      # 50ms → 초당 20개, 10ms → 초당 100개
```

### 7.2 고액 거래 임계값 변경

```yaml
# docker-compose.yml
services:
  fraud-detector:
    environment:
      - HIGH_VALUE_THRESHOLD=500000  # 기본값: 1000000 (100만원)
```

### 7.3 알림 저장 개수 변경

```yaml
# docker-compose.yml
services:
  alert-service:
    environment:
      - MAX_ALERTS_CAPACITY=200  # 기본값: 100
```

---

## 8. 성능 측정

### 8.1 지연 시간 측정

```bash
# 측정 스크립트 실행
./scripts/measure-latency.sh

# 예상 출력:
# 평균 지연 시간: 2.5초
# p95 지연 시간: 4.2초
# p99 지연 시간: 5.8초
```

### 8.2 처리량 측정

```bash
# 1분간 생성된 거래 수
docker exec -it realfds-kafka-1 kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list kafka:9092 \
  --topic virtual-transactions \
  --time -1 | awk -F':' '{sum += $3} END {print "총 거래:", sum}'
```

---

## 9. 다음 단계

### 9.1 코드 탐색
- `transaction-generator/`: Python 거래 생성기
- `fraud-detector/`: Scala Flink 탐지 엔진
- `alert-service/`: Java Spring 알림 서비스
- `websocket-gateway/`: Java Spring WebSocket 게이트웨이
- `frontend-dashboard/`: React TypeScript 프론트엔드

### 9.2 새로운 탐지 규칙 추가
1. `fraud-detector/src/main/scala/com/realfds/detector/rules/` 에 새 규칙 파일 추가
2. `FraudDetectionJob.scala`에서 규칙 통합
3. `docker-compose build fraud-detector` 재빌드
4. `docker-compose up fraud-detector` 재시작

### 9.3 문서 참조
- [Architecture](../../docs/architecture.md): 아키텍처 설명
- [Development](../../docs/development.md): 개발 가이드
- [Troubleshooting](../../docs/troubleshooting.md): 문제 해결

---

## 10. 체크리스트

### 10.1 시스템 시작 체크리스트
- [ ] Docker Desktop 실행 중
- [ ] 포트 8081, 8082, 8083, 9092 사용 가능
- [ ] 최소 8GB RAM 여유
- [ ] `docker-compose up` 명령 실행
- [ ] 5분 이내 모든 서비스 시작 완료
- [ ] http://localhost:8083 접속 가능
- [ ] "실시간 연결됨" 상태 표시

### 10.2 정상 작동 체크리스트
- [ ] 1분 이내에 알림 표시 시작
- [ ] 고액 거래 알림 표시 (금액 > 100만원)
- [ ] 해외 거래 알림 표시 (국가 != KR)
- [ ] 빈번한 거래 알림 표시 (1분 내 5회 초과)
- [ ] WebSocket 연결 상태 "실시간 연결됨" 유지
- [ ] 헬스 체크 API 응답 "UP"

---

**Quick Start Guide 완료**: 개발자가 5분 내에 시스템 실행 및 검증 가능
