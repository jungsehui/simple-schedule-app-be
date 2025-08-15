package com.example.simplescheduleapp.kafka.topic;

import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.lecture.domain.PendingLectureEnrollment;

public record RequestLectureEnrollmentTopicMessage(
        Long senderId, // 행위자 ID (학생)
        Long targetId, // 알림 대상 ID
        String lectureTitle
) {

    public static RequestLectureEnrollmentTopicMessage of(PendingLectureEnrollment pending, Lecture lecture) {
        return new RequestLectureEnrollmentTopicMessage(
                pending.getStudentId(),
                lecture.getTutor().getId(),
                lecture.getTitle()
        );
    }
}
