# Quickstart Guide: 실시간 시스템 대시보드

**Feature**: 004-dashboard-realtime
**Version**: 1.0
**Last Updated**: 2025-11-12
**Estimated Implementation Time**: 3-4 days

---

## 1. 빠른 개요 (Quick Overview)

실시간 시스템 대시보드는 5개 마이크로서비스의 Health Check 상태, 초당 거래 처리량(TPS), 분당 알림 발생률을 실시간으로 모니터링하는 웹 기반 대시보드입니다. Spring Boot Actuator를 통한 메트릭 수집, WebSocket 브로드캐스트를 통한 실시간 데이터 푸시, Recharts 기반 시계열 차트 시각화를 제공합니다.

**핵심 기능**:
- 5개 서비스의 실시간 상태 모니터링 (UP/DOWN)
- 초당 거래 처리량(TPS) 시계열 차트 (최근 1시간)
- 분당 알림 발생률 차트 (규칙별 분류, 최근 1시간)
- 5초마다 자동 업데이트
- WebSocket 재연결 및 백필(backfill) 지원

**주요 기술 스택**:
- Backend: Spring Boot 3.2+, Spring WebFlux (WebClient), Spring WebSocket
- Frontend: React 18+, TypeScript 5+, Recharts
- Data Storage: 메모리 기반 (ConcurrentLinkedDeque)
- Communication: WebSocket (JSON 메시지)

**아키텍처 다이어그램** (ASCII):
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ 5개 서비스      │     │ Kafka Topics    │     │ 브라우저        │
│ Health Check    │     │ - transactions  │     │ React Dashboard │
│ 엔드포인트      │     │ - alerts        │     └─────────────────┘
└─────────────────┘     └─────────────────┘              ▲
        │                       │                         │
        │ HTTP GET (5초마다)    │ AdminClient (5초마다)   │ WebSocket
        ▼                       ▼                         │
┌─────────────────────────────────────────────────────────┐
│          WebSocket Gateway (모니터링 추가)              │
│                                                          │
│  ┌──────────────────┐  ┌───────────────────┐           │
│  │HealthCheckCollector│ │KafkaMetricsCollector│         │
│  │ - WebClient      │  │ - AdminClient     │           │
│  │ - 3초 타임아웃   │  │ - TPS, 알림률 계산│           │
│  └──────────────────┘  └───────────────────┘           │
│           ↓                      ↓                      │
│  ┌────────────────────────────────────────┐            │
│  │         MetricsStore                   │            │
│  │  (ConcurrentLinkedDeque)               │            │
│  │  - 최근 1시간 데이터 (720개)           │            │
│  └────────────────────────────────────────┘            │
│                      ↓                                  │
│  ┌────────────────────────────────────────┐            │
│  │     WebSocketHandler                   │            │
│  │  - 5초마다 브로드캐스트                │            │
│  │  - 재연결 시 백필 지원                 │            │
│  └────────────────────────────────────────┘            │
└─────────────────────────────────────────────────────────┘
```

**예상 구현 시간**:
- Phase 0: 연구 및 설계 (완료)
- Phase 1: 백엔드 메트릭 수집 (6-8시간)
- Phase 2: WebSocket 통신 (4-6시간)
- Phase 3: 프론트엔드 대시보드 (8-10시간)
- Phase 4: 테스트 및 검증 (4-6시간)
- **총 예상 시간**: 22-30시간 (3-4일)

---

## 2. 사전 요구사항 (Prerequisites)

### 필수 도구 및 버전

**백엔드**:
- Java 17 이상
- Spring Boot 3.2+
- Maven 3.8+ 또는 Gradle 8+
- Apache Kafka 3.6+

**프론트엔드**:
- Node.js 18+ (LTS)
- npm 9+ 또는 yarn 1.22+
- TypeScript 5+
- React 18+

**개발 도구**:
- Docker 24+, Docker Compose 2.20+
- Git
- IDE (IntelliJ IDEA, VS Code 권장)

### 실행 중이어야 하는 서비스

본 대시보드 기능을 구현하기 전에 다음 서비스들이 정상 작동해야 합니다:

1. **transaction-generator** (포트: 8080)
   - Health Check: `http://localhost:8080/actuator/health`
   - 거래 생성 및 Kafka 전송

2. **fraud-detector** (포트: 8081)
   - Health Check: `http://localhost:8081/actuator/health`
   - 사기 탐지 로직 실행

3. **alert-service** (포트: 8082)
   - Health Check: `http://localhost:8082/actuator/health`
   - 알림 생성 및 저장

4. **websocket-gateway** (포트: 8083)
   - Health Check: `http://localhost:8083/actuator/health`
   - WebSocket 서버 역할 (본 기능에서 확장)

5. **frontend-dashboard** (포트: 3000)
   - Health Check: `http://localhost:3000/health`
   - React 프론트엔드 (본 기능에서 확장)

6. **Kafka Broker** (포트: 9092)
   - 토픽: `virtual-transactions`, `alerts`

### 필요한 사전 지식

- Spring Boot Actuator 기본 개념
- WebSocket 프로토콜 이해
- React Hooks (useState, useEffect, useCallback)
- Recharts 차트 라이브러리 기본 사용법
- Java ConcurrentLinkedDeque (Circular Buffer)
- TypeScript 타입 시스템

---

## 3. 개발 환경 설정 (Development Setup)

### 디렉토리 구조

본 기능은 기존 서비스에 코드를 추가하는 방식으로 구현됩니다:

```text
RealFDS/
├── websocket-gateway/               # 백엔드 (기존 서비스 확장)
│   ├── src/main/java/com/realfds/websocket/
│   │   ├── collector/               # 신규 추가
│   │   │   ├── HealthCheckCollector.java
│   │   │   └── KafkaMetricsCollector.java
│   │   ├── store/                   # 신규 추가
│   │   │   └── MetricsStore.java
│   │   ├── model/                   # 신규 추가
│   │   │   ├── ServiceHealth.java
│   │   │   ├── TransactionMetrics.java
│   │   │   ├── AlertMetrics.java
│   │   │   └── MetricsDataPoint.java
│   │   ├── websocket/               # 기존 디렉토리 (확장)
│   │   │   ├── MetricsWebSocketHandler.java  # 신규 추가
│   │   │   └── WebSocketConfig.java          # 수정
│   │   └── scheduler/               # 신규 추가
│   │       └── MetricsScheduler.java
│   └── src/test/java/com/realfds/websocket/
│       ├── collector/
│       ├── store/
│       └── integration/
│
├── frontend-dashboard/              # 프론트엔드 (기존 서비스 확장)
│   ├── src/
│   │   ├── hooks/                   # 신규 추가
│   │   │   └── useWebSocket.ts
│   │   ├── components/dashboard/   # 신규 추가
│   │   │   ├── ServiceHealthCard.tsx
│   │   │   ├── TpsChart.tsx
│   │   │   ├── AlertRateChart.tsx
│   │   │   └── DashboardLayout.tsx
│   │   ├── types/                   # 신규 추가
│   │   │   └── metrics.ts
│   │   └── pages/                   # 기존 디렉토리 (확장)
│   │       └── DashboardPage.tsx    # 신규 추가
│   └── __tests__/
│       ├── hooks/
│       └── components/
│
└── specs/004-dashboard-realtime/   # 명세 문서
    ├── spec.md
    ├── research.md
    ├── data-model.md
    ├── plan.md
    └── quickstart.md               # 본 문서
```

### 백엔드 의존성 추가

**websocket-gateway/pom.xml** (Maven 사용 시):
```xml
<!-- Spring Boot Actuator (이미 추가되어 있음) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Spring WebFlux (WebClient 사용) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Spring WebSocket (이미 추가되어 있음) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- Kafka AdminClient -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Lombok (코드 간결화) -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- JSON 처리 (이미 추가되어 있음) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

**websocket-gateway/build.gradle** (Gradle 사용 시):
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.springframework.kafka:spring-kafka'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // 테스트 의존성
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'io.projectreactor:reactor-test'
}
```

### 프론트엔드 의존성 추가

**frontend-dashboard/package.json**:
```bash
# Recharts 차트 라이브러리 설치
npm install recharts

# TypeScript 타입 정의
npm install --save-dev @types/recharts

# (선택) 날짜 포맷팅
npm install date-fns
```

### 환경 변수 설정

**websocket-gateway/src/main/resources/application.yml**:
```yaml
spring:
  application:
    name: websocket-gateway

# Actuator 엔드포인트 노출
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always

# Kafka 설정
kafka:
  bootstrap-servers: localhost:9092
  consumer:
    group-id: websocket-gateway-group

# 서비스 URL 설정 (Health Check용)
services:
  urls:
    transaction-generator: http://localhost:8080
    fraud-detector: http://localhost:8081
    alert-service: http://localhost:8082
    websocket-gateway: http://localhost:8083
    frontend-dashboard: http://localhost:3000

# WebSocket 설정
websocket:
  endpoint: /ws/metrics
  allowed-origins: http://localhost:3000

# 메트릭 수집 설정
metrics:
  collection:
    interval: 5000  # 5초
    health-check-timeout: 3000  # 3초
  retention:
    period: 3600  # 1시간 (초 단위)
```

