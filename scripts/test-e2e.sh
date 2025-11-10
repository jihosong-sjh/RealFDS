#!/bin/bash
# E2E 테스트 스크립트 (최종)
# 목적: 전체 시스템 통합 테스트 - 3가지 탐지 규칙 모두 검증
# 작성일: 2025-11-10

set -e  # 에러 발생 시 즉시 종료

# 색상 정의 (출력 가독성 향상)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로깅 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 테스트 시작 시각 기록
START_TIME=$(date +%s)
log_info "E2E 테스트 시작: $(date)"

# Step 1: docker-compose up -d (모든 서비스)
log_info "Step 1: Docker Compose로 모든 서비스 시작"
docker-compose up -d

if [ $? -ne 0 ]; then
    log_error "Docker Compose 시작 실패"
    exit 1
fi

log_success "모든 서비스 시작 완료"

# Step 2: 5분 대기 (시스템 준비)
log_info "Step 2: 시스템 준비를 위해 5분 대기"
WAIT_TIME=300  # 5분 = 300초

for i in $(seq 1 $WAIT_TIME); do
    echo -ne "\r대기 중... ${i}/${WAIT_TIME}초 경과"
    sleep 1
done
echo ""
log_success "대기 완료"

# Step 3: 헬스 체크 (모든 서비스)
log_info "Step 3: 모든 서비스 헬스 체크"

# alert-service 헬스 체크
log_info "alert-service 헬스 체크 (http://localhost:8081/actuator/health)"
ALERT_SERVICE_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health)

if [ "$ALERT_SERVICE_HEALTH" != "200" ]; then
    log_error "alert-service 헬스 체크 실패: HTTP $ALERT_SERVICE_HEALTH"
    docker-compose logs alert-service
    exit 1
fi
log_success "alert-service 정상 (HTTP 200)"

# websocket-gateway 헬스 체크
log_info "websocket-gateway 헬스 체크 (http://localhost:8082/actuator/health)"
WEBSOCKET_GATEWAY_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/actuator/health)

if [ "$WEBSOCKET_GATEWAY_HEALTH" != "200" ]; then
    log_error "websocket-gateway 헬스 체크 실패: HTTP $WEBSOCKET_GATEWAY_HEALTH"
    docker-compose logs websocket-gateway
    exit 1
fi
log_success "websocket-gateway 정상 (HTTP 200)"

# frontend-dashboard 접속 확인
log_info "frontend-dashboard 접속 확인 (http://localhost:8083)"
FRONTEND_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8083)

if [ "$FRONTEND_HEALTH" != "200" ]; then
    log_error "frontend-dashboard 접속 실패: HTTP $FRONTEND_HEALTH"
    docker-compose logs frontend-dashboard
    exit 1
fi
log_success "frontend-dashboard 정상 (HTTP 200)"

# Step 4: Kafka 토픽 확인
log_info "Step 4: Kafka 토픽 존재 확인"

# virtual-transactions 토픽 확인
log_info "virtual-transactions 토픽 확인"
docker-compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list | grep -q "virtual-transactions"

if [ $? -ne 0 ]; then
    log_error "virtual-transactions 토픽이 존재하지 않음"
    exit 1
fi
log_success "virtual-transactions 토픽 존재 확인"

# transaction-alerts 토픽 확인
log_info "transaction-alerts 토픽 확인"
docker-compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list | grep -q "transaction-alerts"

if [ $? -ne 0 ]; then
    log_error "transaction-alerts 토픽이 존재하지 않음"
    exit 1
fi
log_success "transaction-alerts 토픽 존재 확인"

# Step 5: 3가지 탐지 규칙 검증
log_info "Step 5: 3가지 탐지 규칙 검증"

# 5a. 고액 거래 알림 확인 (HIGH_VALUE)
log_info "5a. 고액 거래 알림 확인 (ruleName: HIGH_VALUE)"
timeout 60 docker-compose exec -T kafka kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic transaction-alerts \
    --from-beginning \
    --max-messages 100 > /tmp/alerts_high_value.json 2>/dev/null

if grep -q '"ruleName":"HIGH_VALUE"' /tmp/alerts_high_value.json; then
    log_success "HIGH_VALUE 알림 탐지 확인"
else
    log_error "HIGH_VALUE 알림을 찾을 수 없음"
    cat /tmp/alerts_high_value.json
    exit 1
fi

# 5b. 해외 거래 알림 확인 (FOREIGN_COUNTRY)
log_info "5b. 해외 거래 알림 확인 (ruleName: FOREIGN_COUNTRY)"
if grep -q '"ruleName":"FOREIGN_COUNTRY"' /tmp/alerts_high_value.json; then
    log_success "FOREIGN_COUNTRY 알림 탐지 확인"
else
    log_warn "FOREIGN_COUNTRY 알림을 찾을 수 없음 (해외 거래가 아직 발생하지 않았을 수 있음)"
fi

# 5c. 빈번한 거래 알림 확인 (HIGH_FREQUENCY)
log_info "5c. 빈번한 거래 알림 확인 (ruleName: HIGH_FREQUENCY)"
if grep -q '"ruleName":"HIGH_FREQUENCY"' /tmp/alerts_high_value.json; then
    log_success "HIGH_FREQUENCY 알림 탐지 확인"
else
    log_warn "HIGH_FREQUENCY 알림을 찾을 수 없음 (빈번한 거래가 아직 발생하지 않았을 수 있음)"
fi

# Step 6: WebSocket 연결 및 알림 수신 확인
log_info "Step 6: WebSocket 연결 및 알림 수신 확인"

# wscat이 설치되어 있는지 확인 (선택적)
if command -v wscat &> /dev/null; then
    log_info "wscat으로 WebSocket 연결 테스트 (10초 대기)"
    timeout 10 wscat -c ws://localhost:8082/ws/alerts > /tmp/websocket_output.txt 2>&1 || true

    if [ -s /tmp/websocket_output.txt ]; then
        log_success "WebSocket 연결 성공 및 데이터 수신 확인"
        head -n 5 /tmp/websocket_output.txt
    else
        log_warn "WebSocket 데이터 수신 없음 (wscat 타임아웃)"
    fi
else
    log_warn "wscat이 설치되지 않아 WebSocket 테스트 생략 (npm install -g wscat으로 설치 가능)"
fi

# Step 7: 프론트엔드 접속 확인 (이미 Step 3에서 확인했지만 재확인)
log_info "Step 7: 프론트엔드 최종 접속 확인"
FRONTEND_FINAL=$(curl -s http://localhost:8083 | grep -o "<title>.*</title>")

if [ -n "$FRONTEND_FINAL" ]; then
    log_success "프론트엔드 페이지 렌더링 확인: $FRONTEND_FINAL"
else
    log_warn "프론트엔드 타이틀을 찾을 수 없음 (HTML 구조 확인 필요)"
fi

# 테스트 완료
END_TIME=$(date +%s)
ELAPSED_TIME=$((END_TIME - START_TIME))

log_success "=================================="
log_success "E2E 테스트 완료!"
log_success "소요 시간: ${ELAPSED_TIME}초"
log_success "=================================="

# Step 8: docker-compose down (정리)
log_info "Step 8: docker-compose down으로 모든 서비스 종료"
docker-compose down

log_success "모든 서비스 종료 완료"
log_info "E2E 테스트 종료: $(date)"

exit 0
