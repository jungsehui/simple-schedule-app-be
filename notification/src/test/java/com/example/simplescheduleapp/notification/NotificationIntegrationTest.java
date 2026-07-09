package com.example.simplescheduleapp.notification;

import com.example.simplescheduleapp.NotificationApplication;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.fcm.application.FcmService;
import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.notification.application.NotificationDispatcher;
import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.notification.client.CourseClient;
import com.example.simplescheduleapp.notification.client.response.GetEnrolledStudentInfosResponse;
import com.example.simplescheduleapp.notification.domain.FailedNotification;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import com.example.simplescheduleapp.sse.application.SseConnectionPublisher;
import com.example.simplescheduleapp.redis.cache.RedisClientManager;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
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

    // 실제 로직을 호출하면서도 특정 메서드를 Mocking하기 위해 @SpyBean 사용
    @SpyBean
    private NotificationDispatcher notificationDispatcher;

    @SpyBean
    private SseConnectionPublisher sseConnectionPublisher;

    @SpyBean
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

    private final Long LECTURE_ID = 100L;
    private final Long TARGET_MEMBER_ID = 1L;

    @MockitoBean
    private CourseClient courseClient;

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

        // FCM 실패를 ApiFutures/FirebaseMessaging의 스레드 로컬 static mock으로 주입하는데,
        // 알림 디스패치가 비동기 스레드에서 실행되면 static mock이 적용되지 않아 onFailure→save가
        // 실행되지 않는다(FCM 전송 자체는 호출됨 = 라우팅은 정상). 동기적으로 실패 콜백을 구동하도록
        // 테스트를 재설계해야 안정화됨 → 재설계 전까지 비활성화.
        @Disabled("async 경계에서 static mock 미적용으로 불안정 — 실패 주입 방식 재설계 필요")
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

            // 3. FcmService의 비동기 실패 시나리오 Mocking
            ArgumentCaptor<ApiFutureCallback<String>> callbackCaptor = ArgumentCaptor.forClass(ApiFutureCallback.class);

            try (MockedStatic<FirebaseMessaging> fbMock = mockStatic(FirebaseMessaging.class);
                 MockedStatic<ApiFutures> afMock = mockStatic(ApiFutures.class)) {

                fbMock.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
                when(firebaseMessaging.sendAsync(any(Message.class))).thenReturn(mock(ApiFuture.class));

                afMock.when(() -> ApiFutures.addCallback(any(), callbackCaptor.capture(), any()))
                        .then(invocation -> {
                            // 콜백이 등록되면, onFailure를 실행하여 실패 상황 시뮬레이션
                            callbackCaptor.getValue().onFailure(new RuntimeException("FCM 서버 에러"));
                            return null;
                        });

                // when
                kafkaTemplate.send(KafkaTopics.COURSE_EVENT_TOPIC, message);

                // then
                await().atMost(Duration.ofSeconds(50)).untilAsserted(() -> {
                    // FCM 전송이 1번 시도되었는지 확인
                    verify(fcmService, times(1)).sendFcmNotification(any());
                    // 최종적으로 DB에 저장되었는지 확인
                    verify(failedNotificationRepository, times(1)).save(any(FailedNotification.class));
                });
            }
        }
    }

    // TODO: 스케줄러 테스트는 별도의 테스트 클래스로 분리 ?
    // @DisplayName("알림 재처리 스케줄러 동작 시") ...
    // @Nested
}
