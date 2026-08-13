package com.example.simplescheduleapp.notification.application.strategy.lecture;

import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.notification.application.strategy.NotificationStrategy;
import com.example.simplescheduleapp.notification.application.NotificationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class RequestEnrollmentStrategy implements NotificationStrategy {

    private final NotificationFacade notificationFacade;

    @Override
    public LectureEventType getSupportType() {
        return LectureEventType.ENROLLMENT_REQUESTED;
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
