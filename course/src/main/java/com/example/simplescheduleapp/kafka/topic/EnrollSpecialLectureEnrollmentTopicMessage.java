package com.example.simplescheduleapp.kafka.topic;

public record EnrollSpecialLectureEnrollmentTopicMessage(
        Long specialLectureId,
        Long studentId
) {

    public static EnrollSpecialLectureEnrollmentTopicMessage of(Long specialLectureId, Long studentId) {
        return new EnrollSpecialLectureEnrollmentTopicMessage(
                specialLectureId,
                studentId
        );
    }
}
