# Research: 실시간 시스템 대시보드 구현 결정

**작성일**: 2025-11-12
**Phase**: Phase 0 - Outline & Research
**목적**: 실시간 시스템 대시보드 구현을 위한 메트릭 수집, 실시간 데이터 푸시, 시계열 데이터 관리, 차트 라이브러리, 서비스 헬스 모니터링 전략 연구

---

## 1. Metrics Collection Strategy (메트릭 수집 전략)

### Decision (결정)

**Spring Boot Actuator + 스케줄링 폴링** 방식을 사용하여 5개 마이크로서비스의 헬스 체크 및 메트릭 수집

**구체적 접근**:
- **Health Check**: Spring Boot Actuator의 `/actuator/health` 엔드포인트 사용
- **메트릭 수집**: Spring `@Scheduled` 어노테이션으로 5초마다 각 서비스 폴링
- **HTTP 클라이언트**: Spring WebClient (비동기, 논블로킹)
- **Kafka 메트릭**: Kafka AdminClient API로 토픽별 TPS 및 알림률 집계

### Rationale (근거)

1. **비침투적 수집**: Spring Boot Actuator는 각 서비스에 이미 내장되어 있어 추가 에이전트나 사이드카 불필요 (Constitution II - 단순성)
2. **표준화**: 모든 Spring Boot 서비스가 동일한 `/actuator/health` 형식 제공, 일관된 파싱 가능
3. **WebClient 성능**: RestTemplate 대비 비동기 처리로 5개 서비스 동시 폴링 시 효율적 (블로킹 없음)
4. **Constitution III 준수**: WebClient는 Reactive Streams 기반으로 실시간 처리 원칙에 부합
5. **메모리 효율**: Kafka AdminClient는 경량이며 메타데이터 조회만 수행 (메시지 consume 불필요)

### Alternatives Considered (고려한 대안)

- **Prometheus + Pull 방식**: 각 서비스가 `/metrics` 엔드포인트를 노출하고 Prometheus가 scraping
  - **거부 이유**:
    - 추가 인프라(Prometheus 서버) 필요 (Constitution II 위반 - 단순함 우선)
    - 학습용 프로젝트에 과도한 복잡성
    - 5개 서비스만 모니터링하므로 Prometheus 오버엔지니어링

- **Micrometer + Push 방식**: 각 서비스가 메트릭을 중앙 저장소(Graphite, InfluxDB)로 푸시
  - **거부 이유**:
    - 시계열 데이터베이스 추가 필요 (외부 의존성 증가)
    - 1시간 메모리 보관 요구사항에 과도한 솔루션
    - Constitution II 위반 - 단일 명령어 배포(`docker-compose up`) 복잡해짐

- **RestTemplate 사용**: 동기식 HTTP 클라이언트
  - **거부 이유**:
    - Spring 5 이후 maintenance 모드, WebClient 권장
    - 블로킹 I/O로 5개 서비스 순차 폴링 시 총 지연 시간 증가 (각 100ms × 5 = 500ms)
    - Constitution III 위반 - 실시간 처리 원칙에 부적합

### Implementation Notes (구현 세부사항)

#### 1. Health Check 수집 구조
```java
@Service
public class HealthCheckCollector {
    private final WebClient webClient;
    private final Map<String, String> serviceUrls = Map.of(
        "transaction-generator", "http://transaction-generator:8080",
        "fraud-detector", "http://fraud-detector:8081",
        "alert-service", "http://alert-service:8082",
        "websocket-gateway", "http://websocket-gateway:8083",
        "frontend-dashboard", "http://frontend-dashboard:8084"
    );

    @Scheduled(fixedRate = 5000) // 5초마다 실행
    public void collectHealthMetrics() {
        serviceUrls.forEach((serviceName, url) -> {
            webClient.get()
                .uri(url + "/actuator/health")
                .retrieve()
                .bodyToMono(HealthResponse.class)
                .timeout(Duration.ofSeconds(3)) // 3초 타임아웃
                .subscribe(
                    health -> updateServiceStatus(serviceName, health),
                    error -> markServiceDown(serviceName, error)
                );
        });
    }
}
```

#### 2. Actuator 엔드포인트 설정
각 서비스의 `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always  # 메모리, 디스크 사용량 포함
```

#### 3. Kafka 메트릭 수집 (TPS)
```java
@Service
public class KafkaMetricsCollector {
    private final AdminClient adminClient;

    @Scheduled(fixedRate = 5000)
    public void collectTPS() {
        // virtual-transactions 토픽의 offset 증가량으로 TPS 계산
        Map<TopicPartition, OffsetAndMetadata> offsets =
            adminClient.listConsumerGroupOffsets("fraud-detector-group").get();

        long currentOffset = offsets.values().stream()
            .mapToLong(OffsetAndMetadata::offset)
            .sum();

        long tps = (currentOffset - lastOffset) / 5; // 5초 간격
        lastOffset = currentOffset;

        metricsStore.addTpsDataPoint(Instant.now(), tps);
    }
}
```

