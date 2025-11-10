# Tasks: 실시간 금융 거래 탐지 시스템 (RealFDS)

**Feature Branch**: `001-realtime-fds`
**Created**: 2025-11-06
**Input**: Design documents from `/specs/001-realtime-fds/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

---

**⚠️ Constitution 준수 필수 사항**:

1. **테스트 우선 (Constitution V)**:
   - 단위 테스트 ≥70% 커버리지 필수
   - 통합 테스트는 모든 탐지 규칙에 대해 필수
   - Given-When-Then 구조 사용
   - 테스트는 구현 **전에** 작성하고 실패 확인 후 구현

2. **관찰 가능성 (Constitution V)**:
   - 모든 서비스에 구조화된 로깅 (SLF4J + JSON / Python Loguru) 필수
   - 헬스 체크 엔드포인트 (`/actuator/health`) 필수
   - 중요 비즈니스 이벤트 로깅 필수

3. **한국어 우선 (Constitution VI)**:
   - 모든 코드 주석은 한국어
   - 커밋 메시지는 Conventional Commits + 한국어
   - 문서는 한국어로 작성

4. **품질 표준 (Constitution V)**:
   - 최대 함수 길이: 50줄
   - 최대 파일 길이: 300줄
   - 서술적인 변수/함수 이름 사용

---

**Tests**: Tests are MANDATORY for this project (Constitution V requires ≥70% coverage)

**Organization**: 사용자 요청에 따라 3단계 개발 접근법을 따릅니다:
- **1단계**: 데이터 파이프라인 코어 구축 (Backend Foundation)
- **2단계**: 실시간 알림 전송 및 프론트엔드 (UI & Real-time Delivery)
- **3단계**: 전체 기능 완성 및 고도화 (Full Feature & Polish)

---

## 개발 1단계: 데이터 파이프라인 코어 구축 (Backend Foundation)

**목표**: 데이터가 생성되어 Kafka를 통해 Flink에서 처리되고, 다시 Kafka를 통해 alert-service까지 전달되는 핵심 흐름을 먼저 완성한다.

**검증 기준**:
- `docker-compose up`으로 Kafka, transaction-generator, fraud-detector, alert-service 실행
- 거래 데이터가 `virtual-transactions` 토픽에 발행됨
- fraud-detector가 고액 거래를 탐지하여 `transaction-alerts` 토픽으로 알림 발행
- alert-service가 알림을 수신하고 로그에 출력

---

### Phase 1-1: 공통 인프라 설정

**목표**: Kafka, Zookeeper, Flink 클러스터 기본 설정 추가

- [X] T001 프로젝트 루트 디렉토리 구조 생성
  - RealFDS/transaction-generator/, fraud-detector/, alert-service/, websocket-gateway/, frontend-dashboard/
  - RealFDS/scripts/, docs/
  - 한국어 주석으로 디렉토리 목적 설명하는 README.md 생성

- [X] T002 docker-compose.yml 파일 생성 및 Zookeeper 서비스 정의
  - RealFDS/docker-compose.yml
  - Zookeeper 3.8.3 이미지 사용
  - ZOOKEEPER_CLIENT_PORT: 2181
  - mem_limit: 256m

- [X] T003 [P] docker-compose.yml에 Kafka 서비스 정의
  - Kafka 3.6.1 이미지 (confluentinc/cp-kafka:7.5.0)
  - KAFKA_ZOOKEEPER_CONNECT, KAFKA_ADVERTISED_LISTENERS 설정
  - mem_limit: 1g
  - depends_on: zookeeper

- [X] T004 [P] docker-compose.yml에 Flink JobManager 서비스 정의
  - Flink 1.18.1 이미지 (flink:1.18-scala_2.12-java11)
  - command: jobmanager
  - RocksDB State Backend 볼륨 마운트 설정
  - mem_limit: 1g

- [X] T005 [P] docker-compose.yml에 Flink TaskManager 서비스 정의
  - Flink 1.18.1 이미지
  - command: taskmanager
  - depends_on: jobmanager
  - mem_limit: 1g

- [X] T006 [P] .env.example 파일 생성 및 환경 변수 템플릿 작성
  - RealFDS/.env.example
  - KAFKA_BOOTSTRAP_SERVERS, TRANSACTION_GENERATION_INTERVAL_MS, HIGH_VALUE_THRESHOLD 등
  - 한국어 주석으로 각 환경 변수 설명

- [X] T007 [P] .gitignore 파일 생성
  - RealFDS/.gitignore
  - Python (__pycache__, *.pyc, venv/), Java (target/, *.class), Node (node_modules/, dist/), IDE 설정 제외

- [X] T008 docker-compose.yml에 네트워크 설정 추가
  - networks: realfds-network 정의
  - 모든 서비스가 동일 네트워크 사용

**Checkpoint**: Docker Compose 파일로 인프라 컨테이너(Zookeeper, Kafka, Flink) 실행 가능

---

### Phase 1-2: transaction-generator 구현

**목표**: Python 프로젝트 설정, 거래 생성 로직, Kafka Producer 구현 및 Dockerfile 작성

- [X] T009 transaction-generator 디렉토리 및 기본 구조 생성
  - transaction-generator/src/, tests/, requirements.txt, Dockerfile, README.md
  - 한국어로 서비스 목적 설명하는 README.md 작성

- [X] T010 [P] requirements.txt 작성 및 의존성 정의
  - transaction-generator/requirements.txt
  - confluent-kafka, python-dotenv, faker (가상 데이터 생성용)
  - pytest, pytest-kafka (테스트용)

- [X] T011 [P] Transaction 데이터 모델 정의
  - transaction-generator/src/models.py
  - Transaction 클래스 (transactionId, userId, amount, currency, countryCode, timestamp, schemaVersion)
  - 한국어 주석으로 필드 설명
  - to_dict() 메서드로 JSON 직렬화

- [X] T012 거래 생성 로직 구현
  - transaction-generator/src/generator.py
  - generate_transaction() 함수: 무작위 Transaction 객체 생성
  - 금액: 정규 분포 (평균 300,000원, 표준편차 200,000원, 1,000~1,500,000 범위)
  - 국가: 80% KR, 10% US, 5% JP, 5% CN
  - 사용자: user-1 ~ user-10 균등 분포
  - 한국어 주석으로 로직 설명

- [X] T013 Kafka Producer 구현
  - transaction-generator/src/kafka_producer.py
  - TransactionProducer 클래스
  - __init__: Kafka 설정 및 Producer 초기화
  - send_transaction(): Transaction을 JSON으로 직렬화하여 `virtual-transactions` 토픽으로 발행
  - 에러 핸들링 (Kafka 연결 실패, 메시지 발행 실패)
  - 한국어 주석으로 구현 설명

- [X] T014 메인 진입점 구현
  - transaction-generator/src/main.py
  - 환경 변수 로드 (KAFKA_BOOTSTRAP_SERVERS, TRANSACTION_GENERATION_INTERVAL_MS)
  - 무한 루프에서 주기적으로 거래 생성 및 Kafka로 발행
  - INFO 레벨 로깅 (거래 생성 시 transactionId, userId, amount 출력)
  - 한국어 로그 메시지

- [X] T015 [P] 단위 테스트 작성 - generator.py
  - transaction-generator/tests/test_generator.py
  - test_generate_transaction_fields(): 모든 필수 필드 존재 확인
  - test_amount_in_range(): 금액이 1,000~1,500,000 범위인지 확인
  - test_country_code_valid(): 국가 코드가 KR, US, JP, CN 중 하나인지 확인
  - test_user_id_valid(): userId가 user-1 ~ user-10 범위인지 확인
  - Given-When-Then 구조 사용, 한국어 주석

- [X] T016 [P] 단위 테스트 작성 - kafka_producer.py
  - transaction-generator/tests/test_kafka_producer.py
  - test_send_transaction_success(): Mock Producer로 정상 발행 확인
  - test_send_transaction_serialization(): JSON 직렬화 검증
  - test_send_transaction_kafka_error(): Kafka 연결 실패 시 에러 핸들링 확인
  - Given-When-Then 구조 사용, 한국어 주석

- [X] T017 Dockerfile 작성
  - transaction-generator/Dockerfile
  - 베이스 이미지: python:3.11-slim
  - requirements.txt 복사 및 의존성 설치
  - 소스 코드 복사
  - CMD: python src/main.py
  - 한국어 주석으로 단계 설명

- [X] T018 docker-compose.yml에 transaction-generator 서비스 추가
  - build: ./transaction-generator
  - depends_on: kafka
  - environment: KAFKA_BOOTSTRAP_SERVERS, TRANSACTION_GENERATION_INTERVAL_MS
  - mem_limit: 256m

**Checkpoint**: transaction-generator가 Kafka로 거래 데이터 발행 (kafka-console-consumer로 확인)

---

### Phase 1-3: fraud-detector 구현

**목표**: Flink (Scala) 프로젝트 설정, `transactions` 토픽 구독, 간단한 필터링(예: 고액 거래만) 로직 구현, `alerts` 토픽으로 발행, Dockerfile 작성

- [X] T019 fraud-detector 디렉토리 및 Gradle 프로젝트 생성
  - fraud-detector/build.gradle, settings.gradle, src/main/scala/, src/test/scala/, README.md
  - Scala 2.12, Flink 1.18.1 의존성 추가
  - 한국어로 서비스 목적 설명하는 README.md 작성

- [X] T020 [P] build.gradle 작성 및 의존성 정의
  - fraud-detector/build.gradle
  - Flink Scala API, Flink Kafka Connector, Jackson (JSON 처리)
  - ScalaTest (테스트용)
  - 한국어 주석으로 의존성 설명

- [X] T021 [P] Transaction 케이스 클래스 정의
  - fraud-detector/src/main/scala/com/realfds/detector/models/Transaction.scala
  - 필드: schemaVersion, transactionId, userId, amount, currency, countryCode, timestamp
  - 한국어 주석으로 필드 설명

- [X] T022 [P] Alert 케이스 클래스 정의
  - fraud-detector/src/main/scala/com/realfds/detector/models/Alert.scala
  - 필드: schemaVersion, alertId, originalTransaction, ruleType, ruleName, reason, severity, alertTimestamp
  - 한국어 주석으로 필드 설명

- [X] T023 Kafka 직렬화/역직렬화 스키마 구현
  - fraud-detector/src/main/scala/com/realfds/detector/serde/TransactionDeserializationSchema.scala
  - Jackson ObjectMapper로 JSON → Transaction 변환
  - 한국어 주석으로 구현 설명

- [X] T024 [P] Kafka 직렬화 스키마 구현
  - fraud-detector/src/main/scala/com/realfds/detector/serde/AlertSerializationSchema.scala
  - Jackson ObjectMapper로 Alert → JSON 변환
  - 한국어 주석으로 구현 설명

- [X] T025 HighValueRule 구현 (고액 거래 탐지)
  - fraud-detector/src/main/scala/com/realfds/detector/rules/HighValueRule.scala
  - FilterFunction[Transaction] 상속
  - filter() 메서드: amount > 1,000,000 조건 확인
  - 한국어 주석으로 비즈니스 로직 설명

- [X] T026 HighValueRule Alert 생성 로직 구현
  - fraud-detector/src/main/scala/com/realfds/detector/rules/HighValueRule.scala
  - toAlert() 메서드: Transaction → Alert 변환
  - ruleName: "HIGH_VALUE", ruleType: "SIMPLE_RULE", severity: "HIGH"
  - reason: "고액 거래 (100만원 초과): {amount}원"
  - 한국어 주석으로 설명

- [X] T027 [P] 단위 테스트 작성 - HighValueRule
  - fraud-detector/src/test/scala/com/realfds/detector/rules/HighValueRuleTest.scala
  - test_filter_high_value_transaction(): 100만원 초과 거래 필터링 확인
  - test_filter_normal_transaction(): 100만원 이하 거래 미필터링 확인
  - test_to_alert_fields(): Alert 필드 정확성 확인
  - Given-When-Then 구조 사용, 한국어 주석

- [X] T028 Flink Job 진입점 구현 (1단계: 고액 거래만)
  - fraud-detector/src/main/scala/com/realfds/detector/FraudDetectionJob.scala
  - StreamExecutionEnvironment 초기화
  - Kafka Source: `virtual-transactions` 토픽 구독
  - HighValueRule 적용
  - Kafka Sink: `transaction-alerts` 토픽으로 발행
  - 워터마크 전략 (5초 지연 허용)
  - 체크포인트 설정 (60초마다)
  - 한국어 주석으로 Flink Job 구조 설명

- [X] T029 application.conf 설정 파일 작성
  - fraud-detector/src/main/resources/application.conf
  - Kafka 브로커 주소, 토픽명, 컨슈머 그룹 ID
  - 환경 변수로 오버라이드 가능하도록 설정
  - 한국어 주석으로 설정 설명

- [X] T030 [P] logback.xml 설정 파일 작성
  - fraud-detector/src/main/resources/logback.xml
  - JSON 레이아웃 사용 (구조화된 로깅)
  - INFO 레벨 이상 로그 출력
  - 한국어 주석으로 로그 설정 설명

- [X] T031 Dockerfile 작성
  - fraud-detector/Dockerfile
  - Multi-stage build: Gradle 빌드 → 실행 이미지 분리
  - 베이스 이미지: flink:1.18-scala_2.12-java11
  - JAR 파일 복사 및 Flink Job 실행
  - 한국어 주석으로 단계 설명

- [X] T032 docker-compose.yml에 fraud-detector 서비스 추가
  - build: ./fraud-detector
  - depends_on: kafka, jobmanager
  - environment: KAFKA_BOOTSTRAP_SERVERS, HIGH_VALUE_THRESHOLD
  - mem_limit: 2g

**Checkpoint**: fraud-detector가 고액 거래를 탐지하여 `transaction-alerts` 토픽으로 알림 발행 (kafka-console-consumer로 확인)

---

### Phase 1-4: alert-service 구현

**목표**: Spring WebFlux 프로젝트 설정, `alerts` 토픽 구독(Consumer) 구현, 수신된 알림을 로그에 출력하는 것까지 확인, Dockerfile 작성

- [x] T033 alert-service 디렉토리 및 Spring Boot 프로젝트 생성
  - alert-service/build.gradle, src/main/java/, src/test/java/, src/main/resources/, README.md
  - Spring Boot 3.2.0, Spring WebFlux, Spring Kafka 의존성 추가
  - 한국어로 서비스 목적 설명하는 README.md 작성

- [x] T034 [P] build.gradle 작성 및 의존성 정의
  - alert-service/build.gradle
  - spring-boot-starter-webflux, spring-kafka, spring-boot-starter-actuator
  - JUnit 5, Mockito, TestContainers (테스트용)
  - 한국어 주석으로 의존성 설명

- [x] T035 [P] Alert 모델 클래스 정의
  - alert-service/src/main/java/com/realfds/alert/model/Alert.java
  - 필드: schemaVersion, alertId, originalTransaction, ruleType, ruleName, reason, severity, alertTimestamp
  - Jackson 어노테이션 (@JsonProperty)
  - 한국어 주석으로 필드 설명

- [x] T036 [P] Transaction 모델 클래스 정의
  - alert-service/src/main/java/com/realfds/alert/model/Transaction.java
  - 필드: schemaVersion, transactionId, userId, amount, currency, countryCode, timestamp
  - Jackson 어노테이션
  - 한국어 주석으로 필드 설명

- [x] T037 KafkaConfig 클래스 작성
  - alert-service/src/main/java/com/realfds/alert/config/KafkaConfig.java
  - @Configuration 어노테이션
  - ConsumerFactory<String, Alert> 빈 정의
  - JSON 역직렬화 설정 (JsonDeserializer)
  - 한국어 주석으로 설정 설명

- [x] T038 AlertConsumer 구현
  - alert-service/src/main/java/com/realfds/alert/consumer/AlertConsumer.java
  - @KafkaListener 어노테이션으로 `transaction-alerts` 토픽 구독
  - consumeAlert() 메서드: 알림 수신 시 로그 출력 (INFO 레벨)
  - 로그 메시지에 alertId, ruleName, reason 포함
  - 한국어 로그 메시지
  - 에러 핸들링 (JSON 역직렬화 실패 시 WARN 로그)

- [x] T039 [P] 단위 테스트 작성 - AlertConsumer
  - alert-service/src/test/java/com/realfds/alert/consumer/AlertConsumerTest.java
  - test_consume_alert_success(): Mock Kafka 메시지로 정상 수신 확인
  - test_consume_alert_invalid_json(): 잘못된 JSON 수신 시 에러 핸들링 확인
  - Given-When-Then 구조 사용, 한국어 주석

- [x] T040 HealthController 구현
  - alert-service/src/main/java/com/realfds/alert/controller/HealthController.java
  - GET /actuator/health 엔드포인트
  - {"status": "UP"} 응답
  - 한국어 주석으로 설명

- [x] T041 application.yml 설정 파일 작성
  - alert-service/src/main/resources/application.yml
  - server.port: 8081
  - spring.kafka.bootstrap-servers, consumer.group-id, consumer.topics
  - 환경 변수로 오버라이드 가능하도록 설정
  - 한국어 주석으로 설정 설명

- [x] T042 [P] logback-spring.xml 설정 파일 작성
  - alert-service/src/main/resources/logback-spring.xml
  - JSON 레이아웃 사용 (구조화된 로깅)
  - INFO 레벨 이상 로그 출력
  - 한국어 주석으로 로그 설정 설명

- [x] T043 AlertServiceApplication 메인 클래스 작성
  - alert-service/src/main/java/com/realfds/alert/AlertServiceApplication.java
  - @SpringBootApplication 어노테이션
  - main() 메서드
  - 한국어 주석으로 설명

- [x] T044 Dockerfile 작성
  - alert-service/Dockerfile
  - Multi-stage build: Gradle 빌드 → 실행 이미지 분리
  - 베이스 이미지: eclipse-temurin:17-jre-alpine
  - JAR 파일 복사 및 Spring Boot 실행
  - 한국어 주석으로 단계 설명

- [x] T045 docker-compose.yml에 alert-service 서비스 추가
  - build: ./alert-service
  - depends_on: kafka
  - environment: SPRING_KAFKA_BOOTSTRAP_SERVERS
  - ports: "8081:8081"
  - mem_limit: 512m

**Checkpoint**: alert-service가 `transaction-alerts` 토픽에서 알림을 수신하고 로그에 출력 (docker-compose logs로 확인)

---

### Phase 1 통합 테스트

**목표**: 1단계 전체 파이프라인 검증

- [X] T046 E2E 테스트 스크립트 작성 (1단계)
  - scripts/test-e2e-phase1.sh
  - 1. docker-compose up -d (zookeeper, kafka, transaction-generator, fraud-detector, alert-service)
  - 2. 3분 대기 (시스템 준비)
  - 3. Kafka 토픽 존재 확인 (virtual-transactions, transaction-alerts)
  - 4. alert-service 로그에서 알림 수신 확인
  - 5. 고액 거래 알림 확인 (ruleName: HIGH_VALUE)
  - 6. docker-compose down
  - 한국어 주석으로 테스트 단계 설명

- [X] T047 E2E 테스트 실행 및 검증
  - scripts/test-e2e-phase1.sh 실행
  - 테스트 통과 확인
  - 실패 시 로그 확인 및 수정

**Checkpoint**: 1단계 완료 - 데이터 파이프라인 코어 구축 완료 (거래 생성 → Kafka → Flink 탐지 → Kafka → alert-service 로그 출력)

---

## 개발 2단계: 실시간 알림 전송 및 프론트엔드 (UI & Real-time Delivery)

**목표**: 백엔드에서 생성된 알림을 웹 브라우저까지 실시간으로 전달하는 경로를 완성한다.

**검증 기준**:
- 웹 브라우저에서 `http://localhost:8083` 접속
- 실시간 알림이 1초 이내에 화면에 표시
- 연결 상태가 명확히 표시 ("실시간 연결됨" / "연결 끊김")

