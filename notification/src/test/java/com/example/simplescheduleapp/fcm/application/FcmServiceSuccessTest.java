package com.example.simplescheduleapp.fcm.application;

import com.example.simplescheduleapp.NotificationApplication;
import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.notification.domain.FailedNotificationRepository;
import com.example.simplescheduleapp.support.ApplicationTest;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("FcmService 성공 테스트")
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@SpringBootTest(classes = NotificationApplication.class)
class FcmServiceSuccessTest extends ApplicationTest {

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
    private NotificationRequest event;

    @BeforeEach
    void setUp() {
        fcmToken = new FcmToken(studentId, "test-token");
        event = new NotificationRequest(tutorId, studentId, "제목", "내용");

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
    void FCM_비동기_전송에_성공하면_실패_알림을_저장하지_않는다() {
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
            callbackCaptor.getValue().onSuccess("test-fcm-response-id-success !!"); // 성공 콜백 수동 실행

            // then
            verify(failedNotificationRepository, never()).save(any());
        }
    }
}
