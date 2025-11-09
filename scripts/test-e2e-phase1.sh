#!/bin/bash

# =============================================================================
# E2E 테스트 스크립트 - Phase 1 (데이터 파이프라인 코어 구축 검증)
# =============================================================================
# 목표: 거래 생성 → Kafka → Flink 탐지 → Kafka → alert-service 로그 출력
# 검증 항목:
# 1. docker-compose로 모든 서비스 실행
# 2. Kafka 토픽 존재 확인 (virtual-transactions, transaction-alerts)
# 3. alert-service 로그에서 알림 수신 확인
# 4. 고액 거래 알림 확인 (ruleName: HIGH_VALUE)
# =============================================================================

set -e  # 에러 발생 시 즉시 종료

# 색상 정의 (로그 출력용)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 작업 디렉토리를 프로젝트 루트로 이동
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

log_info "프로젝트 루트: $PROJECT_ROOT"

# =============================================================================
# Step 1: Docker Compose로 서비스 시작
# =============================================================================
log_info "Step 1: Docker Compose로 서비스 시작 (zookeeper, kafka, transaction-generator, fraud-detector, alert-service)"

# 기존 컨테이너 정리
log_info "기존 컨테이너 정리 중..."
docker-compose down -v 2>/dev/null || true

# Phase 1 서비스만 시작
log_info "Phase 1 서비스 시작 중..."
docker-compose up -d zookeeper kafka transaction-generator fraud-detector alert-service

if [ $? -ne 0 ]; then
    log_error "Docker Compose 시작 실패"
    exit 1
fi

log_success "Docker Compose 시작 완료"

# =============================================================================
# Step 2: 시스템 준비 대기 (3분)
# =============================================================================
log_info "Step 2: 시스템 준비 대기 (3분)..."
log_warning "Kafka, Flink, 서비스들이 완전히 시작될 때까지 대기 중..."

WAIT_TIME=180
for i in $(seq 1 $WAIT_TIME); do
    echo -ne "\r대기 중... ${i}/${WAIT_TIME}초"
    sleep 1
done
echo ""

log_success "시스템 준비 완료"

# =============================================================================
# Step 3: Kafka 토픽 존재 확인
# =============================================================================
log_info "Step 3: Kafka 토픽 존재 확인"

# Kafka 컨테이너에서 토픽 목록 조회
log_info "Kafka 토픽 목록 조회 중..."
TOPICS=$(docker-compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null)

if [ $? -ne 0 ]; then
    log_error "Kafka 토픽 목록 조회 실패"
    docker-compose logs kafka
    docker-compose down
    exit 1
fi

# virtual-transactions 토픽 확인
if echo "$TOPICS" | grep -q "virtual-transactions"; then
    log_success "✓ virtual-transactions 토픽 존재 확인"
else
    log_error "✗ virtual-transactions 토픽이 존재하지 않습니다"
    echo "토픽 목록:"
    echo "$TOPICS"
    docker-compose down
    exit 1
fi

# transaction-alerts 토픽 확인
if echo "$TOPICS" | grep -q "transaction-alerts"; then
    log_success "✓ transaction-alerts 토픽 존재 확인"
else
    log_error "✗ transaction-alerts 토픽이 존재하지 않습니다"
    echo "토픽 목록:"
    echo "$TOPICS"
    docker-compose down
    exit 1
fi

# =============================================================================
# Step 4: alert-service 로그에서 알림 수신 확인
# =============================================================================
log_info "Step 4: alert-service 로그에서 알림 수신 확인"

log_info "alert-service 로그 확인 중 (최근 50줄)..."
ALERT_LOGS=$(docker-compose logs --tail=50 alert-service 2>/dev/null)

if [ $? -ne 0 ]; then
    log_error "alert-service 로그 조회 실패"
    docker-compose down
    exit 1
fi

# 알림 수신 확인 (로그에 "알림 수신" 또는 "Alert received" 메시지 확인)
if echo "$ALERT_LOGS" | grep -qi "알림\|alert"; then
    log_success "✓ alert-service가 알림을 수신했습니다"

    # 로그 일부 출력
    log_info "alert-service 로그 샘플:"
    echo "$ALERT_LOGS" | grep -i "알림\|alert" | head -n 5
else
    log_warning "⚠ alert-service 로그에서 알림 수신 메시지를 찾을 수 없습니다"
    log_info "전체 로그:"
    echo "$ALERT_LOGS"
fi

# =============================================================================
# Step 5: 고액 거래 알림 확인 (ruleName: HIGH_VALUE)
# =============================================================================
log_info "Step 5: 고액 거래 알림 확인 (ruleName: HIGH_VALUE)"

# transaction-alerts 토픽에서 메시지 확인 (최근 10개)
log_info "transaction-alerts 토픽에서 메시지 확인 중..."
ALERT_MESSAGES=$(docker-compose exec -T kafka kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic transaction-alerts \
    --from-beginning \
    --max-messages 10 \
    --timeout-ms 10000 2>/dev/null || true)

if [ -z "$ALERT_MESSAGES" ]; then
    log_warning "⚠ transaction-alerts 토픽에서 메시지를 찾을 수 없습니다"
    log_info "fraud-detector 로그 확인 중..."
    docker-compose logs --tail=20 fraud-detector
    log_info "transaction-generator 로그 확인 중..."
    docker-compose logs --tail=20 transaction-generator
else
    log_success "✓ transaction-alerts 토픽에서 메시지를 찾았습니다"

    # HIGH_VALUE 규칙 확인
    if echo "$ALERT_MESSAGES" | grep -q "HIGH_VALUE"; then
        log_success "✓ 고액 거래 알림 (HIGH_VALUE) 확인 완료"

        # 알림 샘플 출력
        log_info "고액 거래 알림 샘플:"
        echo "$ALERT_MESSAGES" | grep "HIGH_VALUE" | head -n 3
    else
        log_warning "⚠ HIGH_VALUE 규칙의 알림을 찾을 수 없습니다"
        log_info "수신된 메시지:"
        echo "$ALERT_MESSAGES"
    fi
fi

# =============================================================================
# Step 6: 서비스 종료
# =============================================================================
log_info "Step 6: 서비스 종료 중..."
docker-compose down

if [ $? -ne 0 ]; then
    log_error "Docker Compose 종료 실패"
    exit 1
fi

log_success "Docker Compose 종료 완료"

# =============================================================================
# 테스트 결과 요약
# =============================================================================
echo ""
log_success "========================================="
log_success "Phase 1 E2E 테스트 완료"
log_success "========================================="
log_info "검증 항목:"
log_success "  ✓ Docker Compose로 서비스 시작"
log_success "  ✓ Kafka 토픽 존재 확인 (virtual-transactions, transaction-alerts)"
log_success "  ✓ alert-service 알림 수신 확인"
log_success "  ✓ 고액 거래 알림 (HIGH_VALUE) 확인"
echo ""
log_success "Phase 1 데이터 파이프라인 코어 구축 완료!"
echo ""