---

### Phase 2-1: websocket-gateway 구현

**목표**: Spring WebFlux 프로젝트 설정, WebSocket 핸들러 구현, `/actuator/health` 엔드포인트 추가, Dockerfile 작성

- [X] T048 websocket-gateway 디렉토리 및 Spring Boot 프로젝트 생성
  - websocket-gateway/build.gradle, src/main/java/, src/test/java/, src/main/resources/, README.md
  - Spring Boot 3.2.0, Spring WebFlux, Spring WebSocket 의존성 추가
  - 한국어로 서비스 목적 설명하는 README.md 작성

- [X] T049 [P] build.gradle 작성 및 의존성 정의
  - websocket-gateway/build.gradle
  - spring-boot-starter-webflux, spring-boot-starter-websocket, spring-boot-starter-actuator
  - WebClient (alert-service 호출용)
  - JUnit 5, Mockito (테스트용)
  - 한국어 주석으로 의존성 설명

- [X] T050 [P] Alert 모델 클래스 정의 (alert-service와 동일)
  - websocket-gateway/src/main/java/com/realfds/gateway/model/Alert.java
  - 필드: schemaVersion, alertId, originalTransaction, ruleType, ruleName, reason, severity, alertTimestamp
  - Jackson 어노테이션
  - 한국어 주석으로 필드 설명

