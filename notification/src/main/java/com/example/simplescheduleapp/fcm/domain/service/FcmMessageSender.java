package com.example.simplescheduleapp.fcm.domain.service;

import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.utils.FcmUtils;
import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Component;

@Component
public class FcmMessageSender {

    public ApiFuture<String> sendFcmNotificationAsync(FcmToken fcmToken, String title, String body) {
        Notification notification = FcmUtils.createNotification(title, body);
        Message toSend = FcmUtils.buildMessage(fcmToken.getFcmToken(), notification);

        return FirebaseMessaging.getInstance().sendAsync(toSend);
    }
}