#### 4. 타임아웃 및 에러 처리
- **타임아웃**: 3초 (서비스 응답이 3초 이상 걸리면 DOWN으로 간주)
- **재시도 로직**: 실패 시 즉시 DOWN 상태로 전환, 다음 폴링(5초 후)에서 자동 복구 확인
- **로깅**: 모든 health check 결과를 INFO 레벨로 로깅 (Constitution V - 관찰 가능성)

---

## 2. Real-time Data Push Architecture (실시간 데이터 푸시 아키텍처)

### Decision (결정)

**Server-side Broadcast Pattern** 사용: 모든 연결된 클라이언트에게 동일한 메트릭 데이터를 브로드캐스트

**구체적 구조**:
- **WebSocket 프로토콜**: 양방향 통신 (클라이언트 ↔ 서버)
- **메시지 형식**: JSON (`{ type: 'METRICS_UPDATE', timestamp: ..., data: {...} }`)
- **브로드캐스트**: 새로운 메트릭 수집 시 모든 연결된 WebSocket 세션에 동시 전송
- **재연결 전략**: Exponential Backoff (1초 → 2초 → 4초 → 8초, 최대 32초)
- **백필(Backfill) 로직**: 재연결 시 마지막 수신 타임스탬프를 서버에 전송하여 누락된 데이터 요청

### Rationale (근거)

1. **단순성**: Broadcast 패턴은 구현이 간단하며, 5명 이하 동시 사용자에게 충분 (Constitution II)
2. **일관성**: 모든 클라이언트가 동일한 데이터를 동시에 수신하여 화면 불일치 방지
3. **Constitution III 준수**: WebSocket은 폴링보다 실시간성 높음 (서버 → 클라이언트 푸시)
4. **효율성**: 하나의 메트릭 수집 → 여러 클라이언트 전송 (1:N 전송)
5. **학습 가치**: WebSocket 브로드캐스트 패턴은 실시간 대시보드의 표준 접근 방식

### Alternatives Considered (고려한 대안)

- **Per-client Subscription Pattern**: 각 클라이언트가 특정 메트릭만 구독
  - **거부 이유**:
    - 현재 요구사항에서 모든 클라이언트가 동일한 메트릭(5개 서비스 상태, TPS, 알림률) 필요
    - 개별 구독 관리 로직 추가로 복잡성 증가 (Constitution II 위반)
    - 5명 이하 사용자에게 불필요한 최적화

- **Server-Sent Events (SSE)**: 서버 → 클라이언트 단방향 통신
  - **거부 이유**:
    - 양방향 통신 불가 (클라이언트가 백필 요청을 보낼 수 없음)
    - WebSocket 대비 기능 제한적 (연결 상태 확인 어려움)
    - 브라우저 연결 수 제한 (HTTP/1.1 기준 6개)

- **Long Polling**: HTTP 요청을 길게 유지하다가 데이터 발생 시 응답
  - **거부 이유**:
    - Constitution III 위반 - WebSocket보다 실시간성 낮음
    - 연결 재수립 오버헤드 (매 응답마다 새 요청 필요)
    - 네트워크 효율 낮음 (HTTP 헤더 반복 전송)

### Implementation Notes (구현 세부사항)

#### 1. WebSocket 메시지 형식
```typescript
// 메트릭 업데이트 메시지
{
  type: 'METRICS_UPDATE',
  timestamp: '2025-11-12T10:30:05Z',
  data: {
    services: [
      { name: 'transaction-generator', status: 'UP', responseTime: 45, memoryUsage: 128 },
      { name: 'fraud-detector', status: 'UP', responseTime: 92, memoryUsage: 1024 },
      // ...
    ],
    tps: {
      current: 87,
      average: 65,
      max: 150,
      history: [/* 최근 1시간 데이터 포인트 */]
    },
    alerts: {
      current: 12,
      average: 8,
      max: 25,
      byRule: { HIGH_VALUE: 5, FOREIGN_COUNTRY: 4, HIGH_FREQUENCY: 3 }
    }
  }
}

// 백필 요청 메시지 (클라이언트 → 서버)
{
  type: 'BACKFILL_REQUEST',
  lastReceivedTimestamp: '2025-11-12T10:25:00Z'
}

// 백필 응답 메시지 (서버 → 클라이언트)
{
  type: 'BACKFILL_RESPONSE',
  data: [
    { timestamp: '2025-11-12T10:25:05Z', tps: 65, alertsPerMinute: 8 },
    { timestamp: '2025-11-12T10:25:10Z', tps: 70, alertsPerMinute: 9 },
    // ...
  ]
}
```