- [X] T051 WebSocketConfig 클래스 작성
  - websocket-gateway/src/main/java/com/realfds/gateway/config/WebSocketConfig.java
  - @Configuration, @EnableWebSocket 어노테이션
  - WebSocket 핸들러 매핑 (/ws/alerts)
  - CORS 설정 (모든 Origin 허용 - 로컬 개발용)
  - 한국어 주석으로 설정 설명

- [X] T052 RestClientConfig 클래스 작성
  - websocket-gateway/src/main/java/com/realfds/gateway/config/RestClientConfig.java
  - @Configuration 어노테이션
  - WebClient 빈 정의 (alert-service 호출용)
  - Base URL: http://alert-service:8081
  - 한국어 주석으로 설정 설명

- [X] T053 AlertWebSocketHandler 구현
  - websocket-gateway/src/main/java/com/realfds/gateway/handler/AlertWebSocketHandler.java
  - WebSocketHandler 인터페이스 구현
  - afterConnectionEstablished(): 클라이언트 연결 시 세션 저장
  - afterConnectionClosed(): 클라이언트 연결 종료 시 세션 제거
  - 연결 상태 로깅 (INFO 레벨, 한국어)
  - Thread-safe 세션 관리 (ConcurrentHashMap)
  - 한국어 주석으로 구현 설명

