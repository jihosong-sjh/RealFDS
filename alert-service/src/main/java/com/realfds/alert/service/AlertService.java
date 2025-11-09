package com.realfds.alert.service;

import com.realfds.alert.model.Alert;
import com.realfds.alert.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AlertService - 알림 비즈니스 로직 처리
 *
 * 알림 저장 및 조회를 담당하는 서비스 계층입니다.
 * - AlertConsumer에서 Kafka로부터 수신한 알림을 처리
 * - AlertController에서 REST API 요청 시 알림 조회
 * - AlertRepository를 통한 데이터 접근
 *
 * 비즈니스 로직:
 * - 알림 검증 (null 체크)
 * - 알림 저장 로깅
 * - 최근 알림 조회
 */
@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;

    /**
     * 생성자 기반 의존성 주입
     *
     * @param alertRepository 알림 저장소
     */
    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * 알림 처리
     *
     * Kafka로부터 수신한 알림을 검증하고 저장합니다.
     *
     * @param alert 처리할 알림
     */
    public void processAlert(Alert alert) {
        if (alert == null) {
            logger.warn("알림 처리 실패: null 알림");
            return;
        }

        logger.info("알림 처리 시작 - alertId={}, ruleName={}, severity={}",
            alert.getAlertId(), alert.getRuleName(), alert.getSeverity());

        // 알림 저장
        alertRepository.addAlert(alert);

        logger.info("알림 처리 완료 - alertId={}, 저장소 크기={}",
            alert.getAlertId(), alertRepository.size());
    }

    /**
     * 최근 알림 조회
     *
     * REST API 요청 시 최근 알림을 조회하여 반환합니다.
     *
     * @param limit 조회할 알림 개수
     * @return 최근 알림 리스트 (최신순)
     */
    public List<Alert> getRecentAlerts(int limit) {
        if (limit <= 0) {
            logger.warn("잘못된 limit 값: {}, 빈 리스트 반환", limit);
            return List.of();
        }

        logger.debug("최근 알림 조회 요청 - limit={}", limit);

        List<Alert> alerts = alertRepository.getRecentAlerts(limit);

        logger.debug("최근 알림 조회 완료 - 반환 개수={}", alerts.size());

        return alerts;
    }
}
