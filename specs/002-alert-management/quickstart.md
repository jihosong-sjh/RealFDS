# Quick Start Guide: 알림 확인 및 처리 시스템

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)
**Created**: 2025-11-11

## Overview

이 가이드는 알림 확인 및 처리 시스템의 주요 기능을 빠르게 체험할 수 있도록 단계별 사용법을 제공합니다.

**주요 기능**:
1. 알림 상태 관리 (미확인 → 확인중 → 완료)
2. 담당자 할당 및 필터링
3. 조치 내용 기록
4. 심각도별 색상 구분 및 정렬

**소요 시간**: 약 10분

---

## Prerequisites

### 시스템 요구사항

- Docker & Docker Compose 설치
- 최소 8GB RAM, 4 core CPU
- 포트 사용 가능: 8080, 8081, 8082, 5173, 9092, 2181

### 프로젝트 실행

```bash
# 1. 프로젝트 루트로 이동
cd RealFDS

# 2. 브랜치 전환 (002-alert-management)
git checkout 002-alert-management

# 3. Docker Compose로 모든 서비스 시작
docker-compose up -d

# 4. 서비스 준비 대기 (약 2-3분)
# 모든 서비스가 healthy 상태가 될 때까지 대기
docker-compose ps

# 5. 프론트엔드 접속
# 브라우저에서 http://localhost:5173 열기
```

**서비스 헬스 체크**:
```bash
# fraud-detector
curl http://localhost:8080/actuator/health

# alert-service
curl http://localhost:8081/actuator/health

# websocket-gateway
curl http://localhost:8082/actuator/health
```

---

## Step 1: 알림 생성 확인

### 1.1. 신규 알림 발생 확인

**브라우저에서**:
1. `http://localhost:5173` 접속
2. 대시보드 화면에 알림 목록 표시 확인
3. 알림 항목에 다음 정보 표시 확인:
   - **상태 뱃지**: "미확인" (회색)
   - **심각도 뱃지**: "높음" (주황색) 또는 "보통" (노란색)
   - **거래 정보**: 금액, 가맹점, 국가
   - **담당자**: "미할당" 표시

**예시 화면**:
```
┌─────────────────────────────────────────────┐
│ 실시간 사기 탐지 시스템                         │
├─────────────────────────────────────────────┤
│ [미확인] [높음] 100만원 이상 고액 거래           │
│ 금액: ₩1,500,000 | 가맹점: 해외 쇼핑몰          │
│ 담당자: 미할당 | 2025-11-11 10:15:01         │
├─────────────────────────────────────────────┤
│ [미확인] [보통] 해외 국가에서 거래 발생          │
│ 금액: ₩50,000 | 가맹점: Amazon US            │
│ 담당자: 미할당 | 2025-11-11 10:16:00         │
└─────────────────────────────────────────────┘
```

### 1.2. 실시간 알림 수신 확인

**테스트 거래 생성** (transaction-generator가 자동 생성):
- 고액 거래 (100만원 이상) → "높음" 심각도 (주황색)
- 해외 거래 → "보통" 심각도 (노란색)
- 빈번한 거래 (1분 내 3회 이상) → "높음" 심각도 (주황색)

**확인 사항**:
- ✅ 알림이 실시간으로 목록 상단에 추가됨
- ✅ 브라우저 알림 표시 (권한 허용 시)
- ✅ 알림음 재생 (긴급 알림은 다른 소리)

---

## Step 2: 알림 상태 관리

### 2.1. 알림 상세 보기

**브라우저에서**:
1. 알림 항목 클릭
2. 알림 상세 모달 표시 확인
3. 모달 내용:
   - 원본 거래 정보 (전체)
   - 탐지 규칙 및 사유
   - 심각도 및 현재 상태
   - 담당자 할당 영역
   - 조치 내용 입력 영역
   - 상태 변경 버튼