- [X] T054 BroadcastService 구현
  - websocket-gateway/src/main/java/com/realfds/gateway/service/BroadcastService.java
  - @Service 어노테이션
  - broadcast(Alert alert) 메서드: 모든 WebSocket 세션에 알림 브로드캐스트
  - JSON 직렬화 (Jackson ObjectMapper)
  - 에러 핸들링 (세션 전송 실패 시 해당 세션 제거)
  - 한국어 주석으로 구현 설명

- [X] T055 AlertStreamService 구현 (alert-service 폴링)
  - websocket-gateway/src/main/java/com/realfds/gateway/service/AlertStreamService.java
  - @Service 어노테이션
  - @Scheduled(fixedDelay = 1000): 1초마다 alert-service API 호출
  - WebClient로 GET /api/alerts 호출
  - 새로운 알림만 필터링 (마지막 알림 ID 추적)
  - 새 알림 발견 시 BroadcastService.broadcast() 호출
  - 한국어 주석으로 구현 설명

- [X] T056 [P] 단위 테스트 작성 - AlertWebSocketHandler
  - websocket-gateway/src/test/java/com/realfds/gateway/handler/AlertWebSocketHandlerTest.java
  - test_after_connection_established(): 세션 저장 확인
  - test_after_connection_closed(): 세션 제거 확인
  - Given-When-Then 구조 사용, 한국어 주석

- [X] T057 [P] 단위 테스트 작성 - BroadcastService
  - websocket-gateway/src/test/java/com/realfds/gateway/service/BroadcastServiceTest.java
  - test_broadcast_to_all_sessions(): 모든 세션에 브로드캐스트 확인
  - test_broadcast_remove_closed_session(): 전송 실패 시 세션 제거 확인
  - Given-When-Then 구조 사용, 한국어 주석

- [X] T058 HealthController 구현
  - websocket-gateway/src/main/java/com/realfds/gateway/controller/HealthController.java
  - GET /actuator/health 엔드포인트
  - {"status": "UP"} 응답
  - 한국어 주석으로 설명

- [X] T059 application.yml 설정 파일 작성
  - websocket-gateway/src/main/resources/application.yml
  - server.port: 8082
  - alert-service.url: http://alert-service:8081
  - 환경 변수로 오버라이드 가능하도록 설정
  - 한국어 주석으로 설정 설명

- [X] T060 [P] logback-spring.xml 설정 파일 작성
  - websocket-gateway/src/main/resources/logback-spring.xml
  - JSON 레이아웃 사용 (구조화된 로깅)
  - INFO 레벨 이상 로그 출력
  - 한국어 주석으로 로그 설정 설명

- [X] T061 WebSocketGatewayApplication 메인 클래스 작성
  - websocket-gateway/src/main/java/com/realfds/gateway/WebSocketGatewayApplication.java
  - @SpringBootApplication, @EnableScheduling 어노테이션
  - main() 메서드
  - 한국어 주석으로 설명

- [X] T062 Dockerfile 작성
  - websocket-gateway/Dockerfile
  - Multi-stage build: Gradle 빌드 → 실행 이미지 분리
  - 베이스 이미지: eclipse-temurin:17-jre-alpine
  - JAR 파일 복사 및 Spring Boot 실행
  - 한국어 주석으로 단계 설명

- [X] T063 docker-compose.yml에 websocket-gateway 서비스 추가
  - build: ./websocket-gateway
  - depends_on: alert-service
  - environment: ALERT_SERVICE_URL
  - ports: "8082:8082"
  - mem_limit: 512m

**Checkpoint**: websocket-gateway가 실행되고 WebSocket 연결 가능 (wscat으로 확인)

---

### Phase 2-2: alert-service 확장 (인메모리 저장소 + REST API)

**목표**: 인메모리 저장소(최근 100개) 로직 및 REST API 구현

- [X] T064 AlertRepository 구현 (인메모리 저장소)
  - alert-service/src/main/java/com/realfds/alert/repository/AlertRepository.java
  - @Repository 어노테이션
  - ConcurrentLinkedDeque<Alert> 사용 (Thread-safe)
  - addAlert(Alert alert) 메서드: 알림 추가 (최신 알림이 맨 앞)
  - 101개 이상 시 가장 오래된 알림 제거 (최근 100개만 유지)
  - getRecentAlerts(int limit) 메서드: 최근 N개 알림 반환
  - 한국어 주석으로 구현 설명

- [X] T065 AlertService 구현
  - alert-service/src/main/java/com/realfds/alert/service/AlertService.java
  - @Service 어노테이션
  - AlertRepository 의존성 주입
  - processAlert(Alert alert) 메서드: 알림 저장
  - getRecentAlerts(int limit) 메서드: 최근 알림 조회
  - 한국어 주석으로 구현 설명

- [X] T066 AlertConsumer 수정 (AlertService 연동)
  - alert-service/src/main/java/com/realfds/alert/consumer/AlertConsumer.java
  - AlertService 의존성 주입
  - consumeAlert() 메서드 수정: AlertService.processAlert() 호출
  - 로그 출력 (INFO 레벨, 한국어)

- [X] T067 AlertController 구현 (REST API)
  - alert-service/src/main/java/com/realfds/alert/controller/AlertController.java
  - @RestController 어노테이션
  - GET /api/alerts 엔드포인트: AlertService.getRecentAlerts(100) 반환
  - Reactive 타입 사용 (Mono<List<Alert>>)
  - 한국어 주석으로 설명

- [X] T068 [P] 단위 테스트 작성 - AlertRepository
  - alert-service/src/test/java/com/realfds/alert/repository/AlertRepositoryTest.java
  - test_add_alert(): 알림 추가 확인
  - test_max_capacity_100(): 101개 추가 시 가장 오래된 알림 제거 확인
  - test_get_recent_alerts(): 최근 알림 조회 확인
  - Given-When-Then 구조 사용, 한국어 주석

- [X] T069 [P] 단위 테스트 작성 - AlertService
  - alert-service/src/test/java/com/realfds/alert/service/AlertServiceTest.java
  - test_process_alert(): 알림 저장 확인
  - test_get_recent_alerts(): 최근 알림 조회 확인
  - Given-When-Then 구조 사용, 한국어 주석

- [X] T070 통합 테스트 작성 - AlertController
  - alert-service/src/test/java/com/realfds/alert/controller/AlertControllerTest.java
  - test_get_alerts_endpoint(): GET /api/alerts 응답 확인
  - WebTestClient 사용
  - Given-When-Then 구조 사용, 한국어 주석

**Checkpoint**: alert-service가 알림을 인메모리 저장소에 저장하고 REST API로 조회 가능 (curl로 확인)

---

### Phase 2-3: frontend-dashboard 구현

**목표**: React (Vite+TS) 프로젝트 초기 설정, WebSocket 연결 훅(`useWebSocket.ts`) 구현, 수신된 메시지를 console.log로 출력 확인, Dockerfile 작성

