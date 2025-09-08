package com.example.simplescheduleapp.notification.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("client")
public record ClientProperties(
        String courseServerInternalUrl
) {
}
