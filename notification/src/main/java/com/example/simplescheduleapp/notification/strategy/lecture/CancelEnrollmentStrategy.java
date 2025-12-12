package com.example.simplescheduleapp.notification.strategy.lecture;

import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.notification.strategy.NotificationStrategy;
import com.example.simplescheduleapp.notification.application.NotificationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CancelEnrollmentStrategy implements NotificationStrategy {

    private final NotificationFacade notificationFacade;

    @Override
    public LectureEventType getSupportType() {
        return LectureEventType.ENROLLMENT_CANCELED;
    }

    @Override
    public void handle(KafkaLectureEventMessage message) {
        // 강사(tutorId)에게 발송
        notificationFacade.sendNotification(
                message.studentId(),          // Sender: 학생
                List.of(message.tutorId()),   // Target: 강사
                message.lectureTitle(),
                message.details()
        );
    }
}