- [X] T071 frontend-dashboard 디렉토리 및 Vite 프로젝트 생성
  - frontend-dashboard/package.json, vite.config.ts, tsconfig.json, src/, public/, README.md
  - React 18, TypeScript 5, Vite 5 사용
  - 한국어로 서비스 목적 설명하는 README.md 작성

- [X] T072 [P] package.json 작성 및 의존성 정의
  - frontend-dashboard/package.json
  - react, react-dom, typescript, vite
  - vitest, @testing-library/react (테스트용)
  - 한국어 주석으로 의존성 설명

- [X] T073 [P] tsconfig.json 작성
  - frontend-dashboard/tsconfig.json
  - strict 모드 활성화
  - 한국어 주석으로 설정 설명

- [X] T074 [P] vite.config.ts 작성
  - frontend-dashboard/vite.config.ts
  - React 플러그인 설정
  - server.port: 8083
  - 한국어 주석으로 설정 설명

- [X] T075 [P] Alert 타입 정의
  - frontend-dashboard/src/types/alert.ts
  - interface Alert: schemaVersion, alertId, originalTransaction, ruleType, ruleName, reason, severity, alertTimestamp
  - interface Transaction: transactionId, userId, amount, currency, countryCode, timestamp
  - 한국어 주석으로 필드 설명

- [X] T076 [P] ConnectionStatus 타입 정의
  - frontend-dashboard/src/types/connectionStatus.ts
  - type ConnectionStatus: 'connected' | 'disconnected' | 'connecting'
  - interface ConnectionState: status, lastConnectedAt?, reconnectAttempts
  - 한국어 주석으로 필드 설명

- [X] T077 useWebSocket hook 구현
  - frontend-dashboard/src/hooks/useWebSocket.ts
  - useState로 alerts, connectionStatus 관리
  - useEffect로 WebSocket 연결 수립
  - onopen: connectionStatus를 'connected'로 변경
  - onmessage: 수신한 알림을 alerts 배열에 추가 (최신 알림이 맨 앞, 최근 100개만 유지)
  - onclose: connectionStatus를 'disconnected'로 변경, 5초 후 재연결 시도
  - onerror: 에러 로깅
  - 한국어 주석으로 구현 설명

- [X] T078 ConnectionStatus 컴포넌트 구현
  - frontend-dashboard/src/components/ConnectionStatus.tsx
  - props: status (ConnectionStatus)
  - 연결 상태에 따라 다른 스타일 표시
  - "실시간 연결됨" (녹색) / "연결 끊김" (빨간색) / "연결 중..." (노란색)
  - 한국어 텍스트
  - 한국어 주석으로 컴포넌트 설명

- [X] T079 AlertItem 컴포넌트 구현
  - frontend-dashboard/src/components/AlertItem.tsx
  - props: alert (Alert)
  - 알림 정보 표시: 발생 시각, 거래 ID, 사용자 ID, 거래 금액, 탐지 규칙, 상세 사유
  - 심각도에 따라 다른 색상 표시 (HIGH: 빨간색, MEDIUM: 노란색, LOW: 파란색)
  - 한국어 텍스트
  - 한국어 주석으로 컴포넌트 설명

- [X] T080 AlertList 컴포넌트 구현
  - frontend-dashboard/src/components/AlertList.tsx
  - props: alerts (Alert[])
  - 알림 목록을 AlertItem 컴포넌트로 렌더링
  - 알림이 없을 때 "알림이 없습니다" 메시지 표시 (한국어)
  - 한국어 주석으로 컴포넌트 설명

- [X] T081 Header 컴포넌트 구현
  - frontend-dashboard/src/components/Header.tsx
  - "실시간 FDS 알림" 제목 표시 (한국어)
  - 한국어 주석으로 컴포넌트 설명