#### 2. 서버측 WebSocket 핸들러 (Spring Boot)
```java
@Component
public class MetricsWebSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket 연결: sessionId={}, 총 연결 수={}", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket 연결 종료: sessionId={}, 총 연결 수={}", session.getId(), sessions.size());
    }

    public void broadcastMetrics(MetricsUpdate metrics) {
        String message = objectMapper.writeValueAsString(metrics);
        sessions.forEach(session -> {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("메트릭 전송 실패: sessionId={}", session.getId(), e);
            }
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 백필 요청 처리
        BackfillRequest request = objectMapper.readValue(message.getPayload(), BackfillRequest.class);
        List<MetricsDataPoint> backfillData = metricsStore.getDataSince(request.getLastReceivedTimestamp());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
            new BackfillResponse(backfillData)
        )));
    }
}
```

#### 3. 클라이언트측 재연결 전략 (React)
```typescript
// useWebSocket.ts
const useWebSocket = (url: string) => {
  const [status, setStatus] = useState<'connecting' | 'connected' | 'disconnected'>('disconnected');
  const [lastReceivedTimestamp, setLastReceivedTimestamp] = useState<string | null>(null);
  const reconnectAttempt = useRef(0);
  const ws = useRef<WebSocket | null>(null);

  const connect = useCallback(() => {
    setStatus('connecting');
    ws.current = new WebSocket(url);

    ws.current.onopen = () => {
      setStatus('connected');
      reconnectAttempt.current = 0;

      // 백필 요청 (재연결 시)
      if (lastReceivedTimestamp) {
        ws.current?.send(JSON.stringify({
          type: 'BACKFILL_REQUEST',
          lastReceivedTimestamp
        }));
      }
    };

    ws.current.onmessage = (event) => {
      const message = JSON.parse(event.data);
      setLastReceivedTimestamp(message.timestamp);
      // 메트릭 업데이트 처리
    };

    ws.current.onclose = () => {
      setStatus('disconnected');

      // Exponential Backoff 재연결
      const delay = Math.min(1000 * Math.pow(2, reconnectAttempt.current), 32000);
      reconnectAttempt.current++;

      setTimeout(() => connect(), delay);
    };
  }, [url, lastReceivedTimestamp]);

  useEffect(() => {
    connect();
    return () => ws.current?.close();
  }, [connect]);

  return { status };
};
```

#### 4. 동시 다수 사용자 처리
- **동시 연결 제한**: 최대 10개 WebSocket 세션 (요구사항: 5명)
- **브로드캐스트 성능**: ConcurrentHashMap.newKeySet()으로 스레드 안전 세션 관리
- **메모리 사용량**: 각 세션당 약 10KB (5명 × 10KB = 50KB, 무시 가능)

---

## 3. Time-Series Data Management (시계열 데이터 관리)

### Decision (결정)

**ConcurrentLinkedDeque 기반 Circular Buffer** 사용 (Java 표준 라이브러리)

**구체적 구조**:
- **데이터 구조**: `ConcurrentLinkedDeque<MetricsDataPoint>`
- **보관 기간**: 1시간 (720개 데이터 포인트, 5초 간격)
- **자동 삭제**: 새 데이터 추가 시 1시간 이전 데이터 자동 제거 (FIFO)
- **메모리 크기**: 약 100KB (720 × 150 bytes/datapoint)

### Rationale (근거)

1. **단순성**: Java 표준 라이브러리 사용, 외부 의존성 불필요 (Constitution II)
2. **스레드 안전**: `ConcurrentLinkedDeque`는 lock-free, 멀티 스레드 환경에서 안전
3. **O(1) 성능**: 삽입(addLast), 삭제(removeFirst) 모두 상수 시간
4. **메모리 효율**: 1시간 데이터만 유지, 예측 가능한 메모리 사용량 (<1MB)
5. **Constitution IV 준수**: 별도 데이터베이스 서비스 불필요, 마이크로서비스 경계 존중

### Alternatives Considered (고려한 대안)

- **LMAX Disruptor RingBuffer**: Lock-free 고성능 circular buffer
  - **거부 이유**:
    - 외부 라이브러리 추가 (Constitution II - 외부 의존성 최소화)
    - 5초 간격 업데이트에 Disruptor의 초고성능(나노초 단위) 불필요
    - 학습 곡선 가파름 (Constitution I - 명시적 패턴 선호)

- **Apache Commons CircularFifoQueue**: 고정 크기 큐
  - **거부 이유**:
    - 외부 의존성 추가 (Apache Commons Collections)
    - `ConcurrentLinkedDeque`와 기능 동일하나 표준 라이브러리가 더 가벼움

- **Guava EvictingQueue**: 자동 제거 큐
  - **거부 이유**:
    - 외부 의존성 추가 (Guava)
    - Thread-safe 버전이 없어 수동 동기화 필요 (`Collections.synchronizedQueue`)
    - `ConcurrentLinkedDeque`보다 성능 낮음

