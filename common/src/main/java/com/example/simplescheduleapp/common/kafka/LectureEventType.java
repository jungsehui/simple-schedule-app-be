package com.example.simplescheduleapp.common.kafka;

public enum LectureEventType {

    ENROLLMENT_REQUESTED, // 수강 신청 요청
    ENROLLMENT_ACCEPTED,  // 수강 신청 승낙
    ENROLLMENT_REJECTED,  // 수강 신청 거절
    ENROLLMENT_CANCELED,  // 수강 신청 취소
    LECTURE_UPDATED,      // 강의 내용 수정
    ;
}
