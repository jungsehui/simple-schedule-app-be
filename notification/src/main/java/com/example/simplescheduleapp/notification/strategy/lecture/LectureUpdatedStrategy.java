package com.example.simplescheduleapp.notification.strategy.lecture;

import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.notification.strategy.NotificationStrategy;
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
    public LectureEventType getSupportType() {
        return LectureEventType.LECTURE_UPDATED;
    }

    @Override
    public void handle(KafkaLectureEventMessage message) {
        // 수강생 목록 조회 (lectureId 사용)
        GetEnrolledStudentInfosResponse studentInfos =
                courseClient.getEnrolledStudentInfosByLectureId(message.lectureId());

        // 다건 발송
        notificationFacade.sendNotification(
                message.tutorId(),
                studentInfos.studentIds(),
                message.lectureTitle(),
                "강의 내용이 수정되었습니다: %s".formatted(message.details())
        );
    }
}
