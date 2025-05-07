package com.example.simplescheduleapp.lecture.presentation.response;

import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.lecture.domain.LectureEnrollment;

import java.time.LocalDateTime;
import java.util.List;

public record LectureEnrollmentGetResponse(
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        List<StudentInfoResponse> students
) {

    public static LectureEnrollmentGetResponse of(
            List<LectureEnrollment> lectureEnrollments,
            List<StudentInfoResponse> students
    ) {
        Lecture lecture = lectureEnrollments.getFirst().getLecture();
        return new LectureEnrollmentGetResponse(
                lecture.getTitle(),
                lecture.getStartTime(),
                lecture.getEndTime(),
                lecture.getMemo(),
                students
        );
    }
}