**frontend-dashboard/.env.local**:
```bash
REACT_APP_WEBSOCKET_URL=ws://localhost:8083/ws/metrics
```

---

## 4. 백엔드 구현 가이드 (Backend Implementation Guide)

### Step 1: 메트릭 데이터 모델 생성

**ServiceHealth.java**:
```java
package com.realfds.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * 마이크로서비스의 Health Check 상태를 나타내는 엔티티
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealth {
    /** 서비스 식별자 (5개 중 하나) */
    private String serviceName;

    /** 현재 서비스 상태 (UP 또는 DOWN) */
    private ServiceStatus status;

    /** 마지막 Health Check 수행 시각 */
    private Instant lastChecked;

    /** Health Check 응답 시간 (밀리초, DOWN 시 null) */
    private Long responseTime;

    /** JVM 메모리 사용량 (메가바이트, DOWN 시 null) */
    private Long memoryUsage;

    /** DOWN 상태 시 오류 유형 (UP 시 null) */
    private ErrorType errorType;

    /** DOWN 상태 시 오류 상세 메시지 (UP 시 null) */
    private String errorMessage;

    public enum ServiceStatus {
        UP, DOWN
    }

    public enum ErrorType {
        TIMEOUT,        // Health Check 응답 시간 초과 (>3초)
        HTTP_ERROR,     // HTTP 4xx/5xx 에러
        NETWORK_ERROR   // 네트워크 연결 실패
    }
}
```

**MetricsDataPoint.java**:
```java
package com.realfds.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.Map;

/**
 * TPS와 알림 발생률을 통합한 메트릭 데이터 포인트
 * 메모리 저장소(ConcurrentLinkedDeque)에 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsDataPoint {
    /** 메트릭 측정 시각 (5초 간격) */
    private Instant timestamp;

    /** 초당 거래 수 (TPS) */
    private long tps;

    /** 누적 거래 수 */
    private long totalTransactions;

    /** 분당 알림 발생 수 */
    private long alertsPerMinute;

    /** 규칙별 알림 발생 수 (HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY) */
    private Map<String, Long> byRule;
}
```

### Step 2: 메모리 기반 메트릭 저장소 구현

**MetricsStore.java**:
```java
package com.realfds.websocket.store;

import com.realfds.websocket.model.MetricsDataPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * 메모리 기반 메트릭 데이터 저장소
 * ConcurrentLinkedDeque를 사용한 Circular Buffer 구현
 *
 * 보관 기간: 최근 1시간 (720개 데이터 포인트)
 * 메모리 사용량: 약 160KB
 */
@Slf4j
@Service
public class MetricsStore {
    /** 최대 데이터 포인트 수: 1시간 = 3600초 / 5초 = 720개 */
    private static final int MAX_DATA_POINTS = 720;

    /** 데이터 보관 기간: 1시간 */
    private static final Duration RETENTION_PERIOD = Duration.ofHours(1);

    /** 시계열 데이터 저장소 (Thread-safe, Lock-free) */
    private final ConcurrentLinkedDeque<MetricsDataPoint> dataPoints = new ConcurrentLinkedDeque<>();

    /**
     * 새로운 메트릭 데이터 포인트 추가
     * 1시간 이전 데이터 자동 삭제 (FIFO)
     *
     * @param point 추가할 메트릭 데이터 포인트
     */
    public void addDataPoint(MetricsDataPoint point) {
        dataPoints.addLast(point);

        // 1시간 이전 데이터 자동 삭제
        Instant cutoff = Instant.now().minus(RETENTION_PERIOD);
        while (!dataPoints.isEmpty() && dataPoints.peekFirst().getTimestamp().isBefore(cutoff)) {
            MetricsDataPoint removed = dataPoints.removeFirst();
            log.trace("오래된 데이터 포인트 삭제: timestamp={}", removed.getTimestamp());
        }

        log.debug("메트릭 데이터 추가: timestamp={}, tps={}, alerts={}, 총 데이터 포인트={}",
                point.getTimestamp(), point.getTps(), point.getAlertsPerMinute(), dataPoints.size());
    }

    /**
     * 모든 메트릭 데이터 조회 (최근 1시간)
     * 불변 리스트 반환 (스냅샷)
     */
    public List<MetricsDataPoint> getAll() {
        return new ArrayList<>(dataPoints);
    }

    /**
     * 특정 시각 이후의 메트릭 데이터 조회 (백필 용도)
     */
    public List<MetricsDataPoint> getDataSince(Instant since) {
        return dataPoints.stream()
                .filter(dp -> dp.getTimestamp().isAfter(since))
                .collect(Collectors.toList());
    }

    /**
     * 현재 저장된 데이터 포인트 수 조회
     */
    public int size() {
        return dataPoints.size();
    }

    /**
     * 모든 데이터 삭제 (테스트 용도)
     */
    public void clear() {
        dataPoints.clear();
        log.info("메트릭 데이터 저장소 초기화");
    }
}
```

### Step 3: Health Check 수집 서비스 구현

**HealthCheckCollector.java**:
```java
package com.realfds.websocket.collector;

import com.realfds.websocket.model.ServiceHealth;
import com.realfds.websocket.model.ServiceHealth.ErrorType;
import com.realfds.websocket.model.ServiceHealth.ServiceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * 5개 마이크로서비스의 Health Check 상태 수집
 * Spring Boot Actuator 엔드포인트 호출 (비동기, 논블로킹)
 */
@Slf4j
@Service
public class HealthCheckCollector {
    private final WebClient webClient;
    private final Map<String, ServiceHealth> healthStatusMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> consecutiveFailures = new ConcurrentHashMap<>();

    @Value("${services.urls.transaction-generator}")
    private String transactionGeneratorUrl;

    @Value("${services.urls.fraud-detector}")
    private String fraudDetectorUrl;

    @Value("${services.urls.alert-service}")
    private String alertServiceUrl;

    @Value("${services.urls.websocket-gateway}")
    private String websocketGatewayUrl;

    @Value("${services.urls.frontend-dashboard}")
    private String frontendDashboardUrl;

    @Value("${metrics.collection.health-check-timeout:3000}")
    private long healthCheckTimeout;

    public HealthCheckCollector(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * 모든 서비스의 Health Check 수행
     */
    public void checkAllServices() {
        Map<String, String> serviceUrls = Map.of(
            "transaction-generator", transactionGeneratorUrl,
            "fraud-detector", fraudDetectorUrl,
            "alert-service", alertServiceUrl,
            "websocket-gateway", websocketGatewayUrl,
            "frontend-dashboard", frontendDashboardUrl
        );

        serviceUrls.forEach((serviceName, url) -> {
            checkServiceHealth(serviceName, url);
        });
    }

    /**
     * 단일 서비스 Health Check
     */
    private void checkServiceHealth(String serviceName, String baseUrl) {
        long startTime = System.currentTimeMillis();

        webClient.get()
            .uri(baseUrl + "/actuator/health")
            .retrieve()
            .bodyToMono(ActuatorHealthResponse.class)
            .timeout(Duration.ofMillis(healthCheckTimeout))
            .doOnSuccess(health -> {
                long responseTime = System.currentTimeMillis() - startTime;
                handleHealthSuccess(serviceName, health, responseTime);
            })
            .doOnError(TimeoutException.class, e -> handleTimeout(serviceName))
            .doOnError(WebClientResponseException.class, e -> handleHttpError(serviceName, e))
            .doOnError(WebClientRequestException.class, e -> handleNetworkError(serviceName, e))
            .onErrorResume(e -> Mono.empty())  // 에러 시 빈 결과 반환
            .subscribe();
    }

    /**
     * Health Check 성공 처리
     */
    private void handleHealthSuccess(String serviceName, ActuatorHealthResponse health, long responseTime) {
        long memoryUsage = extractMemoryUsage(health);

        healthStatusMap.put(serviceName, ServiceHealth.builder()
            .serviceName(serviceName)
            .status(ServiceStatus.UP)
            .responseTime(responseTime)
            .memoryUsage(memoryUsage)
            .lastChecked(Instant.now())
            .build());

        consecutiveFailures.put(serviceName, 0);
        log.info("서비스 정상: service={}, responseTime={}ms, memory={}MB",
                 serviceName, responseTime, memoryUsage);
    }

    /**
     * 타임아웃 처리
     */
    private void handleTimeout(String serviceName) {
        markServiceDown(serviceName, ErrorType.TIMEOUT,
                        String.format("Health check 응답 시간 초과 (>%dms)", healthCheckTimeout));
    }

    /**
     * HTTP 에러 처리
     */
    private void handleHttpError(String serviceName, WebClientResponseException e) {
        markServiceDown(serviceName, ErrorType.HTTP_ERROR,
                        String.format("HTTP %d: %s", e.getStatusCode().value(), e.getMessage()));
    }

    /**
     * 네트워크 에러 처리
     */
    private void handleNetworkError(String serviceName, WebClientRequestException e) {
        markServiceDown(serviceName, ErrorType.NETWORK_ERROR,
                        String.format("연결 실패: %s", e.getMessage()));
    }

    /**
     * 서비스를 DOWN 상태로 표시
     */
    private void markServiceDown(String serviceName, ErrorType errorType, String errorMessage) {
        healthStatusMap.put(serviceName, ServiceHealth.builder()
            .serviceName(serviceName)
            .status(ServiceStatus.DOWN)
            .errorType(errorType)
            .errorMessage(errorMessage)
            .lastChecked(Instant.now())
            .build());

        int failures = consecutiveFailures.compute(serviceName, (k, v) -> v == null ? 1 : v + 1);

        if (failures == 1) {
            log.warn("서비스 중단 감지: service={}, type={}, message={}",
                     serviceName, errorType, errorMessage);
        } else if (failures >= 3) {
            log.error("서비스 지속적 장애: service={}, 연속 실패 횟수={}, type={}",
                      serviceName, failures, errorType);
        }
    }

    /**
     * 메모리 사용량 추출 (Actuator 응답에서)
     */
    private long extractMemoryUsage(ActuatorHealthResponse health) {
        if (health.getComponents() != null &&
            health.getComponents().getMemory() != null &&
            health.getComponents().getMemory().getDetails() != null) {
            return health.getComponents().getMemory().getDetails().getUsed() / (1024 * 1024); // bytes to MB
        }
        return 0L;
    }

    /**
     * 현재 모든 서비스의 Health 상태 조회
     */
    public Map<String, ServiceHealth> getAllHealthStatuses() {
        return new ConcurrentHashMap<>(healthStatusMap);
    }

    // Inner classes for Actuator response parsing
    @lombok.Data
    private static class ActuatorHealthResponse {
        private String status;
        private Components components;
    }

    @lombok.Data
    private static class Components {
        private Memory memory;
    }

    @lombok.Data
    private static class Memory {
        private Details details;
    }

    @lombok.Data
    private static class Details {
        private long total;
        private long used;
        private long free;
    }
}
```

