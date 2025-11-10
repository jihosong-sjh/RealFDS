# Transaction Generator (거래 생성기)

**서비스 종류**: 데이터 생성기 (Producer)
**기술 스택**: Python 3.11+
**역할**: RealFDS 시스템의 데이터 소스

---

## 목적

가상 금융 거래 데이터를 주기적으로 생성하여 Kafka `virtual-transactions` 토픽으로 발행하는 Python 기반 서비스입니다. 실시간 스트리밍 데이터 처리 파이프라인의 시작점 역할을 합니다.

---

## 주요 책임

- **거래 데이터 생성**: 무작위 Transaction 객체 생성 (사용자 ID, 금액, 국가 코드, 타임스탬프 등)
- **Kafka Producer**: `virtual-transactions` 토픽으로 JSON 형식 이벤트 발행
- **주기적 실행**: 설정 가능한 주기로 거래 생성 (기본: 1초마다)
- **데이터 분포**: 현실적인 거래 패턴 시뮬레이션
  - 금액: 정규 분포 (평균 30만원, 표준편차 20만원)
  - 사용자: user-1 ~ user-10 균등 분포
  - 국가: 80% KR, 10% US, 5% JP, 5% CN

---

## 기술 스택

- **Python 3.11+**: 핵심 언어
- **confluent-kafka 2.3+**: Kafka 클라이언트 라이브러리
- **Faker 22.0+**: 가상 데이터 생성 (UUID 등)
- **python-dotenv 1.0+**: 환경 변수 로드

---

## 입력/출력

### 입력
- 없음 (자체적으로 데이터 생성)

### 출력
- **Kafka 토픽**: `virtual-transactions`
- **메시지 형식**: JSON
- **스키마 버전**: 1.0

---

## 환경 변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Kafka 브로커 주소 |
| `TRANSACTION_GENERATION_INTERVAL_MS` | `1000` | 거래 생성 주기 (밀리초) |

---

## 로컬 실행

### 사전 요구사항

- Python 3.11+
- Kafka 실행 중 (로컬 또는 Docker)

### 실행 방법

```bash
cd transaction-generator
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
python src/main.py
```

---

## 참고 문서

- [전체 시스템 아키텍처](../docs/architecture.md)
- [데이터 모델 상세](../specs/001-realtime-fds/data-model.md)
- [개발 가이드](../docs/development.md)
