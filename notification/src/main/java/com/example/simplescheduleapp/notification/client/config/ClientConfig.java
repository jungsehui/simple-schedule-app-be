package com.example.simplescheduleapp.notification.client.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ClientConfig {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final ClientProperties clientProperties;

    @Bean
    public RestClient courseServerClient() {
        String url = clientProperties.courseServerInternalUrl();
        log.info("Course server url is {}", url);
        RestClient.Builder builder = RestClient.builder().baseUrl(url);

        // 운영에서 내부 API 키가 설정되면 모든 서버 간 요청에 공유 시크릿 헤더를 실어 보낸다.
        String internalApiKey = clientProperties.internalApiKey();
        if (StringUtils.hasText(internalApiKey)) {
            builder.defaultHeader(INTERNAL_API_KEY_HEADER, internalApiKey);
        }
        return builder.build();
    }
}