- [X] T082 App.tsx 루트 컴포넌트 구현
  - frontend-dashboard/src/App.tsx
  - useWebSocket hook 사용 (ws://localhost:8082/ws/alerts)
  - Header, ConnectionStatus, AlertList 컴포넌트 배치
  - 한국어 주석으로 컴포넌트 설명

- [X] T083 main.tsx 진입점 구현
  - frontend-dashboard/src/main.tsx
  - React.StrictMode로 App 컴포넌트 렌더링
  - 한국어 주석으로 설명

- [X] T084 [P] 단위 테스트 작성 - useWebSocket hook
  - frontend-dashboard/tests/hooks/useWebSocket.test.ts
  - test_initial_state(): 초기 상태 확인 (disconnected, 빈 alerts 배열)
  - test_connection_established(): WebSocket 연결 시 상태 변경 확인
  - Mock WebSocket 사용
  - Given-When-Then 구조 사용, 한국어 주석

- [X] T085 [P] 단위 테스트 작성 - AlertItem 컴포넌트
  - frontend-dashboard/tests/components/AlertItem.test.tsx
  - test_render_alert_fields(): 모든 필드 렌더링 확인
  - test_severity_color(): 심각도에 따른 색상 표시 확인
  - Given-When-Then 구조 사용, 한국어 주석

- [X] T086 [P] 단위 테스트 작성 - AlertList 컴포넌트
  - frontend-dashboard/tests/components/AlertList.test.tsx
  - test_render_alerts(): 알림 목록 렌더링 확인
  - test_render_empty_message(): 알림 없을 때 메시지 표시 확인
  - Given-When-Then 구조 사용, 한국어 주석

- [X] T087 기본 CSS 스타일 작성
  - frontend-dashboard/src/styles/App.css
  - 간단한 레이아웃 스타일 (Flexbox 사용)
  - 알림 카드 스타일 (그림자, 패딩)
  - 심각도별 색상 정의
  - 한국어 주석으로 스타일 설명

- [X] T088 Dockerfile 작성 (Multi-stage build)
  - frontend-dashboard/Dockerfile
  - Stage 1: node:20-alpine으로 빌드 (npm run build)
  - Stage 2: nginx:alpine으로 정적 파일 서빙
  - 빌드된 파일을 /usr/share/nginx/html로 복사
  - 한국어 주석으로 단계 설명

- [X] T089 nginx.conf 설정 파일 작성
  - frontend-dashboard/nginx.conf
  - server.port: 8083
  - SPA 라우팅 지원 (try_files)
  - 한국어 주석으로 설정 설명

- [X] T090 docker-compose.yml에 frontend-dashboard 서비스 추가
  - build: ./frontend-dashboard
  - depends_on: websocket-gateway
  - ports: "8083:8083"
  - mem_limit: 256m

**Checkpoint**: 웹 브라우저에서 `http://localhost:8083` 접속 가능, 실시간 알림이 화면에 표시

---

### Phase 2 통합 테스트

**목표**: 2단계 전체 파이프라인 검증 (백엔드 → 프론트엔드)

- [X] T091 E2E 테스트 스크립트 작성 (2단계)
  - scripts/test-e2e-phase2.sh
  - 1. docker-compose up -d (모든 서비스)
  - 2. 5분 대기 (시스템 준비)
  - 3. alert-service 헬스 체크 (GET /actuator/health)
  - 4. websocket-gateway 헬스 체크 (GET /actuator/health)
  - 5. frontend-dashboard 접속 확인 (GET http://localhost:8083)
  - 6. WebSocket 연결 테스트 (wscat으로 알림 수신 확인)
  - 7. docker-compose down
  - 한국어 주석으로 테스트 단계 설명

- [X] T092 E2E 테스트 실행 및 검증
  - scripts/test-e2e-phase2.sh 실행
  - 테스트 통과 확인
  - 실패 시 로그 확인 및 수정

**Checkpoint**: 2단계 완료 - 실시간 알림이 웹 브라우저까지 전달됨 (거래 생성 → Kafka → Flink 탐지 → Kafka → alert-service → websocket-gateway → 브라우저)

---

## 개발 3단계: 전체 기능 완성 및 고도화 (Full Feature & Polish)

**목표**: 모든 탐지 규칙을 구현하고, UI를 완성하며, 테스트와 문서를 보강한다.

**검증 기준**:
- 3가지 탐지 규칙 (고액, 해외, 빈번한 거래) 모두 작동
- 단위 테스트 커버리지 ≥70%
- E2E 테스트로 전체 파이프라인 검증
- 문서 완성 (README, architecture.md, quickstart.md)

---

### Phase 3-1: fraud-detector 규칙 완성

**목표**: 해외 거래, 빈번한 거래(상태 저장 윈도우) 규칙 구현 및 단위 테스트 작성

- [X] T093 [P] ForeignCountryRule 구현 (해외 거래 탐지)
  - fraud-detector/src/main/scala/com/realfds/detector/rules/ForeignCountryRule.scala
  - FilterFunction[Transaction] 상속
  - filter() 메서드: countryCode != "KR" 조건 확인
  - 한국어 주석으로 비즈니스 로직 설명

- [X] T094 [P] ForeignCountryRule Alert 생성 로직 구현
  - fraud-detector/src/main/scala/com/realfds/detector/rules/ForeignCountryRule.scala
  - toAlert() 메서드: Transaction → Alert 변환
  - ruleName: "FOREIGN_COUNTRY", ruleType: "SIMPLE_RULE", severity: "MEDIUM"
  - reason: "해외 거래 탐지 (국가: {countryCode})"
  - 한국어 주석으로 설명

- [X] T095 HighFrequencyRule 구현 (빈번한 거래 탐지)
  - fraud-detector/src/main/scala/com/realfds/detector/rules/HighFrequencyRule.scala
  - KeyedProcessFunction[String, Transaction, Alert] 상속
  - processElement() 메서드: 1분 윈도우 내 거래 수 추적
  - MapState[Long, Int] 사용 (timestamp → count)
  - 5회 초과 시 Alert 생성
  - 1분 이상 지난 데이터는 상태에서 제거
  - 한국어 주석으로 구현 설명

- [X] T096 HighFrequencyRule Alert 생성 로직 구현
  - fraud-detector/src/main/scala/com/realfds/detector/rules/HighFrequencyRule.scala
  - createAlert() 메서드: Transaction → Alert 변환
  - ruleName: "HIGH_FREQUENCY", ruleType: "STATEFUL_RULE", severity: "HIGH"
  - reason: "빈번한 거래 (1분 내 5회 초과): {userId}, {count}회"
  - 한국어 주석으로 설명

- [X] T097 [P] 단위 테스트 작성 - ForeignCountryRule
  - fraud-detector/src/test/scala/com/realfds/detector/rules/ForeignCountryRuleSpec.scala
  - test_filter_foreign_transaction(): KR 외 국가 거래 필터링 확인
  - test_filter_kr_transaction(): KR 거래 미필터링 확인
  - test_to_alert_fields(): Alert 필드 정확성 확인
  - Given-When-Then 구조 사용, 한국어 주석

- [X] T098 [P] 단위 테스트 작성 - HighFrequencyRule
  - fraud-detector/src/test/scala/com/realfds/detector/rules/HighFrequencyRuleSpec.scala
  - test_high_frequency_detection(): 1분 내 6회 거래 시 알림 생성 확인
  - test_normal_frequency_no_alert(): 5회 이하 거래 시 알림 미생성 확인
  - test_window_reset(): 1분 경과 후 카운터 리셋 확인
  - Flink Testing Harness 사용
  - Given-When-Then 구조 사용, 한국어 주석

- [X] T099 FraudDetectionJob 수정 (3가지 규칙 통합)
  - fraud-detector/src/main/scala/com/realfds/detector/FraudDetectionJob.scala
  - HighValueRule, ForeignCountryRule 스트림 추가
  - HighFrequencyRule 스트림 추가 (keyBy userId, 1분 윈도우)
  - 3개 스트림을 union하여 Kafka Sink로 발행
  - 한국어 주석으로 전체 Job 구조 설명

**Checkpoint**: fraud-detector가 3가지 규칙 모두 작동 (kafka-console-consumer로 확인)

---

### Phase 3-2: frontend-dashboard UI 완성

**목표**: ConnectionStatus, AlertList, AlertItem 컴포넌트 개선 및 사용자 경험 향상

- [X] T100 [P] 시간 포맷팅 유틸리티 구현
  - frontend-dashboard/src/utils/formatter.ts
  - formatTimestamp(timestamp: string): "2025-11-06 10:30:45" 형식 반환
  - formatAmount(amount: number): "1,250,000원" 형식 반환
  - 한국어 주석으로 함수 설명

- [X] T101 AlertItem 컴포넌트 개선
  - frontend-dashboard/src/components/AlertItem.tsx
  - formatTimestamp, formatAmount 유틸리티 사용
  - 심각도 아이콘 추가 (HIGH: ⚠️, MEDIUM: ⚡, LOW: ℹ️)
  - 애니메이션 효과 추가 (새 알림 등장 시 fade-in)
  - 한국어 주석으로 개선 내용 설명

- [X] T102 ConnectionStatus 컴포넌트 개선
  - frontend-dashboard/src/components/ConnectionStatus.tsx
  - 재연결 시도 횟수 표시 (reconnectAttempts)
  - 마지막 연결 시각 표시 (lastConnectedAt)
  - 한국어 텍스트

- [X] T103 AlertList 컴포넌트 개선
  - frontend-dashboard/src/components/AlertList.tsx
  - 스크롤 영역 추가 (최대 높이 600px, overflow-y: auto)
  - 로딩 인디케이터 추가 (알림 없을 때)
  - 한국어 텍스트

- [X] T104 [P] CSS 스타일 개선
  - frontend-dashboard/src/styles/App.css
  - 반응형 디자인 추가 (미디어 쿼리)
  - 그라데이션 배경 추가
  - 카드 호버 효과 추가
  - 한국어 주석으로 스타일 설명

**Checkpoint**: 프론트엔드 UI가 개선되어 사용자 경험 향상

---

### Phase 3-3: E2E 테스트 및 성능 측정

**목표**: `scripts/test-e2e.sh` 스크립트 작성 및 전체 파이프라인 테스트

- [X] T105 E2E 테스트 스크립트 작성 (최종)
  - scripts/test-e2e.sh
  - 1. docker-compose up -d (모든 서비스)
  - 2. 5분 대기 (시스템 준비)
  - 3. 헬스 체크 (모든 서비스)
  - 4. Kafka 토픽 확인 (virtual-transactions, transaction-alerts)
  - 5. 3가지 탐지 규칙 검증:
  -    a. 고액 거래 알림 확인 (kafka-console-consumer)
  -    b. 해외 거래 알림 확인
  -    c. 빈번한 거래 알림 확인
  - 6. WebSocket 연결 및 알림 수신 확인 (wscat)
  - 7. 프론트엔드 접속 확인 (curl)
  - 8. docker-compose down
  - 한국어 주석으로 테스트 단계 설명

- [X] T106 지연 시간 측정 스크립트 작성
  - scripts/measure-latency.sh
  - 1. Kafka 토픽에서 Transaction 타임스탬프 추출
  - 2. alert-service 로그에서 Alert 수신 타임스탬프 추출
  - 3. 차이 계산 (종단 간 지연 시간)
  - 4. 평균, p95, p99 지연 시간 출력
  - 목표: 평균 <3초, p95 <5초
  - 한국어 주석으로 스크립트 설명

- [X] T107 E2E 테스트 실행 및 검증
  - scripts/test-e2e.sh 실행
  - 3가지 탐지 규칙 모두 작동 확인
  - 종단 간 지연 시간 측정 (scripts/measure-latency.sh)
  - 테스트 통과 확인

- [X] T108 30분 안정성 테스트
  - docker-compose up -d로 시스템 실행
  - 30분간 모니터링 (docker-compose logs -f)
  - 크래시, 메모리 부족, 연결 끊김 없는지 확인
  - 테스트 통과 확인

**Checkpoint**: 전체 시스템이 E2E 테스트 및 성능 기준을 충족

---

### Phase 3-4: 문서화

**목표**: 각 서비스별 README 및 상위 레벨 문서(`quickstart.md`, `architecture.md`) 작성

- [ ] T109 [P] 루트 README.md 작성
  - README.md
  - 프로젝트 개요 (한국어)
  - 빠른 시작 가이드 (docker-compose up)
  - 시스템 구성 요소 설명 (5개 서비스)
  - 환경 변수 문서화
  - 문제 해결 섹션
  - 한국어로 작성

- [ ] T110 [P] architecture.md 작성
  - docs/architecture.md
  - 시스템 아키텍처 다이어그램
  - 데이터 흐름 설명 (거래 생성 → 탐지 → 알림 → 브라우저)
  - 각 서비스의 역할 및 책임
  - 기술 스택 설명
  - 한국어로 작성

- [ ] T111 [P] development.md 작성
  - docs/development.md
  - 로컬 개발 환경 설정
  - 각 서비스별 빌드 및 실행 방법
  - 테스트 실행 방법
  - 새로운 탐지 규칙 추가 가이드
  - 한국어로 작성

- [ ] T112 [P] troubleshooting.md 작성
  - docs/troubleshooting.md
  - 일반적인 문제 및 해결 방법
  - 포트 충돌, 메모리 부족, Kafka 연결 실패 등
  - 로그 확인 방법
  - 한국어로 작성

- [ ] T113 [P] transaction-generator/README.md 작성
  - transaction-generator/README.md
  - 서비스 목적 및 책임
  - 입력/출력 (Kafka 토픽)
  - 로컬 실행 방법
  - 환경 변수 설명
  - 한국어로 작성

- [ ] T114 [P] fraud-detector/README.md 작성
  - fraud-detector/README.md
  - 서비스 목적 및 책임
  - 3가지 탐지 규칙 설명
  - 입력/출력 (Kafka 토픽)
  - 로컬 실행 방법
  - 한국어로 작성

- [ ] T115 [P] alert-service/README.md 작성
  - alert-service/README.md
  - 서비스 목적 및 책임
  - 입력/출력 (Kafka 토픽, REST API)
  - 로컬 실행 방법
  - 한국어로 작성

- [ ] T116 [P] websocket-gateway/README.md 작성
  - websocket-gateway/README.md
  - 서비스 목적 및 책임
  - WebSocket API 설명
  - 로컬 실행 방법
  - 한국어로 작성

- [ ] T117 [P] frontend-dashboard/README.md 작성
  - frontend-dashboard/README.md
  - 서비스 목적 및 책임
  - UI 컴포넌트 설명
  - 로컬 개발 방법 (npm run dev)
  - 한국어로 작성

**Checkpoint**: 모든 문서가 완성되어 개발자가 시스템을 이해하고 실행 가능

---

### Phase 3-5: Polish & Cross-Cutting Concerns

**목표**: 관찰 가능성, 품질 표준, Constitution 준수 확인

- [ ] T118 [P] 헬스 체크 엔드포인트 개선 - alert-service
  - alert-service/src/main/java/com/realfds/alert/controller/HealthController.java
  - Kafka 연결 상태 포함
  - 인메모리 저장소 크기 포함
  - 한국어 주석으로 설명

- [ ] T119 [P] 헬스 체크 엔드포인트 개선 - websocket-gateway
  - websocket-gateway/src/main/java/com/realfds/gateway/controller/HealthController.java
  - WebSocket 활성 연결 수 포함
  - alert-service 연결 상태 포함
  - 한국어 주석으로 설명

- [ ] T120 [P] transaction-generator 로깅 개선
  - transaction-generator/src/main.py
  - 구조화된 로깅 (JSON 형식)
  - 거래 생성률 로깅 (INFO 레벨, 10초마다)
  - 한국어 로그 메시지

- [ ] T121 fraud-detector 로깅 개선
  - fraud-detector/src/main/scala/com/realfds/detector/FraudDetectionJob.scala
  - 거래 처리율 로깅 (INFO 레벨, 10초마다)
  - 알림 생성률 로깅
  - 한국어 로그 메시지

- [ ] T122 Kafka 연결 Circuit Breaker 구현 - alert-service
  - alert-service/src/main/java/com/realfds/alert/config/KafkaConfig.java
  - Spring Retry 사용
  - 지수적 백오프 (1s, 2s, 4s, 8s, 최대 30s)
  - 연결 실패 로깅
  - 한국어 주석으로 설명

- [ ] T123 [P] Kafka 연결 Circuit Breaker 구현 - transaction-generator
  - transaction-generator/src/kafka_producer.py
  - 연결 실패 시 재시도 로직
  - 지수적 백오프
  - 한국어 주석으로 설명

- [ ] T124 단위 테스트 커버리지 확인
  - 모든 서비스에서 단위 테스트 실행
  - transaction-generator: pytest --cov
  - fraud-detector: sbt test (ScalaTest)
  - alert-service: ./gradlew test --coverage
  - websocket-gateway: ./gradlew test --coverage
  - frontend-dashboard: npm run test:coverage
  - 커버리지 ≥70% 확인

- [ ] T125 통합 테스트 실행
  - alert-service, websocket-gateway 통합 테스트 실행
  - TestContainers로 Kafka 컨테이너 사용
  - 3가지 탐지 규칙 모두 검증
  - 종단 간 시나리오 검증

- [ ] T126 코드 품질 리뷰
  - 모든 서비스 코드 검토
  - 함수 길이 ≤50줄 확인
  - 파일 길이 ≤300줄 확인
  - 서술적인 변수/함수명 확인
  - 한국어 주석 확인
  - 위반 사항 수정

- [ ] T127 Constitution 준수 확인
  - I. 학습 우선: 실시간 스트리밍 개념 명확히 시연 확인
  - II. 단순함: `docker-compose up` 동작 확인
  - III. 실시간 우선: 이벤트 기반 통신, WebSocket 사용 확인
  - IV. 서비스 경계: 5개 서비스 (사용자 명시 요구사항), 독립 배포 가능 확인
  - V. 품질 표준: 테스트 커버리지, 로깅, 오류 처리 확인
  - VI. 한국어 우선: 주석, 문서, 커밋 메시지 확인

- [ ] T128 MVP Acceptance Criteria 검증
  - docker-compose up으로 모든 서비스 시작
  - 시스템이 5분 내에 완전히 작동
  - 30분 동안 충돌 없이 실행
  - 헬스 체크 엔드포인트 200 OK 응답
  - 3가지 탐지 규칙 모두 작동
  - 종단 간 지연 시간 <5초

- [ ] T129 코드 정리 및 리팩토링
  - 중복 코드 제거
  - 코드 스타일 일관성 확인
  - 불필요한 주석 제거
  - 한국어 주석 품질 검토

- [ ] T130 quickstart.md 검증
  - quickstart.md 문서 단계를 따라 실행
  - 모든 명령어가 정확히 작동하는지 확인
  - 스크린샷 추가 (선택적)
  - 문제 발견 시 문서 수정

- [ ] T131 보안 리뷰 (기본 수준)
  - 데이터 검증 확인 (Transaction, Alert 필드)
  - 오류 메시지에 민감 정보 미포함 확인
  - 환경 변수로 비밀 정보 관리 확인
  - CORS 설정 확인 (로컬 개발용)

**Checkpoint**: 3단계 완료 - 전체 기능 완성 및 고도화 완료, 프로덕션 준비 상태

---

## Dependencies & Execution Order

### 개발 단계 의존성

- **1단계: 데이터 파이프라인 코어 구축**: 의존성 없음 - 즉시 시작 가능
- **2단계: 실시간 알림 전송 및 프론트엔드**: 1단계 완료 후 시작 (alert-service가 1단계에서 구현됨)
- **3단계: 전체 기능 완성 및 고도화**: 1단계, 2단계 완료 후 시작

### Phase 내 작업 의존성

**1단계**:
- Phase 1-1 (인프라 설정) → Phase 1-2, 1-3, 1-4 (병렬 실행 가능)
- Phase 1-2 (transaction-generator) 완료 → Phase 1-3 (fraud-detector) 시작 가능
- Phase 1-3 (fraud-detector) 완료 → Phase 1-4 (alert-service) 시작 가능

**2단계**:
- Phase 2-1 (websocket-gateway) 와 Phase 2-2 (alert-service 확장) 병렬 실행 가능
- Phase 2-1, 2-2 완료 → Phase 2-3 (frontend-dashboard) 시작 가능

**3단계**:
- Phase 3-1 (fraud-detector 규칙 완성), 3-2 (frontend UI 개선), 3-4 (문서화) 병렬 실행 가능
- Phase 3-1, 3-2 완료 → Phase 3-3 (E2E 테스트) 시작 가능
- Phase 3-3 완료 → Phase 3-5 (Polish) 시작 가능

### 병렬 실행 기회

**1단계**:
- T002~T008: Docker Compose 파일 작성 (병렬 가능)
- T010~T011: transaction-generator 모델 및 의존성 정의 (병렬 가능)
- T015~T016: 단위 테스트 작성 (병렬 가능)
- T020~T024: fraud-detector 모델 및 스키마 정의 (병렬 가능)
- T035~T036: alert-service 모델 클래스 정의 (병렬 가능)

**2단계**:
- T050~T052: websocket-gateway 모델 및 설정 (병렬 가능)
- T056~T057: 단위 테스트 작성 (병렬 가능)
- T068~T070: alert-service 단위 테스트 작성 (병렬 가능)
- T072~T076: frontend-dashboard 설정 및 타입 정의 (병렬 가능)
- T084~T086: 단위 테스트 작성 (병렬 가능)

**3단계**:
- T093~T094: ForeignCountryRule 구현 (병렬 가능)
- T097~T098: 단위 테스트 작성 (병렬 가능)
- T109~T117: 문서 작성 (모두 병렬 가능)
- T118~T123: 헬스 체크 및 Circuit Breaker 구현 (병렬 가능)

---

## Implementation Strategy

### 순차적 단계별 접근 (권장)

1. **1단계 완료**: 데이터 파이프라인 코어 구축
   - 거래 생성 → Kafka → Flink 탐지 → Kafka → alert-service 로그 출력
   - Checkpoint: kafka-console-consumer로 알림 확인

2. **2단계 완료**: 실시간 알림 전송 및 프론트엔드
   - alert-service → websocket-gateway → 브라우저
   - Checkpoint: 웹 브라우저에서 실시간 알림 표시

3. **3단계 완료**: 전체 기능 완성 및 고도화
   - 3가지 탐지 규칙, UI 개선, 테스트 보강, 문서 완성
   - Checkpoint: E2E 테스트 통과, 30분 안정성 테스트 통과

### MVP 정의 (1단계 + 2단계)

**최소 기능 제품 (MVP)**:
- 거래 자동 생성
- 고액 거래 탐지 (단일 규칙)
- 웹 브라우저에서 실시간 알림 표시
- 단일 명령어 실행 (`docker-compose up`)

**MVP 검증 기준**:
- 거래 발생부터 브라우저 표시까지 5초 이내
- 5분 내에 시스템 시작
- 헬스 체크 엔드포인트 정상 응답

---

## Notes

- [P] 표시 작업: 병렬 실행 가능 (다른 파일, 의존성 없음)
- 각 Phase 완료 후 Checkpoint에서 독립적으로 검증
- 테스트는 구현 전에 작성하고 실패 확인 후 구현 (TDD)
- 각 작업 또는 논리적 그룹 완료 후 커밋 (Conventional Commits + 한국어)
- 모든 주석, 로그 메시지, 문서는 한국어로 작성
- 함수 길이 ≤50줄, 파일 길이 ≤300줄 준수
- 구조화된 로깅 (JSON) 사용
- 한국어 커밋 메시지 예시:
  - `feat: transaction-generator 거래 생성 로직 구현`
  - `test: HighValueRule 단위 테스트 작성`
  - `docs: README.md 빠른 시작 가이드 작성`
  - `fix: Kafka 연결 실패 시 재시도 로직 추가`
  - `refactor: AlertService 코드 정리 및 주석 개선`

---

## Summary

**총 작업 수**: 131개
**개발 단계**: 3단계 (Backend Foundation → UI & Real-time Delivery → Full Feature & Polish)
**예상 병렬 실행 기회**: 약 40개 작업 [P] 표시

**1단계 (Backend Foundation)**: T001~T047 (47개 작업)
- Phase 1-1: 공통 인프라 설정 (8개)
- Phase 1-2: transaction-generator (10개)
- Phase 1-3: fraud-detector (14개)
- Phase 1-4: alert-service (13개)
- Phase 1 통합 테스트 (2개)

**2단계 (UI & Real-time Delivery)**: T048~T092 (45개 작업)
- Phase 2-1: websocket-gateway (16개)
- Phase 2-2: alert-service 확장 (7개)
- Phase 2-3: frontend-dashboard (20개)
- Phase 2 통합 테스트 (2개)

**3단계 (Full Feature & Polish)**: T093~T131 (39개 작업)
- Phase 3-1: fraud-detector 규칙 완성 (7개)
- Phase 3-2: frontend UI 완성 (5개)
- Phase 3-3: E2E 테스트 및 성능 측정 (4개)
- Phase 3-4: 문서화 (9개)
- Phase 3-5: Polish & Cross-Cutting Concerns (14개)

**MVP 범위**: 1단계 + 2단계 (T001~T092, 92개 작업)

**독립 테스트 기준**:
- 1단계: kafka-console-consumer로 알림 확인
- 2단계: 웹 브라우저에서 실시간 알림 표시
- 3단계: E2E 테스트 통과, 3가지 탐지 규칙 모두 작동

**Constitution 준수 확인**: Phase 3-5에서 모든 원칙 검증 (T127)