### Step 4: Kafka 메트릭 수집 서비스 구현

**KafkaMetricsCollector.java**:
```java
package com.realfds.websocket.collector;

import com.realfds.websocket.model.MetricsDataPoint;
import com.realfds.websocket.store.MetricsStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Kafka 토픽으로부터 TPS 및 알림 발생률 메트릭 수집
 */
@Slf4j
@Service
public class KafkaMetricsCollector {
    private final AdminClient adminClient;
    private final MetricsStore metricsStore;

    private long lastTransactionOffset = 0L;
    private long lastAlertOffset = 0L;
    private long totalTransactions = 0L;

    public KafkaMetricsCollector(KafkaAdmin kafkaAdmin, MetricsStore metricsStore) {
        this.adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        this.metricsStore = metricsStore;
    }

    /**
     * TPS 및 알림 발생률 수집
     */
    public void collectMetrics() {
        try {
            long currentTps = calculateTps();
            Map<String, Long> alertsByRule = calculateAlertsByRule();
            long totalAlerts = alertsByRule.values().stream().mapToLong(Long::longValue).sum();

            MetricsDataPoint dataPoint = MetricsDataPoint.builder()
                .timestamp(Instant.now())
                .tps(currentTps)
                .totalTransactions(totalTransactions)
                .alertsPerMinute(totalAlerts)
                .byRule(alertsByRule)
                .build();

            metricsStore.addDataPoint(dataPoint);

            log.debug("메트릭 수집 완료: tps={}, totalAlerts={}", currentTps, totalAlerts);

        } catch (Exception e) {
            log.error("메트릭 수집 실패", e);
        }
    }

    /**
     * TPS 계산 (virtual-transactions 토픽의 offset 증가량)
     */
    private long calculateTps() throws ExecutionException, InterruptedException {
        TopicPartition partition = new TopicPartition("virtual-transactions", 0);

        Map<TopicPartition, OffsetSpec> requestLatestOffsets = Map.of(
            partition, OffsetSpec.latest()
        );

        Map<TopicPartition, org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo> offsets =
            adminClient.listOffsets(requestLatestOffsets).all().get();

        long currentOffset = offsets.get(partition).offset();
        long offsetDelta = currentOffset - lastTransactionOffset;

        // 5초 간격으로 수집하므로 TPS = offsetDelta / 5
        long tps = offsetDelta / 5;

        lastTransactionOffset = currentOffset;
        totalTransactions += offsetDelta;

        return tps;
    }

    /**
     * 규칙별 알림 수 계산 (alerts 토픽의 메시지 파싱)
     * 실제 구현에서는 Kafka Consumer로 메시지를 읽어 규칙별 집계 필요
     * 여기서는 간단한 예시로 Mock 데이터 반환
     */
    private Map<String, Long> calculateAlertsByRule() {
        // TODO: 실제 구현에서는 Kafka Consumer로 alerts 토픽 메시지 읽기
        Map<String, Long> alertsByRule = new HashMap<>();
        alertsByRule.put("HIGH_VALUE", (long) (Math.random() * 10));
        alertsByRule.put("FOREIGN_COUNTRY", (long) (Math.random() * 10));
        alertsByRule.put("HIGH_FREQUENCY", (long) (Math.random() * 10));

        return alertsByRule;
    }
}
```

### Step 5: 스케줄러 구성 (5초마다 메트릭 수집)

**MetricsScheduler.java**:
```java
package com.realfds.websocket.scheduler;

import com.realfds.websocket.collector.HealthCheckCollector;
import com.realfds.websocket.collector.KafkaMetricsCollector;
import com.realfds.websocket.websocket.MetricsWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 5초마다 메트릭 수집 및 WebSocket 브로드캐스트
 */
@Slf4j
@Component
public class MetricsScheduler {
    private final HealthCheckCollector healthCheckCollector;
    private final KafkaMetricsCollector kafkaMetricsCollector;
    private final MetricsWebSocketHandler webSocketHandler;

    public MetricsScheduler(HealthCheckCollector healthCheckCollector,
                            KafkaMetricsCollector kafkaMetricsCollector,
                            MetricsWebSocketHandler webSocketHandler) {
        this.healthCheckCollector = healthCheckCollector;
        this.kafkaMetricsCollector = kafkaMetricsCollector;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * 5초마다 실행: Health Check 수집
     */
    @Scheduled(fixedRateString = "${metrics.collection.interval:5000}")
    public void collectHealthMetrics() {
        log.trace("Health Check 수집 시작");
        healthCheckCollector.checkAllServices();
    }

    /**
     * 5초마다 실행: Kafka 메트릭 수집
     */
    @Scheduled(fixedRateString = "${metrics.collection.interval:5000}")
    public void collectKafkaMetrics() {
        log.trace("Kafka 메트릭 수집 시작");
        kafkaMetricsCollector.collectMetrics();
    }

    /**
     * 5초마다 실행: WebSocket 브로드캐스트
     */
    @Scheduled(fixedRateString = "${metrics.collection.interval:5000}")
    public void broadcastMetrics() {
        log.trace("메트릭 브로드캐스트 시작");
        webSocketHandler.broadcastMetrics();
    }
}
```

### Step 6: WebSocket 핸들러 구현

