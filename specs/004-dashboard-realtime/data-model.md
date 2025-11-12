# Data Model: 실시간 시스템 대시보드

**Feature**: 004-dashboard-realtime
**Version**: 1.0
**Last Updated**: 2025-11-12

---

## Overview

본 데이터 모델은 실시간 시스템 대시보드의 핵심 엔티티와 메시지 형식을 정의합니다. 5개 마이크로서비스의 Health Check 상태, 초당 거래 처리량(TPS), 분당 알림 발생률을 실시간으로 수집·저장·전송하기 위한 데이터 구조와 메모리 관리 전략을 포함합니다.

**설계 원칙**:
- **메모리 기반 저장소**: 최근 1시간 데이터만 메모리에 보관 (데이터베이스 미사용)
- **실시간 전송**: WebSocket을 통한 JSON 메시지 브로드캐스트
- **타입 안전성**: Java와 TypeScript의 명시적 타입 정의
- **단순성**: Java 표준 라이브러리(`ConcurrentLinkedDeque`) 사용

---

## Core Entities

### 1. ServiceHealth (서비스 상태)

**Purpose**: 5개 마이크로서비스의 Health Check 상태와 성능 지표를 나타냅니다.

**Fields**:
| Field Name | Type | Required | Validation | Description |
|------------|------|----------|------------|-------------|
| serviceName | String | Yes | Enum: [transaction-generator, fraud-detector, alert-service, websocket-gateway, frontend-dashboard] | 서비스 식별자 |
| status | String | Yes | Enum: [UP, DOWN] | 현재 서비스 상태 |
| lastChecked | Instant | Yes | ISO-8601 timestamp | 마지막 Health Check 수행 시각 |
| responseTime | Long | No | 0 ≤ value ≤ 5000 (ms) | Health Check 응답 시간 (밀리초 단위). 타임아웃(3초) 초과 시 null |
| memoryUsage | Long | No | 0 ≤ value ≤ 8192 (MB) | JVM 메모리 사용량 (메가바이트 단위). DOWN 상태 시 null |
| errorType | String | No | Enum: [TIMEOUT, HTTP_ERROR, NETWORK_ERROR] | DOWN 상태 시 오류 유형 |
| errorMessage | String | No | max 256 chars | DOWN 상태 시 오류 상세 메시지 |

**Validation Rules**:
- **FR-001**: `serviceName`은 5개 마이크로서비스 중 하나여야 함
- **FR-002**: `status`는 "UP" 또는 "DOWN" 중 하나
- **FR-004**: `lastChecked`는 현재 시각으로부터 10초 이내 (5초 폴링 + 5초 여유)
- **FR-006**: UP 상태일 때 `responseTime`과 `memoryUsage`는 non-null
- DOWN 상태일 때 `errorType`과 `errorMessage`는 non-null

**State Transitions**:
```
[초기 상태] ---> [UP] (Health Check 성공)
[UP] ---> [DOWN] (타임아웃 또는 HTTP 에러)
[DOWN] ---> [UP] (Health Check 성공, 복구)
```

**State Transition Conditions**:
- **UP → DOWN**:
  - Health Check 응답 시간 > 3초 (타임아웃)
  - HTTP 4xx/5xx 에러
  - 네트워크 연결 실패 (Connection Refused, DNS 실패)
- **DOWN → UP**:
  - Health Check 응답 성공 (status: "UP")
  - 응답 시간 < 3초

**Lifecycle**:
- **Created**: 시스템 시작 시 5개 서비스에 대한 ServiceHealth 초기화 (status: "DOWN", lastChecked: null)
- **Updated**: 5초마다 Health Check 수행 후 상태 업데이트
- **Deleted**: 없음 (서비스 개수 고정, 런타임 중 삭제 불가)

---

### 2. TransactionMetrics (거래량 메트릭)

**Purpose**: 초당 거래 처리량(TPS)을 시계열 데이터로 관리합니다.

**Fields**:
| Field Name | Type | Required | Validation | Description |
|------------|------|----------|------------|-------------|
| timestamp | Instant | Yes | ISO-8601 timestamp, 5초 단위 정렬 | 메트릭 측정 시각 |
| tps | Long | Yes | 0 ≤ value ≤ 10000 | 초당 거래 수 (Transactions Per Second) |
| totalTransactions | Long | Yes | value ≥ 0 | 시스템 시작 이후 누적 거래 수 |

**Validation Rules**:
- **FR-007**: `tps`는 Kafka `virtual-transactions` 토픽의 offset 증가량으로 계산
- **FR-008**: 1시간(720개 데이터 포인트) 이상의 데이터는 자동 제거
- **FR-010**: `timestamp`는 5초 간격으로 정렬 (예: 10:00:00, 10:00:05, 10:00:10, ...)
- `tps`는 음수 불가 (초당 처리량)
- `totalTransactions`는 단조 증가 (monotonically increasing)

**Computed Fields** (프론트엔드 계산):
| Field Name | Formula | Description |
|------------|---------|-------------|
| averageTps | `sum(tps) / count(dataPoints)` | 1시간 평균 TPS |
| maxTps | `max(tps)` | 1시간 최대 TPS |
| currentTps | `tps[latest]` | 가장 최근 TPS |

**State Transitions**: 없음 (상태 개념 없는 시계열 데이터)

**Lifecycle**:
- **Created**: 5초마다 새로운 `TransactionMetrics` 데이터 포인트 생성
- **Updated**: 없음 (불변 데이터, 생성 후 수정 불가)
- **Deleted**: `timestamp`가 1시간 이전인 데이터 포인트 자동 삭제

---

### 3. AlertMetrics (알림 메트릭)

**Purpose**: 분당 알림 발생 수를 규칙별로 분류하여 관리합니다.

**Fields**:
| Field Name | Type | Required | Validation | Description |
|------------|------|----------|------------|-------------|
| timestamp | Instant | Yes | ISO-8601 timestamp, 5초 단위 정렬 | 메트릭 측정 시각 |
| alertsPerMinute | Long | Yes | 0 ≤ value ≤ 1000 | 분당 총 알림 발생 수 |
| byRule | Map<String, Long> | Yes | Keys: [HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY] | 규칙별 알림 발생 수 |

