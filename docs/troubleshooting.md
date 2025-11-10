# 문제 해결 가이드: RealFDS

**작성일**: 2025-11-06
**목적**: RealFDS 실행 시 발생할 수 있는 일반적인 문제 및 해결 방법 안내

---

## 목차

1. [시스템 시작 문제](#1-시스템-시작-문제)
2. [네트워크 및 포트 문제](#2-네트워크-및-포트-문제)
3. [메모리 및 리소스 문제](#3-메모리-및-리소스-문제)
4. [Kafka 연결 문제](#4-kafka-연결-문제)
5. [서비스별 문제](#5-서비스별-문제)
6. [WebSocket 연결 문제](#6-websocket-연결-문제)
7. [성능 문제](#7-성능-문제)
8. [로그 확인 방법](#8-로그-확인-방법)
9. [일반적인 Docker 문제](#9-일반적인-docker-문제)
10. [FAQ](#10-faq)

---

## 1. 시스템 시작 문제

### 문제: `docker-compose up` 실행 시 오류 발생

**증상**:
```
ERROR: Couldn't connect to Docker daemon
```

**원인**: Docker Desktop이 실행되지 않음

**해결 방법**:
1. Docker Desktop 실행 확인
   - Windows/macOS: 시스템 트레이에서 Docker 아이콘 확인
   - Linux: `sudo systemctl status docker`
2. Docker Desktop이 완전히 시작될 때까지 대기 (1-2분)
3. 재시도: `docker-compose up -d`

---

### 문제: 일부 서비스만 시작됨

**증상**:
```
transaction-generator is up
kafka is up
fraud-detector exited with code 1
```

**원인**: 의존성 서비스(Kafka)가 완전히 준비되기 전에 fraud-detector가 시작됨

**해결 방법**:
1. 모든 서비스 중지
   ```bash
   docker-compose down
   ```

2. 인프라 서비스부터 순차적으로 시작
   ```bash
   # 1단계: Zookeeper, Kafka 시작
   docker-compose up -d zookeeper kafka

   # 2-3분 대기
   sleep 180

   # 2단계: Flink 클러스터 시작
   docker-compose up -d jobmanager taskmanager

   # 1분 대기
   sleep 60

   # 3단계: 나머지 서비스 시작
   docker-compose up -d
   ```

3. 로그 확인
   ```bash
   docker-compose logs -f fraud-detector
   ```

---

## 2. 네트워크 및 포트 문제

### 문제: 포트 충돌 (Port already in use)

**증상**:
```
Error starting userland proxy: listen tcp4 0.0.0.0:8083: bind: address already in use
```

**원인**: 해당 포트를 다른 프로세스가 사용 중

**해결 방법**:

#### Windows
```bash
# 포트 사용 중인 프로세스 확인
netstat -ano | findstr :8083

# PID로 프로세스 종료
taskkill /PID <PID> /F
```

#### macOS/Linux
```bash
# 포트 사용 중인 프로세스 확인
lsof -i :8083

# 프로세스 종료
kill -9 <PID>
```

#### 대안: 포트 변경
`.env` 파일에서 포트 변경:
```bash
# .env
FRONTEND_PORT=9083  # 8083 → 9083
```

`docker-compose.yml`에서도 해당 포트 변경:
```yaml
frontend-dashboard:
  ports:
    - "${FRONTEND_PORT:-9083}:8083"
```

---

### 문제: 서비스 간 통신 실패

**증상**:
```
websocket-gateway | Connection refused: alert-service:8081
```

**원인**: Docker 네트워크 문제 또는 서비스 이름 오타

**해결 방법**:
1. 네트워크 재생성
   ```bash
   docker-compose down
   docker network prune
   docker-compose up -d
   ```

2. 서비스명 확인
   - `docker-compose.yml`의 서비스명과 코드의 URL이 일치하는지 확인
   - 예: `http://alert-service:8081` (서비스명은 `alert-service`)

3. DNS 확인
   ```bash
   # websocket-gateway 컨테이너 내부에서 확인
   docker-compose exec websocket-gateway ping alert-service
   ```

---

## 3. 메모리 및 리소스 문제

### 문제: 메모리 부족 오류

**증상**:
```
fraud-detector | java.lang.OutOfMemoryError: Java heap space
```

**원인**: Docker Desktop의 메모리 할당이 부족

**해결 방법**:
1. Docker Desktop 메모리 할당 증가
   - **Windows/macOS**: Docker Desktop → Settings → Resources → Memory
   - 최소 8GB 권장, 가능하면 12GB 이상

2. 서비스별 메모리 제한 조정 (`docker-compose.yml`)
   ```yaml
   fraud-detector:
     mem_limit: 3g  # 2g → 3g로 증가
   ```

3. 불필요한 컨테이너 종료
   ```bash
   docker container prune
   docker image prune -a
   ```

---

### 문제: CPU 사용률 100%

**증상**: 시스템이 느려지고 팬이 강하게 돌아감

**원인**: Flink 또는 Kafka가 과도한 CPU 사용

**해결 방법**:
1. 거래 생성 주기 늘리기 (`.env`)
   ```bash
   TRANSACTION_GENERATION_INTERVAL_MS=5000  # 1000 → 5000 (5초)
   ```

2. Docker Desktop CPU 코어 수 조정
   - Docker Desktop → Settings → Resources → CPUs
   - 최소 4 Core 권장

3. 불필요한 로그 출력 줄이기
   - `logback.xml`: INFO → WARN 레벨로 변경

---

## 4. Kafka 연결 문제

### 문제: Kafka 연결 실패

**증상**:
```
transaction-generator | Failed to connect to Kafka broker: localhost:9092
```

**원인**: Kafka가 완전히 시작되지 않음 또는 네트워크 문제

**해결 방법**:
1. Kafka 시작 확인
   ```bash
   docker-compose logs kafka | grep "started (kafka.server.KafkaServer)"
   ```

   출력 예시:
   ```
   kafka | [KafkaServer id=1] started (kafka.server.KafkaServer)
   ```

2. Kafka가 시작될 때까지 대기 (2-3분 소요)

3. Kafka 헬스 체크
   ```bash
   docker-compose exec kafka kafka-broker-api-versions \
     --bootstrap-server localhost:9092
   ```

4. 수동으로 토픽 확인
   ```bash
   docker-compose exec kafka kafka-topics \
     --list \
     --bootstrap-server localhost:9092
   ```

   출력 예시:
   ```
   virtual-transactions
   transaction-alerts
   ```

---

### 문제: Kafka 토픽이 생성되지 않음

**증상**: 위 명령어 실행 시 빈 출력

**원인**: 자동 토픽 생성 실패

**해결 방법**:
수동으로 토픽 생성:
```bash
# virtual-transactions 토픽 생성
docker-compose exec kafka kafka-topics \
  --create \
  --topic virtual-transactions \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1

# transaction-alerts 토픽 생성
docker-compose exec kafka kafka-topics \
  --create \
  --topic transaction-alerts \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1
```

---

### 문제: Kafka Consumer가 메시지를 소비하지 않음

**증상**: 거래 데이터는 발행되지만 알림이 생성되지 않음

**원인**: Consumer Group Offset 문제

**해결 방법**:
1. Consumer Group 확인
   ```bash
   docker-compose exec kafka kafka-consumer-groups \
     --bootstrap-server localhost:9092 \
     --list
   ```

2. Consumer Group 상태 확인
   ```bash
   docker-compose exec kafka kafka-consumer-groups \
     --bootstrap-server localhost:9092 \
     --group fraud-detector-group \
     --describe
   ```

3. Offset 리셋 (주의: 모든 메시지 재처리)
   ```bash
   docker-compose exec kafka kafka-consumer-groups \
     --bootstrap-server localhost:9092 \
     --group fraud-detector-group \
     --reset-offsets \
     --to-earliest \
     --all-topics \
     --execute
   ```

---

## 5. 서비스별 문제

### transaction-generator

#### 문제: 거래가 생성되지 않음

**로그 확인**:
```bash
docker-compose logs -f transaction-generator
```

**흔한 오류**:
```
TypeError: 'NoneType' object is not callable
```

**해결**: Python 의존성 재설치 및 재빌드
```bash
docker-compose build --no-cache transaction-generator
docker-compose up -d transaction-generator
```

---

### fraud-detector

#### 문제: Flink Job이 시작되지 않음

**로그 확인**:
```bash
docker-compose logs -f fraud-detector
docker-compose logs -f jobmanager
docker-compose logs -f taskmanager
```

**Flink Web UI 접속**:
```
http://localhost:8081
```
- Running Jobs 탭에서 Job 상태 확인

**흔한 오류**:
```
ClassNotFoundException: com.realfds.detector.FraudDetectionJob
```

**해결**: JAR 파일 재빌드
```bash
cd fraud-detector
./gradlew clean build
docker-compose build fraud-detector
docker-compose up -d fraud-detector
```

---

### alert-service

#### 문제: 알림이 저장되지 않음

**헬스 체크**:
```bash
curl http://localhost:8081/actuator/health
```

**로그 확인**:
```bash
docker-compose logs -f alert-service
```

**흔한 오류**:
```
JsonParseException: Unexpected character
```

**원인**: Alert JSON 스키마 불일치

**해결**: 데이터 모델 확인 후 재빌드
```bash
cd alert-service
./gradlew clean build
docker-compose build alert-service
docker-compose up -d alert-service
```

---

### websocket-gateway

#### 문제: WebSocket 연결 실패

**헬스 체크**:
```bash
curl http://localhost:8082/actuator/health
```

**로그 확인**:
```bash
docker-compose logs -f websocket-gateway
```

**WebSocket 연결 테스트** (wscat):
```bash
npm install -g wscat
wscat -c ws://localhost:8082/ws/alerts
```

**흔한 오류**:
```
Connection refused
```

**해결**: 서비스 재시작
```bash
docker-compose restart websocket-gateway
```

---

### frontend-dashboard

#### 문제: 웹 페이지가 로드되지 않음

**브라우저 접속**:
```
http://localhost:8083
```

**로그 확인**:
```bash
docker-compose logs -f frontend-dashboard
```

**흔한 오류**:
```
404 Not Found
```

**해결**: Nginx 설정 확인 및 재빌드
```bash
cd frontend-dashboard
npm run build
docker-compose build frontend-dashboard
docker-compose up -d frontend-dashboard
```

---

## 6. WebSocket 연결 문제

### 문제: "연결 끊김" 상태가 계속 표시됨

**증상**: 프론트엔드에서 "연결 끊김" 표시, 재연결 시도 반복

**원인**:
1. websocket-gateway가 실행되지 않음
2. CORS 설정 문제
3. 네트워크 방화벽

**해결 방법**:

1. **websocket-gateway 상태 확인**
   ```bash
   docker-compose ps websocket-gateway
   ```

   상태가 "Up"인지 확인

2. **WebSocket 포트 확인**
   ```bash
   curl http://localhost:8082/actuator/health
   ```

3. **브라우저 개발자 도구 확인**
   - F12 → Console 탭
   - 오류 메시지 확인 (예: "WebSocket connection failed")

4. **방화벽 확인**
   - Windows Defender 또는 방화벽에서 포트 8082 허용

5. **WebSocket URL 확인**
   - 프론트엔드 코드: `ws://localhost:8082/ws/alerts` (올바른 형식)

---

## 7. 성능 문제

### 문제: 알림 표시가 너무 느림 (5초 이상)

**원인**: 종단 간 지연 시간 초과

**해결 방법**:

1. **지연 시간 측정**
   ```bash
   bash scripts/measure-latency.sh
   ```

2. **병목 구간 확인**
   - Kafka 지연: `docker-compose logs kafka`
   - Flink 처리 지연: Flink Web UI (http://localhost:8081)
   - WebSocket 폴링 간격: `application.yml`에서 `fixedDelay` 확인

3. **최적화**
   - WebSocket 폴링 주기 줄이기 (1000ms → 500ms)
   - Flink 병렬 처리 증가 (TaskManager 슬롯 추가)

---

### 문제: 시스템이 30분 이상 실행 시 크래시

**원인**: 메모리 누수 또는 리소스 부족

**해결 방법**:

1. **메모리 사용량 확인**
   ```bash
   docker stats
   ```

   출력 예시:
   ```
   CONTAINER          MEM USAGE / LIMIT
   fraud-detector     1.8GB / 2GB
   kafka              900MB / 1GB
   ```

2. **메모리 제한 증가**
   - `docker-compose.yml`에서 `mem_limit` 조정

3. **로그 로테이션 설정**
   - 과도한 로그 출력 방지

---

## 8. 로그 확인 방법

### 모든 서비스 로그

```bash
docker-compose logs -f
```

### 특정 서비스 로그

```bash
docker-compose logs -f transaction-generator
docker-compose logs -f fraud-detector
docker-compose logs -f alert-service
docker-compose logs -f websocket-gateway
docker-compose logs -f frontend-dashboard
```

### 최근 100줄만 확인

```bash
docker-compose logs --tail=100 fraud-detector
```

### 특정 키워드 검색

```bash
docker-compose logs fraud-detector | grep "ERROR"
docker-compose logs alert-service | grep "HIGH_VALUE"
```

### 로그 파일로 저장

```bash
docker-compose logs fraud-detector > fraud-detector.log
```

---

## 9. 일반적인 Docker 문제

### 문제: "No space left on device"

**원인**: Docker 이미지/볼륨이 디스크 공간을 가득 채움

**해결 방법**:
```bash
# 사용하지 않는 컨테이너 삭제
docker container prune -f

# 사용하지 않는 이미지 삭제
docker image prune -a -f

# 사용하지 않는 볼륨 삭제
docker volume prune -f

# 모든 사용하지 않는 리소스 삭제
docker system prune -a --volumes -f
```

---

### 문제: 이미지 빌드 실패

**증상**:
```
ERROR [internal] load metadata for docker.io/library/python:3.11-slim
```

**원인**: Docker Hub 연결 문제 또는 네트워크 오류

**해결 방법**:
1. 인터넷 연결 확인
2. Docker Desktop 재시작
3. 캐시 없이 재빌드
   ```bash
   docker-compose build --no-cache
   ```

---

### 문제: 컨테이너가 즉시 종료됨

**증상**:
```
fraud-detector exited with code 137
```

**원인**: 메모리 부족 (OOM Killed)

**해결 방법**:
1. Docker Desktop 메모리 증가
2. 서비스 메모리 제한 조정
   ```yaml
   fraud-detector:
     mem_limit: 3g
   ```

---

## 10. FAQ

### Q1: 시스템이 완전히 시작되는 데 얼마나 걸리나요?

**A**: 약 3-5분 소요됩니다.
- Zookeeper, Kafka: 2-3분
- Flink 클러스터: 1분
- 나머지 서비스: 1분

---

### Q2: 알림이 전혀 표시되지 않는데 어떻게 확인하나요?

**A**: 다음 순서로 확인:
1. **거래 생성 확인**
   ```bash
   docker-compose logs transaction-generator | grep "발행"
   ```

2. **Kafka 토픽 확인**
   ```bash
   docker-compose exec kafka kafka-console-consumer \
     --bootstrap-server localhost:9092 \
     --topic virtual-transactions \
     --max-messages 5
   ```

3. **Flink 알림 생성 확인**
   ```bash
   docker-compose logs fraud-detector | grep "Alert"
   ```

4. **alert-service 알림 수신 확인**
   ```bash
   docker-compose logs alert-service | grep "알림"
   ```

5. **프론트엔드 WebSocket 연결 확인**
   - 브라우저 F12 → Console 탭

---

### Q3: 특정 규칙(예: 고액 거래)만 테스트하고 싶은데 어떻게 하나요?

**A**: 환경 변수로 임계값 조정:
```bash
# .env
HIGH_VALUE_THRESHOLD=100000  # 10만원으로 낮춤 (테스트용)
```

재시작:
```bash
docker-compose down
docker-compose up -d
```

---

### Q4: 프론트엔드를 수정했는데 변경사항이 반영되지 않아요.

**A**: 브라우저 캐시 문제일 수 있습니다.
1. 브라우저 캐시 클리어 (Ctrl+Shift+Delete)
2. 하드 리프레시 (Ctrl+F5)
3. 컨테이너 재빌드
   ```bash
   docker-compose build frontend-dashboard
   docker-compose up -d frontend-dashboard
   ```

---

### Q5: 윈도우즈에서 `bash` 명령어가 작동하지 않아요.

**A**: Git Bash 또는 WSL2 사용:
- **Git Bash**: Git 설치 시 포함됨
- **WSL2**: Windows Subsystem for Linux 설치
  ```bash
  wsl --install
  ```

---

### Q6: Mac M1/M2에서 이미지 빌드가 느려요.

**A**: ARM64 아키텍처 호환성 문제일 수 있습니다.
1. Docker Desktop 최신 버전 사용
2. Rosetta 2 에뮬레이션 활성화
   - Docker Desktop → Settings → General → "Use Rosetta for x86/amd64 emulation"

---

## 추가 지원

문제가 해결되지 않는 경우:
1. 전체 로그 수집
   ```bash
   docker-compose logs > full-logs.txt
   ```

2. 시스템 정보 수집
   ```bash
   docker version
   docker-compose version
   docker info
   ```

3. GitHub Issues에 문제 보고 (로그 첨부)

---

**문제 해결 가이드 완료**: 일반적인 문제 및 해결 방법, 로그 확인, 디버깅 팁 제공
