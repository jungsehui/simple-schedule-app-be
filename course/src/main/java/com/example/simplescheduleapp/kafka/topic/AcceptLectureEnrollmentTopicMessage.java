package com.example.simplescheduleapp.kafka.topic;

import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.student.domain.Student;

public record AcceptLectureEnrollmentTopicMessage(
        Long senderId,  // 행위자 ID (강사)
        Long targetId, // 알림 대상 ID
        String lectureTitle
) {

    public static AcceptLectureEnrollmentTopicMessage of(Lecture lecture, Student student) {
        return new AcceptLectureEnrollmentTopicMessage(
                lecture.getTutor().getId(),
                student.getId(),
                lecture.getTitle()
        );
    }
}
