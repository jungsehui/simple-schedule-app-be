package com.example.simplescheduleapp.fcm.infrastructure;

import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class FcmUtils {

    public static Notification createNotification(String title, String body) {
        return Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();
    }

    public static Message buildMessage(String token, Notification notification) {
        return Message.builder()
                .setNotification(notification)
                .putData("time", LocalDateTime.now().toString())
                .setToken(token)
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
}
