# Kafka Events Contract: 실시간 금융 거래 탐지 시스템

**작성일**: 2025-11-06
**Phase**: Phase 1 - Design & Contracts
**목적**: Kafka 토픽, 이벤트 스키마, 프로듀서/컨슈머 계약 정의

---

## 1. Kafka 토픽 구성

### 1.1 virtual-transactions

**목적**: transaction-generator에서 생성한 가상 거래 데이터 스트림

**설정**:
- **파티션 수**: 3개 (userId 기반 파티셔닝)
- **복제 계수**: 1 (로컬 단일 브로커)
- **보관 기간**: 1시간 (retention.ms=3600000)
- **압축**: none
- **키**: userId (String)
- **값**: Transaction JSON

**생성 명령** (자동 생성, 참고용):
```bash
kafka-topics --create \
  --bootstrap-server kafka:9092 \
  --topic virtual-transactions \
  --partitions 3 \
  --replication-factor 1 \
  --config retention.ms=3600000
```

---

### 1.2 transaction-alerts

**목적**: fraud-detector에서 탐지한 알림 스트림

**설정**:
- **파티션 수**: 3개 (userId 기반 파티셔닝)
- **복제 계수**: 1 (로컬 단일 브로커)
- **보관 기간**: 24시간 (retention.ms=86400000)
- **압축**: none
- **키**: userId (String)
- **값**: Alert JSON

**생성 명령** (자동 생성, 참고용):
```bash
kafka-topics --create \
  --bootstrap-server kafka:9092 \
  --topic transaction-alerts \
  --partitions 3 \
  --replication-factor 1 \
  --config retention.ms=86400000
```

---

## 2. 이벤트 스키마

### 2.1 TransactionEvent (virtual-transactions 토픽)

**JSON Schema**:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["schemaVersion", "transactionId", "userId", "amount", "currency", "countryCode", "timestamp"],
  "properties": {
    "schemaVersion": {
      "type": "string",
      "const": "1.0",
      "description": "스키마 버전"
    },
    "transactionId": {
      "type": "string",
      "format": "uuid",
      "description": "거래 고유 식별자 (UUID v4)"
    },
    "userId": {
      "type": "string",
      "pattern": "^user-(1[0]|[1-9])$",
      "description": "사용자 ID (user-1 ~ user-10)"
    },
    "amount": {
      "type": "integer",
      "minimum": 1000,
      "maximum": 1500000,
      "description": "거래 금액 (KRW 정수)"
    },
    "currency": {
      "type": "string",
      "const": "KRW",
      "description": "통화 코드"
    },
    "countryCode": {
      "type": "string",
      "pattern": "^[A-Z]{2}$",
      "enum": ["KR", "US", "JP", "CN"],
      "description": "거래 발생 국가 코드 (ISO 3166-1 alpha-2)"
    },
    "timestamp": {
      "type": "string",
      "format": "date-time",
      "description": "거래 발생 시각 (ISO 8601, UTC)"
    }
  }
}
```

**예시 메시지**:
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

**Kafka 메시지 구조**:
- **Key**: `"user-3"` (String, userId 값)
- **Value**: 위 JSON (String, UTF-8 인코딩)
- **Headers**:
  - `schema-version`: `"1.0"`
  - `source-service`: `"transaction-generator"`

---

### 2.2 AlertEvent (transaction-alerts 토픽)

**JSON Schema**:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["schemaVersion", "alertId", "originalTransaction", "ruleType", "ruleName", "reason", "severity", "alertTimestamp"],
  "properties": {
    "schemaVersion": {
      "type": "string",
      "const": "1.0"
    },
    "alertId": {
      "type": "string",
      "format": "uuid"
    },
    "originalTransaction": {
      "$ref": "#/definitions/Transaction",
      "description": "탐지된 원본 거래 (전체 Transaction 엔터티)"
    },
    "ruleType": {
      "type": "string",
      "enum": ["SIMPLE_RULE", "STATEFUL_RULE"]
    },
    "ruleName": {
      "type": "string",
      "enum": ["HIGH_VALUE", "FOREIGN_COUNTRY", "HIGH_FREQUENCY"]
    },
    "reason": {
      "type": "string",
      "minLength": 1,
      "maxLength": 200,
      "description": "한국어 설명"
    },
    "severity": {
      "type": "string",
      "enum": ["HIGH", "MEDIUM", "LOW"]
    },
    "alertTimestamp": {
      "type": "string",
      "format": "date-time"
    }
  },
  "definitions": {
    "Transaction": {
      "type": "object",
      "required": ["schemaVersion", "transactionId", "userId", "amount", "currency", "countryCode", "timestamp"],
      "properties": {
        "schemaVersion": { "type": "string" },
        "transactionId": { "type": "string", "format": "uuid" },
        "userId": { "type": "string" },
        "amount": { "type": "integer" },
        "currency": { "type": "string" },
        "countryCode": { "type": "string" },
        "timestamp": { "type": "string", "format": "date-time" }
      }
    }
  }
}
```