**Validation Rules**:
- **FR-013**: `alertsPerMinute`는 최근 1분간 발생한 알림 수 (Kafka `alerts` 토픽 집계)
- **FR-014**: 1시간(720개 데이터 포인트) 이상의 데이터는 자동 제거
- **FR-016**: `byRule`은 3개 규칙(HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY)만 포함
- **FR-020**: `alertsPerMinute > averageAlerts * 2` 시 경고 상태
- `alertsPerMinute`는 `sum(byRule.values())`와 일치해야 함
- 모든 규칙의 알림 수는 음수 불가

**Computed Fields** (프론트엔드 계산):
| Field Name | Formula | Description |
|------------|---------|-------------|
| averageAlerts | `sum(alertsPerMinute) / count(dataPoints)` | 1시간 평균 알림 발생률 |
| maxAlerts | `max(alertsPerMinute)` | 1시간 최대 알림 발생률 |
| currentAlerts | `alertsPerMinute[latest]` | 가장 최근 알림 발생률 |
| isWarning | `currentAlerts > averageAlerts * 2` | 경고 상태 여부 (Boolean) |

**State Transitions**: 없음 (상태 개념 없는 시계열 데이터)

**Lifecycle**:
- **Created**: 5초마다 새로운 `AlertMetrics` 데이터 포인트 생성
- **Updated**: 없음 (불변 데이터, 생성 후 수정 불가)
- **Deleted**: `timestamp`가 1시간 이전인 데이터 포인트 자동 삭제

---

## WebSocket Message Formats

### METRICS_UPDATE Message

**Direction**: Server → Client
**Frequency**: 5초마다 브로드캐스트
**Purpose**: 모든 메트릭 데이터를 클라이언트에 실시간 전송

**JSON Schema**:
```json
{
  "type": "METRICS_UPDATE",
  "timestamp": "2025-11-12T10:30:05Z",
  "data": {
    "services": [
      {
        "serviceName": "transaction-generator",
        "status": "UP",
        "lastChecked": "2025-11-12T10:30:05Z",
        "responseTime": 45,
        "memoryUsage": 128,
        "errorType": null,
        "errorMessage": null
      },
      {
        "serviceName": "fraud-detector",
        "status": "DOWN",
        "lastChecked": "2025-11-12T10:30:02Z",
        "responseTime": null,
        "memoryUsage": null,
        "errorType": "TIMEOUT",
        "errorMessage": "Health check 응답 시간 초과 (>3초)"
      }
      // ... 나머지 3개 서비스
    ],
    "tps": {
      "current": 87,
      "average": 65,
      "max": 150,
      "history": [
        { "timestamp": "2025-11-12T09:30:05Z", "tps": 60, "totalTransactions": 216000 },
        { "timestamp": "2025-11-12T09:30:10Z", "tps": 65, "totalTransactions": 216325 }
        // ... 최근 1시간(720개) 데이터 포인트
      ]
    },
    "alerts": {
      "current": 12,
      "average": 8,
      "max": 25,
      "byRule": {
        "HIGH_VALUE": 5,
        "FOREIGN_COUNTRY": 4,
        "HIGH_FREQUENCY": 3
      },
      "history": [
        {
          "timestamp": "2025-11-12T09:30:05Z",
          "alertsPerMinute": 8,
          "byRule": { "HIGH_VALUE": 3, "FOREIGN_COUNTRY": 3, "HIGH_FREQUENCY": 2 }
        },
        {
          "timestamp": "2025-11-12T09:30:10Z",
          "alertsPerMinute": 10,
          "byRule": { "HIGH_VALUE": 4, "FOREIGN_COUNTRY": 4, "HIGH_FREQUENCY": 2 }
        }
        // ... 최근 1시간(720개) 데이터 포인트
      ]
    }
  }
}
```

**Field Descriptions**:
- `type`: 메시지 타입 (항상 "METRICS_UPDATE")
- `timestamp`: 메시지 생성 시각 (클라이언트 백필 요청 시 사용)
- `data.services`: 5개 서비스의 현재 상태 (배열)
- `data.tps.current`: 현재 TPS
- `data.tps.average`: 1시간 평균 TPS
- `data.tps.max`: 1시간 최대 TPS
- `data.tps.history`: 최근 1시간 TPS 시계열 데이터 (배열, 720개)
- `data.alerts.current`: 현재 분당 알림 수
- `data.alerts.average`: 1시간 평균 알림 발생률
- `data.alerts.max`: 1시간 최대 알림 발생률
- `data.alerts.byRule`: 현재 규칙별 알림 수 (객체)
- `data.alerts.history`: 최근 1시간 알림 발생률 시계열 데이터 (배열, 720개)

---

### BACKFILL_REQUEST Message

**Direction**: Client → Server
**Frequency**: WebSocket 재연결 시 1회
**Purpose**: 연결 끊김 중 누락된 메트릭 데이터 요청

**JSON Schema**:
```json
{
  "type": "BACKFILL_REQUEST",
  "lastReceivedTimestamp": "2025-11-12T10:25:00Z"
}
```

**Field Descriptions**:
- `type`: 메시지 타입 (항상 "BACKFILL_REQUEST")
- `lastReceivedTimestamp`: 클라이언트가 마지막으로 수신한 메트릭의 타임스탬프

**Validation**:
- `lastReceivedTimestamp`는 현재 시각으로부터 1시간 이내 (이전 데이터는 이미 삭제됨)

---

### BACKFILL_RESPONSE Message

**Direction**: Server → Client
**Frequency**: BACKFILL_REQUEST 수신 시 1회
**Purpose**: 누락된 메트릭 데이터 전송

**JSON Schema**:
```json
{
  "type": "BACKFILL_RESPONSE",
  "data": [
    {
      "timestamp": "2025-11-12T10:25:05Z",
      "tps": 65,
      "totalTransactions": 234000,
      "alertsPerMinute": 8,
      "byRule": { "HIGH_VALUE": 3, "FOREIGN_COUNTRY": 3, "HIGH_FREQUENCY": 2 }
    },
    {
      "timestamp": "2025-11-12T10:25:10Z",
      "tps": 70,
      "totalTransactions": 234350,
      "alertsPerMinute": 9,
      "byRule": { "HIGH_VALUE": 4, "FOREIGN_COUNTRY": 3, "HIGH_FREQUENCY": 2 }
    }
    // ... lastReceivedTimestamp 이후의 모든 데이터 포인트
  ]
}
```

