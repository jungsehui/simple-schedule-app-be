package com.example.simplescheduleapp.fcm.application;

import com.example.simplescheduleapp.NotificationApplication;
import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.fcm.exception.FcmTokenExceptionCode;
import com.example.simplescheduleapp.notification.domain.FailedNotification;
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

@DisplayName("FcmService 실패 테스트")
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@SpringBootTest(classes = NotificationApplication.class)
public class FcmServiceFailureTest extends ApplicationTest {

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
    // fcm이 소유한 입력 계약. 실패 기록은 실제 FcmFailureRecorderAdapter(포트 구현)를 거쳐
    // FailedNotificationRepository에 저장되므로 아래 단정은 그대로 유효하다. (ADR-0004)
    private FcmSendRequest event;

    @BeforeEach
    void setUp() {
        fcmToken = new FcmToken(studentId, "test-token");
        event = new FcmSendRequest(tutorId, studentId, "제목", "내용");

        given(fcmTokenRepository.getByMemberId(studentId)).willReturn(fcmToken);
    }

    @Test
    void FCM_비동기_전송에_실패하면_실패_알림을_DB에_저장한다() {
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
            callbackCaptor.getValue().onFailure(new RuntimeException("FCM 실패 성공 !")); // 실패 콜백 수동 실행

            // then
            ArgumentCaptor<FailedNotification> captor = ArgumentCaptor.forClass(FailedNotification.class);
            verify(failedNotificationRepository).save(captor.capture());
            assertThat(captor.getValue().getTargetId()).isEqualTo(studentId);
            assertThat(captor.getValue().getFailReason()).isEqualTo("FCM 실패 성공 !");
        }
    }

    @Test
    void FCM_토큰이_없으면_즉시_실패_처리하고_DB에_저장한다() {
        // given
        given(fcmTokenRepository.getByMemberId(2L)).willReturn(null);

        // when
        fcmService.sendFcmNotification(event);

        // then
        verify(firebaseMessaging, never()).sendAsync(any(Message.class));

        ArgumentCaptor<FailedNotification> captor = ArgumentCaptor.forClass(FailedNotification.class);
        verify(failedNotificationRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetId()).isEqualTo(2L);
        assertThat(captor.getValue().getFailReason()).isEqualTo(FcmTokenExceptionCode.FCM_TOKEN_NOT_FOUND.getMessage());
    }

    @Test
    void FCM_토큰이_없으면_즉시_실패_처리하고_DB에_저장한다_호출_1회() {
        // given
        FcmSendRequest event = new FcmSendRequest(tutorId, studentId, "제목", "내용");
        given(fcmTokenRepository.getByMemberId(studentId)).willReturn(null); // 토큰이 없는 상황

        // when
        fcmService.sendFcmNotification(event);

        // then
        // FCM 전송(sendAsync) 자체가 호출되지 않았는지 검증
        verify(firebaseMessaging, never()).sendAsync(any(Message.class));

        // 실패 알림이 즉시 DB에 저장되었는지 검증
        ArgumentCaptor<FailedNotification> failedNotificationCaptor = ArgumentCaptor.forClass(FailedNotification.class);
        verify(failedNotificationRepository, times(1)).save(failedNotificationCaptor.capture());

        assertThat(failedNotificationCaptor.getValue().getTargetId()).isEqualTo(studentId);
        assertThat(failedNotificationCaptor.getValue().getFailReason()).isEqualTo(FcmTokenExceptionCode.FCM_TOKEN_NOT_FOUND.getMessage());
    }
}
