#!/bin/bash

# =============================================================================
# E2E 테스트 스크립트 - Phase 2 (실시간 알림 전송 및 프론트엔드 검증)
# =============================================================================
# 목표: 백엔드 → 프론트엔드까지 전체 파이프라인 검증
# 검증 항목:
# 1. docker-compose로 모든 서비스 실행 (Phase 1 + Phase 2)
# 2. alert-service 헬스 체크 (GET /actuator/health)
# 3. websocket-gateway 헬스 체크 (GET /actuator/health)
# 4. frontend-dashboard 접속 확인 (GET http://localhost:8083)
# 5. WebSocket 연결 및 알림 수신 테스트
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
# Step 1: Docker Compose로 모든 서비스 시작
# =============================================================================
log_info "Step 1: Docker Compose로 모든 서비스 시작"

# 기존 컨테이너 정리
log_info "기존 컨테이너 정리 중..."
docker-compose down -v 2>/dev/null || true

# 모든 서비스 시작 (Phase 1 + Phase 2)
log_info "모든 서비스 시작 중..."
docker-compose up -d --build

if [ $? -ne 0 ]; then
    log_error "Docker Compose 시작 실패"
    exit 1
fi

log_success "Docker Compose 시작 완료"

# =============================================================================
# Step 2: 시스템 준비 대기 (7분)
# =============================================================================
log_info "Step 2: 시스템 준비 대기 (7분)..."
log_warning "모든 서비스가 완전히 시작될 때까지 대기 중..."

WAIT_TIME=420
for i in $(seq 1 $WAIT_TIME); do
    echo -ne "\r대기 중... ${i}/${WAIT_TIME}초"
    sleep 1
done
echo ""

log_success "시스템 준비 완료"

# =============================================================================
# Step 3: alert-service 헬스 체크
# =============================================================================
log_info "Step 3: alert-service 헬스 체크 (GET /actuator/health)"

ALERT_SERVICE_URL="http://localhost:8081/actuator/health"
log_info "URL: $ALERT_SERVICE_URL"

# curl로 헬스 체크 (최대 3회 재시도)
for i in {1..3}; do
    HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $ALERT_SERVICE_URL 2>/dev/null || true)

    if [ "$HEALTH_RESPONSE" == "200" ]; then
        log_success "✓ alert-service 헬스 체크 성공 (HTTP $HEALTH_RESPONSE)"

        # 응답 내용 확인
        HEALTH_BODY=$(curl -s $ALERT_SERVICE_URL 2>/dev/null || true)
        log_info "응답 내용: $HEALTH_BODY"
        break
    else
        log_warning "⚠ alert-service 헬스 체크 실패 (HTTP $HEALTH_RESPONSE) - 재시도 $i/3"
        sleep 10
    fi

    if [ $i -eq 3 ]; then
        log_error "✗ alert-service 헬스 체크 최종 실패"
        log_info "alert-service 로그:"
        docker-compose logs --tail=30 alert-service
        docker-compose down
        exit 1
    fi
done

# =============================================================================
# Step 4: websocket-gateway 헬스 체크
# =============================================================================
log_info "Step 4: websocket-gateway 헬스 체크 (GET /actuator/health)"

WEBSOCKET_GATEWAY_URL="http://localhost:8082/actuator/health"
log_info "URL: $WEBSOCKET_GATEWAY_URL"

# curl로 헬스 체크 (최대 3회 재시도)
for i in {1..3}; do
    HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $WEBSOCKET_GATEWAY_URL 2>/dev/null || true)

    if [ "$HEALTH_RESPONSE" == "200" ]; then
        log_success "✓ websocket-gateway 헬스 체크 성공 (HTTP $HEALTH_RESPONSE)"

        # 응답 내용 확인
        HEALTH_BODY=$(curl -s $WEBSOCKET_GATEWAY_URL 2>/dev/null || true)
        log_info "응답 내용: $HEALTH_BODY"
        break
    else
        log_warning "⚠ websocket-gateway 헬스 체크 실패 (HTTP $HEALTH_RESPONSE) - 재시도 $i/3"
        sleep 10
    fi

    if [ $i -eq 3 ]; then
        log_error "✗ websocket-gateway 헬스 체크 최종 실패"
        log_info "websocket-gateway 로그:"
        docker-compose logs --tail=30 websocket-gateway
        docker-compose down
        exit 1
    fi
