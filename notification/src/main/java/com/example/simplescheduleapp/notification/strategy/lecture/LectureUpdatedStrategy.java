package com.example.simplescheduleapp.notification.strategy.lecture;

import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.notification.strategy.NotificationStrategy;
import com.example.simplescheduleapp.notification.application.NotificationFacade;
import com.example.simplescheduleapp.notification.application.port.out.EnrolledStudentsPort;
import com.example.simplescheduleapp.notification.application.port.out.GetEnrolledStudentInfosResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LectureUpdatedStrategy implements NotificationStrategy {

    private final NotificationFacade notificationFacade;
    // 포트에만 의존 — course 서버 통신 수단(HTTP/캐시 등)이 바뀌어도 이 전략은 불변 (ADR-0002)
    private final EnrolledStudentsPort courseClient;

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