- **시계열 데이터베이스 (InfluxDB, TimescaleDB)**: 전문 시계열 저장소
  - **거부 이유**:
    - Constitution II 위반 - 추가 인프라, 단일 명령어 배포 복잡해짐
    - 1시간 메모리 보관에 데이터베이스 과도함 (Out of Scope)
    - 외부 의존성 증가, 로컬 실행 복잡도 증가

### Implementation Notes (구현 세부사항)

#### 1. 메트릭 데이터 구조
```java
@Data
public class MetricsDataPoint {
    private Instant timestamp;        // 8 bytes
    private long tps;                  // 8 bytes
    private long alertsPerMinute;     // 8 bytes
    private Map<String, Long> alertsByRule; // ~50 bytes (3 rules)
    // 총 약 150 bytes per data point
}

@Service
public class MetricsStore {
    private static final int MAX_DATA_POINTS = 720; // 1시간 = 720 × 5초
    private static final Duration RETENTION_PERIOD = Duration.ofHours(1);

    private final ConcurrentLinkedDeque<MetricsDataPoint> dataPoints = new ConcurrentLinkedDeque<>();

    public void addDataPoint(MetricsDataPoint point) {
        dataPoints.addLast(point);

        // 1시간 이전 데이터 자동 삭제
        Instant cutoff = Instant.now().minus(RETENTION_PERIOD);
        while (!dataPoints.isEmpty() && dataPoints.peekFirst().getTimestamp().isBefore(cutoff)) {
            dataPoints.removeFirst();
        }

        log.debug("메트릭 데이터 추가: timestamp={}, 총 데이터 포인트={}",
                  point.getTimestamp(), dataPoints.size());
    }

    public List<MetricsDataPoint> getAll() {
        return new ArrayList<>(dataPoints); // 스냅샷 반환 (불변성)
    }

    public List<MetricsDataPoint> getDataSince(Instant since) {
        return dataPoints.stream()
            .filter(dp -> dp.getTimestamp().isAfter(since))
            .collect(Collectors.toList());
    }
}
```

#### 2. 메모리 사용량 분석
- **단일 데이터 포인트**: ~150 bytes
- **1시간 데이터**: 720 × 150 bytes = 108 KB
- **오버헤드 (Deque 노드)**: ~50% → 총 약 160 KB
- **5개 서비스 상태**: 5 × 100 bytes = 500 bytes (무시 가능)
- **총 메모리 사용량**: < 1 MB

#### 3. 성능 특성
- **삽입 시간**: O(1) - `addLast()` 상수 시간
- **삭제 시간**: O(1) - `removeFirst()` 상수 시간
- **조회 시간**: O(n) - 전체 데이터 순회, n=720 (매우 작은 크기)
- **스레드 안전성**: Lock-free CAS (Compare-And-Swap) 알고리즘

#### 4. 데이터 정합성 보장
- **시간 순서 보장**: `addLast()`로 항상 최신 데이터가 뒤에 추가
- **중복 방지**: 타임스탬프를 고유 키로 사용, 동일 시각 데이터 덮어쓰기 가능
- **동시성 제어**: `ConcurrentLinkedDeque`가 내부적으로 처리, 추가 락 불필요

---

## 4. Frontend Charting Library (프론트엔드 차트 라이브러리)

### Decision (결정)

**Recharts** 선택 (React 기반 차트 라이브러리)

**근거**:
- **실시간 업데이트**: Recharts는 React의 렌더링 최적화 활용, 5초마다 부드러운 업데이트
- **TypeScript 지원**: 완전한 타입 정의 제공 (Constitution I - 명시적 타입)
- **단순성**: 선언적 API, React 컴포넌트 구조로 학습 곡선 낮음 (Constitution II)
- **애니메이션**: SVG 기반 부드러운 트랜지션, 깜빡임 없음
- **경량**: 번들 크기 약 400KB (gzipped 100KB), 프론트엔드 메모리 사용 최소화

### Rationale (근거)

1. **React 통합**: Recharts는 React 컴포넌트로 설계되어 useState, useEffect와 자연스럽게 연동
2. **실시간 성능**: SVG 렌더링으로 720개 데이터 포인트 표시 시에도 60fps 유지 (1시간 차트)
3. **Constitution 준수**:
   - **I (학습 우선)**: 명시적 props로 차트 설정, "마법" 없음
   - **II (단순함)**: npm install recharts만으로 설치 완료
   - **V (품질)**: TypeScript 타입 안전성, 런타임 오류 방지
4. **커뮤니티 지원**: 2024년 기준 GitHub 24k+ stars, 활발한 유지보수
5. **학습 가치**: 업계 표준 React 차트 라이브러리, 포트폴리오 가치 높음

### Alternatives Considered (고려한 대안)

