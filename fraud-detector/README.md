# Fraud Detector (실시간 탐지 엔진)

## 목적
Apache Flink를 사용하여 실시간으로 금융 거래를 분석하고 의심스러운 패턴을 탐지하는 Scala/Java 기반 스트림 처리 서비스입니다.

## 주요 책임
- Kafka `virtual-transactions` 토픽에서 거래 데이터 구독
- 3가지 탐지 규칙 적용:
  - **고액 거래** (HIGH_VALUE): 100만원 초과 거래
  - **해외 거래** (FOREIGN_COUNTRY): KR 외 국가 거래
  - **빈번한 거래** (HIGH_FREQUENCY): 1분 내 5회 초과 거래
- 탐지된 알림을 Kafka `transaction-alerts` 토픽으로 발행

## 기술 스택
- Apache Flink 1.18.1
- Scala 2.12
- Kafka Connector for Flink
- RocksDB State Backend (상태 저장용)

## 입력/출력
- **입력**: Kafka `virtual-transactions` 토픽
- **출력**: Kafka `transaction-alerts` 토픽

## 환경 변수
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka 브로커 주소 (기본: kafka:9092)
- `HIGH_VALUE_THRESHOLD`: 고액 거래 임계값 (기본: 1000000)
