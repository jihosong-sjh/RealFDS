package com.realfds.alert.config;

import com.realfds.alert.model.Alert;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 설정 클래스
 *
 * Spring Kafka Consumer 및 Producer 설정을 정의합니다.
 * - Kafka 브로커 연결 설정
 * - Consumer Factory 설정 (JSON 역직렬화)
 * - Producer Factory 설정 (JSON 직렬화)
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

    /**
     * Kafka Producer Factory 빈 생성
     *
     * Producer 설정을 정의합니다:
     * - KEY_SERIALIZER: String 직렬화
     * - VALUE_SERIALIZER: JSON 직렬화 (Map 객체를 JSON으로 변환)
     * - ACKS: all (모든 replica가 확인할 때까지 대기)
     * - RETRIES: 3 (전송 실패 시 재시도 횟수)
     *
     * @return ProducerFactory<String, Map<String, Object>>
     */
    @Bean
    public ProducerFactory<String, Map<String, Object>> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        // Kafka 브로커 주소
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Key Serializer: String
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Value Serializer: JSON
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // ACK 설정: all (모든 replica 확인)
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        // 재시도 횟수
        config.put(ProducerConfig.RETRIES_CONFIG, 3);

        // 멱등성 보장 (중복 방지)
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Kafka Template 빈 생성
     *
     * Kafka 메시지 전송을 위한 Template입니다.
     * ProducerFactory를 사용하여 메시지를 전송합니다.
     *
     * @return KafkaTemplate<String, Map<String, Object>>
     */
    @Bean
    public KafkaTemplate<String, Map<String, Object>> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