**Field Descriptions**:
- `type`: 메시지 타입 (항상 "BACKFILL_RESPONSE")
- `data`: `lastReceivedTimestamp` 이후의 모든 메트릭 데이터 포인트 배열 (시간 순서 정렬)

**Validation**:
- `data` 배열의 크기는 최대 720개 (1시간 데이터)
- 모든 `timestamp`는 `lastReceivedTimestamp`보다 나중 시각

---

## Backend Implementation (Java)

### 1. ServiceHealth Entity

```java
package com.realfds.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * 마이크로서비스의 Health Check 상태를 나타내는 엔티티
 *
 * @see com.realfds.dashboard.collector.HealthCheckCollector
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealth {
    /**
     * 서비스 식별자 (5개 중 하나)
     * - transaction-generator
     * - fraud-detector
     * - alert-service
     * - websocket-gateway
     * - frontend-dashboard
     */
    private String serviceName;

    /**
     * 현재 서비스 상태 (UP 또는 DOWN)
     */
    private ServiceStatus status;

    /**
     * 마지막 Health Check 수행 시각 (ISO-8601 형식)
     */
    private Instant lastChecked;

    /**
     * Health Check 응답 시간 (밀리초 단위)
     * DOWN 상태일 경우 null
     */
    private Long responseTime;

    /**
     * JVM 메모리 사용량 (메가바이트 단위)
     * DOWN 상태일 경우 null
     */
    private Long memoryUsage;

    /**
     * DOWN 상태 시 오류 유형
     * UP 상태일 경우 null
     */
    private ErrorType errorType;

    /**
     * DOWN 상태 시 오류 상세 메시지 (최대 256자)
     * UP 상태일 경우 null
     */
    private String errorMessage;

    /**
     * 서비스 상태 Enum
     */
    public enum ServiceStatus {
        UP, DOWN
    }

    /**
     * 오류 유형 Enum
     */
    public enum ErrorType {
        TIMEOUT,        // Health Check 응답 시간 초과 (>3초)
        HTTP_ERROR,     // HTTP 4xx/5xx 에러
        NETWORK_ERROR   // 네트워크 연결 실패
    }
}
```

---

### 2. TransactionMetrics Entity

```java
package com.realfds.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * 거래량 메트릭 데이터 포인트 (시계열)
 *
 * @see com.realfds.dashboard.collector.KafkaMetricsCollector
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMetrics {
    /**
     * 메트릭 측정 시각 (5초 간격, ISO-8601 형식)
     */
    private Instant timestamp;

    /**
     * 초당 거래 수 (TPS)
     * 0 이상 10,000 이하
     */
    private long tps;

    /**
     * 시스템 시작 이후 누적 거래 수
     * 단조 증가 (monotonically increasing)
     */
    private long totalTransactions;
}
```

---

### 3. AlertMetrics Entity

```java
package com.realfds.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.Map;

/**
 * 알림 발생률 메트릭 데이터 포인트 (시계열)
 *
 * @see com.realfds.dashboard.collector.KafkaMetricsCollector
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertMetrics {
    /**
     * 메트릭 측정 시각 (5초 간격, ISO-8601 형식)
     */
    private Instant timestamp;

    /**
     * 분당 총 알림 발생 수
     * 0 이상 1,000 이하
     */
    private long alertsPerMinute;

    /**
     * 규칙별 알림 발생 수
     * Keys: HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY
     * Values: 각 규칙의 알림 수 (0 이상)
     *
     * 제약: sum(byRule.values()) == alertsPerMinute
     */
    private Map<String, Long> byRule;
}
```

---

### 4. MetricsDataPoint (Unified)

```java
package com.realfds.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.Map;

/**
 * TPS와 알림 발생률을 통합한 메트릭 데이터 포인트
 * 메모리 저장소(ConcurrentLinkedDeque)에 사용
 *
 * @see com.realfds.dashboard.store.MetricsStore
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsDataPoint {
    /**
     * 메트릭 측정 시각 (5초 간격)
     */
    private Instant timestamp;  // 8 bytes

    /**
     * 초당 거래 수 (TPS)
     */
    private long tps;  // 8 bytes

    /**
     * 누적 거래 수
     */
    private long totalTransactions;  // 8 bytes

    /**
     * 분당 알림 발생 수
     */
    private long alertsPerMinute;  // 8 bytes

    /**
     * 규칙별 알림 발생 수
     * 평균 ~50 bytes (3개 규칙)
     */
    private Map<String, Long> byRule;

    // 총 메모리 사용량: 약 150 bytes per data point
}
```

---

### 5. MetricsStore (Data Storage)

```java
package com.realfds.dashboard.store;

import com.realfds.dashboard.model.MetricsDataPoint;
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
 * 메모리 사용량: 약 160KB (720 × 150 bytes × 1.5 오버헤드)
 */
@Slf4j
@Service
public class MetricsStore {
    /**
     * 최대 데이터 포인트 수: 1시간 = 3600초 / 5초 = 720개
     */
    private static final int MAX_DATA_POINTS = 720;

    /**
     * 데이터 보관 기간: 1시간
     */
    private static final Duration RETENTION_PERIOD = Duration.ofHours(1);

    /**
     * 시계열 데이터 저장소 (Thread-safe, Lock-free)
     */
    private final ConcurrentLinkedDeque<MetricsDataPoint> dataPoints = new ConcurrentLinkedDeque<>();

    /**
     * 새로운 메트릭 데이터 포인트 추가
     * 1시간 이전 데이터 자동 삭제 (FIFO)
     *
     * 시간 복잡도: O(1) - addLast()
     * 공간 복잡도: O(720) - 고정 크기
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
     *
     * 시간 복잡도: O(n), n=720
     *
     * @return 메트릭 데이터 포인트 리스트 (시간 순서 정렬)
     */
    public List<MetricsDataPoint> getAll() {
        return new ArrayList<>(dataPoints);
    }

    /**
     * 특정 시각 이후의 메트릭 데이터 조회 (백필 용도)
     *
     * 시간 복잡도: O(n), n=720
     *
     * @param since 조회 시작 시각
     * @return since 이후의 메트릭 데이터 포인트 리스트
     */
    public List<MetricsDataPoint> getDataSince(Instant since) {
        return dataPoints.stream()
                .filter(dp -> dp.getTimestamp().isAfter(since))
                .collect(Collectors.toList());
    }

    /**
     * 현재 저장된 데이터 포인트 수 조회
     *
     * @return 데이터 포인트 수 (0 ≤ count ≤ 720)
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

---

## Frontend Implementation (TypeScript)

### 1. ServiceHealth Interface

```typescript
/**
 * 마이크로서비스의 Health Check 상태
 */
