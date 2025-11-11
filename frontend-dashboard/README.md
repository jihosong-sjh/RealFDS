# Frontend Dashboard (실시간 알림 대시보드)

**서비스 종류**: 웹 프론트엔드 (SPA)
**기술 스택**: React 18+, TypeScript 5+, Vite 5+
**역할**: 실시간 알림 모니터링 UI

---

## 목적

WebSocket을 통해 실시간 알림을 수신하고 사용자에게 시각적으로 표시하는 웹 대시보드입니다.

---

## 주요 책임

- **WebSocket 클라이언트**: `ws://localhost:8082/ws/alerts` 연결
- **실시간 알림 표시**: 최근 100개 알림 유지
- **연결 상태 관리**: "연결됨" / "끊김" / "연결 중" 표시
- **자동 재연결**: 연결 끊김 시 5초 후 재연결 시도

---

## UI 컴포넌트

### 기본 컴포넌트

| 컴포넌트 | 역할 |
|----------|------|
| `Header` | 제목 표시 ("실시간 FDS 알림") |
| `ConnectionStatus` | 연결 상태 표시 |
| `AlertList` | 알림 목록 스크롤 영역 |
| `AlertItem` | 개별 알림 카드 (심각도별 색상, 상태 뱃지, 담당자 표시) |

### 신규 컴포넌트 (002-alert-management)

| 컴포넌트 | 역할 |
|----------|------|
| `AlertDetailModal` | 알림 상세 정보 및 상태 관리 모달 |
| `AlertFilterPanel` | 상태/담당자/심각도별 필터링 패널 |
| `SeverityBadge` | 심각도 뱃지 (색상 및 텍스트) |

### 커스텀 Hook

| Hook | 역할 |
|------|------|
| `useWebSocket` | WebSocket 연결 및 이벤트 처리 |
| `useAlertManagement` | 알림 상태 변경, 담당자 할당, 조치 기록 API |

---

## 로컬 개발

```bash
cd frontend-dashboard

# 의존성 설치
npm install

# 개발 서버 실행 (HMR 지원)
npm run dev

# 브라우저 자동 오픈: http://localhost:8083
```

---

## 빌드 및 배포

```bash
# 프로덕션 빌드
npm run build

# 빌드 결과: dist/

# 프리뷰
npm run preview
```

---

## 테스트

```bash
# Vitest 단위 테스트
npm run test

# 커버리지 측정
npm run test:coverage
```

---

## 환경 변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `VITE_WEBSOCKET_URL` | `ws://localhost:8082/ws/alerts` | WebSocket 서버 주소 |

---

## 알림 표시 규칙

### 심각도별 색상

| 심각도 | 색상 | 아이콘 | 한국어 |
|--------|------|--------|--------|
| CRITICAL | 빨간색 | 🔴 | 긴급 |
| HIGH | 주황색 | ⚠️ | 높음 |
| MEDIUM | 노란색 | ⚡ | 보통 |
| LOW | 파란색 | ℹ️ | 낮음 |

### 상태별 뱃지

| 상태 | 색상 | 한국어 |
|------|------|--------|
| UNREAD | 회색 | 미확인 |
| IN_PROGRESS | 파란색 | 확인중 |
| COMPLETED | 초록색 | 완료 |

### 표시 정보

#### AlertItem (목록)
- 발생 시각 (예: "2025-11-06 10:30:45")
- 심각도 뱃지 (색상 + 텍스트)
- 상태 뱃지 (색상 + 텍스트)
- 담당자 (할당 시 표시, 미할당 시 "미할당")
- 거래 ID
- 사용자 ID
- 거래 금액 (예: "1,250,000원")
- 탐지 규칙 (예: "HIGH_VALUE")
- 상세 사유 (한국어)

#### AlertDetailModal (모달)
- 모든 기본 정보 + 추가 정보
- 상태 변경 버튼 (미확인 → 확인중 → 완료)
- 담당자 할당 입력 필드 (최대 100자)
- 조치 내용 입력 영역 (최대 2000자)
- 처리 완료 시각 (`processedAt`)

---

## 상태 관리 Hook 사용법

### useAlertManagement

알림 상태 변경, 담당자 할당, 조치 내용 기록 API를 제공하는 커스텀 hook입니다.

```typescript
import { useAlertManagement } from './hooks/useAlertManagement';

function AlertDetailModal({ alert, onClose }) {
  const { changeAlertStatus, assignAlert, recordAction } = useAlertManagement();

  // 상태 변경
  const handleStatusChange = async (newStatus) => {
    await changeAlertStatus(alert.alertId, newStatus);
  };

  // 담당자 할당
  const handleAssign = async (assignedTo) => {
    await assignAlert(alert.alertId, assignedTo);
  };

  // 조치 내용 기록 + 완료 처리
  const handleComplete = async (actionNote) => {
    await recordAction(alert.alertId, actionNote, true);
  };
}
```

### useWebSocket

WebSocket 연결 및 이벤트 처리를 담당하는 hook입니다.

```typescript
import { useWebSocket } from './hooks/useWebSocket';

function Dashboard() {
  const { isConnected, alerts } = useWebSocket('ws://localhost:8082/ws/alerts');

  // isConnected: 연결 상태 (boolean)
  // alerts: 알림 목록 (Alert[])

  // WebSocket 이벤트 자동 처리:
  // - NEW_ALERT: 신규 알림 추가
  // - ALERT_STATUS_CHANGED: 기존 알림 업데이트
}
```

---

## 주요 컴포넌트 설명

### AlertDetailModal

**위치**: `src/components/AlertDetailModal.tsx`

**기능**:
- 알림 상세 정보 표시
- 상태 변경 버튼 (미확인 → 확인중 → 완료)
- 담당자 할당 입력 필드
- 조치 내용 입력 영역 (최대 2000자)
- 완료 처리 버튼

**사용 예시**:
```typescript
<AlertDetailModal
  alert={selectedAlert}
  isOpen={isModalOpen}
  onClose={() => setIsModalOpen(false)}
/>
```

### AlertFilterPanel

**위치**: `src/components/AlertFilterPanel.tsx`

**기능**:
- 상태별 필터 (전체/미확인/확인중/완료)
- 담당자별 필터
- 심각도별 필터 (전체/낮음/보통/높음/긴급)

**사용 예시**:
```typescript
<AlertFilterPanel
  onFilterChange={(filters) => applyFilters(filters)}
/>
```

### SeverityBadge

**위치**: `src/components/SeverityBadge.tsx`

**기능**:
- 심각도 표시 (색상 + 텍스트)
- CRITICAL: 빨간색, HIGH: 주황색, MEDIUM: 노란색, LOW: 파란색

**사용 예시**:
```typescript
<SeverityBadge severity={alert.severity} />
```

---

## 참고 문서

- [전체 시스템 아키텍처](../docs/architecture.md)
- [데이터 모델 상세 (001-realtime-fds)](../specs/001-realtime-fds/data-model.md)
- [데이터 모델 상세 (002-alert-management)](../specs/002-alert-management/data-model.md)
- [REST API 계약](../specs/002-alert-management/contracts/rest-api.md)
- [WebSocket API 계약](../specs/002-alert-management/contracts/websocket-api.md)
- [빠른 시작 가이드](../specs/002-alert-management/quickstart.md)
- [개발 가이드](../docs/development.md)
