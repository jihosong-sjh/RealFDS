package com.realfds.alert.controller;

import com.realfds.alert.model.Alert;
import com.realfds.alert.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * AlertController - 알림 REST API
 *
 * 알림 조회를 위한 REST API 엔드포인트를 제공합니다.
 * - GET /api/alerts: 최근 100개 알림 조회
 * - Reactive 타입(Mono) 사용으로 비동기 처리
 * - websocket-gateway에서 폴링하여 실시간 알림 전달
 *
 * 엔드포인트:
 * - GET /api/alerts
 *   - 설명: 최근 100개 알림을 최신순으로 반환
 *   - 응답: Mono<List<Alert>>
 *   - Content-Type: application/json
 *
 * 환경:
 * - Spring WebFlux 기반
 * - 비동기 논블로킹 처리
 */
@RestController
@RequestMapping("/api")
public class AlertController {

    private static final Logger logger = LoggerFactory.getLogger(AlertController.class);
    private static final int DEFAULT_ALERT_LIMIT = 100;

    private final AlertService alertService;

    /**
     * 생성자 기반 의존성 주입
     *
     * @param alertService 알림 처리 서비스
     */
    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * 최근 알림 조회 API
     *
     * GET /api/alerts
     * - 최근 100개 알림을 최신순으로 반환
     * - websocket-gateway가 1초마다 폴링하여 새로운 알림 확인
     *
     * @return Mono<List<Alert>> 최근 알림 리스트 (최신순)
     */
    @GetMapping("/alerts")
    public Mono<List<Alert>> getAlerts() {
        logger.debug("알림 조회 요청 수신 - limit={}", DEFAULT_ALERT_LIMIT);

        // AlertService를 통해 최근 알림 조회
        List<Alert> alerts = alertService.getRecentAlerts(DEFAULT_ALERT_LIMIT);

        logger.debug("알림 조회 완료 - 반환 개수={}", alerts.size());

        // Reactive 타입으로 반환 (WebFlux)
        return Mono.just(alerts);
    }
}