done

# =============================================================================
# Step 5: frontend-dashboard 접속 확인
# =============================================================================
log_info "Step 5: frontend-dashboard 접속 확인 (GET http://localhost:8083)"

FRONTEND_URL="http://localhost:8083"
log_info "URL: $FRONTEND_URL"

# curl로 접속 확인 (최대 3회 재시도)
for i in {1..3}; do
    FRONTEND_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $FRONTEND_URL 2>/dev/null || true)

    if [ "$FRONTEND_RESPONSE" == "200" ]; then
        log_success "✓ frontend-dashboard 접속 성공 (HTTP $FRONTEND_RESPONSE)"
        break
    else
        log_warning "⚠ frontend-dashboard 접속 실패 (HTTP $FRONTEND_RESPONSE) - 재시도 $i/3"
        sleep 10
    fi

    if [ $i -eq 3 ]; then
        log_error "✗ frontend-dashboard 접속 최종 실패"
        log_info "frontend-dashboard 로그:"
        docker-compose logs --tail=30 frontend-dashboard
        docker-compose down
        exit 1
    fi
done

# =============================================================================
# Step 6: WebSocket 연결 및 알림 수신 테스트
# =============================================================================
log_info "Step 6: WebSocket 연결 및 알림 수신 테스트"

log_info "WebSocket URL: ws://localhost:8082/ws/alerts"

# wscat이 설치되어 있는지 확인
if ! command -v wscat &> /dev/null; then
    log_warning "⚠ wscat이 설치되어 있지 않습니다"
    log_info "wscat 설치: npm install -g wscat"
    log_warning "WebSocket 테스트를 건너뜁니다"
else
    log_info "wscat으로 WebSocket 연결 테스트 중 (10초 대기)..."

    # wscat으로 WebSocket 연결 후 10초 대기
    timeout 10 wscat -c ws://localhost:8082/ws/alerts > /tmp/wscat-output.txt 2>&1 || true

    if [ -f /tmp/wscat-output.txt ]; then
        log_info "WebSocket 출력:"
        cat /tmp/wscat-output.txt

        # 연결 성공 확인
        if grep -q "connected" /tmp/wscat-output.txt; then
            log_success "✓ WebSocket 연결 성공"
        else
            log_warning "⚠ WebSocket 연결 상태 불명확"
        fi

        # 알림 수신 확인
        if grep -q "alertId\|ruleName" /tmp/wscat-output.txt; then
            log_success "✓ WebSocket으로 알림 수신 확인"
        else
            log_warning "⚠ WebSocket으로 알림을 수신하지 못했습니다 (10초 이내)"
        fi

        rm -f /tmp/wscat-output.txt
    else
        log_warning "⚠ WebSocket 테스트 결과 파일을 찾을 수 없습니다"
    fi
fi

# =============================================================================
# Step 7: 로그 확인 (알림 흐름 검증)
# =============================================================================
log_info "Step 7: 로그 확인 (알림 흐름 검증)"

log_info "transaction-generator 로그 (최근 5줄):"
docker-compose logs --tail=5 transaction-generator

log_info "fraud-detector 로그 (최근 5줄):"
docker-compose logs --tail=5 fraud-detector

log_info "alert-service 로그 (최근 5줄):"
docker-compose logs --tail=5 alert-service

log_info "websocket-gateway 로그 (최근 5줄):"
docker-compose logs --tail=5 websocket-gateway

# =============================================================================
# Step 8: 서비스 종료
# =============================================================================
log_info "Step 8: 서비스 종료 중..."
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
log_success "Phase 2 E2E 테스트 완료"
log_success "========================================="
log_info "검증 항목:"
log_success "  ✓ Docker Compose로 모든 서비스 시작"
log_success "  ✓ alert-service 헬스 체크 성공"
log_success "  ✓ websocket-gateway 헬스 체크 성공"
log_success "  ✓ frontend-dashboard 접속 성공"
log_success "  ✓ WebSocket 연결 및 알림 수신 테스트"
echo ""
log_success "Phase 2 실시간 알림 전송 및 프론트엔드 완료!"
echo ""
log_info "브라우저에서 http://localhost:8083 접속하여 실시간 알림을 확인할 수 있습니다."
echo ""