**MetricsWebSocketHandler.java**:
```java
package com.realfds.websocket.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realfds.websocket.collector.HealthCheckCollector;
import com.realfds.websocket.model.MetricsDataPoint;
import com.realfds.websocket.model.ServiceHealth;
import com.realfds.websocket.store.MetricsStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 메시지 핸들러
 * 메트릭 브로드캐스트 및 백필 처리
 */
@Slf4j
@Component
public class MetricsWebSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;
    private final MetricsStore metricsStore;
    private final HealthCheckCollector healthCheckCollector;

    public MetricsWebSocketHandler(ObjectMapper objectMapper,
                                   MetricsStore metricsStore,
                                   HealthCheckCollector healthCheckCollector) {
        this.objectMapper = objectMapper;
        this.metricsStore = metricsStore;
        this.healthCheckCollector = healthCheckCollector;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket 연결: sessionId={}, 총 연결 수={}", session.getId(), sessions.size());

        // 초기 데이터 전송
        sendInitialData(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket 연결 종료: sessionId={}, 총 연결 수={}, status={}",
                 session.getId(), sessions.size(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> request = objectMapper.readValue(payload, Map.class);

        String type = (String) request.get("type");

        if ("BACKFILL_REQUEST".equals(type)) {
            handleBackfillRequest(session, request);
        }
    }

    /**
     * 초기 연결 시 현재 데이터 전송
     */
    private void sendInitialData(WebSocketSession session) {
        try {
            Map<String, Object> message = buildMetricsUpdateMessage();
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
            log.debug("초기 데이터 전송 완료: sessionId={}", session.getId());
        } catch (IOException e) {
            log.error("초기 데이터 전송 실패: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 백필 요청 처리
     */
    private void handleBackfillRequest(WebSocketSession session, Map<String, Object> request) {
        try {
            String lastReceivedTimestamp = (String) request.get("lastReceivedTimestamp");
            Instant since = Instant.parse(lastReceivedTimestamp);

            List<MetricsDataPoint> backfillData = metricsStore.getDataSince(since);

            Map<String, Object> response = new HashMap<>();
            response.put("type", "BACKFILL_RESPONSE");
            response.put("data", backfillData);

            String json = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));

            log.info("백필 완료: sessionId={}, since={}, 데이터 개수={}",
                     session.getId(), lastReceivedTimestamp, backfillData.size());
        } catch (Exception e) {
            log.error("백필 처리 실패: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 모든 연결된 클라이언트에게 메트릭 브로드캐스트
     */
    public void broadcastMetrics() {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> message = buildMetricsUpdateMessage();
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);

            sessions.forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    log.error("메트릭 전송 실패: sessionId={}", session.getId(), e);
                }
            });

            log.debug("메트릭 브로드캐스트 완료: 수신자 수={}", sessions.size());
        } catch (Exception e) {
            log.error("메트릭 브로드캐스트 실패", e);
        }
    }

    /**
     * METRICS_UPDATE 메시지 생성
     */
    private Map<String, Object> buildMetricsUpdateMessage() {
        List<MetricsDataPoint> allData = metricsStore.getAll();
        Map<String, ServiceHealth> serviceHealthMap = healthCheckCollector.getAllHealthStatuses();

        // TPS 집계
        Map<String, Object> tpsData = new HashMap<>();
        if (!allData.isEmpty()) {
            tpsData.put("current", allData.get(allData.size() - 1).getTps());
            tpsData.put("average", allData.stream().mapToLong(MetricsDataPoint::getTps).average().orElse(0));
            tpsData.put("max", allData.stream().mapToLong(MetricsDataPoint::getTps).max().orElse(0));
            tpsData.put("history", allData);
        } else {
            tpsData.put("current", 0);
            tpsData.put("average", 0);
            tpsData.put("max", 0);
            tpsData.put("history", Collections.emptyList());
        }

        // 알림 집계
        Map<String, Object> alertsData = new HashMap<>();
        if (!allData.isEmpty()) {
            MetricsDataPoint latest = allData.get(allData.size() - 1);
            alertsData.put("current", latest.getAlertsPerMinute());
            alertsData.put("average", allData.stream().mapToLong(MetricsDataPoint::getAlertsPerMinute).average().orElse(0));
            alertsData.put("max", allData.stream().mapToLong(MetricsDataPoint::getAlertsPerMinute).max().orElse(0));
            alertsData.put("byRule", latest.getByRule());
            alertsData.put("history", allData);
        } else {
            alertsData.put("current", 0);
            alertsData.put("average", 0);
            alertsData.put("max", 0);
            alertsData.put("byRule", Map.of("HIGH_VALUE", 0, "FOREIGN_COUNTRY", 0, "HIGH_FREQUENCY", 0));
            alertsData.put("history", Collections.emptyList());
        }

        // 메시지 조립
        Map<String, Object> data = new HashMap<>();
        data.put("services", new ArrayList<>(serviceHealthMap.values()));
        data.put("tps", tpsData);
        data.put("alerts", alertsData);

        Map<String, Object> message = new HashMap<>();
        message.put("type", "METRICS_UPDATE");
        message.put("timestamp", Instant.now().toString());
        message.put("data", data);

        return message;
    }
}
```

### Step 7: WebSocket 설정

**WebSocketConfig.java**:
```java
package com.realfds.websocket.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 엔드포인트 설정
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final MetricsWebSocketHandler metricsWebSocketHandler;

    public WebSocketConfig(MetricsWebSocketHandler metricsWebSocketHandler) {
        this.metricsWebSocketHandler = metricsWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(metricsWebSocketHandler, "/ws/metrics")
                .setAllowedOrigins("http://localhost:3000");  // CORS 설정
    }
}
```

### Step 8: 스케줄링 활성화

**Application.java** (main class에 추가):
```java
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 추가
public class WebsocketGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebsocketGatewayApplication.class, args);
    }
}
```

---

## 5. 프론트엔드 구현 가이드 (Frontend Implementation Guide)

### Step 1: TypeScript 타입 정의

**src/types/metrics.ts**:
```typescript
/**
 * 서비스 Health Check 상태
 */
export interface ServiceHealth {
  serviceName: 'transaction-generator' | 'fraud-detector' | 'alert-service' | 'websocket-gateway' | 'frontend-dashboard';
  status: 'UP' | 'DOWN';
  lastChecked: string;
  responseTime: number | null;
  memoryUsage: number | null;
  errorType: 'TIMEOUT' | 'HTTP_ERROR' | 'NETWORK_ERROR' | null;
  errorMessage: string | null;
}

/**
 * 거래량 메트릭 데이터 포인트
 */
export interface TransactionMetrics {
  timestamp: string;
  tps: number;
  totalTransactions: number;
}

/**
 * 거래량 메트릭 집계 데이터
 */
export interface TpsAggregated {
  current: number;
  average: number;
  max: number;
  history: TransactionMetrics[];
}

/**
 * 알림 발생률 메트릭 데이터 포인트
 */
export interface AlertMetrics {
  timestamp: string;
  alertsPerMinute: number;
  byRule: {
    HIGH_VALUE: number;
    FOREIGN_COUNTRY: number;
    HIGH_FREQUENCY: number;
  };
}

/**
 * 알림 발생률 메트릭 집계 데이터
 */
export interface AlertsAggregated {
  current: number;
  average: number;
  max: number;
  byRule: {
    HIGH_VALUE: number;
    FOREIGN_COUNTRY: number;
    HIGH_FREQUENCY: number;
  };
  history: AlertMetrics[];
  isWarning?: boolean;
}

/**
 * 메트릭 업데이트 메시지 (Server → Client)
 */
export interface MetricsUpdateMessage {
  type: 'METRICS_UPDATE';
  timestamp: string;
  data: {
    services: ServiceHealth[];
    tps: TpsAggregated;
    alerts: AlertsAggregated;
  };
}

/**
 * 백필 요청 메시지 (Client → Server)
 */
export interface BackfillRequestMessage {
  type: 'BACKFILL_REQUEST';
  lastReceivedTimestamp: string;
}

/**
 * 백필 응답 메시지 (Server → Client)
 */
export interface BackfillResponseMessage {
  type: 'BACKFILL_RESPONSE';
  data: Array<{
    timestamp: string;
    tps: number;
    totalTransactions: number;
    alertsPerMinute: number;
    byRule: {
      HIGH_VALUE: number;
      FOREIGN_COUNTRY: number;
      HIGH_FREQUENCY: number;
    };
  }>;
}

export type WebSocketMessage = MetricsUpdateMessage | BackfillRequestMessage | BackfillResponseMessage;
```

### Step 2: WebSocket 커스텀 훅 구현