export interface ServiceHealth {
  /** 서비스 식별자 */
  serviceName: 'transaction-generator' | 'fraud-detector' | 'alert-service' | 'websocket-gateway' | 'frontend-dashboard';

  /** 현재 서비스 상태 */
  status: 'UP' | 'DOWN';

  /** 마지막 Health Check 수행 시각 (ISO-8601) */
  lastChecked: string;

  /** Health Check 응답 시간 (밀리초 단위, DOWN 시 null) */
  responseTime: number | null;

  /** JVM 메모리 사용량 (메가바이트 단위, DOWN 시 null) */
  memoryUsage: number | null;

  /** DOWN 상태 시 오류 유형 (UP 시 null) */
  errorType: 'TIMEOUT' | 'HTTP_ERROR' | 'NETWORK_ERROR' | null;

  /** DOWN 상태 시 오류 상세 메시지 (UP 시 null) */
  errorMessage: string | null;
}
```

---

### 2. TransactionMetrics Interface

```typescript
/**
 * 거래량 메트릭 데이터 포인트
 */
export interface TransactionMetrics {
  /** 메트릭 측정 시각 (ISO-8601) */
  timestamp: string;

  /** 초당 거래 수 (TPS) */
  tps: number;

  /** 누적 거래 수 */
  totalTransactions: number;
}

/**
 * 거래량 메트릭 집계 데이터
 */
export interface TpsAggregated {
  /** 현재 TPS */
  current: number;

  /** 1시간 평균 TPS */
  average: number;

  /** 1시간 최대 TPS */
  max: number;

  /** 최근 1시간 TPS 시계열 데이터 (720개) */
  history: TransactionMetrics[];
}
```

---

### 3. AlertMetrics Interface

```typescript
/**
 * 알림 발생률 메트릭 데이터 포인트
 */
export interface AlertMetrics {
  /** 메트릭 측정 시각 (ISO-8601) */
  timestamp: string;

  /** 분당 총 알림 발생 수 */
  alertsPerMinute: number;

  /** 규칙별 알림 발생 수 */
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
  /** 현재 분당 알림 수 */
  current: number;

  /** 1시간 평균 알림 발생률 */
  average: number;

  /** 1시간 최대 알림 발생률 */
  max: number;

  /** 현재 규칙별 알림 수 */
  byRule: {
    HIGH_VALUE: number;
    FOREIGN_COUNTRY: number;
    HIGH_FREQUENCY: number;
  };

  /** 최근 1시간 알림 발생률 시계열 데이터 (720개) */
  history: AlertMetrics[];

  /** 경고 상태 여부 (current > average * 2) */
  isWarning?: boolean;
}
```

---

### 4. WebSocket Message Types

```typescript
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

/**
 * WebSocket 메시지 Union Type
 */
export type WebSocketMessage = MetricsUpdateMessage | BackfillRequestMessage | BackfillResponseMessage;
```

---

### 5. Chart Data Formats (Recharts)

```typescript
/**
 * TPS 차트 데이터 형식 (Line Chart)
 */
export interface TpsChartData {
  /** X축: 시각 (ISO-8601) */
  timestamp: string;

  /** Y축: TPS */
  tps: number;
}

/**
 * 알림 발생률 차트 데이터 형식 (Stacked Area Chart)
 */
export interface AlertRateChartData {
  /** X축: 시각 (ISO-8601) */
  timestamp: string;

  /** Y축 (스택): HIGH_VALUE 규칙 알림 수 */
  HIGH_VALUE: number;

  /** Y축 (스택): FOREIGN_COUNTRY 규칙 알림 수 */
  FOREIGN_COUNTRY: number;

  /** Y축 (스택): HIGH_FREQUENCY 규칙 알림 수 */
  HIGH_FREQUENCY: number;
}
```

---

## Data Storage Strategy

### 1. Circular Buffer Implementation

**선택 근거**:
- **단순성**: Java 표준 라이브러리(`ConcurrentLinkedDeque`) 사용, 외부 의존성 불필요
- **스레드 안전성**: Lock-free CAS(Compare-And-Swap) 알고리즘, 멀티 스레드 환경에서 안전
- **O(1) 성능**: 삽입(`addLast()`), 삭제(`removeFirst()`) 모두 상수 시간
- **메모리 효율**: 1시간 데이터만 유지, 예측 가능한 메모리 사용량 (<1MB)

**데이터 구조**:
```
[가장 오래된 데이터] ← removeFirst()
         ↓
    [데이터 2]
         ↓
    [데이터 3]
         ↓
       ...
         ↓
    [데이터 719]
         ↓