**예시 모달**:
```
┌────────────────────────────────────────┐
│ 알림 상세 정보                          │
├────────────────────────────────────────┤
│ [높음] 100만원 이상 고액 거래            │
│                                        │
│ 거래 ID: txn-12345                     │
│ 사용자 ID: user-001                    │
│ 카드 번호: 1234-****-****-5678         │
│ 금액: ₩1,500,000                       │
│ 가맹점: 해외 쇼핑몰                      │
│ 국가: US                               │
│ 거래 시각: 2025-11-11 10:15:00         │
│                                        │
│ 현재 상태: [미확인]                     │
│                                        │
│ 담당자: [미할당]                        │
│ [할당하기]                              │
│                                        │
│ 조치 내용: (없음)                       │
│ [조치 기록하기]                         │
│                                        │
│ [확인중으로 변경] [완료 처리]            │
└────────────────────────────────────────┘
```

### 2.2. 상태를 "확인중"으로 변경

**브라우저에서**:
1. 알림 상세 모달에서 **"확인중으로 변경"** 버튼 클릭
2. 모달에서 상태 뱃지가 "확인중" (파란색)으로 변경 확인
3. 모달 닫기
4. 알림 목록에서 해당 알림의 상태 뱃지가 "확인중" (파란색)으로 변경 확인

**REST API로 직접 테스트**:
```bash
# 상태를 IN_PROGRESS로 변경
curl -X PATCH http://localhost:8081/api/alerts/{alertId}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'

# 결과 확인
curl http://localhost:8081/api/alerts/{alertId}
```

**확인 사항**:
- ✅ 상태 뱃지가 "미확인" → "확인중"으로 변경
- ✅ 색상이 회색 → 파란색으로 변경
- ✅ 다른 브라우저 탭에서도 실시간으로 동기화됨 (WebSocket)
- ✅ 브라우저 새로고침 후에도 상태 유지

### 2.3. 상태를 "완료"로 변경

**브라우저에서**:
1. 알림 상세 모달에서 **"완료 처리"** 버튼 클릭
2. 모달에서 상태 뱃지가 "완료" (초록색)으로 변경 확인
3. **처리 완료 시각** 자동 설정 확인 (예: "2025-11-11 10:30:00")
4. 모달 닫기
5. 알림 목록에서 해당 알림의 상태 뱃지가 "완료" (초록색)으로 변경 확인

**REST API로 직접 테스트**:
```bash
# 상태를 COMPLETED로 변경
curl -X PATCH http://localhost:8081/api/alerts/{alertId}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}'

# processedAt 자동 설정 확인
curl http://localhost:8081/api/alerts/{alertId} | jq '.processedAt'
```

**확인 사항**:
- ✅ 상태 뱃지가 "확인중" → "완료"로 변경
- ✅ 색상이 파란색 → 초록색으로 변경
- ✅ 처리 완료 시각이 자동으로 기록됨
- ✅ 다른 브라우저 탭에서도 실시간으로 동기화됨

---

## Step 3: 담당자 할당

### 3.1. 담당자 할당하기

**브라우저에서**:
1. 알림 상세 모달 열기
2. **담당자 입력 필드**에 이름 입력 (예: "김보안")
3. **"할당하기"** 버튼 클릭
4. 모달에서 담당자가 "김보안"으로 표시됨 확인
5. 모달 닫기
6. 알림 목록에서 해당 알림에 담당자 "김보안" 표시 확인

**REST API로 직접 테스트**:
```bash
# 담당자 할당
curl -X PATCH http://localhost:8081/api/alerts/{alertId}/assign \
  -H "Content-Type: application/json" \
  -d '{"assignedTo": "김보안"}'

# 결과 확인
curl http://localhost:8081/api/alerts/{alertId} | jq '.assignedTo'
```

**확인 사항**:
- ✅ 담당자 이름이 표시됨
- ✅ 미할당 알림은 "미할당" 텍스트 표시
- ✅ 다른 브라우저 탭에서도 실시간으로 동기화됨

### 3.2. 담당자별 필터링