**src/hooks/useWebSocket.ts**:
```typescript
import { useEffect, useRef, useState, useCallback } from 'react';
import { MetricsUpdateMessage, BackfillRequestMessage } from '../types/metrics';

type ConnectionStatus = 'connecting' | 'connected' | 'disconnected';

interface UseWebSocketReturn {
  status: ConnectionStatus;
  lastMessage: MetricsUpdateMessage | null;
  sendMessage: (message: BackfillRequestMessage) => void;
}

/**
 * WebSocket 연결 및 재연결을 관리하는 커스텀 훅
 * Exponential Backoff 재연결 전략 구현
 */
export const useWebSocket = (url: string): UseWebSocketReturn => {
  const [status, setStatus] = useState<ConnectionStatus>('disconnected');
  const [lastMessage, setLastMessage] = useState<MetricsUpdateMessage | null>(null);
  const [lastReceivedTimestamp, setLastReceivedTimestamp] = useState<string | null>(null);

  const ws = useRef<WebSocket | null>(null);
  const reconnectAttempt = useRef(0);
  const reconnectTimeout = useRef<NodeJS.Timeout | null>(null);

  const connect = useCallback(() => {
    setStatus('connecting');
    ws.current = new WebSocket(url);

    ws.current.onopen = () => {
      setStatus('connected');
      reconnectAttempt.current = 0;
      console.log('WebSocket 연결 성공');

      // 백필 요청 (재연결 시)
      if (lastReceivedTimestamp) {
        const backfillRequest: BackfillRequestMessage = {
          type: 'BACKFILL_REQUEST',
          lastReceivedTimestamp
        };
        ws.current?.send(JSON.stringify(backfillRequest));
        console.log('백필 요청:', lastReceivedTimestamp);
      }
    };

    ws.current.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);

        if (message.type === 'METRICS_UPDATE') {
          setLastMessage(message);
          setLastReceivedTimestamp(message.timestamp);
        } else if (message.type === 'BACKFILL_RESPONSE') {
          // 백필 데이터 처리 (기존 데이터와 병합)
          console.log('백필 데이터 수신:', message.data.length, '개');
        }
      } catch (error) {
        console.error('메시지 파싱 실패:', error);
      }
    };

    ws.current.onerror = (error) => {
      console.error('WebSocket 에러:', error);
    };

    ws.current.onclose = () => {
      setStatus('disconnected');
      console.log('WebSocket 연결 종료');

      // Exponential Backoff 재연결
      const delay = Math.min(1000 * Math.pow(2, reconnectAttempt.current), 32000);
      reconnectAttempt.current++;

      console.log(`재연결 시도 (${reconnectAttempt.current}회): ${delay}ms 후`);

      reconnectTimeout.current = setTimeout(() => {
        connect();
      }, delay);
    };
  }, [url, lastReceivedTimestamp]);

  useEffect(() => {
    connect();

    return () => {
      if (reconnectTimeout.current) {
        clearTimeout(reconnectTimeout.current);
      }
      if (ws.current) {
        ws.current.close();
      }
    };
  }, [connect]);

  const sendMessage = useCallback((message: BackfillRequestMessage) => {
    if (ws.current && ws.current.readyState === WebSocket.OPEN) {
      ws.current.send(JSON.stringify(message));
    }
  }, []);

  return { status, lastMessage, sendMessage };
};
```

### Step 3: 서비스 상태 카드 컴포넌트

**src/components/dashboard/ServiceHealthCard.tsx**:
```typescript
import React from 'react';
import { ServiceHealth } from '../../types/metrics';

interface ServiceHealthCardProps {
  service: ServiceHealth;
  onClick?: () => void;
}

/**
 * 단일 서비스의 Health 상태를 표시하는 카드 컴포넌트
 */
export const ServiceHealthCard: React.FC<ServiceHealthCardProps> = ({ service, onClick }) => {
  const isUp = service.status === 'UP';

  return (
    <div
      className={`service-card ${isUp ? 'service-card-up' : 'service-card-down'}`}
      onClick={onClick}
      style={{ cursor: onClick ? 'pointer' : 'default' }}
    >
      <div className="service-card-header">
        <h3 className="service-name">{service.serviceName}</h3>
        <span className={`status-badge ${isUp ? 'badge-up' : 'badge-down'}`}>
          {service.status}
        </span>
      </div>

      <div className="service-card-body">
        {isUp ? (
          <>
            <div className="metric-row">
              <span className="metric-label">응답 시간:</span>
              <span className="metric-value">{service.responseTime}ms</span>
            </div>
            <div className="metric-row">
              <span className="metric-label">메모리 사용량:</span>
              <span className="metric-value">{service.memoryUsage}MB</span>
            </div>
          </>
        ) : (
          <>
            <div className="error-type">{service.errorType}</div>
            <div className="error-message">{service.errorMessage}</div>
          </>
        )}

        <div className="last-checked">
          마지막 확인: {new Date(service.lastChecked).toLocaleTimeString('ko-KR')}
        </div>
      </div>
    </div>
  );
};
```

**src/components/dashboard/ServiceHealthCard.css**:
```css
.service-card {
  border: 2px solid #ddd;
  border-radius: 8px;
  padding: 16px;
  margin: 8px;
  transition: all 0.3s ease;
}

.service-card-up {
  background-color: #f0f9f0;
  border-color: #4caf50;
}

.service-card-down {
  background-color: #fff0f0;
  border-color: #f44336;
}

.service-card:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.service-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.service-name {
  font-size: 16px;
  font-weight: bold;
  margin: 0;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.badge-up {
  background-color: #4caf50;
  color: white;
}

.badge-down {
  background-color: #f44336;
  color: white;
}

.metric-row {
  display: flex;
  justify-content: space-between;
  margin: 8px 0;
}

.metric-label {
  color: #666;
  font-size: 14px;
}

.metric-value {
  font-weight: bold;
  font-size: 14px;
}

.error-type {
  color: #f44336;
  font-weight: bold;
  margin-bottom: 8px;
}

.error-message {
  color: #666;
  font-size: 13px;
  margin-bottom: 12px;
}

.last-checked {
  color: #999;
  font-size: 12px;
  margin-top: 12px;
  text-align: right;
}
```

### Step 4: TPS 차트 컴포넌트

**src/components/dashboard/TpsChart.tsx**:
```typescript
import React from 'react';
import { LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer } from 'recharts';
import { TpsAggregated } from '../../types/metrics';

interface TpsChartProps {
  data: TpsAggregated;
}

/**
 * 초당 거래 처리량(TPS) 시계열 차트
 */
export const TpsChart: React.FC<TpsChartProps> = ({ data }) => {
  // 차트 데이터 변환 (timestamp를 Date 객체로)
  const chartData = data.history.map(point => ({
    timestamp: new Date(point.timestamp).getTime(),
    tps: point.tps
  }));

  return (
    <div className="chart-container">
      <div className="chart-header">
        <h2 className="chart-title">초당 거래 처리량 (TPS)</h2>
        <div className="chart-stats">
          <div className="stat-item">
            <span className="stat-label">현재:</span>
            <span className="stat-value stat-current">{data.current}</span>
          </div>
          <div className="stat-item">
            <span className="stat-label">평균:</span>
            <span className="stat-value">{Math.round(data.average)}</span>
          </div>
          <div className="stat-item">
            <span className="stat-label">최대:</span>
            <span className="stat-value">{data.max}</span>
          </div>
        </div>
      </div>

      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            dataKey="timestamp"
            type="number"
            domain={['dataMin', 'dataMax']}
            tickFormatter={(timestamp) => new Date(timestamp).toLocaleTimeString('ko-KR')}
          />
          <YAxis label={{ value: 'TPS', angle: -90, position: 'insideLeft' }} />
          <Tooltip
            labelFormatter={(timestamp) => new Date(timestamp as number).toLocaleString('ko-KR')}
            formatter={(value: number) => [`${value} TPS`, '거래량']}
          />
          <Line
            type="monotone"
            dataKey="tps"
            stroke="#2196f3"
            strokeWidth={2}
            dot={false}
            isAnimationActive={true}
            animationDuration={500}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};
```

### Step 5: 알림 발생률 차트 컴포넌트

**src/components/dashboard/AlertRateChart.tsx**:
```typescript
import React, { useState } from 'react';
import { AreaChart, Area, XAxis, YAxis, Tooltip, CartesianGrid, Legend, ResponsiveContainer } from 'recharts';
import { AlertsAggregated } from '../../types/metrics';

interface AlertRateChartProps {
  data: AlertsAggregated;
}

/**
 * 분당 알림 발생률 시계열 차트 (규칙별 스택)
 */
export const AlertRateChart: React.FC<AlertRateChartProps> = ({ data }) => {
  const [hiddenRules, setHiddenRules] = useState<Set<string>>(new Set());

  const toggleRule = (ruleName: string) => {
    setHiddenRules(prev => {
      const newSet = new Set(prev);
      if (newSet.has(ruleName)) {
        newSet.delete(ruleName);
      } else {
        newSet.add(ruleName);
      }
      return newSet;
    });
  };

  // 차트 데이터 변환
  const chartData = data.history.map(point => ({
    timestamp: new Date(point.timestamp).getTime(),
    HIGH_VALUE: point.byRule.HIGH_VALUE,
    FOREIGN_COUNTRY: point.byRule.FOREIGN_COUNTRY,
    HIGH_FREQUENCY: point.byRule.HIGH_FREQUENCY
  }));

  // 경고 상태 판단 (평균의 2배 초과)
  const isWarning = data.current > data.average * 2;

  return (
    <div className="chart-container">
      <div className="chart-header">
        <h2 className="chart-title">분당 알림 발생률</h2>
        <div className="chart-stats">
          <div className="stat-item">
            <span className="stat-label">현재:</span>
            <span className={`stat-value stat-current ${isWarning ? 'stat-warning' : ''}`}>
              {data.current}
            </span>
          </div>
          <div className="stat-item">
            <span className="stat-label">평균:</span>
            <span className="stat-value">{Math.round(data.average)}</span>
          </div>
          <div className="stat-item">
            <span className="stat-label">최대:</span>
            <span className="stat-value">{data.max}</span>
          </div>
        </div>
      </div>

      {isWarning && (
        <div className="warning-banner">
          ⚠️ 알림 발생률이 평균의 2배를 초과했습니다!
        </div>
      )}

      <ResponsiveContainer width="100%" height={300}>
        <AreaChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            dataKey="timestamp"
            type="number"
            domain={['dataMin', 'dataMax']}
            tickFormatter={(timestamp) => new Date(timestamp).toLocaleTimeString('ko-KR')}
          />
          <YAxis label={{ value: '분당 알림 수', angle: -90, position: 'insideLeft' }} />
          <Tooltip
            labelFormatter={(timestamp) => new Date(timestamp as number).toLocaleString('ko-KR')}
          />
          <Legend onClick={(e) => toggleRule(e.value)} />

          {!hiddenRules.has('HIGH_VALUE') && (
            <Area
              type="monotone"
              dataKey="HIGH_VALUE"
              stackId="1"
              stroke="#ff7300"
              fill="#ff7300"
              name="고액 거래"
            />
          )}
          {!hiddenRules.has('FOREIGN_COUNTRY') && (
            <Area
              type="monotone"
              dataKey="FOREIGN_COUNTRY"
              stackId="1"
              stroke="#387908"
              fill="#387908"
              name="해외 거래"
            />
          )}
          {!hiddenRules.has('HIGH_FREQUENCY') && (
            <Area
              type="monotone"
              dataKey="HIGH_FREQUENCY"
              stackId="1"
              stroke="#8884d8"
              fill="#8884d8"
              name="고빈도 거래"
            />
          )}
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};
```

