package com.example.simplescheduleapp.lecture.special.presentation.response;

import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;

import java.util.List;

/**
 * 특강 목록 응답.
 *
 * <p>배열을 그대로 최상위에 두지 않고 객체로 감싼다 — 일반 강의의
 * {@code LectureSearchResponse}/{@code TutorLectureGetResponse}가 이미 그 모양이라
 * 클라이언트가 목록 응답을 한 가지 방식으로만 다루게 된다.
 */
public record SpecialLectureGetResponse(
        List<SpecialLectureResponse> specialLectureResponses
) {

    public static SpecialLectureGetResponse from(List<SpecialLecture> specialLectures) {
        List<SpecialLectureResponse> responses = specialLectures.stream()
                .map(it -> new SpecialLectureResponse(
                        it.getId(),
                        it.getTitle(),
                        it.getStartTime(),
                        it.getEndTime(),
                        it.getMemo(),
                        it.getCapacity()
                ))
                .toList();
        return new SpecialLectureGetResponse(responses);
    }
}
