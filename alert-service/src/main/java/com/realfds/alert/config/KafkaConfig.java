package com.realfds.alert.config;

import com.realfds.alert.model.Alert;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 설정 클래스
 *
 * Spring Kafka Consumer 설정을 정의합니다.
 * - Kafka 브로커 연결 설정
 * - Consumer Factory 설정 (JSON 역직렬화)
 * - Listener Container Factory 설정
 *
 * 환경 변수:
 * - SPRING_KAFKA_BOOTSTRAP_SERVERS: Kafka 브로커 주소 (기본: localhost:9092)
 * - SPRING_KAFKA_CONSUMER_GROUP_ID: Consumer 그룹 ID (기본: alert-service-group)
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:alert-service-group}")
    private String groupId;

    /**
     * Kafka Consumer Factory 빈 생성
     *
     * Consumer 설정을 정의합니다:
     * - KEY_DESERIALIZER: String 역직렬화
     * - VALUE_DESERIALIZER: JSON 역직렬화 (Alert 객체)
     * - GROUP_ID: Consumer 그룹 ID
     * - AUTO_OFFSET_RESET: earliest (처음부터 읽기)
     *
     * @return ConsumerFactory<String, Alert>
     */
    @Bean
    public ConsumerFactory<String, Alert> consumerFactory() {
        Map<String, Object> config = new HashMap<>();

        // Kafka 브로커 주소
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Consumer 그룹 ID
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // Key Deserializer: String
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Value Deserializer: JSON (Alert 객체)
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // 오프셋 리셋 정책: earliest (처음부터 읽기)
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // JsonDeserializer 설정 (프로그래밍 방식으로만 설정)
        JsonDeserializer<Alert> deserializer = new JsonDeserializer<>(Alert.class);
        deserializer.addTrustedPackages("com.realfds.alert.model");
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                deserializer
        );
    }

    /**
     * Kafka Listener Container Factory 빈 생성
     *
     * @KafkaListener 어노테이션에서 사용되는 Factory입니다.
     * ConsumerFactory를 사용하여 Listener Container를 생성합니다.
     *
     * autoStartup을 false로 설정하여 애플리케이션 시작 시 Kafka 연결 실패로 인한
     * 전체 애플리케이션 실패를 방지합니다. Listener는 애플리케이션이 완전히 시작된 후
     * ApplicationReadyEvent에서 수동으로 시작됩니다.
     *
     * @return ConcurrentKafkaListenerContainerFactory<String, Alert>
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Alert> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Alert> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // Kafka 연결 실패 시에도 애플리케이션이 시작될 수 있도록 auto-startup 비활성화
        factory.setAutoStartup(false);
        return factory;
    }
}
