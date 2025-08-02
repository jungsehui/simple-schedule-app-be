package com.example.simplescheduleapp.fcm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class FcmConfig {

    @Value("${fcm.key.json}")
    private String FCM_KEY_JSON;

    @Value("${fcm.key.url}")
    private String FCM_KEY_URL;

    @Bean
    public FirebaseApp firebaseApp() {
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(
                            GoogleCredentials
                                    .fromStream(new ClassPathResource(FCM_KEY_JSON).getInputStream())
                                    .createScoped(List.of(FCM_KEY_URL)))
                    .build();
            log.info("Firebase application has been initialized");
            return FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
