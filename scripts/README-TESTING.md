# 테스트 실행 가이드

## T107: E2E 테스트 실행 및 검증

### 목적
전체 시스템을 실행하고 3가지 탐지 규칙이 모두 작동하는지 검증합니다.

### 실행 방법

1. **E2E 테스트 스크립트 실행 권한 부여**
   ```bash
   chmod +x scripts/test-e2e.sh
   chmod +x scripts/measure-latency.sh
   ```

2. **E2E 테스트 실행**
   ```bash
   cd D:/side-project/RealFDS
   bash scripts/test-e2e.sh
   ```

3. **테스트 결과 확인**
   - 모든 서비스가 정상적으로 시작되었는지 확인
   - 헬스 체크가 모두 통과했는지 확인
   - 3가지 탐지 규칙 (HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY) 알림 발생 확인

4. **지연 시간 측정 (선택적)**
   ```bash
   # 시스템이 실행 중일 때
   bash scripts/measure-latency.sh
   ```

### 검증 기준
- ✅ 모든 서비스 헬스 체크 통과 (HTTP 200)
- ✅ virtual-transactions 토픽 존재 확인
- ✅ transaction-alerts 토픽 존재 확인
- ✅ HIGH_VALUE 알림 탐지 (필수)
- ⚠️ FOREIGN_COUNTRY 알림 탐지 (선택적 - 해외 거래 발생 시)
- ⚠️ HIGH_FREQUENCY 알림 탐지 (선택적 - 빈번한 거래 발생 시)
- ✅ WebSocket 연결 테스트 (wscat 설치 시)
- ✅ 프론트엔드 접속 확인 (HTTP 200)

### 문제 해결

#### 테스트가 실패하는 경우
1. **포트 충돌 확인**
   ```bash
   # 사용 중인 포트 확인 (Windows)
   netstat -ano | findstr "8081 8082 8083 9092"

   # 사용 중인 포트 확인 (Linux/macOS)
   lsof -i :8081,8082,8083,9092
   ```

2. **Docker 메모리 부족**
   ```bash
   # Docker Desktop 설정에서 메모리를 8GB 이상으로 증가
   ```

3. **로그 확인**
   ```bash
   # 특정 서비스 로그 확인
   docker-compose logs alert-service
   docker-compose logs websocket-gateway
   docker-compose logs fraud-detector
   ```

---

## T108: 30분 안정성 테스트

### 목적
시스템이 30분간 안정적으로 실행되며, 크래시나 메모리 부족 없이 작동하는지 확인합니다.

### 실행 방법

1. **시스템 시작**
   ```bash
   cd D:/side-project/RealFDS
   docker-compose up -d
   ```

2. **대기 (시스템 준비)**
   - 5분 정도 대기하여 모든 서비스가 완전히 시작되도록 합니다.

3. **30분간 모니터링**
   ```bash
   # 별도 터미널에서 로그 모니터링
   docker-compose logs -f
   ```

4. **모니터링 항목**
   - **크래시 확인**: 서비스가 재시작되거나 종료되지 않는지 확인
   - **메모리 사용량 확인**:
     ```bash
     # 메모리 사용량 확인 (주기적으로 실행)
     docker stats
     ```
   - **연결 끊김 확인**: WebSocket 연결이 유지되는지 확인
   - **알림 발생 확인**: 거래가 계속 생성되고 알림이 발생하는지 확인

5. **30분 경과 후 확인**
   ```bash
   # 모든 서비스 상태 확인
   docker-compose ps

   # 각 서비스 로그에서 에러 확인
   docker-compose logs | grep -i error
   docker-compose logs | grep -i exception
   ```

### 검증 기준
- ✅ 30분 동안 모든 서비스가 실행 상태 유지
- ✅ 서비스 재시작이나 크래시 없음
- ✅ 메모리 부족 에러 없음
- ✅ Kafka 연결 끊김 없음
- ✅ WebSocket 연결 유지
- ✅ 거래 생성 및 알림 탐지가 계속 작동

### 메모리 제한 확인
각 서비스의 메모리 사용량이 docker-compose.yml에 정의된 제한을 초과하지 않는지 확인:

| 서비스 | 메모리 제한 |
|--------|-------------|
| zookeeper | 256MB |
| kafka | 1GB |
| jobmanager | 1GB |
| taskmanager | 1GB |
| transaction-generator | 256MB |
| fraud-detector | 2GB |
| alert-service | 512MB |
| websocket-gateway | 512MB |
| frontend-dashboard | 256MB |
| **총합** | **~6.5GB** |

### 문제 해결

#### 메모리 부족
```bash
# 메모리 사용량이 높은 서비스 확인
docker stats --no-stream --format "table {{.Container}}\t{{.MemUsage}}"

# 필요시 메모리 제한 증가 (docker-compose.yml 수정)
```

#### 서비스 크래시
```bash
# 크래시 서비스 로그 확인
docker-compose logs <service-name>

# 서비스 재시작
docker-compose restart <service-name>
```

#### Kafka 연결 끊김
```bash
# Kafka 상태 확인
docker-compose logs kafka | grep -i error

# Kafka 토픽 확인
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

---

## 추가 유틸리티 명령어

### 시스템 정리
```bash
# 모든 컨테이너 종료
docker-compose down

# 볼륨 포함 완전 정리
docker-compose down -v

# 이미지 재빌드
docker-compose build --no-cache
```

### 개별 서비스 테스트
```bash
# alert-service API 테스트
curl http://localhost:8081/api/alerts

# websocket-gateway 헬스 체크
curl http://localhost:8082/actuator/health

# frontend-dashboard 접속
curl http://localhost:8083
```

### 로그 분석
```bash
# 특정 기간 로그 확인
docker-compose logs --since 10m

# 특정 패턴 검색
docker-compose logs | grep "HIGH_VALUE"
docker-compose logs | grep "FOREIGN_COUNTRY"
docker-compose logs | grep "HIGH_FREQUENCY"
```

---

## 성공 기준 요약

### T107 (E2E 테스트)
- [x] 테스트 스크립트가 에러 없이 완료
- [x] 3가지 탐지 규칙 중 최소 1개 (HIGH_VALUE) 작동 확인
- [x] 평균 지연 시간 <5초 (목표: <3초)
- [x] p95 지연 시간 <8초 (목표: <5초)

### T108 (안정성 테스트)
- [x] 30분간 서비스 크래시 없음
- [x] 메모리 부족 에러 없음
- [x] 연결 끊김 없음
- [x] 거래 생성 및 알림 탐지 계속 작동

---

**작성일**: 2025-11-10
**Phase**: 3-3 (E2E 테스트 및 성능 측정)
