# Alert Service (알림 저장 및 관리)

**서비스 종류**: Reactive 백엔드 서비스
**기술 스택**: Spring Boot 3.2+, Spring WebFlux
**역할**: 알림 데이터 저장 및 REST API 제공

---

## 목적

Kafka에서 탐지된 알림을 소비하여 인메모리 저장소에 저장하고, REST API를 통해 다른 서비스(websocket-gateway)에 제공하는 서비스입니다.

---

## 주요 책임

- **Kafka Consumer**: `transaction-alerts` 토픽 구독
- **인메모리 저장소**: ConcurrentLinkedDeque (최근 100개만 유지)
- **REST API 제공**:
  - `GET /api/alerts`: 최근 알림 목록 조회
  - `GET /actuator/health`: 헬스 체크
- **Reactive 처리**: Spring WebFlux (Non-blocking I/O)

---

## 입력/출력

### 입력
- **Kafka 토픽**: `transaction-alerts` (Alert JSON)

### 출력
- **REST API**: `GET /api/alerts` (JSON 응답)

---

## 로컬 실행

```bash
cd alert-service
./gradlew clean build
./gradlew bootRun
```

---

## API 테스트

```bash
# 헬스 체크
curl http://localhost:8081/actuator/health

# 알림 목록 조회
curl http://localhost:8081/api/alerts
```

---

## 환경 변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Kafka 브로커 주소 |
| `SERVER_PORT` | `8081` | 서버 포트 |

---

## 참고 문서

- [전체 시스템 아키텍처](../docs/architecture.md)
- [데이터 모델 상세](../specs/001-realtime-fds/data-model.md)
- [개발 가이드](../docs/development.md)