**브라우저에서**:
1. 알림 목록 상단의 **필터 패널** 열기
2. **담당자 필터 드롭다운**에서 "김보안" 선택
3. 알림 목록이 "김보안"이 담당하는 알림만 표시됨 확인

**REST API로 직접 테스트**:
```bash
# 담당자별 필터링
curl "http://localhost:8081/api/alerts?assignedTo=김보안"
```

**확인 사항**:
- ✅ 선택한 담당자의 알림만 표시됨
- ✅ "전체" 선택 시 모든 알림 표시
- ✅ 필터링 응답 시간 <100ms

---

## Step 4: 조치 내용 기록

### 4.1. 조치 내용 입력

**브라우저에서**:
1. 알림 상세 모달 열기
2. **조치 내용 텍스트 영역**에 내용 입력
   - 예: "고객에게 확인 전화. 정상 거래로 확인됨."
3. 선택적으로 **"완료 처리"** 체크박스 선택
4. **"저장"** 버튼 클릭
5. 조치 내용이 저장되고, "완료 처리" 선택 시 상태가 "완료"로 변경됨 확인

**REST API로 직접 테스트**:
```bash
# 조치 내용만 기록
curl -X POST http://localhost:8081/api/alerts/{alertId}/action \
  -H "Content-Type: application/json" \
  -d '{"actionNote": "고객에게 확인 메일 발송"}'

# 조치 내용 + 완료 처리
curl -X POST http://localhost:8081/api/alerts/{alertId}/action \
  -H "Content-Type: application/json" \
  -d '{
    "actionNote": "고객 확인 완료. 정상 거래.",
    "status": "COMPLETED"
  }'

# 결과 확인
curl http://localhost:8081/api/alerts/{alertId} | jq '{actionNote, status, processedAt}'
```

**확인 사항**:
- ✅ 조치 내용이 저장되고 모달에 표시됨
- ✅ "완료 처리" 선택 시 상태가 "완료"로 변경
- ✅ 처리 완료 시각이 자동으로 기록됨
- ✅ 다른 브라우저 탭에서도 실시간으로 동기화됨

### 4.2. 조치 내용 조회

**브라우저에서**:
1. 완료된 알림의 상세 모달 열기
2. 조치 내용 영역에 저장된 내용 표시 확인
3. 처리 완료 시각 표시 확인

---

## Step 5: 심각도별 색상 구분

### 5.1. 심각도별 색상 확인

**브라우저에서**:
1. 알림 목록에서 각 알림의 심각도 뱃지 색상 확인:
   - **긴급** (CRITICAL): 빨간색 뱃지 + 연한 빨간색 배경
   - **높음** (HIGH): 주황색 뱃지 + 연한 주황색 배경
   - **보통** (MEDIUM): 노란색 뱃지 + 연한 노란색 배경
   - **낮음** (LOW): 파란색 뱃지 + 연한 파란색 배경

**예시**:
```
┌─────────────────────────────────────────────┐
│ [미확인] [긴급] 신규 패턴 탐지 ← 빨간색 배경   │
├─────────────────────────────────────────────┤
│ [미확인] [높음] 100만원 이상 고액 거래         │
│                           ← 주황색 배경       │
├─────────────────────────────────────────────┤
│ [미확인] [보통] 해외 국가에서 거래 발생        │
│                           ← 노란색 배경       │
└─────────────────────────────────────────────┘
```

**확인 사항**:
- ✅ 각 심각도별 색상 구분이 명확함
- ✅ 긴급 알림이 시각적으로 가장 눈에 띔
- ✅ 색상 코딩으로 우선순위 판단 용이

### 5.2. 심각도별 필터링

**브라우저에서**:
1. 필터 패널의 **심각도 필터 드롭다운**에서 "높음" 선택
2. 알림 목록이 "높음" 심각도 알림만 표시됨 확인

**REST API로 직접 테스트**:
```bash
# 높은 심각도 알림만 조회
curl "http://localhost:8081/api/alerts?severity=HIGH"

# 긴급 심각도 알림만 조회
curl "http://localhost:8081/api/alerts?severity=CRITICAL"
```

