# Alert Service (알림 관리 서비스)

## 목적
탐지된 알림을 수신하여 인메모리 저장소에 보관하고, REST API를 통해 조회할 수 있는 Spring WebFlux 기반 서비스입니다.

## 주요 책임
- Kafka `transaction-alerts` 토픽에서 알림 구독
- 최근 100개 알림을 인메모리 저장소에 보관
- REST API 제공 (`GET /api/alerts`)
- 헬스 체크 엔드포인트 제공 (`GET /actuator/health`)

## 기술 스택
- Spring Boot 3.2+ with Spring WebFlux
- Spring Kafka
- Java 17+
- ConcurrentLinkedDeque (인메모리 저장소)

## 입력/출력
- **입력**: Kafka `transaction-alerts` 토픽
- **출력**: REST API (`/api/alerts`)

## 환경 변수
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka 브로커 주소 (기본: kafka:9092)
- `SERVER_PORT`: 서버 포트 (기본: 8081)

## API 엔드포인트
- `GET /api/alerts`: 최근 100개 알림 조회
- `GET /actuator/health`: 서비스 헬스 체크
