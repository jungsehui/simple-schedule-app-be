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
public class RejectEnrollmentStrategy implements NotificationStrategy {

    private final NotificationFacade notificationFacade;

    @Override
    public NotificationEventType getSupportType() {
        return NotificationEventType.ENROLLMENT_REJECTED;
    }

    @Override
    public void handle(NotificationCommand command) {
        // 학생(studentId)에게 발송
        notificationFacade.sendNotification(
                command.tutorId(),            // Sender: 강사
                List.of(command.studentId()), // Target: 학생
                command.lectureTitle(),
                command.details()
        );
    }
}