**확인 사항**:
- ✅ 선택한 심각도의 알림만 표시됨
- ✅ "전체" 선택 시 모든 알림 표시

### 5.3. 심각도별 정렬

**브라우저에서**:
1. 알림 목록 상단의 **정렬 버튼** 클릭
2. **"심각도순"** 선택
3. 알림 목록이 긴급 → 높음 → 보통 → 낮음 순서로 정렬됨 확인

**REST API로 직접 테스트**:
```bash
# 심각도순 정렬
curl "http://localhost:8081/api/alerts?sortBy=severity"

# 결과 확인 (CRITICAL → HIGH → MEDIUM → LOW 순서)
curl "http://localhost:8081/api/alerts?sortBy=severity" | jq '.[].severity'
```

**확인 사항**:
- ✅ 알림이 심각도 순서로 정렬됨
- ✅ 긴급 알림이 목록 상단에 표시됨

---

## Step 6: 필터 조합

### 6.1. 복합 필터 테스트

**브라우저에서**:
1. 필터 패널에서 다음 조합 선택:
   - 상태: "미확인"
   - 심각도: "높음"
2. 미확인 + 높은 심각도 알림만 표시됨 확인

**REST API로 직접 테스트**:
```bash
# 미확인 + 높은 심각도
curl "http://localhost:8081/api/alerts?status=UNREAD&severity=HIGH"

# 확인중 + 담당자 "김보안"
curl "http://localhost:8081/api/alerts?status=IN_PROGRESS&assignedTo=김보안"

# 완료 + 심각도순 정렬
curl "http://localhost:8081/api/alerts?status=COMPLETED&sortBy=severity"
```

**확인 사항**:
- ✅ 여러 필터를 동시에 적용 가능
- ✅ 필터링 결과가 즉시 표시됨
- ✅ 필터 초기화 버튼으로 모든 필터 해제 가능

---

## Step 7: 실시간 동기화 테스트

### 7.1. 다중 브라우저 테스트

**시나리오**:
1. 두 개의 브라우저 탭 열기 (또는 다른 브라우저)
2. 탭 1에서 알림 상태를 "확인중"으로 변경
3. 탭 2에서 1초 이내에 상태가 "확인중"으로 자동 업데이트됨 확인

**확인 사항**:
- ✅ 상태 변경이 모든 클라이언트에 실시간 동기화됨
- ✅ 담당자 할당도 실시간 동기화됨
- ✅ 조치 내용도 실시간 동기화됨
- ✅ 신규 알림도 모든 클라이언트에 실시간 표시됨

### 7.2. WebSocket 연결 확인

**브라우저 개발자 도구에서**:
1. F12 키로 개발자 도구 열기
2. **Network** 탭 → **WS** (WebSocket) 필터 선택
3. `ws://localhost:8082/ws/alerts` 연결 확인
4. **Messages** 탭에서 실시간 이벤트 수신 확인:
   - `NEW_ALERT`: 신규 알림
   - `ALERT_STATUS_CHANGED`: 상태 변경

**확인 사항**:
- ✅ WebSocket 연결 상태: "open"
- ✅ 이벤트 실시간 수신
- ✅ 연결 끊김 시 자동 재연결 (exponential backoff)

---

## Step 8: 성능 확인

### 8.1. 응답 시간 측정

**브라우저에서**:
1. 알림 상태 변경 시 반영 시간 확인 (목표: <1초)
2. 필터 적용 시 응답 시간 확인 (목표: <100ms)
3. 알림 상세 모달 로딩 시간 확인 (목표: <200ms)

**REST API 성능 테스트**:
```bash
# 상태 변경 응답 시간 측정
time curl -X PATCH http://localhost:8081/api/alerts/{alertId}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'

# 필터링 응답 시간 측정 (100개 알림 기준)
time curl "http://localhost:8081/api/alerts?status=UNREAD&severity=HIGH"
```

