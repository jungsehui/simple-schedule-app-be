package com.example.simplescheduleapp.fcm.application;

import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.fcm.utils.FcmUtils;
import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.MemberRepository;
import com.example.simplescheduleapp.notification.message.NotificationMessage;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class FcmService {

    private static final int FIRST_RETRY = 1;
    private static final int MAX_RETRY = 3;

    private final MemberRepository memberRepository;
    private final FcmTokenRepository fcmTokenRepository;

    public void addFcmToken(Long memberId, String fcmToken){
        Member member = memberRepository.getById(memberId);
        FcmToken token = new FcmToken(member, fcmToken);
        fcmTokenRepository.save(token);
    }

    public void sendPushNotification(NotificationMessage message) {
        log.info("FCM 을 통해 memberId: {} 에게 이벤트 발행 - event: {}, message: {}", message.memberId(), message.eventName(), message.messageBody());
        FcmToken fcmToken = fcmTokenRepository.getByMemberId(message.memberId());
        sendPushNotification(List.of(fcmToken.getFcmToken()), message.eventName(), message.messageBody());
    }

    public void sendPushNotification(List<String> tokens, String title, String body) {
        Notification notification = FcmUtils.createNotification(title, body);
        List<Message> messages = FcmUtils.buildMessages(tokens, notification);
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages);
            if (response.getFailureCount() > 0) {
                for (int i = 0; i < response.getResponses().size(); i++) {
                    SendResponse res = response.getResponses().get(i);
                    if (!res.isSuccessful()) {
                        String failedToken = tokens.get(i);
                        log.warn("초기 전송 실패 - 재시도 시도 중: token = {}", failedToken);
                        retrySend(failedToken, title, body, FIRST_RETRY);
                    }
                }
            } else {
                log.info("모든 메시지를 성공적으로 송신했습니다. 요청 수: {}, 성공 수: {}", tokens.size(), response.getSuccessCount());
            }
        } catch (FirebaseMessagingException e) {
            log.error("FCM 메시지 전송 중 예외 발생: {}", e.getMessage());
        }
    }

    private void retrySend(String token, String title, String body, int attempt) {
        if (attempt > MAX_RETRY) {
            log.error("최대 재시도 횟수 초과: token = {}, title = {}", token, title);
            return;
        }

        try {
            List<Message> messages = FcmUtils.buildMessages(List.of(token), FcmUtils.createNotification(title, body));
            FirebaseMessaging.getInstance().sendEach(messages);
            log.info("재시도 성공 ({} 회차): token = {}", attempt, token);
        } catch (FirebaseMessagingException e) {
            log.warn("재시도 실패 ({} 회차): token = {}, error = {}", attempt, token, e.getMessage());
            retrySend(token, title, body, attempt + 1);
        }
    }
}
