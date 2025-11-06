# fraud-detector

## 서비스 목적
실시간 금융 거래 탐지 엔진 (Real-time Fraud Detection Engine)으로, Apache Flink를 사용하여 스트림 데이터를 처리하고 의심스러운 거래 패턴을 탐지합니다.

## 주요 책임
- Kafka `virtual-transactions` 토픽에서 거래 데이터 소비
- 3가지 탐지 규칙 적용:
  - **HighValueRule**: 고액 거래 탐지 (100만원 초과)
  - **ForeignCountryRule**: 해외 거래 탐지 (국가 코드 != KR)
  - **HighFrequencyRule**: 빈번한 거래 탐지 (1분 내 5회 초과)
- 탐지된 알림을 Kafka `transaction-alerts` 토픽으로 발행

## 기술 스택
- **언어**: Scala 2.12
- **프레임워크**: Apache Flink 1.18.1
- **빌드 도구**: Gradle 8.x
- **메시지 브로커**: Apache Kafka 3.6+
- **테스트**: ScalaTest

## 로컬 실행 방법

### 전제조건
- JDK 11 이상
- Gradle 8.x 이상
- Kafka 실행 중 (localhost:9092)

### 빌드
```bash
./gradlew clean build
```

### 실행
```bash
./gradlew run
```

### 테스트
```bash
./gradlew test
```

## Docker 실행
```bash
docker build -t fraud-detector:latest .
docker run --rm \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e HIGH_VALUE_THRESHOLD=1000000 \
  fraud-detector:latest
```

## 환경 변수
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka 브로커 주소 (기본값: kafka:9092)
- `HIGH_VALUE_THRESHOLD`: 고액 거래 임계값 (기본값: 1000000)
- `FLINK_CHECKPOINT_INTERVAL`: 체크포인트 간격 (ms, 기본값: 60000)

## 아키텍처
```
Kafka (virtual-transactions)
  ↓
[TransactionDeserializationSchema]
  ↓
[Flink DataStream]
  ├─→ [HighValueRule] → Alert
  ├─→ [ForeignCountryRule] → Alert
  └─→ [HighFrequencyRule (Stateful)] → Alert
  ↓
[Union Alerts]
  ↓
[AlertSerializationSchema]
  ↓
Kafka (transaction-alerts)
```

## 개발 가이드
- 모든 주석은 한국어로 작성
- 함수 길이 ≤50줄, 파일 길이 ≤300줄
- 단위 테스트 커버리지 ≥70%
- 구조화된 로깅 사용 (JSON)
