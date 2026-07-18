package com.example.simplescheduleapp.lecture.general.presentation.response;

import java.time.LocalDateTime;

/**
 * 강의 목록 응답 항목.
 *
 * <p>{@code lectureId}는 목록에서 상세 조회({@code GET /lectures/{lectureId}})나 수강신청
 * ({@code POST /lectures/{lectureId}/enrollments})으로 이어가기 위해 필요하다. 없으면 클라이언트가
 * 목록에서 어떤 후속 동작도 시작할 수 없다. 이름은 {@link LectureCreateResponse}가 이미 쓰는
 * {@code lectureId}에 맞춘다. (필드 추가이므로 기존 클라이언트에 하위호환)
 */
public record LectureResponse(
        Long lectureId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        int capacity,
        int enrolledCount
) {
}
