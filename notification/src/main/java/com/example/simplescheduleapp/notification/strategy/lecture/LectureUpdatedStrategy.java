package com.example.simplescheduleapp.kafka.strategy.lecture;

import com.example.simplescheduleapp.common.kafka.topic.CourseEventMessage;
import com.example.simplescheduleapp.common.kafka.topic.CourseEventType;
import com.example.simplescheduleapp.kafka.strategy.NotificationStrategy;
import com.example.simplescheduleapp.notification.application.NotificationFacade;
import com.example.simplescheduleapp.notification.client.CourseClient;
import com.example.simplescheduleapp.notification.client.response.GetEnrolledStudentInfosResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LectureUpdatedStrategy implements NotificationStrategy {

    private final NotificationFacade notificationFacade;
    private final CourseClient courseClient;

    @Override
    public boolean supports(CourseEventType type) {
        return type == CourseEventType.LECTURE_UPDATED;
    }

    @Override
    public void handle(CourseEventMessage message) {
        GetEnrolledStudentInfosResponse studentInfos = courseClient.getEnrolledStudentInfosByLectureId(message.targetId()); // targetId = lectureId

        notificationFacade.sendNotificationsAsync(
                message.targetId(),
                studentInfos.studentIds(),
                message.title(),
                message.content()
        );
    }
}
