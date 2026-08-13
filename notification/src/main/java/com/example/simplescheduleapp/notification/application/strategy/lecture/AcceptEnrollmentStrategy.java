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
public class AcceptEnrollmentStrategy implements NotificationStrategy {

    private final NotificationFacade notificationFacade;

    @Override
    public LectureEventType getSupportType() {
        return LectureEventType.ENROLLMENT_ACCEPTED;
    }

    @Override
    public void handle(KafkaLectureEventMessage message) {
        // 누구에게 ? 학생(studentId)에게 !
        notificationFacade.sendNotification(
                message.tutorId(),            // sender
                List.of(message.studentId()), // target
                message.lectureTitle(),
                message.details()
        );
    }
}
