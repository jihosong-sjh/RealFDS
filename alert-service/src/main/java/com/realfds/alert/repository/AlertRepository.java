package com.realfds.alert.repository;

import com.realfds.alert.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * AlertRepository - 인메모리 알림 저장소
 *
 * Thread-safe한 인메모리 저장소로 최근 알림을 관리합니다.
 * - ConcurrentLinkedDeque를 사용하여 동시성 문제 해결
 * - 최신 알림이 맨 앞에 위치 (addFirst 사용)
 * - 최대 100개 알림만 유지 (101개 이상 시 가장 오래된 알림 제거)
 * - 실시간 조회를 위한 getRecentAlerts() 메서드 제공
 *
 * 주의사항:
 * - 인메모리 저장소이므로 서비스 재시작 시 데이터 손실
 * - 프로덕션 환경에서는 영구 저장소(DB, Redis 등) 고려 필요
 */
@Repository
public class AlertRepository {

    private static final Logger logger = LoggerFactory.getLogger(AlertRepository.class);
    private static final int MAX_ALERTS = 100;

    /**
     * 알림 저장소 (Thread-safe)
     * - ConcurrentLinkedDeque: 양방향 큐로 앞뒤 삽입/삭제가 O(1)
     * - addFirst: 최신 알림을 맨 앞에 추가
     * - removeLast: 가장 오래된 알림을 제거
     */
    private final ConcurrentLinkedDeque<Alert> alerts = new ConcurrentLinkedDeque<>();

    /**
     * 알림 추가
     *
     * 최신 알림을 맨 앞에 추가하고, 101개 이상 시 가장 오래된 알림을 제거합니다.
     *
     * @param alert 추가할 알림
     */
    public void addAlert(Alert alert) {
        if (alert == null) {
            logger.warn("알림 추가 실패: null 알림");
            return;
        }

        // 최신 알림을 맨 앞에 추가
        alerts.addFirst(alert);

        // 최대 100개 제한: 101개 이상 시 가장 오래된 알림 제거
        if (alerts.size() > MAX_ALERTS) {
            Alert removed = alerts.removeLast();
            logger.debug("최대 용량 초과: 가장 오래된 알림 제거 (alertId={})",
                removed != null ? removed.getAlertId() : "unknown");
        }

        logger.debug("알림 추가 완료 (alertId={}, 현재 저장소 크기={})",
            alert.getAlertId(), alerts.size());
    }

    /**
     * 최근 알림 조회
     *
     * 최신 알림부터 limit 개수만큼 반환합니다.
     *
     * @param limit 조회할 알림 개수
     * @return 최근 알림 리스트 (최신순)
     */
    public List<Alert> getRecentAlerts(int limit) {
        if (limit <= 0) {
            logger.warn("잘못된 limit 값: {}, 빈 리스트 반환", limit);
            return new ArrayList<>();
        }

        List<Alert> result = new ArrayList<>();
        int count = 0;

        // ConcurrentLinkedDeque는 iterator가 weakly consistent하므로 안전
        for (Alert alert : alerts) {
            if (count >= limit) {
                break;
            }
            result.add(alert);
            count++;
        }

        logger.debug("최근 알림 조회 완료 (요청={}, 반환={}, 전체={})",
            limit, result.size(), alerts.size());

        return result;
    }

    /**
     * 저장소 크기 조회
     *
     * @return 현재 저장된 알림 개수
     */
    public int size() {
        return alerts.size();
    }

    /**
     * 저장소 비우기 (테스트용)
     */
    public void clear() {
        alerts.clear();
        logger.debug("저장소 초기화 완료");
    }

    /**
     * 알림 ID로 조회
     *
     * @param alertId 조회할 알림 ID
     * @return 알림 객체 (없으면 null)
     */
    public Alert findByAlertId(String alertId) {
        if (alertId == null || alertId.isEmpty()) {
            logger.warn("잘못된 alertId: {}", alertId);
            return null;
        }

        for (Alert alert : alerts) {
            if (alertId.equals(alert.getAlertId())) {
                logger.debug("알림 조회 성공 - alertId={}", alertId);
                return alert;
            }
        }

        logger.debug("알림을 찾을 수 없음 - alertId={}", alertId);
        return null;
    }

    /**
     * 알림 상태 업데이트
     *
     * @param alertId 업데이트할 알림 ID
     * @param status 새로운 상태 (UNREAD, IN_PROGRESS, COMPLETED)
     * @return 업데이트 성공 여부
     */
    public boolean updateStatus(String alertId, com.realfds.alert.model.AlertStatus status) {
        if (alertId == null || status == null) {
            logger.warn("상태 업데이트 실패: alertId={}, status={}", alertId, status);
            return false;
        }

        Alert alert = findByAlertId(alertId);
        if (alert == null) {
            logger.warn("상태 업데이트 실패: 알림을 찾을 수 없음 - alertId={}", alertId);
            return false;
        }

        alert.setStatus(status);
        logger.debug("상태 업데이트 완료 - alertId={}, status={}", alertId, status);
        return true;
    }

    /**
     * 처리 완료 시각 업데이트
     *
     * @param alertId 업데이트할 알림 ID
     * @param processedAt 처리 완료 시각
     * @return 업데이트 성공 여부
     */
    public boolean updateProcessedAt(String alertId, java.time.Instant processedAt) {
        if (alertId == null || processedAt == null) {
            logger.warn("처리 완료 시각 업데이트 실패: alertId={}, processedAt={}", alertId, processedAt);
            return false;
        }

        Alert alert = findByAlertId(alertId);
        if (alert == null) {
            logger.warn("처리 완료 시각 업데이트 실패: 알림을 찾을 수 없음 - alertId={}", alertId);
            return false;
        }

        alert.setProcessedAt(processedAt);
        logger.debug("처리 완료 시각 업데이트 완료 - alertId={}, processedAt={}", alertId, processedAt);
        return true;
    }
}