[가장 최신 데이터] ← addLast()
```

**작동 방식**:
1. **삽입**: 5초마다 새로운 `MetricsDataPoint`를 `addLast()`로 추가
2. **삭제**: 1시간(3600초) 이전 데이터를 `removeFirst()`로 자동 제거
3. **조회**: `getAll()`로 전체 데이터 조회 (스냅샷, 불변)
4. **백필**: `getDataSince(Instant)`로 특정 시각 이후 데이터 조회

---

### 2. Memory Estimation

**단일 데이터 포인트 크기**:
```
Instant timestamp:        8 bytes
long tps:                 8 bytes
long totalTransactions:   8 bytes
long alertsPerMinute:     8 bytes
Map<String, Long> byRule: ~50 bytes (3개 규칙)
------------------------------------------
Total:                    ~82 bytes (순수 데이터)
```

**메모리 오버헤드** (Deque 노드):
```
ConcurrentLinkedDeque.Node:
- item reference:  8 bytes
- prev reference:  8 bytes
- next reference:  8 bytes
------------------------------------------
Total per node:    24 bytes
```

**총 메모리 사용량**:
```
1시간 데이터 (720개):
- 데이터: 720 × 82 bytes = 59 KB
- 노드: 720 × 24 bytes = 17 KB
- 오버헤드 (50%): 38 KB
------------------------------------------
Total:              ~114 KB

5개 서비스 상태:
- ServiceHealth: 5 × 100 bytes = 500 bytes (무시 가능)

전체 메모리 사용량: < 1 MB
```

**확장성 고려**:
- **10,000 TPS**: 메모리 사용량 동일 (TPS 값 크기 고정, long 타입)
- **10개 서비스**: ServiceHealth 1KB 추가 (무시 가능)
- **10개 규칙**: Map<String, Long> 크기 증가 (~100 bytes), 총 ~200 KB

---

### 3. Data Retention Policy

**보관 기간**: 1시간 (720개 데이터 포인트)

**자동 삭제 로직**:
```java
// 새 데이터 추가 시 실행
Instant cutoff = Instant.now().minus(Duration.ofHours(1));
while (!dataPoints.isEmpty() && dataPoints.peekFirst().getTimestamp().isBefore(cutoff)) {
    dataPoints.removeFirst();  // O(1) 삭제
}
```

**삭제 조건**:
- `timestamp < (현재 시각 - 1시간)` 인 데이터 포인트
- FIFO(First-In-First-Out) 방식

**예시**:
```
현재 시각: 2025-11-12 10:30:00
보관 기간: 2025-11-12 09:30:00 ~ 10:30:00

[09:29:55] ← 삭제 (1시간 이전)
[09:30:00] ← 유지
[09:30:05] ← 유지
...
[10:29:55] ← 유지
[10:30:00] ← 유지 (최신)
```

---

### 4. Thread Safety

**동시성 제어**:
- `ConcurrentLinkedDeque`는 내부적으로 Lock-free CAS 알고리즘 사용
- 추가 동기화(lock, synchronized) 불필요
- 여러 스레드가 동시에 삽입, 삭제, 조회 가능

**스레드 안전성 보장**:
```java
// 스레드 1: 메트릭 수집 (5초마다)
@Scheduled(fixedRate = 5000)
public void collectMetrics() {
    metricsStore.addDataPoint(newPoint);  // Thread-safe
}

// 스레드 2: WebSocket 브로드캐스트 (5초마다)
@Scheduled(fixedRate = 5000)
public void broadcastMetrics() {
    List<MetricsDataPoint> data = metricsStore.getAll();  // Thread-safe 스냅샷
    webSocketHandler.broadcast(data);
}

// 스레드 3: 백필 요청 처리
public void handleBackfill(Instant since) {
    List<MetricsDataPoint> data = metricsStore.getDataSince(since);  // Thread-safe
    return data;
}
```

**성능 특성**:
- **삽입(addLast)**: O(1) - Lock-free CAS
- **삭제(removeFirst)**: O(1) - Lock-free CAS
- **조회(getAll)**: O(n) - n=720, 스냅샷 생성 시 복사 발생
- **필터링(getDataSince)**: O(n) - n=720, Stream API 사용

---

## Relationships & Dependencies

### Entity Relationship Diagram

```
┌─────────────────┐
│  ServiceHealth  │ (5개 인스턴스)
│  - serviceName  │
│  - status       │
│  - lastChecked  │
│  - responseTime │
│  - memoryUsage  │
└─────────────────┘
        │
        │ 포함됨 (1:5)
        ▼
┌────────────────────┐
│  MetricsUpdate     │ (WebSocket 메시지)
│  - services[]      │
│  - tps             │
│  - alerts          │
└────────────────────┘
        │
        │ 참조함 (1:720)
        ▼
┌──────────────────┐       ┌──────────────────┐
│ TransactionMetrics│       │   AlertMetrics   │
│  - timestamp     │       │  - timestamp     │
│  - tps           │       │  - alertsPerMin  │
│  - totalTxns     │       │  - byRule        │
└──────────────────┘       └──────────────────┘
        │                          │
        │ 통합됨 (1:1)              │
        └────────┬─────────────────┘
                 ▼
        ┌──────────────────┐
        │ MetricsDataPoint │ (720개 저장)
        │  - timestamp     │
        │  - tps           │
        │  - totalTxns     │
        │  - alertsPerMin  │
        │  - byRule        │
        └──────────────────┘
                 │
                 │ 저장됨 (N:1)
                 ▼
        ┌──────────────────┐
        │   MetricsStore   │ (Circular Buffer)
        │  - dataPoints    │
        │    (ConcurrentLinkedDeque)
        └──────────────────┘
```

---

### Data Flow Diagram

```
┌──────────────┐
│5개 서비스     │
│Health Check  │
│엔드포인트     │
└──────────────┘
       │
       │ HTTP GET (5초마다)
       ▼
┌────────────────────┐
│HealthCheckCollector│ (Spring @Scheduled)
│ - WebClient        │
│ - 타임아웃: 3초    │
└────────────────────┘
       │
       │ ServiceHealth 생성
       ▼
