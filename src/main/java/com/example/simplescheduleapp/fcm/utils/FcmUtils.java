package com.example.simplescheduleapp.fcm.utils;

import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FcmUtils {

    public static Notification createNotification(String title, String body) {
        return Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();
    }

    public static List<Message> buildMessages(List<String> tokens, Notification notification) {
        return tokens.stream()
                .map(token -> Message.builder()
                        .setNotification(notification)
                        .putData("time", LocalDateTime.now().toString())
                        .setToken(token)
                        .build())
                .toList();
    }

    public static void logFailedTokens(List<String> tokens, List<SendResponse> responses) {
        List<String> failedTokens = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                failedTokens.add(tokens.get(i));
                log.warn("FCM 전송 실패 응답: ", responses.get(i).getException());
            }
        }
        log.warn("전송 실패 토큰 목록: {}", failedTokens);
    }
}
