package com.example.simplescheduleapp.notification;

import com.example.simplescheduleapp.NotificationApplication;
import com.example.simplescheduleapp.common.kafka.KafkaDomainEventMessage;
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
import com.example.simplescheduleapp.sse.application.SseService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;

import static com.example.simplescheduleapp.support.ApplicationWithKafkaTest.PORT;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("알림 시스템 통합 테스트")
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@EmbeddedKafka(
        topics = {KafkaTopics.LECTURE_EVENT_TOPIC},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:" + PORT
        },
        ports = {PORT}
)
@SpringBootTest(classes = NotificationApplication.class)
class NotificationIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // 실제 로직을 호출하면서도 특정 메서드를 Mocking하기 위해 @SpyBean 사용
    @SpyBean
    private NotificationDispatcher notificationDispatcher;

    @SpyBean
    private SseService sseService;

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
        reset(redisClientManager, courseClient, firebaseMessaging, sseService, fcmService);
    }

    @DisplayName("Kafka 메시지 수신 시")
    @Nested
    class ConsumeAndNotify {

        @Test
        void SSE_연결_상태이면_SSE로_알림을_성공적으로_전송한다() {
            // given
            KafkaDomainEventMessage message = new KafkaDomainEventMessage(1L, "sse-success-uuid", LECTURE_ID);

            given(redisClientManager.isClientConnected(TARGET_MEMBER_ID)).willReturn(true);
            List<Long> studentIds = List.of(TARGET_MEMBER_ID);
            GetEnrolledStudentInfosResponse response = new GetEnrolledStudentInfosResponse("테스트 강의", "메모", studentIds);
            given(courseClient.getEnrolledStudentInfosByLectureId(LECTURE_ID)).willReturn(response);

            // when
            kafkaTemplate.send(KafkaTopics.LECTURE_EVENT_TOPIC, message);

            // then
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                verify(sseService, times(1)).sendSseNotification(any(NotificationRequest.class));
                verify(fcmService, never()).sendFcmNotification(any());
            });
        }

        @Test
        void SSE_전송_실패_시_FCM으로_대체_전송을_시도하고_성공한다() {
            // given
            KafkaDomainEventMessage message = new KafkaDomainEventMessage(2L, "fcm-fallback-uuid", LECTURE_ID);

            given(redisClientManager.isClientConnected(TARGET_MEMBER_ID)).willReturn(true);
            List<Long> studentIds = List.of(TARGET_MEMBER_ID);
            GetEnrolledStudentInfosResponse response = new GetEnrolledStudentInfosResponse("테스트 강의", "메모", studentIds);
            given(courseClient.getEnrolledStudentInfosByLectureId(LECTURE_ID)).willReturn(response);

            doCallRealMethod().when(notificationDispatcher).dispatchPushNotification(any());
            doThrow(new RuntimeException("SSE Send Error")).when(sseService).sendSseNotification(any());
            doNothing().when(fcmService).sendFcmNotification(any());

            // when
            kafkaTemplate.send(KafkaTopics.LECTURE_EVENT_TOPIC, message);

            // then
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                verify(sseService, times(1)).sendSseNotification(any());
                verify(fcmService, times(1)).sendFcmNotification(any());
            });
        }

        @Test
        void SSE_미연결_및_FCM_전송_실패_시_최종적으로_실패_알림을_DB에_저장한다() {
            // given
            FcmToken fcmToken = new FcmToken(TARGET_MEMBER_ID, "test-token");
            given(fcmTokenRepository.getByMemberId(TARGET_MEMBER_ID)).willReturn(fcmToken);
            KafkaDomainEventMessage message = new KafkaDomainEventMessage(3L, "total-fail-uuid", LECTURE_ID);

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
                kafkaTemplate.send(KafkaTopics.LECTURE_EVENT_TOPIC, message);

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
