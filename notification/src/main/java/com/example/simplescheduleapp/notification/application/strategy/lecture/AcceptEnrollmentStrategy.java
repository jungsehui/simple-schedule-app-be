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
public class AcceptEnrollmentStrategy implements NotificationStrategy {

    private final NotificationFacade notificationFacade;

    @Override
    public NotificationEventType getSupportType() {
        return NotificationEventType.ENROLLMENT_ACCEPTED;
    }

    @Override
    public void handle(NotificationCommand command) {
        // 누구에게 ? 학생(studentId)에게 !
        notificationFacade.sendNotification(
                command.tutorId(),            // sender
                List.of(command.studentId()), // target
                command.lectureTitle(),
                command.details()
        );
    }
}
