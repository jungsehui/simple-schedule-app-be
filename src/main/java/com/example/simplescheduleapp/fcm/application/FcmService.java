package com.example.simplescheduleapp.fcm.application;

import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.fcm.utils.FcmUtils;
import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.MemberRepository;
import com.example.simplescheduleapp.notification.message.NotificationMessage;
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

    private final MemberRepository memberRepository;
    private final FcmTokenRepository fcmTokenRepository;

    public void addFcmToken(Long memberId, String fcmToken){
        Member member = memberRepository.getById(memberId);
        FcmToken token = new FcmToken(member, fcmToken);
        fcmTokenRepository.save(token);
    }

    @Retryable(
            value = FirebaseMessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendPushNotification(NotificationMessage message) {
        Notification notification = FcmUtils.createNotification(message.eventName(), message.messageBody());
        FcmToken fcmToken = fcmTokenRepository.getByMemberId(message.memberId());
        Message toSend = FcmUtils.buildMessage(fcmToken.getFcmToken(), notification);

        try {
            String response = FirebaseMessaging.getInstance().send(toSend);
            log.info("FCM 전송 성공 - response: {}, memberId: {}, event: {}, message: {}",
                    response, message.memberId(), message.eventName(), message.messageBody());
        } catch (FirebaseMessagingException e) {
            log.warn("FCM 전송 실패 - memberId: {}, token: {}, 이유: {}",
                    message.memberId(), fcmToken.getFcmToken(), e.getMessage());
        }
    }
}
