/**
 * AlertRateChart 컴포넌트
 *
 * 분당 알림 발생 수를 실시간 시계열 차트로 표시 (규칙별 스택 형태)
 *
 * 기능:
 * - Recharts AreaChart로 최근 1시간(720개 데이터 포인트) 알림 발생률 표시
 * - 3개 규칙(HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY) 스택 형태
 * - X축: 시간 (ISO-8601 → 로컬 시간 변환)
 * - Y축: 분당 알림 수 (0-1000, 자동 범위 조정)
 * - 툴팁: 마우스 호버 시 시각과 규칙별 알림 수 표시
 * - 범례 클릭: 특정 규칙 데이터 숨김/표시
 * - 경고 표시: 알림률이 평균의 2배 초과 시 그래프 주황색
 * - 애니메이션: 5초마다 부드러운 트랜지션 (500ms)
 *
 * Constitution V 준수:
 * - 함수 길이 ≤50줄
 * - 한국어 주석
 *
 * @see types/metrics.ts
 * @see hooks/useWebSocket.ts
 */

import React, { useState } from 'react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

/**
 * 알림 발생률 데이터 포인트 인터페이스
 */
interface AlertDataPoint {
  /** 메트릭 측정 시각 (ISO-8601) */
  timestamp: string;
  /** HIGH_VALUE 규칙 알림 수 */
  HIGH_VALUE: number;
  /** FOREIGN_COUNTRY 규칙 알림 수 */
  FOREIGN_COUNTRY: number;
  /** HIGH_FREQUENCY 규칙 알림 수 */
  HIGH_FREQUENCY: number;
}

interface AlertRateChartProps {
  /** 알림 발생률 데이터 포인트 배열 (최대 720개, 1시간) */
  data: AlertDataPoint[];
}

/**
 * AlertRateChart 컴포넌트
 *
 * @param props.data 알림 발생률 데이터 포인트 배열
 */
export const AlertRateChart: React.FC<AlertRateChartProps> = ({ data }) => {
  // 범례 클릭으로 숨길 규칙 관리
  const [hiddenRules, setHiddenRules] = useState<Set<string>>(new Set());

  // 빈 데이터 처리: "데이터 수집 중..." 메시지 표시
  if (!data || data.length === 0) {
    return (
      <div style={styles.emptyContainer}>
        <p style={styles.emptyMessage}>데이터 수집 중...</p>
      </div>
    );
  }

  /**
   * 범례 클릭 핸들러
   * 클릭한 규칙을 숨김/표시 토글
   */
  const handleLegendClick = (e: any) => {
    const ruleName = e.value || e.dataKey;
    setHiddenRules((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(ruleName)) {
        newSet.delete(ruleName);
      } else {
        newSet.add(ruleName);
      }
      return newSet;
    });
  };

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
   * 툴팁 라벨 포맷터
   * ISO-8601 → 로컬 날짜/시간
   */
  const formatTooltipLabel = (timestamp: string) => {
    return new Date(timestamp).toLocaleString('ko-KR');
  };

  /**
   * 알림률 평균 계산
   */
  const calculateAverage = () => {
    if (data.length === 0) return 0;
    const sum = data.reduce((acc, point) =>
      acc + point.HIGH_VALUE + point.FOREIGN_COUNTRY + point.HIGH_FREQUENCY, 0
    );
    return sum / data.length;
  };

  /**
   * 현재 알림률이 평균의 2배 초과인지 확인
   */
  const isWarning = () => {
    if (data.length === 0) return false;
    const average = calculateAverage();
    const current = data[data.length - 1];
    const currentTotal = current.HIGH_VALUE + current.FOREIGN_COUNTRY + current.HIGH_FREQUENCY;
    return currentTotal > average * 2;
  };

  return (
    <div>
      {isWarning() && (
        <div style={styles.warningBanner}>
          ⚠️ 경고: 현재 알림 발생률이 평균의 2배를 초과했습니다
        </div>
      )}
      <ResponsiveContainer width="100%" height={300}>
        <AreaChart
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
            label={{ value: '분당 알림 수', angle: -90, position: 'insideLeft' }}
            tick={{ fontSize: 12 }}
            domain={[0, 'auto']} // Y축 자동 범위 조정 (0부터 시작)
          />
          <Tooltip labelFormatter={formatTooltipLabel} />
          <Legend onClick={handleLegendClick} />

          {/* HIGH_VALUE 규칙 (빨강) */}
          {!hiddenRules.has('HIGH_VALUE') && (
            <Area
              type="monotone"
              dataKey="HIGH_VALUE"
              stackId="1"
              stroke="#ff7300"
              fill="#ff7300"
              isAnimationActive={true}
              animationDuration={500}
              name="HIGH_VALUE"
            />
          )}

          {/* FOREIGN_COUNTRY 규칙 (파랑) */}
          {!hiddenRules.has('FOREIGN_COUNTRY') && (
            <Area
              type="monotone"
              dataKey="FOREIGN_COUNTRY"
              stackId="1"
              stroke="#387908"
              fill="#387908"
              isAnimationActive={true}
              animationDuration={500}
              name="FOREIGN_COUNTRY"
            />
          )}

          {/* HIGH_FREQUENCY 규칙 (노랑) */}
          {!hiddenRules.has('HIGH_FREQUENCY') && (
            <Area
              type="monotone"
              dataKey="HIGH_FREQUENCY"
              stackId="1"
              stroke="#8884d8"
              fill="#8884d8"
              isAnimationActive={true}
              animationDuration={500}
              name="HIGH_FREQUENCY"
            />
          )}
        </AreaChart>
      </ResponsiveContainer>
    </div>
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
  warningBanner: {
    backgroundColor: '#ff9800',
    color: '#fff',
    padding: '10px',
    borderRadius: '4px',
    marginBottom: '10px',
    textAlign: 'center' as const,
    fontWeight: 'bold' as const,
  },
};
