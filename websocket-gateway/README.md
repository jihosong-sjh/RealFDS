# WebSocket Gateway (실시간 통신 게이트웨이)

## 목적
alert-service의 알림을 WebSocket을 통해 브라우저 클라이언트에게 실시간으로 푸시하는 Spring WebFlux 기반 서비스입니다.

## 주요 책임
- WebSocket 연결 관리 (`/ws/alerts`)
- alert-service REST API 폴링 (1초마다)
- 새 알림을 연결된 모든 클라이언트에 브로드캐스트
- 헬스 체크 엔드포인트 제공

## 기술 스택
- Spring Boot 3.2+ with Spring WebFlux
- Spring WebSocket
- Java 17+
- WebClient (alert-service 호출용)

## 입력/출력
- **입력**: alert-service REST API (`GET /api/alerts`)
- **출력**: WebSocket 연결 (`/ws/alerts`)

## 환경 변수
- `SERVER_PORT`: 서버 포트 (기본: 8082)
- `ALERT_SERVICE_URL`: alert-service URL (기본: http://alert-service:8081)

## WebSocket API
- **엔드포인트**: `ws://localhost:8082/ws/alerts`
- **메시지 형식**: JSON (Alert 객체)
