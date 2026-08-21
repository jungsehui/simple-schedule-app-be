package com.example.simplescheduleapp.notification.application.strategy.lecture;

import com.example.simplescheduleapp.notification.application.strategy.NotificationCommand;
import com.example.simplescheduleapp.notification.application.strategy.NotificationEventType;
import com.example.simplescheduleapp.notification.application.strategy.NotificationStrategy;
import com.example.simplescheduleapp.notification.application.NotificationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CancelEnrollmentStrategy implements NotificationStrategy {

    private final NotificationFacade notificationFacade;

    @Override
    public NotificationEventType getSupportType() {
        return NotificationEventType.ENROLLMENT_CANCELED;
    }

    @Override
    public void handle(NotificationCommand command) {
        // 강사(tutorId)에게 발송
        notificationFacade.sendNotification(
                command.studentId(),          // Sender: 학생
                List.of(command.tutorId()),   // Target: 강사
                command.lectureTitle(),
                command.details()
        );
    }
}
