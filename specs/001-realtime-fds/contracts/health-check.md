# Health Check API Contract: 서비스 상태 모니터링

**작성일**: 2025-11-06
**Phase**: Phase 1 - Design & Contracts
**목적**: 각 서비스의 헬스 체크 엔드포인트 정의

---

## 1. 공통 규격

### 1.1 엔드포인트
```
GET /actuator/health
```

### 1.2 응답 형식

**정상 상태 (HTTP 200)**:
```json
{
  "status": "UP",
  "components": {
    "kafka": {
      "status": "UP",
      "details": {
        "bootstrapServers": "kafka:9092",
        "connected": true
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 107374182400,
        "free": 53687091200,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

**장애 상태 (HTTP 503)**:
```json
{
  "status": "DOWN",
  "components": {
    "kafka": {
      "status": "DOWN",
      "details": {
        "error": "Connection refused: kafka:9092"
      }
    }
  }
}
```

---

## 2. 서비스별 헬스 체크

### 2.1 alert-service

**URL**: `http://localhost:8081/actuator/health`

**체크 항목**:
- `kafka`: Kafka 브로커 연결 상태
- `diskSpace`: 디스크 여유 공간
- `alertRepository`: 인메모리 저장소 상태 (알림 개수)

**예시 응답**:
```json
{
  "status": "UP",
  "components": {
    "kafka": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "alertRepository": {
      "status": "UP",
      "details": {
        "alertCount": 45,
        "maxCapacity": 100
      }
    }
  }
}
```

---

### 2.2 websocket-gateway

**URL**: `http://localhost:8082/actuator/health`

**체크 항목**:
- `alertService`: alert-service 연결 상태
- `webSocket`: WebSocket 연결 수
- `diskSpace`: 디스크 여유 공간

**예시 응답**:
```json
{
  "status": "UP",
  "components": {
    "alertService": {
      "status": "UP",
      "details": {
        "url": "http://alert-service:8081",
        "lastCheckTimestamp": "2025-11-06T10:30:00Z"
      }
    },
    "webSocket": {
      "status": "UP",
      "details": {
        "activeConnections": 5,
        "maxConnections": 100
      }
    },
    "diskSpace": { "status": "UP" }
  }
}
```

---

## 3. Spring Actuator 설정

### 3.1 application.yml (alert-service)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: always
  health:
    kafka:
      enabled: true
    diskspace:
      enabled: true
```

---

## 4. Docker Compose Healthcheck

```yaml
# docker-compose.yml
services:
  alert-service:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  websocket-gateway:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
```

---

**Health Check API Contract 완료**: 모든 contracts/ 파일 생성 완료
