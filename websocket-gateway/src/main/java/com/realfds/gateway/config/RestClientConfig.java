package com.realfds.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * REST 클라이언트 설정 클래스
 *
 * alert-service와 통신하기 위한 WebClient를 설정합니다.
 *
 * 주요 설정:
 * - Base URL: alert-service의 URL (환경 변수로 설정 가능)
 * - Reactive 방식의 HTTP 클라이언트
 */
@Configuration
public class RestClientConfig {

    @Value("${alert-service.url:http://alert-service:8081}")
    private String alertServiceUrl;

    /**
     * alert-service 호출용 WebClient 빈
     *
     * WebClient는 Spring WebFlux의 Reactive HTTP 클라이언트입니다.
     * alert-service의 REST API를 호출하여 최근 알림을 조회합니다.
     *
     * @return alert-service 호출용 WebClient
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(alertServiceUrl)
                .build();
    }
}
