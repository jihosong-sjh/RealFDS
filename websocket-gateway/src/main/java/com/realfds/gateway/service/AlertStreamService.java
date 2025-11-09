package com.realfds.gateway.service;

import com.realfds.gateway.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Alert 스트림 서비스
 *
 * alert-service를 주기적으로 폴링하여 새로운 알림을 가져와
 * WebSocket 클라이언트에게 브로드캐스트합니다.
 *
 * 주요 기능:
 * - 1초마다 alert-service API 폴링
 * - 새로운 알림만 필터링 (중복 방지)
 * - BroadcastService를 통한 알림 전송
 */
@Service
public class AlertStreamService {

    private static final Logger logger = LoggerFactory.getLogger(AlertStreamService.class);

    private final WebClient webClient;
    private final BroadcastService broadcastService;

    // 이미 처리한 알림 ID를 추적 (중복 방지)
    private final Set<String> processedAlertIds = new HashSet<>();

    public AlertStreamService(WebClient webClient, BroadcastService broadcastService) {
        this.webClient = webClient;
        this.broadcastService = broadcastService;
    }

    /**
     * 1초마다 alert-service를 폴링하여 새로운 알림을 가져옵니다.
     *
     * @Scheduled의 fixedDelay는 이전 실행이 완료된 후 지연 시간을 의미합니다.
     * 1000ms = 1초
     */
    @Scheduled(fixedDelay = 1000)
    public void pollAlerts() {
        logger.debug("alert-service 폴링 시작");

        webClient.get()
                .uri("/api/alerts")
                .retrieve()
                .bodyToFlux(Alert.class)
                .onErrorResume(error -> {
                    logger.warn("alert-service 호출 실패: {}", error.getMessage());
                    return Flux.empty();  // 에러 발생 시 빈 Flux 반환
                })
                .collectList()
                .subscribe(alerts -> processNewAlerts(alerts));
    }

    /**
     * 새로운 알림만 필터링하고 브로드캐스트합니다.
     *
     * @param alerts alert-service에서 가져온 알림 목록
     */
    private void processNewAlerts(List<Alert> alerts) {
        if (alerts.isEmpty()) {
            logger.debug("새로운 알림 없음");
            return;
        }

        int newAlertCount = 0;

        for (Alert alert : alerts) {
            // 이미 처리한 알림은 건너뛰기
            if (!processedAlertIds.contains(alert.getAlertId())) {
                // 새로운 알림 발견
                processedAlertIds.add(alert.getAlertId());
                broadcastService.broadcast(alert);
                newAlertCount++;

                logger.info("새로운 알림 감지: alertId={}, ruleName={}, severity={}",
                           alert.getAlertId(), alert.getRuleName(), alert.getSeverity());
            }
        }

        if (newAlertCount > 0) {
            logger.info("새로운 알림 {} 건 브로드캐스트 완료", newAlertCount);
        }

        // 메모리 관리: 처리한 알림 ID가 1000개를 초과하면 오래된 것부터 제거
        if (processedAlertIds.size() > 1000) {
            logger.info("처리한 알림 ID {} 개 중 500개 제거 (메모리 관리)", processedAlertIds.size());
            // 간단하게 전체 초기화 (실제 프로덕션에서는 LRU 캐시 사용 권장)
            processedAlertIds.clear();
        }
    }
}
