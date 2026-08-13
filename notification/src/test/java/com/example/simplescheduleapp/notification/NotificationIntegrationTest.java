package com.example.simplescheduleapp.notification;

import com.example.simplescheduleapp.NotificationApplication;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.fcm.application.FcmService;
import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.fcm.infrastructure.FcmMessageSender;
import com.example.simplescheduleapp.notification.application.NotificationDispatcher;
import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.notification.application.port.out.GetEnrolledStudentInfosResponse;
import com.example.simplescheduleapp.notification.application.port.out.EnrolledStudentsPort;
import com.example.simplescheduleapp.notification.domain.FailedNotification;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import com.example.simplescheduleapp.sse.application.SseConnectionPublisher;
import com.example.simplescheduleapp.sse.infrastructure.redis.RedisClientManager;
import com.google.api.core.ApiFutures;
import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("알림 시스템 통합 테스트")
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
// 고정 포트(49092) 대신 랜덤 포트로 브로커를 띄우고, 앱의 bootstrap-servers를 그 브로커로 덮어쓴다.
// (고정 포트는 다른 EmbeddedKafka 컨텍스트/잔존 브로커와 충돌해 CI에서 TopicExistsException 유발)
@EmbeddedKafka(topics = {KafkaTopics.COURSE_EVENT_TOPIC}, partitions = 1)
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@SpringBootTest(classes = NotificationApplication.class)
class NotificationIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // 실제 로직을 호출하면서도 특정 메서드를 Mocking하기 위해 @MockitoSpyBean 사용
    @MockitoSpyBean
    private NotificationDispatcher notificationDispatcher;

    @MockitoSpyBean
    private SseConnectionPublisher sseConnectionPublisher;

    @MockitoSpyBean
    private FcmService fcmService;

    @MockitoBean
    private FcmTokenRepository fcmTokenRepository;

    @MockitoBean
    private FailedNotificationRepository failedNotificationRepository;

    // 외부 의존성은 완전히 Mocking
    @MockitoBean
    private RedisClientManager redisClientManager;

    @MockitoBean
    private FirebaseMessaging firebaseMessaging; // FcmConfig에서 FirebaseApp 초기화를 막기 위해 Mocking

    // FCM 전송 어댑터(빈)를 mock으로 교체 — static mock과 달리 Spring DI를 타므로
    // Kafka 리스너 스레드 등 어떤 스레드에서 호출되어도 실패 주입이 적용된다.
    @MockitoBean
    private FcmMessageSender fcmMessageSender;

    private final Long LECTURE_ID = 100L;
    private final Long TARGET_MEMBER_ID = 1L;

    @MockitoBean
    private EnrolledStudentsPort courseClient;

    @BeforeEach
    void setUp() {
        reset(redisClientManager, courseClient, firebaseMessaging, sseConnectionPublisher, fcmService);
    }

    @DisplayName("Kafka 메시지 수신 시")
    @Nested
    class ConsumeAndNotify {

        @Test
        void SSE_연결_상태이면_SSE로_알림을_성공적으로_전송한다() {
            // given
            // 실제 컨슈머가 역직렬화하는 타입(KafkaLectureEventMessage)으로 발행한다.
            // courseClient mock으로 수강생을 조회하는 LectureUpdatedStrategy를 타도록 LECTURE_UPDATED 사용.
            KafkaLectureEventMessage message = KafkaLectureEventMessage.create(
                    "sse-success-uuid", LectureEventType.LECTURE_UPDATED, LECTURE_ID, null, 10L, "테스트 강의", "강의가 수정되었습니다");

            given(redisClientManager.isClientConnected(TARGET_MEMBER_ID)).willReturn(true);
            List<Long> studentIds = List.of(TARGET_MEMBER_ID);
            GetEnrolledStudentInfosResponse response = new GetEnrolledStudentInfosResponse("테스트 강의", "메모", studentIds);
            given(courseClient.getEnrolledStudentInfosByLectureId(LECTURE_ID)).willReturn(response);

            // when
            kafkaTemplate.send(KafkaTopics.COURSE_EVENT_TOPIC, message);

            // then
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                verify(sseConnectionPublisher, times(1)).publishSseNotification(any(NotificationRequest.class));
                verify(fcmService, never()).sendFcmNotification(any());
            });
        }

        @Test
        void SSE_전송_실패_시_FCM으로_대체_전송을_시도하고_성공한다() {
            // given
            KafkaLectureEventMessage message = KafkaLectureEventMessage.create(
                    "fcm-fallback-uuid", LectureEventType.LECTURE_UPDATED, LECTURE_ID, null, 10L, "테스트 강의", "강의가 수정되었습니다");

            given(redisClientManager.isClientConnected(TARGET_MEMBER_ID)).willReturn(true);
            List<Long> studentIds = List.of(TARGET_MEMBER_ID);
            GetEnrolledStudentInfosResponse response = new GetEnrolledStudentInfosResponse("테스트 강의", "메모", studentIds);
            given(courseClient.getEnrolledStudentInfosByLectureId(LECTURE_ID)).willReturn(response);

            doCallRealMethod().when(notificationDispatcher).dispatchPushNotification(any());
            doThrow(new RuntimeException("SSE Send Error")).when(sseConnectionPublisher).publishSseNotification(any());
            doNothing().when(fcmService).sendFcmNotification(any());

            // when
            kafkaTemplate.send(KafkaTopics.COURSE_EVENT_TOPIC, message);

            // then
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                verify(sseConnectionPublisher, times(1)).publishSseNotification(any());
                verify(fcmService, times(1)).sendFcmNotification(any());
            });
        }

        // 실패 주입 재설계: 스레드 로컬이라 async 경계를 넘지 못하던 static mock 대신,
        // Spring 빈인 FcmMessageSender(@MockitoBean)가 '이미 실패한 Future'를 반환하게 한다.
        // 실제 FcmService가 그 Future에 콜백을 directExecutor로 등록하는 순간 onFailure가
        // 등록 스레드(Kafka 리스너 스레드)에서 동기 실행되므로, 어떤 스레드에서 디스패치돼도 결정적이다.
        @Test
        void SSE_미연결_및_FCM_전송_실패_시_최종적으로_실패_알림을_DB에_저장한다() {
            // given
            FcmToken fcmToken = new FcmToken(TARGET_MEMBER_ID, "test-token");
            given(fcmTokenRepository.getByMemberId(TARGET_MEMBER_ID)).willReturn(fcmToken);
            KafkaLectureEventMessage message = KafkaLectureEventMessage.create(
                    "total-fail-uuid", LectureEventType.LECTURE_UPDATED, LECTURE_ID, null, 10L, "테스트 강의", "강의가 수정되었습니다");

            // 1. SSE 미연결 상태 Mocking
            given(redisClientManager.isClientConnected(TARGET_MEMBER_ID)).willReturn(false);

            // 2. KafkaConsumer 로직을 위한 CourseClient Mocking
            List<Long> studentIds = List.of(TARGET_MEMBER_ID);
            GetEnrolledStudentInfosResponse response = new GetEnrolledStudentInfosResponse("테스트 강의", "메모", studentIds);
            given(courseClient.getEnrolledStudentInfosByLectureId(LECTURE_ID)).willReturn(response);

            // 3. FCM 전송 실패 주입: 전송 어댑터가 즉시 실패한 Future를 반환
            //    → FcmService의 FcmApiFutureCallback.onFailure → failedNotificationRepository.save
            given(fcmMessageSender.sendFcmNotificationAsync(any(FcmToken.class), any(), any()))
                    .willReturn(ApiFutures.immediateFailedFuture(new RuntimeException("FCM 서버 에러")));

            // when
            kafkaTemplate.send(KafkaTopics.COURSE_EVENT_TOPIC, message);

            // then
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                // FCM 전송이 1번 시도되었는지 확인
                verify(fcmService, times(1)).sendFcmNotification(any());
                // 최종적으로 DB에 저장되었는지 확인
                verify(failedNotificationRepository, times(1)).save(any(FailedNotification.class));
            });
        }
    }

    // TODO: 스케줄러 테스트는 별도의 테스트 클래스로 분리 ?
    // @DisplayName("알림 재처리 스케줄러 동작 시") ...
    // @Nested
}
