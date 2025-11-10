#!/bin/bash
# 지연 시간 측정 스크립트
# 목적: 거래 발생부터 알림 수신까지의 종단 간 지연 시간 측정
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

# 측정 시작
log_info "종단 간 지연 시간 측정 시작: $(date)"

# 임시 파일 경로
TRANSACTIONS_FILE="/tmp/transactions.json"
ALERTS_FILE="/tmp/alerts.json"
LATENCY_DATA="/tmp/latency_data.txt"

# 기존 임시 파일 삭제
rm -f "$TRANSACTIONS_FILE" "$ALERTS_FILE" "$LATENCY_DATA"

# Step 1: Kafka 토픽에서 Transaction 타임스탬프 추출
log_info "Step 1: virtual-transactions 토픽에서 거래 데이터 수집"

timeout 30 docker-compose exec -T kafka kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic virtual-transactions \
    --from-beginning \
    --max-messages 100 > "$TRANSACTIONS_FILE" 2>/dev/null || true

if [ ! -s "$TRANSACTIONS_FILE" ]; then
    log_error "virtual-transactions 토픽에서 데이터를 가져올 수 없음"
    exit 1
fi

TRANSACTION_COUNT=$(wc -l < "$TRANSACTIONS_FILE")
log_success "거래 데이터 수집 완료: ${TRANSACTION_COUNT}건"

# Step 2: alert-service 로그 또는 Kafka alerts 토픽에서 Alert 수신 타임스탬프 추출
log_info "Step 2: transaction-alerts 토픽에서 알림 데이터 수집"

timeout 30 docker-compose exec -T kafka kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic transaction-alerts \
    --from-beginning \
    --max-messages 100 > "$ALERTS_FILE" 2>/dev/null || true

if [ ! -s "$ALERTS_FILE" ]; then
    log_error "transaction-alerts 토픽에서 데이터를 가져올 수 없음"
    exit 1
fi

ALERT_COUNT=$(wc -l < "$ALERTS_FILE")
log_success "알림 데이터 수집 완료: ${ALERT_COUNT}건"

# Step 3: 지연 시간 계산 (Python 스크립트 사용)
log_info "Step 3: 지연 시간 계산"

cat > /tmp/calculate_latency.py << 'EOF'
#!/usr/bin/env python3
# 지연 시간 계산 스크립트
import json
import sys
from datetime import datetime
import statistics

def parse_timestamp(ts_str):
    """ISO 8601 타임스탬프를 datetime 객체로 변환"""
    # 마이크로초 포함 형식: "2025-11-06T10:30:45.123Z"
    # 또는 밀리초 포함 형식: "2025-11-06T10:30:45.123456Z"
    for fmt in ["%Y-%m-%dT%H:%M:%S.%fZ", "%Y-%m-%dT%H:%M:%SZ"]:
        try:
            return datetime.strptime(ts_str, fmt)
        except ValueError:
            continue
    raise ValueError(f"Invalid timestamp format: {ts_str}")

