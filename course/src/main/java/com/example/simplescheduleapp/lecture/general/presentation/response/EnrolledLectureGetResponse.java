package com.example.simplescheduleapp.lecture.general.presentation.response;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;

import java.util.List;

/**
 * 내 수강목록 응답.
 *
 * <p>항목 타입은 {@link LectureResponse}를 그대로 쓴다 — 강사의 강의 목록
 * ({@link TutorLectureGetResponse})과 검색 결과({@link LectureSearchResponse})가 이미 같은
 * 항목이라, 클라이언트는 강의 카드 컴포넌트 하나로 세 화면을 다 그린다.
 */
public record EnrolledLectureGetResponse(
        List<LectureResponse> lectureResponses
) {

    public static EnrolledLectureGetResponse from(List<Lecture> lectures) {
        List<LectureResponse> lectureResponses = lectures.stream()
                .map(it -> new LectureResponse(
                        it.getId(),
                        it.getTitle(),
                        it.getStartTime(),
                        it.getEndTime(),
                        it.getMemo(),
                        it.getCapacity(),
                        it.getEnrolledCount()
                ))
                .toList();
        return new EnrolledLectureGetResponse(lectureResponses);
    }
}
