/**
 * Jest 테스트 환경 설정
 *
 * 모든 테스트 실행 전에 로드되는 설정 파일입니다.
 */

import '@testing-library/jest-dom';

// WebSocket Mock (전역)
global.WebSocket = class WebSocket {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;

  url: string;
  readyState: number;
  onopen: ((event: Event) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;

  constructor(url: string) {
    this.url = url;
    this.readyState = WebSocket.CONNECTING;
  }

  send(data: string) {
    console.log('WebSocket.send:', data);
  }

  close() {
    this.readyState = WebSocket.CLOSED;
    if (this.onclose) {
      this.onclose(new CloseEvent('close'));
    }
  }
} as any;

// console 경고 억제 (테스트 중 불필요한 경고 제거)
global.console = {
  ...console,
  warn: jest.fn(),
  error: jest.fn(),
};
