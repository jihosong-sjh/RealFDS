# Fraud Detector (실시간 탐지 엔진)

**서비스 종류**: 스트림 처리 엔진 (Stream Processor)
**기술 스택**: Apache Flink 1.18+, Scala 2.12
**역할**: 실시간 거래 패턴 탐지

---

## 목적

Apache Flink를 사용하여 실시간으로 거래 스트림을 분석하고 의심스러운 패턴을 탐지하는 서비스입니다. 3가지 탐지 규칙을 병렬로 실행하여 알림을 생성합니다.

---

## 주요 책임

- **Kafka Consumer**: `virtual-transactions` 토픽 구독
- **3가지 탐지 규칙 실행**:
  - **HighValueRule**: 고액 거래 탐지 (100만원 초과)
  - **ForeignCountryRule**: 해외 거래 탐지 (KR 외 국가)
  - **HighFrequencyRule**: 빈번한 거래 탐지 (1분 내 5회 초과)
- **상태 관리**: RocksDB State Backend (빈번한 거래 추적용)
- **Kafka Producer**: `transaction-alerts` 토픽으로 Alert 발행

---

## 탐지 규칙 상세

| 규칙명 | 유형 | 조건 | 심각도 |
|--------|------|------|--------|
| HIGH_VALUE | SIMPLE_RULE | amount > 1,000,000 | HIGH |
| FOREIGN_COUNTRY | SIMPLE_RULE | countryCode != "KR" | MEDIUM |
| HIGH_FREQUENCY | STATEFUL_RULE | 1분 내 5회 초과 거래 | HIGH |

---

## 입력/출력

### 입력
- **Kafka 토픽**: `virtual-transactions` (Transaction JSON)

### 출력
- **Kafka 토픽**: `transaction-alerts` (Alert JSON)

---

## 로컬 빌드 및 실행

```bash
cd fraud-detector
./gradlew clean build

# Flink 클러스터가 실행 중이어야 함
docker-compose up -d jobmanager taskmanager

# Flink Job 제출
docker-compose exec jobmanager flink run \
  /opt/flink/usrlib/fraud-detector-1.0.0.jar
```

---

## Flink Web UI

```
http://localhost:8081
```

- Running Jobs, TaskManager 상태 확인 가능

---

## 참고 문서

- [전체 시스템 아키텍처](../docs/architecture.md)
- [데이터 모델 상세](../specs/001-realtime-fds/data-model.md)
- [개발 가이드](../docs/development.md)
