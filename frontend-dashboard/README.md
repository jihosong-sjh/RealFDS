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

| 컴포넌트 | 역할 |
|----------|------|
| `Header` | 제목 표시 ("실시간 FDS 알림") |
| `ConnectionStatus` | 연결 상태 표시 |
| `AlertList` | 알림 목록 스크롤 영역 |
| `AlertItem` | 개별 알림 카드 (심각도별 색상) |

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

| 심각도 | 색상 | 아이콘 |
|--------|------|--------|
| HIGH | 빨간색 | ⚠️ |
| MEDIUM | 노란색 | ⚡ |
| LOW | 파란색 | ℹ️ |

### 표시 정보

- 발생 시각 (예: "2025-11-06 10:30:45")
- 거래 ID
- 사용자 ID
- 거래 금액 (예: "1,250,000원")
- 탐지 규칙 (예: "HIGH_VALUE")
- 상세 사유 (한국어)

---

## 참고 문서

- [전체 시스템 아키텍처](../docs/architecture.md)
- [데이터 모델 상세](../specs/001-realtime-fds/data-model.md)
- [개발 가이드](../docs/development.md)
