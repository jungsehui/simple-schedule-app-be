package com.example.simplescheduleapp.lecture.general.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;

import java.util.List;
import java.util.Optional;

public interface LectureRepository {

    Lecture save(Lecture lecture);

    Optional<Lecture> findById(Long id);

    List<Lecture> findByKeyword(String keyword);

    List<Lecture> findAllByTutorId(Long id);

    /**
     * 주어진 ID들의 강의 — 시작 시각 오름차순.
     *
     * <p>수강목록처럼 "먼저 ID 집합을 구한 뒤 강의를 채우는" 조회를 위한 것이다. 한 건씩
     * {@code getByLectureId}로 도는 대신 한 방에 가져와 N+1을 막는다. 정렬을 포트 계약에
     * 박아 두는 이유는 {@code findAllOrderByStartTime}과 같다 — 반환 순서를 DB 재량에
     * 맡기면 화면이 조용히 흔들린다.
     */
    List<Lecture> findAllByIdsOrderByStartTime(List<Long> ids);

    default Lecture getByLectureId(Long lectureId) {
        return findById(lectureId).orElseThrow(() -> new ApplicationException(LectureExceptionCode.LECTURE_NOT_FOUND));
    }
}
