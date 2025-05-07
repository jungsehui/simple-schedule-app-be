package com.example.simplescheduleapp.fcm.application;

import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.fcm.utils.FcmUtils;
import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.MemberRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public void sendPushNotification(String token, String title, String body) {
        sendPushNotification(List.of(token), title, body);
    }

    public void sendPushNotification(List<String> tokens, String title, String body) {
        Notification notification = FcmUtils.createNotification(title, body);
        List<Message> messages = FcmUtils.buildMessages(tokens, notification);
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages);
            if (response.getFailureCount() > 0) {
                FcmUtils.logFailedTokens(tokens, response.getResponses());
            } else {
                log.info("모든 메시지를 성공적으로 송신했습니다. 요청 수 : {}, 성공 수: {}", tokens.size(), response.getSuccessCount());
            }
        } catch (FirebaseMessagingException e) {
            log.error("FCM 메시지 전송 중 예외 발생: {}", e.getMessage());
        }
    }
}
