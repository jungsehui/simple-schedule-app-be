package com.example.simplescheduleapp.lecture.general.presentation.response;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;

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

    /**
     * 목록 항목과 상세 응답이 <b>같은 매핑</b>을 쓰도록 한 곳에 모은다.
     *
     * <p>이 7필드 매핑은 목록 응답 3종에 글자 그대로 반복돼 있었다. 상세 조회까지 네 번째
     * 사본을 만드는 대신 여기로 올린다. 필드가 하나 늘 때 네 곳을 고쳐야 하는 상태였고,
     * 하나를 빠뜨리면 화면마다 다른 모양이 나간다.
     */
    public static LectureResponse from(Lecture lecture) {
        return new LectureResponse(
                lecture.getId(),
                lecture.getTitle(),
                lecture.getStartTime(),
                lecture.getEndTime(),
                lecture.getMemo(),
                lecture.getCapacity(),
                lecture.getEnrolledCount()
        );
    }
}