### Step 6: 대시보드 레이아웃 컴포넌트

**src/components/dashboard/DashboardLayout.tsx**:
```typescript
import React from 'react';
import { useWebSocket } from '../../hooks/useWebSocket';
import { ServiceHealthCard } from './ServiceHealthCard';
import { TpsChart } from './TpsChart';
import { AlertRateChart } from './AlertRateChart';

/**
 * 실시간 시스템 대시보드 메인 레이아웃
 */
export const DashboardLayout: React.FC = () => {
  const wsUrl = process.env.REACT_APP_WEBSOCKET_URL || 'ws://localhost:8083/ws/metrics';
  const { status, lastMessage } = useWebSocket(wsUrl);

  // 연결 상태 배너
  const renderConnectionStatus = () => {
    if (status === 'connecting') {
      return <div className="connection-banner connection-connecting">연결 중...</div>;
    }
    if (status === 'disconnected') {
      return <div className="connection-banner connection-disconnected">연결 끊김. 재연결 시도 중...</div>;
    }
    return null;
  };

  // 데이터가 없을 때
  if (!lastMessage) {
    return (
      <div className="dashboard-container">
        {renderConnectionStatus()}
        <div className="loading-message">데이터 수집 중...</div>
      </div>
    );
  }

  const { services, tps, alerts } = lastMessage.data;

  return (
    <div className="dashboard-container">
      {renderConnectionStatus()}

      <header className="dashboard-header">
        <h1>실시간 시스템 대시보드</h1>
        <div className="last-update">
          마지막 업데이트: {new Date(lastMessage.timestamp).toLocaleString('ko-KR')}
        </div>
      </header>

      {/* 서비스 상태 카드 */}
      <section className="dashboard-section">
        <h2 className="section-title">서비스 상태</h2>
        <div className="service-cards-grid">
          {services.map(service => (
            <ServiceHealthCard key={service.serviceName} service={service} />
          ))}
        </div>
      </section>

      {/* TPS 차트 */}
      <section className="dashboard-section">
        <TpsChart data={tps} />
      </section>

      {/* 알림 발생률 차트 */}
      <section className="dashboard-section">
        <AlertRateChart data={alerts} />
      </section>
    </div>
  );
};
```

**src/components/dashboard/DashboardLayout.css**:
```css
.dashboard-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.connection-banner {
  padding: 12px;
  text-align: center;
  font-weight: bold;
  margin-bottom: 20px;
  border-radius: 4px;
}

.connection-connecting {
  background-color: #fff3cd;
  color: #856404;
}

.connection-disconnected {
  background-color: #f8d7da;
  color: #721c24;
}

.dashboard-header {
  margin-bottom: 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dashboard-header h1 {
  margin: 0;
  font-size: 32px;
  color: #333;
}

.last-update {
  color: #666;
  font-size: 14px;
}

.loading-message {
  text-align: center;
  padding: 50px;
  font-size: 18px;
  color: #666;
}

.dashboard-section {
  margin-bottom: 40px;
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.section-title {
  font-size: 24px;
  margin-bottom: 20px;
  color: #333;
}

.service-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.chart-container {
  width: 100%;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-title {
  font-size: 20px;
  margin: 0;
  color: #333;
}

.chart-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.stat-value {
  font-weight: bold;
  font-size: 18px;
  color: #333;
}

.stat-current {
  color: #2196f3;
  font-size: 24px;
}

.stat-warning {
  color: #ff9800;
}

.warning-banner {
  background-color: #fff3cd;
  border: 1px solid #ff9800;
  color: #856404;
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 16px;
  font-weight: bold;
  text-align: center;
}
```

### Step 7: 대시보드 페이지 통합

**src/pages/DashboardPage.tsx**:
```typescript
import React from 'react';
import { DashboardLayout } from '../components/dashboard/DashboardLayout';

export const DashboardPage: React.FC = () => {
  return <DashboardLayout />;
};
```

**src/App.tsx** (라우팅 추가):
```typescript
import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import { DashboardPage } from './pages/DashboardPage';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        {/* 기존 라우트들... */}
      </Routes>
    </Router>
  );
}

export default App;
```

---

## 6. 테스트 가이드 (Testing Guide)

### 단위 테스트 (Unit Tests)

**백엔드 - MetricsStore 테스트**:
```java
package com.realfds.websocket.store;

import com.realfds.websocket.model.MetricsDataPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsStoreTest {
    private MetricsStore metricsStore;

    @BeforeEach
    void setUp() {
        metricsStore = new MetricsStore();
    }

    @Test
    void addDataPoint_성공() {
        // Given
        MetricsDataPoint point = MetricsDataPoint.builder()
            .timestamp(Instant.now())
            .tps(100L)
            .totalTransactions(1000L)
            .alertsPerMinute(5L)
            .byRule(Map.of("HIGH_VALUE", 2L, "FOREIGN_COUNTRY", 2L, "HIGH_FREQUENCY", 1L))
            .build();

        // When
        metricsStore.addDataPoint(point);

        // Then
        assertThat(metricsStore.size()).isEqualTo(1);
        assertThat(metricsStore.getAll()).contains(point);
    }

    @Test
    void addDataPoint_1시간_이전_데이터_자동_삭제() {
        // Given
        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(61, ChronoUnit.MINUTES);

        MetricsDataPoint oldPoint = MetricsDataPoint.builder()
            .timestamp(oneHourAgo)
            .tps(50L)
            .totalTransactions(500L)
            .alertsPerMinute(3L)
            .byRule(Map.of("HIGH_VALUE", 1L, "FOREIGN_COUNTRY", 1L, "HIGH_FREQUENCY", 1L))
            .build();

        MetricsDataPoint newPoint = MetricsDataPoint.builder()
            .timestamp(now)
            .tps(100L)
            .totalTransactions(1000L)
            .alertsPerMinute(5L)
            .byRule(Map.of("HIGH_VALUE", 2L, "FOREIGN_COUNTRY", 2L, "HIGH_FREQUENCY", 1L))
            .build();

        // When
        metricsStore.addDataPoint(oldPoint);
        metricsStore.addDataPoint(newPoint);

        // Then
        assertThat(metricsStore.size()).isEqualTo(1);
        assertThat(metricsStore.getAll()).doesNotContain(oldPoint);
        assertThat(metricsStore.getAll()).contains(newPoint);
    }

    @Test
    void getDataSince_특정_시각_이후_데이터_조회() {
        // Given
        Instant base = Instant.now().minus(30, ChronoUnit.MINUTES);
        Instant since = Instant.now().minus(15, ChronoUnit.MINUTES);

        MetricsDataPoint point1 = createDataPoint(base);
        MetricsDataPoint point2 = createDataPoint(since.plus(1, ChronoUnit.MINUTES));
        MetricsDataPoint point3 = createDataPoint(Instant.now());

        metricsStore.addDataPoint(point1);
        metricsStore.addDataPoint(point2);
        metricsStore.addDataPoint(point3);

        // When
        List<MetricsDataPoint> result = metricsStore.getDataSince(since);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).doesNotContain(point1);
        assertThat(result).contains(point2, point3);
    }

    private MetricsDataPoint createDataPoint(Instant timestamp) {
        return MetricsDataPoint.builder()
            .timestamp(timestamp)
            .tps(100L)
            .totalTransactions(1000L)
            .alertsPerMinute(5L)
            .byRule(Map.of("HIGH_VALUE", 2L, "FOREIGN_COUNTRY", 2L, "HIGH_FREQUENCY", 1L))
            .build();
    }
}
```

