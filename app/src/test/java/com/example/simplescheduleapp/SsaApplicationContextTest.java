package com.example.simplescheduleapp;

import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 단일 JVM 통합 컨텍스트 부팅 테스트 (ADR-0003 Stage 2).
 * <p>
 * course + notification + common을 하나의 Spring 컨텍스트에 조합했을 때만 드러나는 문제를
 * 잡는 유일한 테스트: (1) 두 모듈 @Bean 메서드명 충돌, (2) {@code EnrolledStudentsPort}가
 * 단일 in-process 어댑터로 유일 배선(NoUniqueBean 없음), (3) 두 컨텍스트 엔티티가 단일
 * DataSource에 매핑, (4) Kafka 프로듀서(course)+컨슈머(notification)가 한 컨텍스트 공존.
 * <p>
 * <b>app에 ArchUnit 규칙이 없는 것은 누락이 아니라 판단이다.</b> app의 프로덕션 코드는 부팅 클래스
 * {@link SsaApplication}과 인프로세스 어댑터 {@code InProcessEnrolledStudentsAdapter} 둘뿐이다.
 * 레이어 규칙을 걸면 둘 다(각각 루트 패키지와 {@code ..integration..}) 제외 목록에 들어가
 * 검사 대상이 0개인 빈 규칙이 된다. 게다가 app은 합성 루트라
 * {@code @AnalyzeClasses(packages = "com.example.simplescheduleapp")}가 {@code DoNotIncludeJars}로
 * course/notification을 걷어내면 공허하고, 걷어내지 않으면 두 모듈이 이미 소유한 규칙의 기준선을
 * 중복으로 만든다. 어느 쪽이든 규칙이 없는 편보다 나쁘다. 같은 이유로 app에는 freeze 스토어가 없어
 * {@code FreezeStoreIntegrityTest}(스토어 고아 검사)도 두지 않는다.
 */
@SpringBootTest
// Boot 4 KRaft EmbeddedKafka는 랜덤 포트로 기동 — 앱의 bootstrap-servers를 그 브로커로 덮어쓴다. (ADR-0003 Stage 3)
@EmbeddedKafka(partitions = 1)
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
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
