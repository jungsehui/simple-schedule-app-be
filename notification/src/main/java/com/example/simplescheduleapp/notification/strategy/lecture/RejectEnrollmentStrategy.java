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
public class RejectEnrollmentStrategy implements NotificationStrategy {

    private final NotificationFacade notificationFacade;

    @Override
    public LectureEventType getSupportType() {
        return LectureEventType.ENROLLMENT_REJECTED;
    }

    @Override
    public void handle(KafkaLectureEventMessage message) {
        // 학생(studentId)에게 발송
        notificationFacade.sendNotification(
                message.tutorId(),            // Sender: 강사
                List.of(message.studentId()), // Target: 학생
                message.lectureTitle(),
                message.details()
        );
    }
}
