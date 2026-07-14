package com.example.simplescheduleapp;

import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 단일 JVM 통합 컨텍스트 부팅 테스트 (ADR-0003 Stage 2).
 * <p>
 * course + notification + common을 하나의 Spring 컨텍스트에 조합했을 때만 드러나는 문제를
 * 잡는 유일한 테스트: (1) 두 모듈 @Bean 메서드명 충돌, (2) {@code EnrolledStudentsPort}가
 * 단일 in-process 어댑터로 유일 배선(NoUniqueBean 없음), (3) 두 컨텍스트 엔티티가 단일
 * DataSource에 매핑, (4) Kafka 프로듀서(course)+컨슈머(notification)가 한 컨텍스트 공존.
 */
@SpringBootTest
@EmbeddedKafka(
        brokerProperties = {"listeners=PLAINTEXT://localhost:49092"},
        ports = {49092}
)
class SsaApplicationContextTest {

    // FcmConfig(@Profile("!test"))가 test에선 FirebaseMessaging을 만들지 않으므로 모킹 (프로덕션은 무관)
    @MockitoBean
    private FirebaseMessaging firebaseMessaging;

    @Test
    @DisplayName("course+notification 단일 JVM 컨텍스트가 정상 기동한다")
    void 통합_컨텍스트가_정상_기동한다() {
        // 컨텍스트 로드 성공 자체가 검증 — 실패 시 위 4가지 중 하나가 원인.
    }
}
