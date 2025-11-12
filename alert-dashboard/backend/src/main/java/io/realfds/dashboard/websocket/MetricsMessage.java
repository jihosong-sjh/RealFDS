package io.realfds.dashboard.websocket;

import io.realfds.dashboard.model.MetricsDataPoint;

import java.time.Instant;
import java.util.List;

/**
 * WebSocket 메시지 DTO
 *
 * 클라이언트-서버 간 WebSocket 통신에 사용되는 메시지 포맷을 정의합니다.
 * 메시지 타입에 따라 다른 페이로드를 포함할 수 있습니다.
 */
public class MetricsMessage {

    /**
     * 메시지 타입 Enum
     */
    public enum Type {
        /**
         * 서버 → 클라이언트: 5초마다 메트릭 업데이트 전송
         */
        METRICS_UPDATE,

        /**
         * 클라이언트 → 서버: 재연결 시 누락된 데이터 요청
         */
        BACKFILL_REQUEST,

        /**
         * 서버 → 클라이언트: 누락된 메트릭 데이터 응답
         */
        BACKFILL_RESPONSE,

        /**
         * 서버 → 클라이언트: 에러 알림
         */
        ERROR
    }

    /**
     * 메시지 타입 (필수)
     */
    private Type type;

    /**
     * 메시지 생성 시각 (필수)
     */
    private Instant timestamp;

    /**
     * 메시지 페이로드 (타입에 따라 다름)
     * - METRICS_UPDATE: MetricsDataPoint
     * - BACKFILL_REQUEST: BackfillRequest
     * - BACKFILL_RESPONSE: List<MetricsDataPoint>
     * - ERROR: ErrorPayload
     */
    private Object payload;

    /**
     * 기본 생성자 (Jackson 직렬화용)
     */
    public MetricsMessage() {
    }

    /**
     * METRICS_UPDATE 메시지 생성자
     *
     * @param dataPoint 메트릭 데이터 포인트
     */
    public MetricsMessage(MetricsDataPoint dataPoint) {
        this.type = Type.METRICS_UPDATE;
        this.timestamp = Instant.now();
        this.payload = dataPoint;
    }

    /**
     * BACKFILL_RESPONSE 메시지 생성자
     *
     * @param dataPoints 누락된 메트릭 데이터 포인트 리스트
     */
    public MetricsMessage(List<MetricsDataPoint> dataPoints) {
        this.type = Type.BACKFILL_RESPONSE;
        this.timestamp = Instant.now();
        this.payload = dataPoints;
    }

    /**
     * ERROR 메시지 생성자
     *
     * @param errorCode    에러 코드
     * @param errorMessage 에러 메시지
     */
    public MetricsMessage(String errorCode, String errorMessage) {
        this.type = Type.ERROR;
        this.timestamp = Instant.now();
        this.payload = new ErrorPayload(errorCode, errorMessage);
    }

    /**
     * 전체 필드 생성자
     *
     * @param type      메시지 타입
     * @param timestamp 메시지 생성 시각
     * @param payload   메시지 페이로드
     */
    public MetricsMessage(Type type, Instant timestamp, Object payload) {
        this.type = type;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    // Getters and Setters

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    /**
     * BACKFILL_REQUEST 페이로드
     */
    public static class BackfillRequest {
        /**
         * 클라이언트가 마지막으로 수신한 메트릭의 타임스탬프
         * 이 시각 이후의 모든 데이터를 요청합니다.
         */
        private Instant lastReceivedTimestamp;

        public BackfillRequest() {
        }

        public BackfillRequest(Instant lastReceivedTimestamp) {
            this.lastReceivedTimestamp = lastReceivedTimestamp;
        }

        public Instant getLastReceivedTimestamp() {
            return lastReceivedTimestamp;
        }

        public void setLastReceivedTimestamp(Instant lastReceivedTimestamp) {
            this.lastReceivedTimestamp = lastReceivedTimestamp;
        }
    }

    /**
     * ERROR 페이로드
     */
    public static class ErrorPayload {
        /**
         * 에러 코드 (예: INVALID_MESSAGE, INTERNAL_ERROR)
         */
        private String errorCode;

        /**
         * 사용자 친화적인 에러 메시지 (한국어)
         */
        private String errorMessage;

        public ErrorPayload() {
        }

        public ErrorPayload(String errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    @Override
    public String toString() {
        return "MetricsMessage{" +
                "type=" + type +
                ", timestamp=" + timestamp +
                ", payload=" + payload +
                '}';
    }
}