┌────────────────────┐       ┌──────────────┐
│ ServiceHealthMap   │       │ Kafka Topics │
│ (ConcurrentHashMap)│       │ - transactions│
└────────────────────┘       │ - alerts      │
       │                      └──────────────┘
       │                             │
       │                             │ AdminClient (5초마다)
       │                             ▼
       │                      ┌────────────────────┐
       │                      │KafkaMetricsCollector│
       │                      │ - offset 증가량     │
       │                      │ - TPS, 알림률 계산  │
       │                      └────────────────────┘
       │                             │
       │                             │ MetricsDataPoint 생성
       │                             ▼
       │                      ┌────────────────────┐
       │                      │   MetricsStore     │
       │                      │ (Circular Buffer)  │
       │                      │ - 1시간 보관       │
       │                      └────────────────────┘
       │                             │
       └──────────┬──────────────────┘
                  │
                  │ 집계 (current, avg, max)
                  ▼
        ┌────────────────────┐
        │  MetricsUpdate     │ (DTO)
        │  - services        │
        │  - tps             │
        │  - alerts          │
        └────────────────────┘
                  │
                  │ JSON 직렬화
                  ▼
        ┌────────────────────┐
        │ WebSocketHandler   │
        │ - sessions (Set)   │
        │ - broadcast()      │
        └────────────────────┘
                  │
                  │ WebSocket 브로드캐스트
                  ▼
        ┌────────────────────┐
        │  5개 클라이언트    │ (브라우저)
        │  - useWebSocket()  │
        │  - Recharts        │
        └────────────────────┘
                  │
                  │ 상태 업데이트 (React setState)
                  ▼
        ┌────────────────────┐
        │  대시보드 UI       │
        │  - 서비스 상태 카드│
        │  - TPS 차트        │
        │  - 알림 발생률 차트│
        └────────────────────┘
```

---

### Dependency Graph

**Backend Dependencies**:
```
MetricsStore
    ↑
    │ 사용됨
    │
KafkaMetricsCollector
    │
    │ 의존
    ↓
Kafka AdminClient

HealthCheckCollector
    │
    │ 의존
    ↓
Spring WebClient

MetricsStore + ServiceHealthMap
    │
    │ 읽음
    ↓
MetricsScheduler (5초마다 실행)
    │
    │ 생성
    ↓
MetricsUpdate (DTO)
    │
    │ 전달
    ↓
WebSocketHandler
    │
    │ 브로드캐스트
    ↓
WebSocket 클라이언트
```

**Frontend Dependencies**:
```
useWebSocket (커스텀 훅)
    │
    │ WebSocket 연결
    ↓
WebSocket 서버
    │
    │ 메시지 수신
    ↓
MetricsUpdateMessage (파싱)
    │
    │ 상태 업데이트
    ↓
React State (services, tps, alerts)
    │
    │ props 전달
    ↓
ServiceHealthCard, TpsChart, AlertRateChart
    │
    │ 렌더링
    ↓
Recharts (LineChart, AreaChart)
```

---

## Memory Estimation (Detailed)

### Backend Memory Usage

**1. MetricsStore (Circular Buffer)**:
```
ConcurrentLinkedDeque<MetricsDataPoint>:
- 720개 데이터 포인트 × 150 bytes = 108 KB
- Deque 노드 오버헤드 (50%): 54 KB
------------------------------------------
Total:                         ~160 KB
```

**2. ServiceHealthMap (5개 서비스)**:
```
Map<String, ServiceHealth>:
- serviceName (String): 25 bytes
- ServiceHealth 객체: 100 bytes
- 5개 서비스: 5 × 125 bytes = 625 bytes
------------------------------------------
Total:                         ~1 KB
```

**3. WebSocket Sessions (5명 사용자)**:
```
Set<WebSocketSession>:
- 세션당 버퍼: 10 KB
- 5명: 5 × 10 KB = 50 KB
------------------------------------------
Total:                         ~50 KB
```

**Backend 총 메모리 사용량**: < 250 KB

---

### Frontend Memory Usage

**1. React State (메트릭 데이터)**:
```
services: ServiceHealth[] (5개):
- 5 × 200 bytes = 1 KB

tps.history: TransactionMetrics[] (720개):
- 720 × 100 bytes = 72 KB

alerts.history: AlertMetrics[] (720개):
- 720 × 150 bytes = 108 KB
------------------------------------------
Total:                         ~180 KB
```

**2. Recharts DOM 노드**:
```
TpsChart (LineChart):
- SVG 요소 720개 (데이터 포인트): ~2 MB

