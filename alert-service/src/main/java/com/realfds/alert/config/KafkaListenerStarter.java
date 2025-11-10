package com.realfds.alert.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

/**
 * Kafka Listener 시작 컴포넌트
 *
 * 애플리케이션이 완전히 시작된 후 Kafka listener를 수동으로 시작합니다.
 * 이를 통해 Kafka 연결 실패 시에도 애플리케이션이 정상적으로 시작되고
 * health check 엔드포인트가 응답할 수 있도록 합니다.
 *
 * ApplicationReadyEvent를 수신하여 Kafka listener container들을 시작합니다.
 * Kafka 연결 실패 시 재시도 로직을 포함합니다.
 */
@Component
public class KafkaListenerStarter {

    private static final Logger logger = LoggerFactory.getLogger(KafkaListenerStarter.class);
    private static final int MAX_RETRY_ATTEMPTS = 10;
    private static final long RETRY_DELAY_MS = 5000; // 5초

    private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    /**
     * 생성자 기반 의존성 주입
     *
     * @param kafkaListenerEndpointRegistry Kafka listener endpoint registry
     */
    public KafkaListenerStarter(KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry) {
        this.kafkaListenerEndpointRegistry = kafkaListenerEndpointRegistry;
    }

    /**
     * 애플리케이션 준비 완료 이벤트 핸들러
     *
     * ApplicationReadyEvent 발생 시 Kafka listener를 비동기로 시작합니다.
     * 별도 스레드에서 실행하여 애플리케이션 시작을 블로킹하지 않습니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startKafkaListeners() {
        logger.info("애플리케이션이 준비되었습니다. Kafka listener 시작을 시도합니다.");

        // 별도 스레드에서 Kafka listener 시작 (non-blocking)
        new Thread(() -> {
            int attempt = 0;
            boolean started = false;

            while (attempt < MAX_RETRY_ATTEMPTS && !started) {
                attempt++;
                try {
                    logger.info("Kafka listener 시작 시도 {}/{}", attempt, MAX_RETRY_ATTEMPTS);

                    // 모든 Kafka listener container 시작
                    kafkaListenerEndpointRegistry.start();

                    logger.info("Kafka listener가 성공적으로 시작되었습니다.");
                    started = true;

                } catch (Exception e) {
                    logger.warn("Kafka listener 시작 실패 (시도 {}/{}): {}",
                            attempt, MAX_RETRY_ATTEMPTS, e.getMessage());

                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        try {
                            logger.info("{}ms 후 재시도합니다...", RETRY_DELAY_MS);
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            logger.error("재시도 대기 중 인터럽트 발생", ie);
                            break;
                        }
                    } else {
                        logger.error("Kafka listener 시작 최종 실패. 최대 재시도 횟수({})를 초과했습니다.",
                                MAX_RETRY_ATTEMPTS);
                    }
                }
            }
        }, "kafka-listener-starter").start();
    }
}
