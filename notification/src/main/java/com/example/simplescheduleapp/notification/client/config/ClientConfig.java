package com.example.simplescheduleapp.notification.client.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ClientConfig {

    private final ClientProperties clientProperties;

    @Bean
    public RestClient courseServerClient() {
        String url = clientProperties.courseServerInternalUrl();
        log.info("Course server url is {}", url);
        return RestClient.builder()
                .baseUrl(url)
                .build();
    }
}
