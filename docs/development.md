# 개발 가이드: RealFDS

**작성일**: 2025-11-06
**목적**: 로컬 개발 환경 설정, 각 서비스 빌드 및 실행, 테스트, 새로운 탐지 규칙 추가 방법 안내

---

## 1. 로컬 개발 환경 설정

### 사전 요구사항

#### 필수 도구

- **Docker Desktop**: 최신 버전 (Windows/macOS/Linux)
  - [Docker 공식 사이트](https://www.docker.com/products/docker-desktop/)
  - 최소 메모리 할당: 8GB
  - 최소 CPU: 4 Core
- **Git**: 버전 관리
- **텍스트 에디터**: VS Code, IntelliJ IDEA 등

#### 선택적 도구 (서비스별 로컬 실행 시)

- **Python 3.11+**: transaction-generator 개발
  - pip, virtualenv/venv
- **Java 17+**: alert-service, websocket-gateway 개발
  - Gradle 7.6+
- **Scala 2.12**: fraud-detector 개발
  - SBT 1.9+ 또는 Gradle
- **Node.js 20+**: frontend-dashboard 개발
  - npm 또는 yarn

### 저장소 클론

```bash
git clone <repository-url>
cd RealFDS
```

### 환경 변수 설정

```bash
# .env.example 파일 복사
cp .env.example .env

# 필요시 .env 파일 수정
# (기본값으로도 로컬 실행 가능)
```

---

## 2. 전체 시스템 실행 (Docker Compose)

### 기본 실행

```bash
# 모든 서비스 백그라운드 실행
docker-compose up -d

# 로그 확인 (전체)
docker-compose logs -f

# 특정 서비스 로그만 확인
docker-compose logs -f transaction-generator
```

### 서비스 재시작

특정 서비스만 재빌드 및 재시작:

```bash
# transaction-generator 재빌드 및 재시작
docker-compose up -d --build transaction-generator

# fraud-detector 재빌드 및 재시작
docker-compose up -d --build fraud-detector
```

### 시스템 종료

```bash
# 모든 서비스 중지 (데이터 유지)
docker-compose down

# 모든 서비스 중지 + 볼륨 삭제 (데이터 초기화)
docker-compose down -v
```

---

## 3. 각 서비스별 로컬 개발

### 3.1 transaction-generator (Python)

#### 디렉토리 구조

```
transaction-generator/
├── Dockerfile
├── requirements.txt
├── src/
│   ├── main.py              # 진입점
│   ├── generator.py         # 거래 생성 로직
│   ├── kafka_producer.py    # Kafka Producer
│   └── models.py            # Transaction 모델
└── tests/
    ├── test_generator.py
    └── test_kafka_producer.py
```

#### 로컬 실행

```bash
cd transaction-generator

# 가상 환경 생성 및 활성화
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt

# 환경 변수 설정
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export TRANSACTION_GENERATION_INTERVAL_MS=1000

# 실행 (Kafka가 미리 실행 중이어야 함)
python src/main.py
```

#### 테스트 실행

```bash
# 단위 테스트
pytest tests/ -v

# 커버리지 측정
pytest tests/ --cov=src --cov-report=html

# 커버리지 리포트: htmlcov/index.html
```

#### 코드 스타일 검사

```bash
# Flake8 (PEP 8 검사)
flake8 src/ tests/

# Black (자동 포맷팅)
black src/ tests/
```

---

### 3.2 fraud-detector (Flink + Scala)

#### 디렉토리 구조

```
fraud-detector/
├── Dockerfile
├── build.gradle
├── src/
│   ├── main/
│   │   ├── scala/com/realfds/detector/
│   │   │   ├── FraudDetectionJob.scala
│   │   │   ├── models/
│   │   │   │   ├── Transaction.scala
│   │   │   │   └── Alert.scala
│   │   │   ├── rules/
│   │   │   │   ├── HighValueRule.scala
│   │   │   │   ├── ForeignCountryRule.scala
│   │   │   │   └── HighFrequencyRule.scala
│   │   │   └── serde/
│   │   │       ├── TransactionDeserializationSchema.scala
│   │   │       └── AlertSerializationSchema.scala
│   │   └── resources/
│   │       ├── logback.xml
│   │       └── application.conf
│   └── test/
│       └── scala/com/realfds/detector/rules/
│           ├── HighValueRuleSpec.scala
│           ├── ForeignCountryRuleSpec.scala
│           └── HighFrequencyRuleSpec.scala
└── README.md
```

#### 로컬 빌드

```bash
cd fraud-detector

# Gradle로 빌드
./gradlew clean build

# JAR 파일 생성: build/libs/fraud-detector-1.0.0.jar
```

#### 로컬 실행 (Flink 클러스터 필요)

```bash
# Flink 로컬 클러스터 시작 (docker-compose 사용)
docker-compose up -d jobmanager taskmanager kafka

# Flink Job 제출
docker-compose exec jobmanager flink run \
  /opt/flink/usrlib/fraud-detector-1.0.0.jar
```

#### 테스트 실행

```bash
# ScalaTest 단위 테스트
./gradlew test

# 테스트 리포트: build/reports/tests/test/index.html
```

#### Flink Web UI 확인

```
http://localhost:8081
```

- Running Jobs, Completed Jobs 확인
- TaskManager 상태 확인
- 메트릭 확인

---

### 3.3 alert-service (Spring Boot)

#### 디렉토리 구조

```
alert-service/
├── Dockerfile
├── build.gradle
├── src/
│   ├── main/
│   │   ├── java/com/realfds/alert/
│   │   │   ├── AlertServiceApplication.java
│   │   │   ├── config/
│   │   │   │   ├── KafkaConfig.java
│   │   │   │   └── WebFluxConfig.java
│   │   │   ├── consumer/
│   │   │   │   └── AlertConsumer.java
│   │   │   ├── repository/
│   │   │   │   └── AlertRepository.java
│   │   │   ├── service/
│   │   │   │   └── AlertService.java
│   │   │   ├── controller/
│   │   │   │   ├── HealthController.java
│   │   │   │   └── AlertController.java
│   │   │   └── model/
│   │   │       └── Alert.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── logback-spring.xml
│   └── test/
│       └── java/com/realfds/alert/
│           ├── consumer/AlertConsumerTest.java
│           ├── repository/AlertRepositoryTest.java
│           └── service/AlertServiceTest.java
└── README.md
```

#### 로컬 실행

```bash
cd alert-service

# Gradle로 빌드
./gradlew clean build

# Spring Boot 실행 (Kafka가 미리 실행 중이어야 함)
./gradlew bootRun

# 또는 JAR 파일 직접 실행
java -jar build/libs/alert-service-1.0.0.jar
```

#### 테스트 실행

```bash
# 단위 테스트 + 통합 테스트
./gradlew test

# 커버리지 측정 (JaCoCo)
./gradlew jacocoTestReport

# 리포트: build/reports/jacoco/test/html/index.html
```

#### API 테스트

```bash
# 헬스 체크
curl http://localhost:8081/actuator/health

# 알림 목록 조회
curl http://localhost:8081/api/alerts
```

---

### 3.4 websocket-gateway (Spring Boot)

#### 디렉토리 구조

```
websocket-gateway/
├── Dockerfile
├── build.gradle
├── src/
│   ├── main/
│   │   ├── java/com/realfds/gateway/
│   │   │   ├── WebSocketGatewayApplication.java
│   │   │   ├── config/
│   │   │   │   ├── WebSocketConfig.java
│   │   │   │   └── RestClientConfig.java
│   │   │   ├── handler/
│   │   │   │   └── AlertWebSocketHandler.java
│   │   │   ├── service/
│   │   │   │   ├── AlertStreamService.java
│   │   │   │   └── BroadcastService.java
│   │   │   └── controller/
│   │   │       └── HealthController.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── logback-spring.xml
│   └── test/
│       └── java/com/realfds/gateway/
│           ├── handler/AlertWebSocketHandlerTest.java
│           └── service/BroadcastServiceTest.java
└── README.md
```

#### 로컬 실행

```bash
cd websocket-gateway

# Gradle로 빌드
./gradlew clean build

# Spring Boot 실행
./gradlew bootRun
```

#### WebSocket 테스트 (wscat)

```bash
# wscat 설치 (Node.js 필요)
npm install -g wscat

# WebSocket 연결 테스트
wscat -c ws://localhost:8082/ws/alerts

# 연결 성공 시 실시간 알림 수신
```

---

### 3.5 frontend-dashboard (React + TypeScript)

#### 디렉토리 구조

```
frontend-dashboard/
├── Dockerfile
├── package.json
├── vite.config.ts
├── tsconfig.json
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── components/
│   │   ├── AlertList.tsx
│   │   ├── AlertItem.tsx
│   │   ├── ConnectionStatus.tsx
│   │   └── Header.tsx
│   ├── hooks/
│   │   └── useWebSocket.ts
│   ├── types/
│   │   ├── alert.ts
│   │   └── connectionStatus.ts
│   ├── utils/
│   │   └── formatter.ts
│   └── styles/
│       └── App.css
├── tests/
│   ├── components/
│   │   ├── AlertList.test.tsx
│   │   └── AlertItem.test.tsx
│   └── hooks/
│       └── useWebSocket.test.ts
└── README.md
```

#### 로컬 실행

```bash
cd frontend-dashboard

# 의존성 설치
npm install

# 개발 서버 실행 (HMR 지원)
npm run dev

# 브라우저 자동 오픈: http://localhost:8083
```

#### 빌드 및 프리뷰

```bash
# 프로덕션 빌드
npm run build

# 빌드된 파일 프리뷰
npm run preview
```

#### 테스트 실행

```bash
# Vitest 단위 테스트
npm run test

# 커버리지 측정
npm run test:coverage

# 리포트: coverage/index.html
```

#### 코드 스타일 검사

```bash
# ESLint
npm run lint

# Prettier (자동 포맷팅)
npm run format
```

---

## 4. 새로운 탐지 규칙 추가하기

### 예시: "다중 국가 거래" 규칙 추가

**목표**: 동일 사용자가 10분 내에 3개 이상의 서로 다른 국가에서 거래 시 알림 생성

### 4.1 데이터 모델 확인

`specs/001-realtime-fds/data-model.md` 참고:
- Transaction 필드: `userId`, `countryCode`, `timestamp`
- Alert ruleName: 새로운 규칙명 "MULTI_COUNTRY" 추가 예정

### 4.2 fraud-detector에 규칙 추가

#### 4.2.1 규칙 클래스 생성

```scala
// fraud-detector/src/main/scala/com/realfds/detector/rules/MultiCountryRule.scala

package com.realfds.detector.rules

import com.realfds.detector.models.{Transaction, Alert}
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

import java.util.UUID
import scala.collection.mutable

/**
 * 다중 국가 거래 탐지 규칙
 *
 * 조건: 10분 윈도우 내에 동일 사용자가 3개 이상의 서로 다른 국가에서 거래
 */
class MultiCountryRule extends KeyedProcessFunction[String, Transaction, Alert] {

  // 사용자별 국가 집합 상태 (10분 내)
  private var countryState: ValueState[mutable.Set[String]] = _

  override def open(parameters: Configuration): Unit = {
    val descriptor = new ValueStateDescriptor[mutable.Set[String]](
      "country-state",
      classOf[mutable.Set[String]]
    )
    countryState = getRuntimeContext.getState(descriptor)
  }

  override def processElement(
    transaction: Transaction,
    ctx: KeyedProcessFunction[String, Transaction, Alert]#Context,
    out: Collector[Alert]
  ): Unit = {
    val currentTime = transaction.timestamp
    val windowStart = currentTime - (10 * 60 * 1000) // 10분 전

    // 현재 상태 가져오기 (없으면 빈 집합)
    val countries = Option(countryState.value()).getOrElse(mutable.Set.empty[String])

    // 현재 국가 추가
    countries.add(transaction.countryCode)

    // 상태 업데이트
    countryState.update(countries)

    // 10분 후 상태 정리 타이머 등록
    ctx.timerService().registerEventTimeTimer(currentTime + (10 * 60 * 1000))

    // 조건 확인: 3개 이상의 서로 다른 국가
    if (countries.size >= 3) {
      val alert = createAlert(transaction, countries.toSet)
      out.collect(alert)
    }
  }

  override def onTimer(
    timestamp: Long,
    ctx: KeyedProcessFunction[String, Transaction, Alert]#OnTimerContext,
    out: Collector[Alert]
  ): Unit = {
    // 10분 경과 시 상태 초기화
    countryState.clear()
  }

  private def createAlert(transaction: Transaction, countries: Set[String]): Alert = {
    Alert(
      schemaVersion = "1.0",
      alertId = UUID.randomUUID().toString,
      originalTransaction = transaction,
      ruleType = "STATEFUL_RULE",
      ruleName = "MULTI_COUNTRY",
      reason = s"다중 국가 거래 (10분 내 ${countries.size}개 국가): ${countries.mkString(", ")}",
      severity = "HIGH",
      alertTimestamp = System.currentTimeMillis().toString
    )
  }
}
```

#### 4.2.2 FraudDetectionJob에 규칙 통합

```scala
// fraud-detector/src/main/scala/com/realfds/detector/FraudDetectionJob.scala

// 기존 코드에 추가

val multiCountryAlerts = transactions
  .keyBy(_.userId)  // userId별로 그룹화
  .process(new MultiCountryRule())

// 4개 스트림을 union
val allAlerts = highValueAlerts
  .union(foreignCountryAlerts)
  .union(highFrequencyAlerts)
  .union(multiCountryAlerts)  // 새 규칙 추가
```

#### 4.2.3 단위 테스트 작성

```scala
// fraud-detector/src/test/scala/com/realfds/detector/rules/MultiCountryRuleSpec.scala

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MultiCountryRuleSpec extends AnyFlatSpec with Matchers {

  "MultiCountryRule" should "다중 국가 거래를 탐지해야 함" in {
    // Given: user-1이 10분 내에 KR, US, JP에서 거래
    val tx1 = createTransaction("user-1", "KR", timestamp = 1000L)
    val tx2 = createTransaction("user-1", "US", timestamp = 2000L)
    val tx3 = createTransaction("user-1", "JP", timestamp = 3000L)

    // When: 규칙 적용
    val harness = new KeyedOneInputStreamOperatorTestHarness(
      new MultiCountryRule(),
      (t: Transaction) => t.userId,
      Types.STRING
    )
    harness.open()
    harness.processElement(tx1, 1000L)
    harness.processElement(tx2, 2000L)
    harness.processElement(tx3, 3000L)

    // Then: Alert 생성 확인
    val output = harness.extractOutputValues()
    output should have size 1
    output.get(0).ruleName shouldBe "MULTI_COUNTRY"
    output.get(0).severity shouldBe "HIGH"
  }

  it should "2개 국가만 방문 시 알림을 생성하지 않아야 함" in {
    // Given: user-2가 KR, US만 거래
    val tx1 = createTransaction("user-2", "KR", timestamp = 1000L)
    val tx2 = createTransaction("user-2", "US", timestamp = 2000L)

    // When
    // ... (하네스 설정)

    // Then: Alert 미생성
    val output = harness.extractOutputValues()
    output should be(empty)
  }
}
```

### 4.3 데이터 모델 업데이트

`specs/001-realtime-fds/data-model.md`에 새 규칙 추가:

```markdown
#### 4.4 MULTI_COUNTRY (다중 국가 거래)
- **유형**: STATEFUL_RULE (상태 있음)
- **조건**: 10분 윈도우 내 동일 userId의 거래가 3개 이상의 서로 다른 국가
- **심각도**: HIGH
- **reason 템플릿**: `"다중 국가 거래 (10분 내 {count}개 국가): {countries}"`
- **윈도우**: 10분 슬라이딩 윈도우
```

### 4.4 테스트 및 검증

```bash
# 1. fraud-detector 테스트 실행
cd fraud-detector
./gradlew test

# 2. Docker 이미지 재빌드
docker-compose build fraud-detector

# 3. 전체 시스템 재시작
docker-compose up -d

# 4. 로그 확인
docker-compose logs -f fraud-detector

# 5. 알림 확인 (Kafka 토픽)
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic transaction-alerts \
  --from-beginning | grep "MULTI_COUNTRY"
```

---

## 5. 테스트 전략

### 5.1 단위 테스트

**각 서비스별 단위 테스트 실행**:

```bash
# transaction-generator
cd transaction-generator && pytest tests/ --cov=src

# fraud-detector
cd fraud-detector && ./gradlew test

# alert-service
cd alert-service && ./gradlew test

# websocket-gateway
cd websocket-gateway && ./gradlew test

# frontend-dashboard
cd frontend-dashboard && npm run test
```

**커버리지 목표**: ≥70% (Constitution V 준수)

### 5.2 통합 테스트

**alert-service와 websocket-gateway**:
- TestContainers로 Kafka 컨테이너 사용
- E2E 시나리오 검증

```bash
cd alert-service
./gradlew integrationTest
```

### 5.3 E2E 테스트

**전체 파이프라인 검증**:

```bash
# E2E 테스트 스크립트 실행
bash scripts/test-e2e.sh
```

**검증 항목**:
- 거래 생성 → Kafka 발행
- Flink 탐지 → Alert 생성
- alert-service 저장
- WebSocket 브로드캐스트
- 브라우저 수신

### 5.4 성능 테스트

**지연 시간 측정**:

```bash
bash scripts/measure-latency.sh
```

**목표**:
- 평균 <3초
- p95 <5초

---

## 6. 코드 품질 관리

### 6.1 코딩 스타일

**Python** (transaction-generator):
- PEP 8 준수
- Black 자동 포맷팅
- Flake8 린팅

**Scala** (fraud-detector):
- Scalafmt 자동 포맷팅
- Scalafix 린팅

**Java** (alert-service, websocket-gateway):
- Google Java Style Guide
- Checkstyle 플러그인

**TypeScript** (frontend-dashboard):
- ESLint (Airbnb 스타일)
- Prettier 자동 포맷팅

### 6.2 품질 기준 (Constitution V)

- **최대 함수 길이**: 50줄
- **최대 파일 길이**: 300줄
- **서술적인 변수/함수명**: 축약어 금지
- **한국어 주석**: 모든 클래스, 함수에 한국어 주석

### 6.3 Git 워크플로우

**브랜치 전략**:
- `main`: 프로덕션 준비 코드
- `001-realtime-fds`: Feature 브랜치 (현재)

**커밋 메시지** (Conventional Commits + 한국어):
```
feat: transaction-generator 거래 생성 로직 구현
test: HighValueRule 단위 테스트 작성
docs: README.md 빠른 시작 가이드 작성
fix: Kafka 연결 실패 시 재시도 로직 추가
refactor: AlertService 코드 정리 및 주석 개선
```

**커밋 전 체크리스트**:
- [ ] 테스트 통과
- [ ] 린터 통과
- [ ] 한국어 주석 작성
- [ ] 함수 길이 ≤50줄 확인

---

## 7. 디버깅 팁

### 7.1 Kafka 메시지 확인

```bash
# 거래 데이터 확인
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic virtual-transactions \
  --from-beginning \
  --max-messages 10

# 알림 데이터 확인
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic transaction-alerts \
  --from-beginning \
  --max-messages 10
```

### 7.2 Flink Job 디버깅

```bash
# Flink Web UI 접속
# http://localhost:8081

# JobManager 로그 확인
docker-compose logs -f jobmanager

# TaskManager 로그 확인
docker-compose logs -f taskmanager
```

### 7.3 Spring Boot 디버깅

**IntelliJ IDEA**:
1. Run → Edit Configurations
2. Add New Configuration → Spring Boot
3. Main class: `com.realfds.alert.AlertServiceApplication`
4. Environment variables: `SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092`
5. Debug 모드로 실행

**VS Code**:
```json
// .vscode/launch.json
{
  "type": "java",
  "name": "Debug alert-service",
  "request": "launch",
  "mainClass": "com.realfds.alert.AlertServiceApplication",
  "env": {
    "SPRING_KAFKA_BOOTSTRAP_SERVERS": "localhost:9092"
  }
}
```

### 7.4 React 디버깅

**Chrome DevTools**:
1. F12 → Console 탭
2. Network 탭 → WS 필터 (WebSocket 연결 확인)
3. React DevTools 확장 프로그램 설치

**VS Code**:
```json
// .vscode/launch.json
{
  "type": "chrome",
  "request": "launch",
  "name": "Debug React App",
  "url": "http://localhost:8083",
  "webRoot": "${workspaceFolder}/frontend-dashboard/src"
}
```

---

## 8. CI/CD (추후 확장)

**현재 범위**: 로컬 개발 및 Docker Compose 배포만

**프로덕션 환경 권장사항** (Out of Scope):
- **CI**: GitHub Actions, Jenkins
  - 자동 테스트 실행
  - 코드 품질 검사 (SonarQube)
  - Docker 이미지 빌드 및 푸시 (Docker Hub, ECR)
- **CD**: ArgoCD, Helm
  - Kubernetes 클러스터 배포
  - Blue-Green 배포
  - 롤백 전략

---

## 9. 참고 문서

- [아키텍처 설명](architecture.md)
- [문제 해결 가이드](troubleshooting.md)
- [데이터 모델 상세](../specs/001-realtime-fds/data-model.md)
- [API 계약서](../specs/001-realtime-fds/contracts/)
- [Apache Flink 공식 문서](https://nightlies.apache.org/flink/flink-docs-release-1.18/)
- [Spring WebFlux 공식 문서](https://docs.spring.io/spring-framework/reference/web/webflux.html)

---

**개발 가이드 완료**: 로컬 환경 설정, 각 서비스 빌드/실행, 새로운 탐지 규칙 추가, 테스트, 디버깅 방법 설명