- **Chart.js (react-chartjs-2)**: Canvas 기반 차트 라이브러리
  - **거부 이유**:
    - Canvas 렌더링은 React의 가상 DOM과 통합 어려움 (명시적 update() 호출 필요)
    - TypeScript 타입 정의가 Recharts보다 덜 완전함
    - 실시간 업데이트 시 깜빡임 발생 가능 (Canvas 전체 재렌더링)
  - **장점**: 고성능 (10,000+ 데이터 포인트), 하지만 현재 요구사항(720개)에 불필요

- **Victory**: Formidable Labs의 React 차트 라이브러리
  - **거부 이유**:
    - 번들 크기 큼 (~1.5MB, Recharts의 3배)
    - 성능이 Recharts보다 낮음 (대용량 데이터셋 시 렌더링 지연)
    - 커뮤니티 규모 작음 (GitHub 11k stars)
  - **장점**: React Native 지원, 하지만 현재는 웹 전용

- **D3.js**: 저수준 데이터 시각화 라이브러리
  - **거부 이유**:
    - Constitution I 위반 - 학습 곡선 매우 가파름
    - 명령형 API로 React의 선언적 패러다임과 충돌
    - 수동 DOM 조작 필요, React의 가상 DOM과 간섭 가능
    - 단순성 위반 (Constitution II) - Recharts로 충분한데 복잡도 증가
  - **장점**: 최대 유연성, 하지만 현재 요구사항(표준 line/area chart)에 불필요

- **Apache ECharts**: 고성능 차트 라이브러리
  - **거부 이유**:
    - React 통합 약함 (echarts-for-react 래퍼 필요)
    - 번들 크기 매우 큰 (~3MB), 프론트엔드 목표(<256MB) 압박
    - 중국 기반 문서로 영어 자료 상대적으로 부족
  - **장점**: 최고 성능 (100,000+ 데이터 포인트), 하지만 오버스펙

### Implementation Notes (구현 세부사항)

#### 1. Recharts 설치 및 설정
```bash
npm install recharts
npm install --save-dev @types/recharts  # TypeScript 타입
```

#### 2. TPS 차트 컴포넌트 (Line Chart)
```typescript
// components/TpsChart.tsx
import { LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer } from 'recharts';

interface TpsChartProps {
  data: Array<{ timestamp: string; tps: number }>;
}

export const TpsChart: React.FC<TpsChartProps> = ({ data }) => {
  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={data}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis
          dataKey="timestamp"
          tickFormatter={(value) => new Date(value).toLocaleTimeString()}
        />
        <YAxis
          label={{ value: 'TPS', angle: -90, position: 'insideLeft' }}
        />
        <Tooltip
          labelFormatter={(value) => new Date(value).toLocaleString()}
        />
        <Line
          type="monotone"
          dataKey="tps"
          stroke="#8884d8"
          strokeWidth={2}
          dot={false}  // 720개 점 표시 시 성능 저하 방지
          isAnimationActive={true}
          animationDuration={500}
        />
      </LineChart>
    </ResponsiveContainer>
  );
};
```

#### 3. 알림 발생률 차트 (Stacked Area Chart)
```typescript
// components/AlertRateChart.tsx
import { AreaChart, Area, XAxis, YAxis, Tooltip, CartesianGrid, Legend, ResponsiveContainer } from 'recharts';

interface AlertRateChartProps {
  data: Array<{
    timestamp: string;
    HIGH_VALUE: number;
    FOREIGN_COUNTRY: number;
    HIGH_FREQUENCY: number;
  }>;
}

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

  return (
    <ResponsiveContainer width="100%" height={300}>
      <AreaChart data={data}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis
          dataKey="timestamp"
          tickFormatter={(value) => new Date(value).toLocaleTimeString()}
        />
        <YAxis label={{ value: '분당 알림 수', angle: -90, position: 'insideLeft' }} />
        <Tooltip />
        <Legend onClick={(e) => toggleRule(e.value)} />

        {!hiddenRules.has('HIGH_VALUE') && (
          <Area
            type="monotone"
            dataKey="HIGH_VALUE"
            stackId="1"
            stroke="#ff7300"
            fill="#ff7300"
          />
        )}
        {!hiddenRules.has('FOREIGN_COUNTRY') && (
          <Area
            type="monotone"
            dataKey="FOREIGN_COUNTRY"
            stackId="1"
            stroke="#387908"
            fill="#387908"
          />
        )}
        {!hiddenRules.has('HIGH_FREQUENCY') && (
          <Area
            type="monotone"
            dataKey="HIGH_FREQUENCY"
            stackId="1"
            stroke="#8884d8"
            fill="#8884d8"
          />
        )}
      </AreaChart>
    </ResponsiveContainer>
  );
};
```