**프론트엔드 - useWebSocket 테스트**:
```typescript
import { renderHook, act, waitFor } from '@testing-library/react';
import { useWebSocket } from '../useWebSocket';

describe('useWebSocket', () => {
  let mockWebSocket: any;

  beforeEach(() => {
    mockWebSocket = {
      send: jest.fn(),
      close: jest.fn(),
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
      readyState: WebSocket.OPEN
    };

    global.WebSocket = jest.fn(() => mockWebSocket) as any;
  });

  it('초기 연결 시 상태가 connecting', () => {
    const { result } = renderHook(() => useWebSocket('ws://localhost:8083/ws/metrics'));

    expect(result.current.status).toBe('connecting');
  });

  it('연결 성공 시 상태가 connected', async () => {
    const { result } = renderHook(() => useWebSocket('ws://localhost:8083/ws/metrics'));

    act(() => {
      mockWebSocket.onopen();
    });

    await waitFor(() => {
      expect(result.current.status).toBe('connected');
    });
  });

  it('메시지 수신 시 lastMessage 업데이트', async () => {
    const { result } = renderHook(() => useWebSocket('ws://localhost:8083/ws/metrics'));

    const testMessage = {
      type: 'METRICS_UPDATE',
      timestamp: '2025-11-12T10:30:00Z',
      data: {
        services: [],
        tps: { current: 100, average: 80, max: 150, history: [] },
        alerts: { current: 10, average: 8, max: 20, byRule: {}, history: [] }
      }
    };

    act(() => {
      mockWebSocket.onopen();
      mockWebSocket.onmessage({ data: JSON.stringify(testMessage) });
    });

    await waitFor(() => {
      expect(result.current.lastMessage).toEqual(testMessage);
    });
  });
});
```

### 통합 테스트 (Integration Tests)

**시나리오 1: 서비스 중단 감지**
```bash
# 1. 모든 서비스 정상 실행
docker-compose up -d

# 2. 대시보드 접속
# http://localhost:3000

# 3. fraud-detector 서비스 중단
docker stop fraud-detector

# 4. 3초 이내에 대시보드에서 fraud-detector가 DOWN 상태로 표시되는지 확인

# 5. 서비스 재시작
docker start fraud-detector

# 6. 5초 이내에 대시보드에서 fraud-detector가 UP 상태로 표시되는지 확인
```

**시나리오 2: 브라우저 탭 전환 후 백필**
```bash
# 1. 대시보드 접속 후 1분간 차트 관찰
# 2. 다른 탭으로 전환 (5초 이상 유지)
# 3. 대시보드 탭으로 복귀
# 4. 누락된 데이터가 자동으로 채워지는지 (백필) 확인
```

### 성능 테스트

**메트릭 업데이트 지연 시간 측정**:
```bash
# Chrome DevTools Network 탭에서 WebSocket 메시지 확인
# 1. WebSocket 연결 필터 적용
# 2. 메시지 수신 시각 기록
# 3. 5초 간격으로 메시지가 도착하는지 확인
# 4. 평균 지연 시간 < 5초 검증
```

---

## 7. 기능 실행 (Running the Feature)

### 전체 시스템 시작

**Step 1: Kafka 및 서비스 시작**
```bash
# 프로젝트 루트 디렉토리에서
docker-compose up -d

# 모든 서비스 상태 확인
docker-compose ps
```

**Step 2: 백엔드 빌드 및 실행** (websocket-gateway)
```bash
cd websocket-gateway

# Maven 사용 시
mvn clean install
mvn spring-boot:run

# Gradle 사용 시
./gradlew clean build
./gradlew bootRun
```

**Step 3: 프론트엔드 실행** (frontend-dashboard)
```bash
cd frontend-dashboard

# 의존성 설치 (최초 1회)
npm install

# 개발 서버 실행
npm start
```

### 대시보드 접속

브라우저에서 http://localhost:3000 접속

**예상되는 화면 구성**:
1. 상단: 실시간 시스템 대시보드 제목, 마지막 업데이트 시각
2. 서비스 상태 섹션: 5개 서비스 카드 (UP/DOWN 상태)
3. TPS 차트 섹션: 최근 1시간 TPS 추이 (Line Chart)
4. 알림 발생률 차트 섹션: 최근 1시간 알림 추이 (Stacked Area Chart)

### 데모 시나리오

**시나리오 1: 정상 작동 확인**
1. 대시보드 접속
2. 5개 서비스가 모두 "UP" 상태로 표시되는지 확인
3. TPS 차트가 실시간으로 업데이트되는지 관찰 (5초마다)
4. 알림 발생률 차트가 실시간으로 업데이트되는지 관찰 (5초마다)

**시나리오 2: 서비스 장애 시뮬레이션**
```bash
# alert-service 중단
docker stop alert-service

# 대시보드에서 alert-service 카드가 빨간색 DOWN 상태로 전환되는지 확인 (3초 이내)

# alert-service 재시작
docker start alert-service

# 대시보드에서 alert-service 카드가 녹색 UP 상태로 전환되는지 확인 (5초 이내)
```

**시나리오 3: 거래량 증가 관찰**
```bash
# transaction-generator의 거래 생성 속도 증가 (환경 변수 조정)
# docker-compose.yml에서 TPS 설정 변경 후 재시작

# TPS 차트에서 그래프가 상승하는지 확인
# "현재 TPS" 수치가 큰 글씨로 강조 표시되는지 확인
```

---

## 8. 검증 체크리스트 (Verification Checklist)

### FR-001 ~ FR-006: 서비스 상태 모니터링

- [ ] 5개 서비스의 Health Check 상태가 수집됨
- [ ] 각 서비스 상태가 "UP" 또는 "DOWN"으로 분류됨
- [ ] 서비스 상태가 5초마다 자동 업데이트됨
- [ ] 서비스 중단 시 3초 이내에 대시보드에 반영됨
- [ ] 서비스 카드에 이름, 상태, 마지막 확인 시간이 표시됨
- [ ] 서비스 카드 클릭 시 상세 정보 모달이 열림

### FR-007 ~ FR-012: 실시간 거래량 메트릭

- [ ] 초당 거래 처리량(TPS)이 실시간으로 수집됨
- [ ] 최근 1시간 데이터가 유지되고 1시간 이전 데이터는 자동 제거됨
- [ ] 거래량 차트가 선 그래프(Line Chart)로 표시됨
- [ ] 거래량 메트릭이 5초마다 업데이트됨
- [ ] 현재 TPS, 평균 TPS, 최대 TPS가 숫자로 표시됨
- [ ] 차트에 마우스 커서 올릴 때 툴팁으로 정확한 값이 표시됨

### FR-013 ~ FR-020: 실시간 알림 발생률 메트릭

- [ ] 분당 알림 발생 수가 실시간으로 수집됨
- [ ] 최근 1시간 데이터가 유지되고 1시간 이전 데이터는 자동 제거됨
- [ ] 알림 발생률 차트가 영역 차트(Area Chart)로 표시됨
- [ ] 알림이 HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY로 분류됨
- [ ] 각 규칙이 다른 색상으로 스택(Stacked) 형태로 표시됨
- [ ] 차트 범례 클릭 시 해당 규칙 데이터가 숨김/표시됨
- [ ] 현재, 평균, 최대 알림 발생률이 숫자로 표시됨
- [ ] 알림 발생률이 평균의 2배 초과 시 차트 영역이 주황색으로 표시됨

### FR-021 ~ FR-026: 사용자 인터페이스 및 사용성

- [ ] 차트 업데이트 시 깜빡임이나 끊김 없음
- [ ] 대시보드 초기 로딩 시간 < 2초
- [ ] 메트릭 수치 업데이트 시 애니메이션 효과 적용
- [ ] WebSocket 연결 끊김 시 "연결 끊김" 배너 표시 및 자동 재연결 시도
- [ ] 데이터 부족 시 "데이터 수집 중..." 메시지 표시
- [ ] 차트 Y축 범위가 데이터에 맞춰 자동 조정됨

### SC-001 ~ SC-008: 성공 기준

- [ ] 메트릭 업데이트가 평균 4초, 최대 5초 이내 완료
- [ ] 차트가 부드럽게 애니메이션되며 끊김 없음
- [ ] 서비스 중단 시 평균 2초, 최대 3초 이내 DOWN 상태 표시
- [ ] 대시보드가 2초 이내에 로딩 완료
- [ ] 1시간 지속 사용 시 성능 저하 없음
- [ ] 5명 동시 사용 시 각 화면이 독립적으로 정상 작동
- [ ] 서비스 장애 발견 및 상세 정보 확인이 평균 10초 이내
- [ ] 차트 특정 시점 수치 확인이 2초 이내

---

