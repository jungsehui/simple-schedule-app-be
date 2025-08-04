package com.example.simplescheduleapp.fcm.application;

import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.fcm.utils.FcmUtils;
import com.example.simplescheduleapp.notification.domain.NotificationMessage;
import com.example.simplescheduleapp.notification.domain.NotificationMessageEvent;
import com.example.simplescheduleapp.support.UnitTest;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class FcmServiceTest extends UnitTest {

    @InjectMocks
    FcmService fcmService;

    @Mock
    FcmTokenRepository fcmTokenRepository;

    Long tutorId = 1L;
    Long studentId = 2L;

    @Test
    void FCM_토큰_저장_성공() {
        String token = "test-fcm-token";
        fcmService.addFcmToken(1L, token);
        then(fcmTokenRepository).should().save(any(FcmToken.class));
    }

    @Test
    void FCM_푸시_알림_성공() throws FirebaseMessagingException {
        NotificationMessageEvent message = new NotificationMessageEvent(
                tutorId,
                studentId,
                "강의"
        );
        FcmToken fcmToken = new FcmToken(tutorId, "test-token");

        given(fcmTokenRepository.getByMemberId(1L)).willReturn(fcmToken);

        try (MockedStatic<FirebaseMessaging> firebaseMessagingMockedStatic = mockStatic(FirebaseMessaging.class);
             MockedStatic<FcmUtils> fcmUtilsMockedStatic = mockStatic(FcmUtils.class)) {

            Notification notification = Notification.builder().setTitle("강의 제목").setBody("알림 메시지").build();
            Message builtMessage = Message.builder()
                    .setNotification(notification)
                    .setToken("test-token")
                    .putData("time", "test-time")
                    .build();
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);

            fcmUtilsMockedStatic.when(() -> FcmUtils.createNotification("강의 제목", "알림 메시지"))
                    .thenReturn(notification);
            fcmUtilsMockedStatic.when(() -> FcmUtils.buildMessage("test-token", notification))
                    .thenReturn(builtMessage);
            firebaseMessagingMockedStatic.when(FirebaseMessaging::getInstance)
                    .thenReturn(firebaseMessaging);
            given(firebaseMessaging.send(builtMessage)).willReturn("response-123");

            fcmService.sendFcmNotification(message);

            then(firebaseMessaging).should().send(builtMessage);
        }
    }
}
