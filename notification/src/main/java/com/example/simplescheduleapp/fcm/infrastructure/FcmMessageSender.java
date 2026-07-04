package com.example.simplescheduleapp.fcm.infrastructure;

import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.utils.FcmUtils;
import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Firebase 전송 어댑터. 벤더 SDK({@code FirebaseMessaging})는 이 infrastructure 계층에만 존재한다.
 * (기존 fcm.domain.service 위치에서 이동 — ADR-0002 Stage 1)
 */
@RequiredArgsConstructor
@Component
public class FcmMessageSender {

    private final FirebaseMessaging firebaseMessaging;

    public ApiFuture<String> sendFcmNotificationAsync(FcmToken fcmToken, String title, String body) {
        Notification notification = FcmUtils.createNotification(title, body);
        Message toSend = FcmUtils.buildMessage(fcmToken.getFcmToken(), notification);

        return firebaseMessaging.sendAsync(toSend);
    }
}
