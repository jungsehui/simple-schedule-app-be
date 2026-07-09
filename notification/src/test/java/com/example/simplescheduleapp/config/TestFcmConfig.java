package com.example.simplescheduleapp.config;

import com.google.firebase.messaging.FirebaseMessaging;
import org.mockito.Mockito;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * 테스트 프로파일 전용 FCM 설정.
 *
 * <p>운영용 {@code FcmConfig}는 {@code @Profile("!test")}로 테스트에서 로드되지 않는다
 * (실제 서비스 계정 JSON을 컨텍스트 기동 시 읽기 때문 — CI에서는 파일이 없어 실패).
 * 대신 여기서 mock {@link FirebaseMessaging} 빈을 전역 공급해, {@code FcmMessageSender}를
 * 로드하는 모든 통합 테스트 컨텍스트가 실제 Firebase 없이 기동되도록 한다.
 *
 * <p>개별 테스트가 {@code @MockitoBean FirebaseMessaging}으로 다시 덮어써도 충돌하지 않는다.
 */
@Profile("test")
@Configuration
public class TestFcmConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return Mockito.mock(FirebaseMessaging.class);
    }
}
