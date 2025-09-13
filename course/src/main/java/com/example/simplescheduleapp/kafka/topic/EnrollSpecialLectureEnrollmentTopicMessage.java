package com.example.simplescheduleapp.kafka.topic;

public record SpecialLectureEnrollmentSuccessTopicMessage(
        Long specialLectureId,
        Long studentId
) {

    public static SpecialLectureEnrollmentSuccessTopicMessage of(Long specialLectureId, Long studentId) {
        return new SpecialLectureEnrollmentSuccessTopicMessage(
                specialLectureId,
                studentId
        );
    }
}