**확인 사항**:
- ✅ 상태 변경 평균 <0.5초, 최대 <1초
- ✅ 필터링 응답 <100ms
- ✅ 모달 로딩 <200ms
- ✅ WebSocket 브로드캐스트 <1초

---

## Troubleshooting

### 문제 1: 알림이 표시되지 않음

**원인**:
- transaction-generator가 아직 거래를 생성하지 않음
- fraud-detector가 아직 규칙을 실행하지 않음

**해결 방법**:
```bash
# 1. 서비스 상태 확인
docker-compose ps

# 2. 로그 확인
docker-compose logs fraud-detector
docker-compose logs alert-service

# 3. 수동으로 거래 생성 (transaction-generator 재시작)
docker-compose restart transaction-generator
```

### 문제 2: WebSocket 연결 실패

**원인**:
- websocket-gateway가 시작되지 않음
- 방화벽이 8082 포트 차단

**해결 방법**:
```bash
# 1. websocket-gateway 상태 확인
curl http://localhost:8082/actuator/health

# 2. 포트 확인
netstat -an | grep 8082

# 3. websocket-gateway 재시작
docker-compose restart websocket-gateway
```

### 문제 3: 상태 변경이 동기화되지 않음

**원인**:
- Kafka 연결 오류
- WebSocket 연결 끊김

**해결 방법**:
```bash
# 1. Kafka 상태 확인
docker-compose logs kafka

# 2. alert-service 로그 확인 (Kafka 이벤트 발행 확인)
docker-compose logs alert-service | grep "alert-status-changed"

# 3. websocket-gateway 로그 확인 (브로드캐스트 확인)
docker-compose logs websocket-gateway | grep "broadcast"

# 4. 브라우저에서 WebSocket 재연결 확인
# F12 → Network → WS → 연결 상태 확인
```

---

## Next Steps

### 추가 기능 체험

1. **대량 알림 테스트**:
   - transaction-generator 설정 변경으로 초당 거래 수 증가
   - 100개 알림 제한 동작 확인 (FIFO)

2. **서버 재시작 테스트**:
   - alert-service 재시작 후 알림 상태 초기화 확인 (인메모리)
   - WebSocket 자동 재연결 확인

3. **성능 스트레스 테스트**:
   - 동시 다중 브라우저 접속 (10개 이상)
   - 동시 상태 변경 요청 (50 req/s)

### 향후 기능 (로드맵)

- **003-alert-history**: 알림 이력 영구 저장 (PostgreSQL)
- **004-dashboard-realtime**: 실시간 통계 대시보드
- **005-alert-analytics**: 알림 분석 및 리포트
- **006-dynamic-rules**: 동적 탐지 규칙 관리

---

## API Reference

### REST API

- `GET /api/alerts?status=&assignedTo=&severity=&sortBy=` - 알림 목록 조회 (필터링)
- `GET /api/alerts/{alertId}` - 알림 상세 조회
- `PATCH /api/alerts/{alertId}/status` - 알림 상태 변경
- `PATCH /api/alerts/{alertId}/assign` - 담당자 할당
- `POST /api/alerts/{alertId}/action` - 조치 내용 기록

**자세한 API 명세**: [contracts/rest-api.md](./contracts/rest-api.md)

### WebSocket Events

- `NEW_ALERT` - 신규 알림 발생
- `ALERT_STATUS_CHANGED` - 알림 상태 변경

**자세한 이벤트 스키마**: [contracts/websocket-api.md](./contracts/websocket-api.md)

---

## Notes

- 모든 알림은 인메모리 저장소에 저장 (최대 100개, FIFO)
- 시스템 재시작 시 알림 상태 초기화
- 영구 저장은 003-alert-history 기능에서 구현 예정
- MVP에서는 인증 생략 (localhost 전용)

---

**문서 상태**: ✅ 완료
**최종 업데이트**: 2025-11-11
**피드백**: 이슈 등록 또는 CLAUDE.md 참조