## 9. 문제 해결 (Troubleshooting)

### 문제 1: WebSocket 연결 실패

**증상**:
- 대시보드에 "연결 끊김" 배너가 계속 표시됨
- Chrome DevTools Console에 "WebSocket connection failed" 에러

**원인**:
- websocket-gateway 서비스가 실행되지 않음
- CORS 설정 오류
- 방화벽이 WebSocket 포트(8083)를 차단

**해결**:
```bash
# 1. websocket-gateway 서비스 상태 확인
docker ps | grep websocket-gateway

# 2. 서비스 로그 확인
docker logs websocket-gateway

# 3. Health Check 엔드포인트 확인
curl http://localhost:8083/actuator/health

# 4. CORS 설정 확인 (WebSocketConfig.java)
# setAllowedOrigins("http://localhost:3000") 확인

# 5. 프론트엔드 환경 변수 확인
echo $REACT_APP_WEBSOCKET_URL
# 결과: ws://localhost:8083/ws/metrics
```

### 문제 2: 차트가 업데이트되지 않음

**증상**:
- 서비스 상태 카드는 정상이지만 차트가 고정됨
- TPS/알림 수치가 0으로 표시됨

**원인**:
- Kafka 메트릭 수집 실패
- MetricsStore에 데이터가 추가되지 않음
- 스케줄러가 실행되지 않음

**해결**:
```bash
# 1. Kafka 브로커 상태 확인
docker ps | grep kafka

# 2. Kafka 토픽 확인
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
# virtual-transactions, alerts 토픽 존재 확인

# 3. 백엔드 로그 확인
docker logs websocket-gateway | grep "메트릭 수집"
# "메트릭 수집 완료" 로그가 5초마다 출력되는지 확인

# 4. MetricsStore 크기 확인 (디버깅 엔드포인트 추가 필요)
curl http://localhost:8083/actuator/metrics/metricsstore.size

# 5. @EnableScheduling 어노테이션 확인
# Application.java에 @EnableScheduling이 추가되어 있는지 확인
```

### 문제 3: 서비스 Health Check 타임아웃

**증상**:
- 모든 서비스가 DOWN 상태로 표시됨
- 로그에 "Health check 응답 시간 초과" 메시지 반복

**원인**:
- Health Check 타임아웃 설정이 너무 짧음 (< 100ms)
- 서비스가 실제로 응답하지 않음
- 네트워크 지연

**해결**:
```bash
# 1. 각 서비스의 Health Check 엔드포인트 수동 확인
curl http://localhost:8080/actuator/health  # transaction-generator
curl http://localhost:8081/actuator/health  # fraud-detector
curl http://localhost:8082/actuator/health  # alert-service

# 2. 응답 시간 측정
time curl http://localhost:8080/actuator/health

# 3. application.yml 타임아웃 설정 조정
metrics:
  collection:
    health-check-timeout: 5000  # 3초 → 5초로 증가

# 4. 서비스 재시작
mvn spring-boot:run
```

### 문제 4: 메모리 누수

**증상**:
- 1시간 이상 실행 시 메모리 사용량이 계속 증가
- OutOfMemoryError 발생

**원인**:
- MetricsStore의 자동 삭제 로직이 작동하지 않음
- WebSocket 세션이 정리되지 않음

**해결**:
```bash
# 1. JVM 메모리 사용량 모니터링
jconsole  # 또는 VisualVM

# 2. MetricsStore.size() 확인
# 720개를 초과하는지 확인

# 3. MetricsStore 자동 삭제 로직 디버깅
# addDataPoint() 메소드에 로그 추가
log.debug("오래된 데이터 삭제: 삭제된 개수={}", deletedCount);

# 4. WebSocket 세션 개수 확인
log.info("활성 WebSocket 세션 수: {}", sessions.size());

# 5. 닫힌 세션 정리 로직 확인
# afterConnectionClosed() 메소드가 호출되는지 확인
```

### 문제 5: 차트 렌더링 성능 저하

**증상**:
- 차트 업데이트 시 화면이 버벅임
- 프레임율 < 30fps

**원인**:
- 데이터 포인트가 너무 많음 (> 720개)
- Recharts dot 속성이 true로 설정됨
- 불필요한 재렌더링 발생

**해결**:
```typescript
// 1. Recharts dot 속성 확인
<Line
  type="monotone"
  dataKey="tps"
  dot={false}  // false로 설정 (성능 향상)
  isAnimationActive={true}
  animationDuration={500}
/>

// 2. useMemo로 차트 데이터 메모이제이션
const chartData = useMemo(() => {
  return data.history.map(point => ({
    timestamp: new Date(point.timestamp).getTime(),
    tps: point.tps
  }));
}, [data.history]);

// 3. React DevTools Profiler로 렌더링 성능 측정
// Chrome DevTools > React Profiler 탭

// 4. 데이터 포인트 수 제한 (디버깅용)
const chartData = data.history.slice(-360);  // 최근 30분만 표시
```

---

## 10. 다음 단계 (Next Steps)

### 관련 기능 링크

- **003-alert-history**: 알림 이력 조회 및 필터링 기능 (이전 구현)
- **005-alert-analytics** (예정): 알림 통계 및 분석 대시보드
- **006-dynamic-rules** (예정): 동적 규칙 설정 및 관리

### 성능 최적화 제안

1. **Prometheus + Grafana 통합** (장기 메트릭 저장)
   - 현재: 메모리 기반 (1시간)
   - 개선: InfluxDB 또는 Prometheus로 메트릭 영구 저장
   - 효과: 과거 데이터 분석, 장기 추세 파악

2. **Circuit Breaker 추가** (서비스 장애 시 부하 감소)
   - 현재: 타임아웃/재시도
   - 개선: Resilience4j Circuit Breaker 도입
   - 효과: 장애 서비스에 대한 불필요한 요청 방지

3. **WebSocket 클러스터링** (다수 사용자 지원)
   - 현재: 단일 WebSocket 서버 (최대 5명)
   - 개선: Redis Pub/Sub + 여러 WebSocket 서버
   - 효과: 50명 이상 동시 사용자 지원

4. **차트 라이브러리 교체** (대용량 데이터 처리)
   - 현재: Recharts (최대 720개 데이터 포인트)
   - 개선: Chart.js 또는 ECharts (10,000개 이상)
   - 효과: 더 긴 시간 범위 (24시간, 1주일) 지원

### 향후 개선 사항

1. **알림 임계값 설정 기능**
   - TPS가 특정 값 이하로 떨어지면 알림 발송
   - 알림 발생률이 평균의 3배 초과 시 긴급 알림

2. **대시보드 레이아웃 커스터마이징**
   - 사용자별 위젯 배치 저장
   - 차트 크기, 색상 테마 변경

3. **메트릭 데이터 내보내기**
   - CSV, JSON 파일로 다운로드
   - 리포트 생성 기능

4. **여러 대시보드 화면 지원**
   - 서비스별 상세 대시보드
   - 알림 중심 대시보드
   - 성능 중심 대시보드

---

## 11. 참고 자료 (References)

### 공식 문서

- [Spring Boot Actuator Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Spring WebFlux WebClient Documentation](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html)
- [WebSocket API - MDN](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [Recharts Documentation](https://recharts.org/en-US/)
- [Java ConcurrentLinkedDeque](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentLinkedDeque.html)

### 모범 사례

- [WebSocket Architecture Best Practices - Ably](https://ably.com/topic/websocket-architecture-best-practices)
- [Spring Boot Health Indicators - Baeldung](https://www.baeldung.com/spring-boot-health-indicators)
- [Best React Chart Libraries 2024 - LogRocket](https://blog.logrocket.com/best-react-chart-libraries-2025/)
- [Real-time Dashboard with WebSockets - Ably](https://ably.com/blog/websockets-react-tutorial)

### Constitution 준수 노트

본 구현은 RealFDS 프로젝트 Constitution의 모든 원칙을 준수합니다:

- **I. 학습 우선**: Spring Actuator 표준 엔드포인트, Recharts 선언적 API 사용
- **II. 단순함 우선**: 외부 의존성 최소화 (Prometheus, 시계열 DB 미사용)
- **III. 실시간 우선**: WebSocket 브로드캐스트, 5초 이내 업데이트
- **IV. 마이크로서비스 경계**: 기존 5개 서비스 구조 유지, 추가 서비스 없음
- **V. 테스트 및 품질**: 단위/통합 테스트, 타입 안전성, 포괄적 로깅
- **VI. 한국어 우선**: 모든 문서, 주석, 로그 메시지 한국어 작성

---

**문서 완료**: 이 Quickstart Guide는 개발자가 004-dashboard-realtime 기능을 신속하게 이해하고 구현할 수 있도록 실용적인 가이드를 제공합니다. 추가 질문이나 문제 발생 시 [GitHub Issues](https://github.com/your-repo/RealFDS/issues)에 보고해 주세요.
