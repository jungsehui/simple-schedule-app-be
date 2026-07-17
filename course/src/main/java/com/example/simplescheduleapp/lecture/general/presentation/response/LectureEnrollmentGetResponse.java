package com.example.simplescheduleapp.lecture.general.presentation.response;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;

import java.time.LocalDateTime;
import java.util.List;

public record LectureEnrollmentGetResponse(
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        List<StudentInfoResponse> studentInfos
) {

    /**
     * 애플리케이션 계층이 로드해 전달한 Lecture로 응답을 만든다.
     * (ADR-0004 Phase A: enrollment→lecture 관통 순회 제거)
     */
    public static LectureEnrollmentGetResponse of(
            Lecture lecture,
            List<StudentInfoResponse> studentInfos
    ) {
        return new LectureEnrollmentGetResponse(
                lecture.getTitle(),
                lecture.getStartTime(),
                lecture.getEndTime(),
                lecture.getMemo(),
                studentInfos
        );
    }
}