#### 4. 실시간 업데이트 성능 최적화
```typescript
// hooks/useRealtimeChart.ts
import { useState, useEffect, useMemo } from 'react';

export const useRealtimeChart = (wsData: MetricsUpdate) => {
  const [chartData, setChartData] = useState<MetricsDataPoint[]>([]);

  useEffect(() => {
    if (wsData) {
      setChartData(prev => {
        const newData = [...prev, wsData.data];
        // 1시간(720개) 초과 시 오래된 데이터 제거
        return newData.slice(-720);
      });
    }
  }, [wsData]);

  // 메모이제이션으로 불필요한 재렌더링 방지
  const memoizedData = useMemo(() => chartData, [chartData]);

  return memoizedData;
};
```

#### 5. 깜빡임 방지 및 애니메이션 설정
- **isAnimationActive={true}**: 부드러운 트랜지션
- **animationDuration={500}**: 0.5초 애니메이션 (5초 업데이트 주기에 적합)
- **dot={false}**: 720개 점 렌더링 생략, 성능 향상
- **ResponsiveContainer**: 반응형 크기 조정, 리사이징 시 부드러운 전환

#### 6. 성능 벤치마크
- **렌더링 시간**: 720개 데이터 포인트 렌더링 < 50ms
- **메모리 사용량**: 차트 컴포넌트당 약 5MB (5개 차트 = 25MB)
- **프레임율**: 60fps 유지 (Chrome DevTools Performance 측정)

---

## 5. Service Health Monitoring (서비스 헬스 모니터링)

### Decision (결정)

**타임아웃 기반 DOWN 상태 감지 + 수동 재시도** (Circuit Breaker 미사용)

**구체적 전략**:
- **타임아웃 임계값**: 3초 (health check 응답 대기)
- **DOWN 상태 조건**: 타임아웃 또는 HTTP 에러(4xx, 5xx) 발생 시 즉시 DOWN
- **재시도 로직**: Circuit Breaker 없이 매 5초마다 자동 재시도 (스케줄러)
- **네트워크 vs 서비스 장애 구분**: 연속 3회 실패 시 로그에 "지속적 장애" 경고 표시

### Rationale (근거)

1. **단순성 우선**: Circuit Breaker(Resilience4j) 추가는 현재 요구사항에 과도함 (Constitution II)
2. **실시간 복구 감지**: 매 5초 재시도로 서비스 복구 즉시 반영 (Circuit Breaker의 Half-Open 대기 불필요)
3. **학습 가치**: 기본 타임아웃/재시도 패턴 먼저 학습 후, 향후 Circuit Breaker 추가 고려
4. **Constitution IV 준수**: 추가 라이브러리 없이 Spring WebClient 내장 기능만 사용
5. **메모리 효율**: Circuit Breaker 상태 저장소 불필요 (5개 서비스만 모니터링)

### Alternatives Considered (고려한 대안)

- **Resilience4j Circuit Breaker**: Spring Boot 통합 가능한 Circuit Breaker 라이브러리
  - **거부 이유**:
    - Constitution II 위반 - 외부 의존성 추가, 설정 복잡도 증가
    - 5개 서비스 모니터링에 Circuit Breaker 상태 관리 오버엔지니어링
    - Open → Half-Open → Closed 전환 로직이 단순 타임아웃보다 복잡
    - 학습 곡선: Circuit Breaker 개념 + Resilience4j API 모두 학습 필요
  - **장점**: 장애 서비스에 대한 반복 요청 방지, 하지만 5초 간격 폴링에 큰 이점 없음

- **Netflix Hystrix**: Circuit Breaker 원조 라이브러리
  - **거부 이유**:
    - 2018년 maintenance 모드 진입, Resilience4j 권장
    - 더 이상 업데이트되지 않음

- **Spring Retry**: 재시도 로직 추상화
  - **거부 이유**:
    - 현재 `@Scheduled(fixedRate = 5000)`로 자동 재시도 달성
    - 추가 라이브러리 불필요 (Constitution II)

- **Manual Exponential Backoff**: 실패 시 재시도 간격 증가 (1초 → 2초 → 4초 ...)
  - **거부 이유**:
    - 요구사항: 5초마다 정기 업데이트 (고정 간격)
    - Exponential Backoff는 일시적 네트워크 오류에 유용하나, 정기 모니터링엔 부적합
    - 복구 감지 지연 (Backoff 중에는 서비스 복구 확인 못함)

### Implementation Notes (구현 세부사항)

