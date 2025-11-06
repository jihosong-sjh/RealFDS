# Frontend Dashboard (실시간 대시보드)

## 목적
금융 거래 알림을 실시간으로 표시하는 React + TypeScript 기반 웹 대시보드입니다.

## 주요 책임
- WebSocket을 통한 실시간 알림 수신
- 알림 목록 표시 (최근 100개)
- 연결 상태 표시 (연결됨/끊김)
- 알림 심각도별 색상 표시 (HIGH: 빨강, MEDIUM: 노랑, LOW: 파랑)

## 기술 스택
- React 18+
- TypeScript 5+
- Vite 5+ (빌드 도구)
- WebSocket API

## 입력/출력
- **입력**: WebSocket 연결 (`ws://localhost:8082/ws/alerts`)
- **출력**: 웹 UI (`http://localhost:8083`)

## 환경 변수
- `VITE_WEBSOCKET_URL`: WebSocket 서버 URL (기본: ws://localhost:8082/ws/alerts)

## UI 컴포넌트
- **Header**: 제목 표시
- **ConnectionStatus**: 연결 상태 표시
- **AlertList**: 알림 목록
- **AlertItem**: 개별 알림 카드

## 로컬 개발
```bash
npm install
npm run dev
```
