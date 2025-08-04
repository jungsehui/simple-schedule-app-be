package com.example.simplescheduleapp.fcm.application;

import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.fcm.utils.FcmUtils;
import com.example.simplescheduleapp.notification.domain.NotificationMessageEvent;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;

    public void addFcmToken(Long memberId, String fcmToken) {
        FcmToken token = new FcmToken(memberId, fcmToken);
        fcmTokenRepository.save(token);
    }

    @Retryable(
            value = FirebaseMessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendFcmNotification(NotificationMessageEvent event) {
        Notification notification = FcmUtils.createNotification(event.title(), event.body());
        FcmToken fcmToken = fcmTokenRepository.getByMemberId(event.targetMemberId());
        Message toSend = FcmUtils.buildMessage(fcmToken.getFcmToken(), notification);

        try {
            String response = FirebaseMessaging.getInstance().send(toSend);
            log.info("FCM 전송 성공 - response: {}, target member ID: {}, title: {}, body: {}",
                    response, event.targetMemberId(), event.title(), event.body());
        } catch (FirebaseMessagingException e) {
            log.error("FCM 전송 실패 - target member Id: {}, token: {}, exception: {}",
                    event.targetMemberId(), fcmToken.getFcmToken(), e.getMessage());
        }
    }
}
