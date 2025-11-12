/**
 * TpsMetricsCard 컴포넌트
 *
 * 현재 TPS, 평균 TPS, 최대 TPS 숫자 표시
 *
 * 기능:
 * - 현재 TPS: 가장 최근 데이터 포인트의 TPS
 * - 평균 TPS: 1시간(720개) 데이터 포인트의 평균
 * - 최대 TPS: 1시간 동안 최대값
 * - 애니메이션: 숫자 업데이트 시 부드러운 페이드 효과
 *
 * Constitution V 준수:
 * - 함수 길이 ≤50줄
 * - 한국어 주석
 *
 * @see TpsChart
 * @see types/metrics.ts
 */

import React, { useState, useEffect } from 'react';

/**
 * TPS 집계 데이터 인터페이스
 */
interface TpsMetrics {
  /** 현재 TPS */
  current: number;
  /** 1시간 평균 TPS */
  average: number;
  /** 1시간 최대 TPS */
  max: number;
}

interface TpsMetricsCardProps {
  /** TPS 집계 데이터 */
  metrics: TpsMetrics;
}

/**
 * TpsMetricsCard 컴포넌트
 *
 * @param props.metrics TPS 집계 데이터
 */
export const TpsMetricsCard: React.FC<TpsMetricsCardProps> = ({ metrics }) => {
  const [isAnimating, setIsAnimating] = useState(false);

  // metrics 변경 시 애니메이션 트리거
  useEffect(() => {
    setIsAnimating(true);
    const timer = setTimeout(() => setIsAnimating(false), 500); // 0.5초 애니메이션
    return () => clearTimeout(timer);
  }, [metrics.current, metrics.average, metrics.max]);

  return (
    <div style={styles.card}>
      <h3 style={styles.title}>TPS 메트릭</h3>

      <div style={styles.metricsContainer}>
        {/* 현재 TPS */}
        <div style={styles.metricItem}>
          <p style={styles.metricLabel}>현재 TPS</p>
          <p style={{ ...styles.metricValue, ...(isAnimating ? styles.animating : {}) }}>
            {metrics.current.toLocaleString()}
          </p>
        </div>

        {/* 평균 TPS */}
        <div style={styles.metricItem}>
          <p style={styles.metricLabel}>평균 TPS (1시간)</p>
          <p style={{ ...styles.metricValue, ...(isAnimating ? styles.animating : {}) }}>
            {metrics.average.toFixed(1)}
          </p>
        </div>

        {/* 최대 TPS */}
        <div style={styles.metricItem}>
          <p style={styles.metricLabel}>최대 TPS (1시간)</p>
          <p style={{ ...styles.metricValue, ...(isAnimating ? styles.animating : {}) }}>
            {metrics.max.toLocaleString()}
          </p>
        </div>
      </div>
    </div>
  );
};

/**
 * 스타일 정의
 */
const styles: { [key: string]: React.CSSProperties } = {
  card: {
    backgroundColor: '#ffffff',
    borderRadius: '8px',
    padding: '20px',
    boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
    marginBottom: '20px',
  },
  title: {
    fontSize: '18px',
    fontWeight: 'bold',
    marginBottom: '20px',
    color: '#333',
  },
  metricsContainer: {
    display: 'flex',
    justifyContent: 'space-around',
    gap: '20px',
  },
  metricItem: {
    flex: 1,
    textAlign: 'center',
    padding: '10px',
    backgroundColor: '#f9f9f9',
    borderRadius: '8px',
  },
  metricLabel: {
    fontSize: '12px',
    color: '#666',
    marginBottom: '8px',
  },
  metricValue: {
    fontSize: '28px',
    fontWeight: 'bold',
    color: '#8884d8',
    transition: 'opacity 0.5s ease-in-out',
  },
  animating: {
    opacity: 0.7,
  },
};