#### 1. 타임아웃 및 에러 처리
```java
@Service
public class ServiceHealthMonitor {
    private final WebClient webClient;
    private final Map<String, ServiceHealthStatus> healthStatusMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> consecutiveFailures = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 5000)
    public void checkAllServices() {
        serviceUrls.forEach((serviceName, url) -> {
            webClient.get()
                .uri(url + "/actuator/health")
                .retrieve()
                .bodyToMono(ActuatorHealthResponse.class)
                .timeout(Duration.ofSeconds(3))  // 3초 타임아웃
                .doOnSuccess(health -> handleHealthSuccess(serviceName, health))
                .doOnError(TimeoutException.class, e -> handleTimeout(serviceName))
                .doOnError(WebClientResponseException.class, e -> handleHttpError(serviceName, e))
                .doOnError(WebClientRequestException.class, e -> handleNetworkError(serviceName, e))
                .onErrorResume(e -> Mono.empty())  // 에러 시 빈 결과 반환 (다음 폴링 계속)
                .subscribe();
        });
    }

    private void handleHealthSuccess(String serviceName, ActuatorHealthResponse health) {
        healthStatusMap.put(serviceName, ServiceHealthStatus.builder()
            .serviceName(serviceName)
            .status("UP")
            .responseTime(health.getResponseTime())
            .memoryUsage(health.getComponents().getMemory().getDetails().getUsed())
            .lastChecked(Instant.now())
            .build());

        consecutiveFailures.put(serviceName, 0);  // 성공 시 카운터 초기화
        log.info("서비스 정상: service={}, responseTime={}ms", serviceName, health.getResponseTime());
    }

    private void handleTimeout(String serviceName) {
        markServiceDown(serviceName, "TIMEOUT", "Health check 응답 시간 초과 (>3초)");
    }

    private void handleHttpError(String serviceName, WebClientResponseException e) {
        markServiceDown(serviceName, "HTTP_ERROR",
            String.format("HTTP %d: %s", e.getRawStatusCode(), e.getMessage()));
    }

    private void handleNetworkError(String serviceName, WebClientRequestException e) {
        markServiceDown(serviceName, "NETWORK_ERROR",
            String.format("연결 실패: %s", e.getMessage()));
    }

    private void markServiceDown(String serviceName, String errorType, String errorMessage) {
        healthStatusMap.put(serviceName, ServiceHealthStatus.builder()
            .serviceName(serviceName)
            .status("DOWN")
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
}
```

#### 2. Actuator Health Response 파싱
```java
@Data
public class ActuatorHealthResponse {
    private String status;  // "UP" or "DOWN"
    private Components components;

    @Data
    public static class Components {
        private DiskSpace diskSpace;
        private Memory memory;
        private Ping ping;
    }

    @Data
    public static class Memory {
        private String status;
        private Details details;

        @Data
        public static class Details {
            private long total;  // 총 메모리 (bytes)
            private long used;   // 사용 메모리 (bytes)
            private long free;   // 여유 메모리 (bytes)
        }
    }

    // responseTime은 별도로 측정 (WebClient filter)
    private long responseTime;
}
```

#### 3. 응답 시간 측정 (WebClient Filter)
```java
@Configuration
public class WebClientConfig {
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .filter((request, next) -> {
                long startTime = System.currentTimeMillis();
                return next.exchange(request)
                    .doOnSuccess(response -> {
                        long responseTime = System.currentTimeMillis() - startTime;
                        // responseTime을 response attribute에 저장
                    });
            })
            .build();
    }
}
```

#### 4. 네트워크 vs 서비스 장애 구분

| 상황 | 에러 타입 | DOWN 판단 | 로그 레벨 |
|------|-----------|-----------|-----------|
| 타임아웃 (3초 초과) | `TimeoutException` | 즉시 DOWN | WARN (1회), ERROR (3회+) |
| HTTP 5xx (서버 에러) | `WebClientResponseException` | 즉시 DOWN | WARN (1회), ERROR (3회+) |
| HTTP 4xx (Not Found 등) | `WebClientResponseException` | 즉시 DOWN | WARN (설정 오류 가능성) |
| 연결 거부 (Connection Refused) | `WebClientRequestException` | 즉시 DOWN | ERROR (서비스 미실행) |
| DNS 실패 | `WebClientRequestException` | 즉시 DOWN | ERROR (네트워크 설정 오류) |

**네트워크 vs 서비스 장애 구분 로직**:
- **네트워크 문제**: `WebClientRequestException` + "Connection refused" 또는 "Unknown host"
- **서비스 문제**: HTTP 5xx 에러 또는 타임아웃
- **연속 3회 실패**: 네트워크 문제로 간주, "지속적 장애" 경고

#### 5. 상태 전환 로그 (Observability)
```java
// Constitution V - 관찰 가능성
log.info("서비스 상태 변화: service={}, 이전={}, 현재={}, 원인={}",
         serviceName, previousStatus, currentStatus, errorType);

// 메트릭 업데이트 시 브로드캐스트
webSocketHandler.broadcastMetrics(MetricsUpdate.builder()
    .type("METRICS_UPDATE")
    .timestamp(Instant.now())
    .services(healthStatusMap.values())
    .build());
```

---

## 6. 개방형 질문 및 추후 고려 사항

### 해결된 질문

