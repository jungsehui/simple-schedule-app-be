package com.example.simplescheduleapp.lecture.general.presentation.response;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollment;

import java.time.LocalDateTime;
import java.util.List;

public record LectureEnrollmentGetResponse(
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        List<StudentInfoResponse> studentInfos
) {

    public static LectureEnrollmentGetResponse of(
            List<LectureEnrollment> lectureEnrollments,
            List<StudentInfoResponse> studentInfos
    ) {
        Lecture lecture = lectureEnrollments.getFirst().getLecture();
        return new LectureEnrollmentGetResponse(
                lecture.getTitle(),
                lecture.getStartTime(),
                lecture.getEndTime(),
                lecture.getMemo(),
                studentInfos
        );
    }
}