def main():
    # 거래 데이터 로드
    transactions = {}
    with open('/tmp/transactions.json', 'r') as f:
        for line in f:
            try:
                tx = json.loads(line.strip())
                tx_id = tx['transactionId']
                tx_time = parse_timestamp(tx['timestamp'])
                transactions[tx_id] = tx_time
            except (json.JSONDecodeError, KeyError, ValueError) as e:
                print(f"거래 파싱 오류: {e}", file=sys.stderr)
                continue

    print(f"파싱된 거래: {len(transactions)}건")

    # 알림 데이터 로드 및 지연 시간 계산
    latencies = []
    with open('/tmp/alerts.json', 'r') as f:
        for line in f:
            try:
                alert = json.loads(line.strip())
                tx_id = alert['originalTransaction']['transactionId']
                alert_time = parse_timestamp(alert['alertTimestamp'])

                if tx_id in transactions:
                    tx_time = transactions[tx_id]
                    latency_seconds = (alert_time - tx_time).total_seconds()

                    # 음수 지연 시간 방지 (시계 동기화 문제)
                    if latency_seconds >= 0:
                        latencies.append(latency_seconds)
                        print(f"{tx_id}: {latency_seconds:.2f}초")
                    else:
                        print(f"{tx_id}: 음수 지연 시간 무시 ({latency_seconds:.2f}초)", file=sys.stderr)
                else:
                    print(f"거래 ID {tx_id}를 찾을 수 없음", file=sys.stderr)
            except (json.JSONDecodeError, KeyError, ValueError) as e:
                print(f"알림 파싱 오류: {e}", file=sys.stderr)
                continue

    # 통계 계산
    if not latencies:
        print("지연 시간 데이터가 없습니다.", file=sys.stderr)
        sys.exit(1)

    latencies.sort()

    avg_latency = statistics.mean(latencies)
    p95_latency = latencies[int(len(latencies) * 0.95)] if len(latencies) > 1 else latencies[0]
    p99_latency = latencies[int(len(latencies) * 0.99)] if len(latencies) > 1 else latencies[0]
    min_latency = min(latencies)
    max_latency = max(latencies)

    print("\n" + "=" * 50)
    print("지연 시간 통계")
    print("=" * 50)
    print(f"측정 샘플 수: {len(latencies)}건")
    print(f"최소: {min_latency:.2f}초")
    print(f"평균: {avg_latency:.2f}초 (목표: <3초)")
    print(f"p95: {p95_latency:.2f}초 (목표: <5초)")
    print(f"p99: {p99_latency:.2f}초")
    print(f"최대: {max_latency:.2f}초")
    print("=" * 50)

    # 목표 달성 여부 확인
    if avg_latency < 3.0:
        print("✅ 평균 지연 시간 목표 달성 (<3초)")
    else:
        print(f"❌ 평균 지연 시간 목표 미달성: {avg_latency:.2f}초 (목표: <3초)")

    if p95_latency < 5.0:
        print("✅ p95 지연 시간 목표 달성 (<5초)")
    else:
        print(f"❌ p95 지연 시간 목표 미달성: {p95_latency:.2f}초 (목표: <5초)")

    # 결과를 파일에 저장
    with open('/tmp/latency_data.txt', 'w') as f:
        for lat in latencies:
            f.write(f"{lat:.2f}\n")

if __name__ == '__main__':
    main()
EOF

# Python 스크립트 실행
python3 /tmp/calculate_latency.py

if [ $? -ne 0 ]; then
    log_error "지연 시간 계산 실패"
    exit 1
fi

log_success "지연 시간 측정 완료"

# Step 4: 히스토그램 출력 (선택적)
if [ -s "$LATENCY_DATA" ]; then
    log_info "Step 4: 지연 시간 분포 히스토그램"

    # 간단한 ASCII 히스토그램 생성 (awk 사용)
    awk '
    BEGIN {
        print "지연 시간 분포 (초):"
        print "0-1초   | "
        print "1-2초   | "
        print "2-3초   | "
        print "3-5초   | "
        print "5-10초  | "
        print "10초+   | "
    }
    {
        if ($1 < 1) count[0]++
        else if ($1 < 2) count[1]++
        else if ($1 < 3) count[2]++
        else if ($1 < 5) count[3]++
        else if ($1 < 10) count[4]++
        else count[5]++
        total++
    }
    END {
        ranges[0] = "0-1초  "
        ranges[1] = "1-2초  "
        ranges[2] = "2-3초  "
        ranges[3] = "3-5초  "
        ranges[4] = "5-10초 "
        ranges[5] = "10초+  "

        for (i = 0; i < 6; i++) {
            pct = (count[i] / total) * 100
            bars = int(pct / 2)  # 1 bar = 2%
            printf "%s | ", ranges[i]
            for (j = 0; j < bars; j++) printf "█"
            printf " %.1f%% (%d건)\n", pct, count[i]
        }
    }
    ' "$LATENCY_DATA"
fi

log_info "측정 종료: $(date)"
exit 0
