/**
 * AlertMetricsCard 컴포넌트
 *
 * 현재 알림 발생률, 평균, 최대값 숫자 표시
 *
 * 기능:
 * - 현재 알림 발생률: 가장 최근 데이터 포인트의 분당 알림 수
 * - 평균 알림 발생률: 1시간(720개) 데이터 포인트의 평균
 * - 최대 알림 발생률: 1시간 동안 최대값
 * - 애니메이션: 숫자 업데이트 시 부드러운 페이드 효과
 * - 경고 표시: 현재 알림률이 평균의 2배 초과 시 빨간색
 *
 * Constitution V 준수:
 * - 함수 길이 ≤50줄
 * - 한국어 주석
 *
 * @see AlertRateChart
 * @see types/metrics.ts
 */

import React, { useState, useEffect } from 'react';

/**
 * 알림 발생률 집계 데이터 인터페이스
 */
interface AlertMetrics {
  /** 현재 분당 알림 수 */
  current: number;
  /** 1시간 평균 분당 알림 수 */
  average: number;
  /** 1시간 최대 분당 알림 수 */
  max: number;
  /** 경고 상태 여부 (current > average * 2) */
  isWarning?: boolean;
}

interface AlertMetricsCardProps {
  /** 알림 발생률 집계 데이터 */
  metrics: AlertMetrics;
}

/**
 * AlertMetricsCard 컴포넌트
 *
 * @param props.metrics 알림 발생률 집계 데이터
 */
export const AlertMetricsCard: React.FC<AlertMetricsCardProps> = ({ metrics }) => {
  const [isAnimating, setIsAnimating] = useState(false);

  // metrics 변경 시 애니메이션 트리거
  useEffect(() => {
    setIsAnimating(true);
    const timer = setTimeout(() => setIsAnimating(false), 500); // 0.5초 애니메이션
    return () => clearTimeout(timer);
  }, [metrics.current, metrics.average, metrics.max]);

  /**
   * 경고 상태 확인
   * 현재 알림률이 평균의 2배 초과 시 true
   */
  const isWarning = metrics.isWarning || metrics.current > metrics.average * 2;

  /**
   * 현재 값 색상 결정
   * 경고 상태: 빨간색, 정상 상태: 주황색
   */
  const getCurrentValueColor = () => {
    return isWarning ? '#ff0000' : '#ff7300';
  };

  return (
    <div style={styles.card}>
      <h3 style={styles.title}>알림 발생률 메트릭</h3>

      {isWarning && (
        <div style={styles.warningIndicator}>
          ⚠️ 경고: 현재 알림률이 평균의 2배를 초과했습니다
        </div>
      )}

      <div style={styles.metricsContainer}>
        {/* 현재 알림 발생률 */}
        <div style={styles.metricItem}>
          <p style={styles.metricLabel}>현재 알림 발생률</p>
          <p
            style={{
              ...styles.metricValue,
              color: getCurrentValueColor(),
              ...(isAnimating ? styles.animating : {}),
            }}
          >
            {metrics.current.toLocaleString()}
            <span style={styles.unit}> /분</span>
          </p>
        </div>

        {/* 평균 알림 발생률 */}
        <div style={styles.metricItem}>
          <p style={styles.metricLabel}>평균 알림 발생률 (1시간)</p>
          <p style={{ ...styles.metricValue, ...(isAnimating ? styles.animating : {}) }}>
            {metrics.average.toFixed(1)}
            <span style={styles.unit}> /분</span>
          </p>
        </div>

        {/* 최대 알림 발생률 */}
        <div style={styles.metricItem}>
          <p style={styles.metricLabel}>최대 알림 발생률 (1시간)</p>
          <p style={{ ...styles.metricValue, ...(isAnimating ? styles.animating : {}) }}>
            {metrics.max.toLocaleString()}
            <span style={styles.unit}> /분</span>
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
    marginBottom: '10px',
    color: '#333',
  },
  warningIndicator: {
    backgroundColor: '#ff0000',
    color: '#fff',
    padding: '8px',
    borderRadius: '4px',
    marginBottom: '15px',
    textAlign: 'center',
    fontSize: '14px',
    fontWeight: 'bold',
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
    color: '#ff7300',
    transition: 'opacity 0.5s ease-in-out',
  },
  unit: {
    fontSize: '16px',
    fontWeight: 'normal',
    color: '#999',
  },
  animating: {
    opacity: 0.7,
  },
};
