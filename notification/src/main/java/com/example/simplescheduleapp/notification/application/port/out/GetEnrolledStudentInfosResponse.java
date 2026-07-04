package com.example.simplescheduleapp.notification.application.port.out;

import java.util.List;

/**
 * {@link EnrolledStudentsPort}의 응답 모델. application 계층이 소유한다 —
 * course 서버의 HTTP 응답 형식이 바뀌어도 이 타입과 사용처(전략)는 영향받지 않는다.
 */
public record GetEnrolledStudentInfosResponse(
        String lectureTitle,
        String lectureMemo,
        List<Long> studentIds
) {
}