**예시 메시지**:
```json
{
  "schemaVersion": "1.0",
  "alertId": "660e9511-f39c-52e5-b827-557766551111",
  "originalTransaction": {
    "schemaVersion": "1.0",
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user-3",
    "amount": 1250000,
    "currency": "KRW",
    "countryCode": "KR",
    "timestamp": "2025-11-06T10:30:45.123Z"
  },
  "ruleType": "SIMPLE_RULE",
  "ruleName": "HIGH_VALUE",
  "reason": "고액 거래 (100만원 초과): 1,250,000원",
  "severity": "HIGH",
  "alertTimestamp": "2025-11-06T10:30:47.456Z"
}
```

**Kafka 메시지 구조**:
- **Key**: `"user-3"` (String, originalTransaction.userId 값)
- **Value**: 위 JSON (String, UTF-8 인코딩)
- **Headers**:
  - `schema-version`: `"1.0"`
  - `source-service`: `"fraud-detector"`
  - `rule-name`: `"HIGH_VALUE"` (필터링 최적화용)

---

## 3. 프로듀서 계약

### 3.1 transaction-generator → virtual-transactions

**서비스**: transaction-generator (Python)
**라이브러리**: confluent-kafka-python 2.3+

**설정**:
```python
producer_config = {
    'bootstrap.servers': 'kafka:9092',
    'client.id': 'transaction-generator',
    'acks': 'all',  # 모든 in-sync replica 확인
    'retries': 3,
    'compression.type': 'snappy',
    'linger.ms': 10,  # 10ms 배칭
    'batch.size': 16384
}
```

**발행 로직**:
```python
def publish_transaction(transaction: dict):
    key = transaction['userId']
    value = json.dumps(transaction)

    producer.produce(
        topic='virtual-transactions',
        key=key,
        value=value,
        headers={
            'schema-version': '1.0',
            'source-service': 'transaction-generator'
        },
        on_delivery=delivery_callback
    )
    producer.poll(0)  # 비동기 처리
```

**에러 처리**:
- Kafka 연결 실패 → 3회 재시도, 실패 시 ERROR 로그 후 종료
- 발행 실패 (네트워크 오류) → 재시도 큐에 추가, 최대 10개 버퍼링
- 직렬화 오류 → WARN 로그, 해당 거래 스킵

---

### 3.2 fraud-detector → transaction-alerts

**서비스**: fraud-detector (Flink)
**라이브러리**: flink-connector-kafka 1.18+

**설정**:
```scala
val producer = new FlinkKafkaProducer[Alert](
  "transaction-alerts",
  new AlertSerializationSchema(),
  producerProperties,
  FlinkKafkaProducer.Semantic.EXACTLY_ONCE  // 정확히 한 번 전달
)

val producerProperties = new Properties()
producerProperties.setProperty("bootstrap.servers", "kafka:9092")
producerProperties.setProperty("acks", "all")
producerProperties.setProperty("retries", "3")
producerProperties.setProperty("compression.type", "snappy")
```

**발행 로직**:
```scala
class AlertSerializationSchema extends KafkaSerializationSchema[Alert] {
  override def serialize(alert: Alert, timestamp: java.lang.Long): ProducerRecord[Array[Byte], Array[Byte]] = {
    val key = alert.originalTransaction.userId.getBytes("UTF-8")
    val value = Json.toJson(alert).toString.getBytes("UTF-8")

    val headers = new util.ArrayList[Header]()
    headers.add(new RecordHeader("schema-version", "1.0".getBytes))
    headers.add(new RecordHeader("source-service", "fraud-detector".getBytes))
    headers.add(new RecordHeader("rule-name", alert.ruleName.getBytes))

    new ProducerRecord("transaction-alerts", null, key, value, headers)
  }
}
```