AlertRateChart (AreaChart):
- SVG 요소 2160개 (720 × 3 규칙): ~6 MB
------------------------------------------
Total:                         ~8 MB
```

**3. WebSocket 버퍼**:
```
WebSocket 수신 버퍼: 256 KB
WebSocket 송신 버퍼: 256 KB
------------------------------------------
Total:                         ~512 KB
```

**Frontend 총 메모리 사용량**: < 10 MB

---

### Total System Memory

```
Backend:  < 1 MB
Frontend: < 10 MB
------------------------------------------
Total:    < 11 MB (매우 경량)
```

**확장성 분석**:
- **동시 사용자 50명**: Frontend 메모리 × 50 = 500 MB (클라이언트 측)
- **동시 사용자 50명**: Backend WebSocket 세션 50 × 10 KB = 500 KB (서버 측)
- **10개 서비스**: ServiceHealthMap 2 KB 추가 (무시 가능)
- **10개 규칙**: AlertMetrics Map 증가, 총 ~200 KB

**결론**: 현재 요구사항(5명 사용자, 5개 서비스)에서 메모리 사용량은 매우 경량이며, 확장성에 문제 없음

---

## Validation Rules Summary

### ServiceHealth Validation

| Rule ID | Field | Validation | Error Message (한국어) |
|---------|-------|------------|------------------------|
| SH-001 | serviceName | Must be one of [transaction-generator, fraud-detector, alert-service, websocket-gateway, frontend-dashboard] | "유효하지 않은 서비스 이름입니다" |
| SH-002 | status | Must be "UP" or "DOWN" | "상태는 UP 또는 DOWN이어야 합니다" |
| SH-003 | lastChecked | Must be within 10 seconds of current time | "Health Check 시각이 너무 오래되었습니다" |
| SH-004 | responseTime | If status=UP, must be non-null and ≤ 5000 | "응답 시간이 유효하지 않습니다" |
| SH-005 | memoryUsage | If status=UP, must be non-null and ≤ 8192 | "메모리 사용량이 유효하지 않습니다" |
| SH-006 | errorType | If status=DOWN, must be non-null | "DOWN 상태일 때 오류 유형이 필요합니다" |
| SH-007 | errorMessage | If status=DOWN, must be non-null and ≤ 256 chars | "DOWN 상태일 때 오류 메시지가 필요합니다" |

---

### TransactionMetrics Validation

| Rule ID | Field | Validation | Error Message (한국어) |
|---------|-------|------------|------------------------|
| TM-001 | timestamp | Must be ISO-8601 format, aligned to 5-second intervals | "타임스탬프 형식이 유효하지 않습니다" |
| TM-002 | tps | Must be ≥ 0 and ≤ 10000 | "TPS는 0 이상 10000 이하여야 합니다" |
| TM-003 | totalTransactions | Must be ≥ 0 and monotonically increasing | "누적 거래 수가 감소할 수 없습니다" |
| TM-004 | history.length | Must be ≤ 720 (1 hour) | "1시간 이상의 데이터는 보관할 수 없습니다" |

---

### AlertMetrics Validation

| Rule ID | Field | Validation | Error Message (한국어) |
|---------|-------|------------|------------------------|
| AM-001 | timestamp | Must be ISO-8601 format, aligned to 5-second intervals | "타임스탬프 형식이 유효하지 않습니다" |
| AM-002 | alertsPerMinute | Must be ≥ 0 and ≤ 1000 | "분당 알림 수는 0 이상 1000 이하여야 합니다" |
| AM-003 | byRule | Must have exactly 3 keys: HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY | "규칙 유형이 유효하지 않습니다" |
| AM-004 | byRule[*] | Each value must be ≥ 0 | "알림 수는 음수일 수 없습니다" |
| AM-005 | sum(byRule.values) | Must equal alertsPerMinute | "규칙별 알림 수의 합이 총 알림 수와 일치하지 않습니다" |
| AM-006 | history.length | Must be ≤ 720 (1 hour) | "1시간 이상의 데이터는 보관할 수 없습니다" |

---

## Performance Characteristics

### Backend Performance

**메트릭 수집 성능**:
```
Health Check (5개 서비스):
- HTTP 요청: 5 × 100ms = 500ms (병렬 처리)
- WebClient (비동기): 실제 지연 ~100ms
- 타임아웃: 3초 (초과 시 DOWN)

Kafka 메트릭 수집:
- AdminClient API 호출: < 50ms
- TPS 계산: O(1) 연산
- 알림률 계산: O(3) 연산 (3개 규칙)
```

**데이터 저장 성능**:
```
MetricsStore.addDataPoint():
- 삽입(addLast): O(1) - 상수 시간
- 삭제(removeFirst): O(1) - 상수 시간
- 전체 시간: < 1ms

MetricsStore.getAll():
- 복사(new ArrayList): O(720) - 선형 시간
- 전체 시간: < 5ms
```

**WebSocket 브로드캐스트 성능**:
```
JSON 직렬화:
- MetricsUpdate 객체: < 10ms
- 크기: ~100KB (720개 데이터 포인트)

브로드캐스트 (5명 사용자):
- 전송: 5 × 100KB = 500KB
- 네트워크 지연: ~50ms (로컬 네트워크)
```

---

### Frontend Performance

**React 렌더링 성능**:
```
TpsChart (LineChart, 720개 점):
- 렌더링 시간: < 50ms
- 프레임율: 60fps (애니메이션 시)

AlertRateChart (AreaChart, 2160개 점):
- 렌더링 시간: < 100ms
- 프레임율: 60fps (애니메이션 시)

ServiceHealthCard (5개):
- 렌더링 시간: < 10ms
```

**WebSocket 메시지 처리**:
```
메시지 수신:
- JSON 파싱: < 5ms
- React setState: < 10ms
- 총 시간: < 15ms
```

**메모리 효율**:
```
React State 업데이트:
- 이전 데이터 GC: 자동 (JavaScript 가비지 컬렉터)
- 메모리 누수 없음 (useEffect cleanup)
```

---

## Error Handling

### Backend Error Scenarios

| Error Type | Cause | Handling | Recovery |
|------------|-------|----------|----------|
| Health Check 타임아웃 | 서비스 응답 시간 > 3초 | 즉시 DOWN 상태 전환, WARN 로그 | 다음 폴링(5초 후) 재시도 |
| HTTP 4xx/5xx | 서비스 내부 오류 | 즉시 DOWN 상태 전환, WARN 로그 | 다음 폴링(5초 후) 재시도 |
| 네트워크 연결 실패 | Connection Refused, DNS 실패 | 즉시 DOWN 상태 전환, ERROR 로그 | 다음 폴링(5초 후) 재시도 |
| Kafka AdminClient 오류 | Kafka 브로커 연결 실패 | TPS/알림률 계산 스킵, ERROR 로그 | 다음 폴링(5초 후) 재시도 |
| WebSocket 전송 실패 | 클라이언트 연결 끊김 | 해당 세션 제거, INFO 로그 | 클라이언트 재연결 시 백필 |

---

### Frontend Error Scenarios

| Error Type | Cause | Handling | Recovery |
|------------|-------|----------|----------|
| WebSocket 연결 실패 | 네트워크 오류, 서버 다운 | "연결 끊김" 배너 표시 | Exponential Backoff 재연결 (1초 → 32초) |
| WebSocket 연결 끊김 | 네트워크 불안정 | "연결 끊김" 배너 표시, 재연결 시도 | 백필 요청으로 누락 데이터 복구 |
| JSON 파싱 오류 | 잘못된 메시지 형식 | 에러 로그 출력, 메시지 무시 | 다음 메시지 정상 처리 |
| 차트 렌더링 실패 | 잘못된 데이터 형식 | 에러 바운더리로 에러 캡처, 폴백 UI 표시 | 다음 업데이트 시 재시도 |
| 메모리 부족 | 데이터 누적 | 1시간 이전 데이터 자동 삭제 | 메모리 안정화 |

---

## Testing Considerations

### Unit Test Scenarios

**Backend**:
```
MetricsStore:
- [x] addDataPoint(): 데이터 추가 확인
- [x] addDataPoint(): 1시간 이전 데이터 자동 삭제 확인
- [x] getAll(): 전체 데이터 조회 (720개 이하)
- [x] getDataSince(): 특정 시각 이후 데이터 필터링

