package com.example.simplescheduleapp.kafka.topic;

import com.example.simplescheduleapp.lecture.domain.Lecture;

public record CancelLectureEnrollmentTopicMessage(
        Long senderId, // 행위자 ID (학생)
        Long targetId, // 알림 대상 ID
        String lectureTitle
) {

    public static CancelLectureEnrollmentTopicMessage of(Long studentId, Lecture lecture) {
        return new CancelLectureEnrollmentTopicMessage(
                studentId,
                lecture.getTutor().getId(),
                lecture.getTitle()
        );
    }
}