**에러 처리**:
- Flink의 체크포인트 메커니즘으로 장애 복구
- Exactly-once 시맨틱으로 중복 알림 방지

---

## 4. 컨슈머 계약

### 4.1 fraud-detector ← virtual-transactions

**서비스**: fraud-detector (Flink)
**라이브러리**: flink-connector-kafka 1.18+

**설정**:
```scala
val consumer = new FlinkKafkaConsumer[Transaction](
  "virtual-transactions",
  new TransactionDeserializationSchema(),
  consumerProperties
)

val consumerProperties = new Properties()
consumerProperties.setProperty("bootstrap.servers", "kafka:9092")
consumerProperties.setProperty("group.id", "fraud-detector-group")
consumerProperties.setProperty("auto.offset.reset", "earliest")
consumerProperties.setProperty("enable.auto.commit", "false")  // Flink 체크포인트 사용
```

**소비 로직**:
```scala
env
  .addSource(consumer)
  .assignTimestampsAndWatermarks(
    WatermarkStrategy
      .forBoundedOutOfOrderness(Duration.ofSeconds(5))
      .withTimestampAssigner((tx, _) => Instant.parse(tx.timestamp).toEpochMilli)
  )
  .process(...)
```

**에러 처리**:
- JSON 파싱 실패 → WARN 로그, 해당 메시지 스킵 (컨슈머 그룹 오프셋은 커밋)
- 알 수 없는 schemaVersion → ERROR 로그, Dead Letter Queue 전송 (추후 구현)
- Kafka 연결 실패 → Flink 재시작 정책에 따라 재시도

---

### 4.2 alert-service ← transaction-alerts

**서비스**: alert-service (Spring Kafka)
**라이브러리**: spring-kafka 3.1+

**설정**:
```java
@Configuration
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "alert-service-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }
}
```

**소비 로직**:
```java
@Service
public class AlertConsumer {

    @KafkaListener(topics = "transaction-alerts", groupId = "alert-service-group")
    public void consume(@Payload String message,
                       @Header("schema-version") String schemaVersion) {
        try {
            Alert alert = objectMapper.readValue(message, Alert.class);
            alertService.saveAlert(alert);
            log.info("Alert received: alertId={}, ruleName={}",
                     alert.getAlertId(), alert.getRuleName());
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse alert: {}", message, e);
            // 메시지 스킵, 오프셋은 커밋
        }
    }
}
```

**에러 처리**:
- JSON 파싱 실패 → WARN 로그, 메시지 스킵
- 알 수 없는 schemaVersion → ERROR 로그, DLQ 전송
- Kafka 연결 실패 → Spring Retry + Circuit Breaker (최대 3회 재시도)

---

## 5. 메시지 흐름 시퀀스

```
transaction-generator                Kafka                    fraud-detector
        │                              │                            │
        │ 1. produce Transaction       │                            │
        │─────────────────────────────>│                            │
        │    key: userId               │                            │
        │    value: Transaction JSON   │                            │
        │                              │                            │
        │                              │ 2. consume Transaction     │
        │                              │<───────────────────────────│
        │                              │    (earliest offset)       │
        │                              │                            │
        │                              │                            │
        │                              │ 3. produce Alert           │
        │                              │<───────────────────────────│
        │                              │    (if rule matched)       │
        │                              │                            │
                                       │
                                       │
                    alert-service      │
                          │            │
                          │ 4. consume Alert
                          │<───────────│
                          │            │
                          │ 5. save to in-memory
                          │            │
```

---

## 6. 파티셔닝 전략

### 6.1 userId 기반 파티셔닝

**이유**:
- 빈번한 거래 탐지 (HIGH_FREQUENCY)는 사용자별 상태 관리가 필요
- 동일 userId의 메시지를 동일 파티션에 배치하여 Flink의 상태 관리 최적화

**파티션 할당**:
```
user-1, user-4, user-7, user-10 → Partition 0
user-2, user-5, user-8          → Partition 1
user-3, user-6, user-9          → Partition 2
```