HealthCheckCollector:
- [x] collectHealthMetrics(): 5개 서비스 상태 수집
- [x] handleTimeout(): 타임아웃 시 DOWN 상태 전환
- [x] handleHttpError(): HTTP 에러 시 DOWN 상태 전환
- [x] handleNetworkError(): 네트워크 에러 시 DOWN 상태 전환

KafkaMetricsCollector:
- [x] collectTPS(): Kafka offset 증가량으로 TPS 계산
- [x] collectAlerts(): 규칙별 알림 수 집계
```

**Frontend**:
```
useWebSocket:
- [x] connect(): WebSocket 연결 성공
- [x] reconnect(): 연결 끊김 시 재연결 (Exponential Backoff)
- [x] handleBackfill(): 재연결 시 백필 요청

TpsChart:
- [x] render(): 720개 데이터 포인트 렌더링
- [x] animation(): 부드러운 트랜지션 (5초마다)
- [x] tooltip(): 마우스 호버 시 정확한 값 표시

AlertRateChart:
- [x] render(): 3개 규칙 스택 형태 렌더링
- [x] toggleRule(): 범례 클릭 시 규칙 숨김/표시
- [x] warningColor(): alertsPerMinute > average * 2 시 주황색 표시
```

---

### Integration Test Scenarios

**End-to-End**:
```
- [x] 시스템 시작 → 5초 후 대시보드 초기 데이터 표시
- [x] 서비스 중단 → 3초 이내 DOWN 상태 대시보드 반영
- [x] 서비스 복구 → 5초 이내 UP 상태 대시보드 반영
- [x] 거래 생성 → TPS 차트 실시간 업데이트
- [x] 알림 발생 → 알림 발생률 차트 실시간 업데이트
- [x] 브라우저 탭 전환 → 복귀 시 백필로 누락 데이터 복구
- [x] 동시 5명 사용자 → 각 화면 독립적 업데이트
```

---

## Appendix: JSON Schema (Formal)

### MetricsUpdateMessage JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["type", "timestamp", "data"],
  "properties": {
    "type": {
      "type": "string",
      "const": "METRICS_UPDATE"
    },
    "timestamp": {
      "type": "string",
      "format": "date-time"
    },
    "data": {
      "type": "object",
      "required": ["services", "tps", "alerts"],
      "properties": {
        "services": {
          "type": "array",
          "minItems": 5,
          "maxItems": 5,
          "items": {
            "type": "object",
            "required": ["serviceName", "status", "lastChecked"],
            "properties": {
              "serviceName": {
                "type": "string",
                "enum": ["transaction-generator", "fraud-detector", "alert-service", "websocket-gateway", "frontend-dashboard"]
              },
              "status": {
                "type": "string",
                "enum": ["UP", "DOWN"]
              },
              "lastChecked": {
                "type": "string",
                "format": "date-time"
              },
              "responseTime": {
                "type": ["number", "null"],
                "minimum": 0,
                "maximum": 5000
              },
              "memoryUsage": {
                "type": ["number", "null"],
                "minimum": 0,
                "maximum": 8192
              },
              "errorType": {
                "type": ["string", "null"],
                "enum": ["TIMEOUT", "HTTP_ERROR", "NETWORK_ERROR", null]
              },
              "errorMessage": {
                "type": ["string", "null"],
                "maxLength": 256
              }
            }
          }
        },
        "tps": {
          "type": "object",
          "required": ["current", "average", "max", "history"],
          "properties": {
            "current": {
              "type": "number",
              "minimum": 0,
              "maximum": 10000
            },
            "average": {
              "type": "number",
              "minimum": 0
            },
            "max": {
              "type": "number",
              "minimum": 0
            },
            "history": {
              "type": "array",
              "maxItems": 720,
              "items": {
                "type": "object",
                "required": ["timestamp", "tps", "totalTransactions"],
                "properties": {
                  "timestamp": {
                    "type": "string",
                    "format": "date-time"
                  },
                  "tps": {
                    "type": "number",
                    "minimum": 0,
                    "maximum": 10000
                  },
                  "totalTransactions": {
                    "type": "number",
                    "minimum": 0
                  }
                }
              }
            }
          }
        },
        "alerts": {
          "type": "object",
          "required": ["current", "average", "max", "byRule", "history"],
          "properties": {
            "current": {
              "type": "number",
              "minimum": 0,
              "maximum": 1000
            },
            "average": {
              "type": "number",
              "minimum": 0
            },
            "max": {
              "type": "number",
              "minimum": 0
            },
            "byRule": {
              "type": "object",
              "required": ["HIGH_VALUE", "FOREIGN_COUNTRY", "HIGH_FREQUENCY"],
              "properties": {
                "HIGH_VALUE": {
                  "type": "number",
                  "minimum": 0
                },
                "FOREIGN_COUNTRY": {
                  "type": "number",
                  "minimum": 0
                },
                "HIGH_FREQUENCY": {
                  "type": "number",
                  "minimum": 0
                }
              }
            },
            "history": {
              "type": "array",
              "maxItems": 720,
              "items": {
                "type": "object",
                "required": ["timestamp", "alertsPerMinute", "byRule"],
                "properties": {
                  "timestamp": {
                    "type": "string",
                    "format": "date-time"
                  },
                  "alertsPerMinute": {
                    "type": "number",
                    "minimum": 0,
                    "maximum": 1000
                  },
                  "byRule": {
                    "type": "object",
                    "required": ["HIGH_VALUE", "FOREIGN_COUNTRY", "HIGH_FREQUENCY"],
                    "properties": {
                      "HIGH_VALUE": {
                        "type": "number",
                        "minimum": 0
                      },
                      "FOREIGN_COUNTRY": {
                        "type": "number",
                        "minimum": 0
                      },
                      "HIGH_FREQUENCY": {
                        "type": "number",
                        "minimum": 0
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
```

---

**문서 완료**: 이 데이터 모델은 Phase 1 (Design & Contracts)의 일부로, 다음 단계인 API 설계 및 구현에서 활용됩니다.