✅ **Prometheus vs 직접 폴링**: 직접 폴링 선택 (단순성, 외부 의존성 최소화)

✅ **WebSocket Broadcast vs Subscription**: Broadcast 선택 (5명 이하 사용자, 모든 메트릭 필요)

✅ **Circular Buffer 구현체**: `ConcurrentLinkedDeque` 선택 (Java 표준, 외부 의존성 없음)

✅ **React 차트 라이브러리**: Recharts 선택 (React 통합, 실시간 업데이트 성능)

✅ **Circuit Breaker 사용 여부**: 미사용 결정 (단순성 우선, 타임아웃/재시도로 충분)

### 추후 고려 사항

🔄 **Prometheus + Grafana 통합**: 현재는 Out of Scope, 향후 메트릭 장기 저장 및 대시보드 확장 시 고려

🔄 **Circuit Breaker 추가**: 서비스가 10개 이상으로 증가하거나, 외부 API 호출 시 Resilience4j 도입 고려

🔄 **차트 라이브러리 교체**: 데이터 포인트가 10,000개 이상으로 증가 시 Chart.js 또는 ECharts로 전환 검토

🔄 **WebSocket 확장**: 동시 사용자가 50명 이상 증가 시 Redis Pub/Sub + 여러 WebSocket 서버 클러스터링

🔄 **시계열 데이터베이스**: 메트릭 데이터를 1개월 이상 보관해야 할 경우 InfluxDB 또는 TimescaleDB 도입

---

## 7. 참고 자료

### 공식 문서
- [Spring Boot Actuator Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Spring WebClient Documentation](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html)
- [WebSocket API MDN](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [Recharts Documentation](https://recharts.org/en-US/)
- [Java ConcurrentLinkedDeque](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentLinkedDeque.html)

### 모범 사례
- [WebSocket Architecture Best Practices - Ably](https://ably.com/topic/websocket-architecture-best-practices)
- [Spring Boot Health Indicators - Baeldung](https://www.baeldung.com/spring-boot-health-indicators)
- [Best React Chart Libraries 2024 - LogRocket](https://blog.logrocket.com/best-react-chart-libraries-2025/)
- [Resilience4j Circuit Breaker Guide - Baeldung](https://www.baeldung.com/spring-boot-resilience4j)

### 성능 및 확장성
- [Scaling Pub/Sub with WebSockets and Redis](https://ably.com/blog/scaling-pub-sub-with-websockets-and-redis)
- [Java Ring Buffer Performance - Baeldung](https://www.baeldung.com/java-ring-buffer)
- [React Performance Optimization](https://react.dev/learn/render-and-commit)

### 유사 프로젝트 참고
- [Spring Boot Microservices Monitoring](https://uptrace.dev/blog/spring-boot-microservices-monitoring)
- [Real-time Dashboard with WebSockets](https://ably.com/blog/websockets-react-tutorial)
- [Recharts Real-time Examples](https://recharts.org/en-US/examples)

---

## 8. Constitution 준수 검증

### I. 학습 우선 접근
- [x] **명시적 패턴**: Spring Actuator 표준 엔드포인트, Recharts 선언적 API
- [x] **포괄적 로깅**: 모든 health check 성공/실패, 상태 전환 로깅
- [x] **문서화**: 각 결정의 "무엇을"과 "왜" 명확히 기술

### II. 단순함 우선
- [x] **외부 의존성 최소화**: Prometheus, Circuit Breaker, 시계열 DB 미사용
- [x] **Java 표준 라이브러리**: ConcurrentLinkedDeque, WebClient
- [x] **단일 명령어 배포**: docker-compose up (추가 설정 파일 불필요)

### III. 실시간 우선
- [x] **WebSocket 사용**: REST 폴링 대신 서버 푸시
- [x] **비동기 처리**: WebClient로 논블로킹 I/O
- [x] **5초 이내 업데이트**: 스케줄러 5초 간격, WebSocket 즉시 브로드캐스트

### IV. 마이크로서비스 경계
- [x] **기존 서비스 유지**: 5개 서비스 구조 변경 없음
- [x] **별도 서비스 미추가**: 모니터링 로직을 websocket-gateway에 통합 (RAD 서비스)

### V. 테스트 및 품질 표준
- [x] **타입 안전성**: TypeScript (프론트엔드), Java 17 (백엔드)
- [x] **로깅**: SLF4J + JSON, 모든 상태 전환 로깅
- [x] **오류 처리**: 타임아웃, HTTP 에러, 네트워크 에러 명시적 처리

### VI. 한국어 우선
- [x] **문서화 언어**: 모든 섹션 한국어 작성
- [x] **로그 메시지**: 한국어 로그 ("서비스 정상", "서비스 중단 감지")

---

**연구 완료**: Phase 0 완료, Phase 1 (Design & Contracts) 진행 준비 완료
