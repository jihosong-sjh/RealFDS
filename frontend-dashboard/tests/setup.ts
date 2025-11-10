// Vitest 테스트 환경 초기화 파일
// 모든 테스트 실행 전에 자동으로 실행됨

import '@testing-library/jest-dom';

// WebSocket Mock 설정
class WebSocketMock {
  url: string;
  onopen: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  readyState: number = WebSocket.CONNECTING;

  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;

  constructor(url: string) {
    this.url = url;
    this.readyState = WebSocket.CONNECTING;
  }

  send(data: string) {
    // Mock send method
  }

  close() {
    this.readyState = WebSocket.CLOSED;
  }
}

// 글로벌 WebSocket을 Mock으로 대체
global.WebSocket = WebSocketMock as any;
