package com.example.simplescheduleapp.fcm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

// 테스트 프로파일에서는 실제 Firebase 초기화를 하지 않는다.
// (테스트는 FirebaseMessaging을 @MockitoBean으로 주입하므로 실제 서비스 계정 JSON이 불필요 → CI에서도 동작)
@Profile("!test")
@Slf4j
@RequiredArgsConstructor
@Configuration
public class FcmConfig {

    private final ResourceLoader resourceLoader;

    @Value("${fcm.key.json}")
    private String FCM_KEY_JSON;

    @Value("${fcm.key.url}")
    private String FCM_KEY_URL;

    @Bean
    public FirebaseApp firebaseApp() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.getInstance();
            }

            try (InputStream credentialStream = loadCredentialResource().getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(
                                GoogleCredentials
                                        .fromStream(credentialStream)
                                        .createScoped(List.of(FCM_KEY_URL)))
                        .build();
                log.info("Firebase application has been initialized from {}", FCM_KEY_JSON);
                return FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            // 원인 스택을 보존해서 재던진다 (기존 코드는 메시지만 남기고 원인을 버렸음)
            throw new IllegalStateException("Failed to initialize FirebaseApp from " + FCM_KEY_JSON, e);
        }
    }

    /**
     * FCM 서비스 계정 키 위치를 해석한다.
     * 접두사가 있으면(classpath:/file: 등) 그대로, 절대경로면 file:, 그 외에는 classpath:로 간주한다.
     * → 로컬(클래스패스 JSON)과 컨테이너(마운트된 절대경로) 모두 코드 변경 없이 동작한다.
     */
    private Resource loadCredentialResource() {
        String location = FCM_KEY_JSON;
        if (!location.contains(":")) {
            location = location.startsWith("/")
                    ? ResourceUtils.FILE_URL_PREFIX + location
                    : ResourceUtils.CLASSPATH_URL_PREFIX + location;
        }
        return resourceLoader.getResource(location);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
