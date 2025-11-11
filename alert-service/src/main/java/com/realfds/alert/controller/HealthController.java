package com.realfds.alert.controller;

import com.realfds.alert.model.AlertStatus;
import com.realfds.alert.model.Severity;
import com.realfds.alert.repository.AlertRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 *
 * 서비스의 헬스 체크를 담당하는 컨트롤러입니다.
 * Spring Boot Actuator를 사용하지 않고 직접 구현하여 학습 목적으로 사용합니다.
 *
 * 엔드포인트:
 * - GET /actuator/health: 서비스 상태 확인 및 알림 통계 포함
 *
 * 헬스 체크 응답 형식:
 * {
 *   "status": "UP",
 *   "alerts": {
 *     "total": 10,
 *     "byStatus": {
 *       "UNREAD": 5,
 *       "IN_PROGRESS": 3,
 *       "COMPLETED": 2
 *     },
 *     "bySeverity": {
 *       "LOW": 2,
 *       "MEDIUM": 3,
 *       "HIGH": 4,
 *       "CRITICAL": 1
 *     }
 *   }
 * }
 */
@RestController
@RequestMapping("/actuator")
public class HealthController {

    private final AlertRepository alertRepository;

    /**
     * 생성자 기반 의존성 주입
     *
     * @param alertRepository 알림 저장소
     */
    public HealthController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * Health check 엔드포인트
     *
     * 서비스가 정상 실행 중임을 확인하고, 알림 관리 통계를 포함한 헬스 체크 정보를 반환합니다.
     * Docker 헬스 체크나 로드 밸런서의 상태 확인에 사용할 수 있습니다.
     *
     * 통계 정보:
     * - 총 알림 개수
     * - 상태별 알림 개수 (UNREAD, IN_PROGRESS, COMPLETED)
     * - 심각도별 알림 개수 (LOW, MEDIUM, HIGH, CRITICAL)
     *
     * @return Mono<Map<String, Object>> - 헬스 체크 및 통계 정보
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> health() {
        // 전체 알림 조회
        var alerts = alertRepository.getRecentAlerts(100);

        // 상태별 통계 계산
        Map<String, Long> statusCounts = new HashMap<>();
        for (AlertStatus status : AlertStatus.values()) {
            long count = alerts.stream()
                .filter(alert -> alert.getStatus() == status)
                .count();
            statusCounts.put(status.name(), count);
        }

        // 심각도별 통계 계산
        Map<String, Long> severityCounts = new HashMap<>();
        for (Severity severity : Severity.values()) {
            long count = alerts.stream()
                .filter(alert -> severity.name().equals(alert.getSeverity()))
                .count();
            severityCounts.put(severity.name(), count);
        }

        // 알림 통계 정보
        Map<String, Object> alertStats = new HashMap<>();
        alertStats.put("total", alerts.size());
        alertStats.put("byStatus", statusCounts);
        alertStats.put("bySeverity", severityCounts);

        // 헬스 체크 응답
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("alerts", alertStats);

        return Mono.just(response);
    }
}
