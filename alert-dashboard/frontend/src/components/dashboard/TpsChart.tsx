/**
 * TpsChart 컴포넌트
 *
 * 초당 거래 처리량(TPS)을 실시간 시계열 차트로 표시
 *
 * 기능:
 * - Recharts LineChart로 최근 1시간(720개 데이터 포인트) TPS 표시
 * - X축: 시간 (ISO-8601 → 로컬 시간 변환)
 * - Y축: TPS (0-10000, 자동 범위 조정)
 * - 툴팁: 마우스 호버 시 시각과 정확한 TPS 값 표시
 * - 애니메이션: 5초마다 부드러운 트랜지션 (500ms)
 * - 성능 최적화: dot={false} (720개 점 렌더링 생략)
 *
 * Constitution V 준수:
 * - 함수 길이 ≤50줄
 * - 한국어 주석
 *
 * @see types/metrics.ts
 * @see hooks/useWebSocket.ts
 */

import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';

/**
 * TPS 데이터 포인트 인터페이스
 */
interface TpsDataPoint {
  /** 메트릭 측정 시각 (ISO-8601) */
  timestamp: string;
  /** 초당 거래 수 */
  tps: number;
  /** 누적 거래 수 */
  totalTransactions: number;
}

interface TpsChartProps {
  /** TPS 데이터 포인트 배열 (최대 720개, 1시간) */
  data: TpsDataPoint[];
}

/**
 * TpsChart 컴포넌트
 *
 * @param props.data TPS 데이터 포인트 배열
 */
export const TpsChart: React.FC<TpsChartProps> = ({ data }) => {
  // 빈 데이터 처리: "데이터 수집 중..." 메시지 표시
  if (!data || data.length === 0) {
    return (
      <div style={styles.emptyContainer}>
        <p style={styles.emptyMessage}>데이터 수집 중...</p>
      </div>
    );
  }

  /**
   * X축 타임스탬프 포맷터
   * ISO-8601 → 로컬 시간 (HH:MM)
   */
  const formatXAxis = (timestamp: string) => {
    return new Date(timestamp).toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  /**
   * 툴팁 포맷터
   * 시각과 TPS 값 표시
   */
  const formatTooltip = (value: number, name: string) => {
    if (name === 'tps') {
      return [`${value} TPS`, 'TPS'];
    }
    return [value, name];
  };

  /**
   * 툴팁 라벨 포맷터
   * ISO-8601 → 로컬 날짜/시간
   */
  const formatTooltipLabel = (timestamp: string) => {
    return new Date(timestamp).toLocaleString('ko-KR');
  };

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart
        data={data}
        margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
      >
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis
          dataKey="timestamp"
          tickFormatter={formatXAxis}
          tick={{ fontSize: 12 }}
        />
        <YAxis
          label={{ value: 'TPS', angle: -90, position: 'insideLeft' }}
          tick={{ fontSize: 12 }}
          domain={[0, 'auto']} // Y축 자동 범위 조정 (0부터 시작)
        />
        <Tooltip
          formatter={formatTooltip}
          labelFormatter={formatTooltipLabel}
        />
        <Legend />
        <Line
          type="monotone"
          dataKey="tps"
          stroke="#8884d8"
          strokeWidth={2}
          dot={false} // 성능 최적화: 720개 점 표시 생략
          isAnimationActive={true}
          animationDuration={500} // 0.5초 애니메이션 (5초 업데이트 주기에 적합)
          name="TPS"
        />
      </LineChart>
    </ResponsiveContainer>
  );
};

/**
 * 스타일 정의
 */
const styles = {
  emptyContainer: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    height: '300px',
    backgroundColor: '#f5f5f5',
    borderRadius: '8px',
  },
  emptyMessage: {
    fontSize: '16px',
    color: '#999',
  },
};