### 6.2 파티션 리밸런싱

**시나리오**: fraud-detector 인스턴스 추가/제거
- Kafka 컨슈머 그룹의 자동 리밸런싱 활용
- 리밸런싱 중 일시적 지연 가능 (수 초)
- Flink 상태는 RocksDB State Backend에 유지되어 복구됨

---

## 7. 성능 최적화

### 7.1 배칭
- **프로듀서**: `linger.ms=10`, `batch.size=16384`
- **컨슈머**: `fetch.min.bytes=1`, `fetch.max.wait.ms=500`

### 7.2 압축
- **압축 타입**: `snappy` (속도와 압축률 균형)
- **예상 압축률**: JSON 메시지 약 50% 압축

### 7.3 처리량 목표
- **프로듀서**: 초당 10~100개 거래 (평균 10개, 피크 100개)
- **컨슈머**: 초당 10,000개 처리 가능 (Flink 성능)
- **지연 시간**: 평균 <100ms (Kafka 프로듀서/컨슈머 합산)

---

## 8. 모니터링 지표

### 8.1 프로듀서 메트릭
- `record-send-rate`: 초당 전송 메시지 수
- `record-error-rate`: 초당 에러 발생 수
- `request-latency-avg`: 평균 요청 지연 시간

### 8.2 컨슈머 메트릭
- `records-consumed-rate`: 초당 소비 메시지 수
- `records-lag`: 컨슈머 랙 (밀린 메시지 수)
- `commit-latency-avg`: 평균 커밋 지연 시간

### 8.3 토픽 메트릭
- `bytes-in-per-sec`: 초당 수신 바이트
- `bytes-out-per-sec`: 초당 전송 바이트
- `messages-in-per-sec`: 초당 메시지 수

---

## 9. 테스트 시나리오

### 9.1 정상 흐름 테스트
```bash
# 1. 콘솔 컨슈머로 virtual-transactions 토픽 모니터링
kafka-console-consumer --bootstrap-server kafka:9092 \
  --topic virtual-transactions \
  --from-beginning \
  --property print.key=true

# 2. transaction-generator 실행
# 3. 메시지 확인: 초당 10개 메시지, userId가 키로 표시

# 4. 콘솔 컨슈머로 transaction-alerts 토픽 모니터링
kafka-console-consumer --bootstrap-server kafka:9092 \
  --topic transaction-alerts \
  --from-beginning \
  --property print.key=true

# 5. 고액/해외/빈번한 거래 알림 확인
```

### 9.2 스키마 검증 테스트
```python
# Python JSON Schema 검증
import jsonschema

transaction_schema = {...}  # 위 JSON Schema
transaction_data = {...}    # 실제 메시지

jsonschema.validate(transaction_data, transaction_schema)
# ValidationError 발생 시 스키마 위반
```

### 9.3 파티션 테스트
```bash
# 특정 파티션의 메시지만 소비
kafka-console-consumer --bootstrap-server kafka:9092 \
  --topic virtual-transactions \
  --partition 0 \
  --from-beginning

# user-1, user-4, user-7, user-10 메시지만 표시되는지 확인
```

---

## 10. 문제 해결 가이드

### 문제: 프로듀서 연결 실패
```
ERROR: Failed to connect to Kafka broker
```
**해결**:
1. Kafka 컨테이너 실행 확인: `docker ps | grep kafka`
2. 네트워크 연결 확인: `telnet kafka 9092`
3. bootstrap.servers 설정 확인 (로컬: localhost:9092, Docker: kafka:9092)

### 문제: 컨슈머 랙 증가
```
WARN: Consumer lag is increasing: lag=500 messages
```
**해결**:
1. 컨슈머 처리 속도 확인 (로그 분석)
2. fraud-detector 메모리/CPU 사용량 확인
3. 파티션 수 증가 또는 컨슈머 인스턴스 추가 고려

### 문제: JSON 파싱 실패
```
WARN: Failed to parse message: Unexpected character...
```
**해결**:
1. 메시지 형식 확인 (콘솔 컨슈머로 원본 확인)
2. 스키마 버전 확인 (schemaVersion 필드)
3. 프로듀서 직렬화 로직 검증

---

**Kafka Events Contract 완료**: Phase 1 계속 진행 (websocket-api.md, health-check.md)
