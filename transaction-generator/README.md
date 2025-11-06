# Transaction Generator (거래 생성기)

## 목적
가상 금융 거래 데이터를 주기적으로 생성하여 Kafka `virtual-transactions` 토픽으로 발행하는 Python 기반 서비스입니다.

## 주요 책임
- 무작위 거래 데이터 생성 (사용자 ID, 금액, 국가 코드 등)
- Kafka Producer로 거래 이벤트 발행
- 설정 가능한 주기로 거래 생성 (기본: 5초마다)

## 기술 스택
- Python 3.11+
- confluent-kafka (Kafka 클라이언트)
- faker (가상 데이터 생성)

## 입력/출력
- **입력**: 없음 (자체적으로 데이터 생성)
- **출력**: Kafka `virtual-transactions` 토픽

## 환경 변수
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka 브로커 주소 (기본: kafka:9092)
- `TRANSACTION_GENERATION_INTERVAL_MS`: 거래 생성 주기 (기본: 5000ms)
