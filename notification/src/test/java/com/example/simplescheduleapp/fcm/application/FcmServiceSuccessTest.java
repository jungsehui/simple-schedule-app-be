package com.example.simplescheduleapp.fcm.application;

import com.example.simplescheduleapp.NotificationApplication;
import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.kafka.event.NotificationMessageEvent;
import com.example.simplescheduleapp.notification.domain.FailedNotification;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import com.example.simplescheduleapp.support.ApplicationTest;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("FcmService 통합 테스트")
@SpringBootTest(classes = NotificationApplication.class)
class FcmServiceTest extends ApplicationTest {

    @Autowired
    private FcmService fcmService;

    @MockitoBean
    private FcmTokenRepository fcmTokenRepository;

    @MockitoBean
    private FailedNotificationRepository failedNotificationRepository;

    @MockitoBean
    private FirebaseMessaging firebaseMessaging;

    private Long tutorId = 1L;
    private Long studentId = 2L;
    private FcmToken fcmToken;
    private NotificationMessageEvent event;

    @BeforeEach
    void setUp() {
        fcmToken = new FcmToken(studentId, "test-token");
        event = new NotificationMessageEvent(tutorId, studentId, "제목", "내용");

        given(fcmTokenRepository.getByMemberId(studentId)).willReturn(fcmToken);
    }

    @Test
    void FCM_토큰_저장_성공() {
        // given
        String token = "test-fcm-token";

        // when
        fcmService.addFcmToken(1L, token);

        // then
        assertThat(fcmTokenRepository.findByMemberId(1L)).isPresent();
    }

    @Test
    @DisplayName("FCM 비동기 전송에 성공하면, 실패 알림을 저장하지 않는다")
    void sendFcmNotification_Async_Success() {
        // given
        FcmToken fcmToken = new FcmToken(studentId, "test-token");
        given(fcmTokenRepository.getByMemberId(studentId)).willReturn(fcmToken);
        ArgumentCaptor<ApiFutureCallback<String>> callbackCaptor = ArgumentCaptor.forClass(ApiFutureCallback.class);

        try (MockedStatic<FirebaseMessaging> fbMock = mockStatic(FirebaseMessaging.class);
             MockedStatic<ApiFutures> afMock = mockStatic(ApiFutures.class)) {

            fbMock.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            when(firebaseMessaging.sendAsync(any(Message.class))).thenReturn(mock(ApiFuture.class));
            afMock.when(() -> ApiFutures.addCallback(any(), callbackCaptor.capture(), any())).then(invocation -> null);

            // when
            fcmService.sendFcmNotification(event);
            callbackCaptor.getValue().onSuccess("fcm-response-id"); // 성공 콜백 수동 실행

            // then
            verify(failedNotificationRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("FCM 비동기 전송에 실패하면, 실패 알림을 DB에 저장한다")
    void sendFcmNotification_Async_Failure() {
        // given
        FcmToken fcmToken = new FcmToken(studentId, "test-token");
        given(fcmTokenRepository.getByMemberId(studentId)).willReturn(fcmToken);
        ArgumentCaptor<ApiFutureCallback<String>> callbackCaptor = ArgumentCaptor.forClass(ApiFutureCallback.class);

        try (MockedStatic<FirebaseMessaging> fbMock = mockStatic(FirebaseMessaging.class);
             MockedStatic<ApiFutures> afMock = mockStatic(ApiFutures.class)) {

            fbMock.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            when(firebaseMessaging.sendAsync(any(Message.class))).thenReturn(mock(ApiFuture.class));
            afMock.when(() -> ApiFutures.addCallback(any(), callbackCaptor.capture(), any())).then(invocation -> null);

            // when
            fcmService.sendFcmNotification(event);
            callbackCaptor.getValue().onFailure(new RuntimeException("FCM 서버 에러")); // 실패 콜백 수동 실행

            // then
            ArgumentCaptor<FailedNotification> captor = ArgumentCaptor.forClass(FailedNotification.class);
            verify(failedNotificationRepository).save(captor.capture());
            assertThat(captor.getValue().getTargetMemberId()).isEqualTo(studentId);
            assertThat(captor.getValue().getFailReason()).isEqualTo("FCM 서버 에러");
        }
    }

    @Test
    @DisplayName("FCM 토큰이 없으면 즉시 실패 처리하고 DB에 저장한다")
    void sendFcmNotification_TokenNotFound() {
        // given
        NotificationMessageEvent event = new NotificationMessageEvent(tutorId, studentId, "제목", "내용");
        given(fcmTokenRepository.findByMemberId(studentId)).willReturn(null); // 토큰이 없는 상황

        // when
        fcmService.sendFcmNotification(event);

        // then
        // FCM 전송(sendAsync) 자체가 호출되지 않았는지 검증
        verify(firebaseMessaging, never()).sendAsync(any(Message.class));

        // 실패 알림이 즉시 DB에 저장되었는지 검증
        ArgumentCaptor<FailedNotification> failedNotificationCaptor = ArgumentCaptor.forClass(FailedNotification.class);
        verify(failedNotificationRepository, times(1)).save(failedNotificationCaptor.capture());

        assertThat(failedNotificationCaptor.getValue().getTargetMemberId()).isEqualTo(studentId);
        assertThat(failedNotificationCaptor.getValue().getFailReason()).isEqualTo("FCM Token not found");
    }
}
