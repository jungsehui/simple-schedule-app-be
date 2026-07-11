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

    default Lecture getByLectureId(Long lectureId) {
        return findById(lectureId).orElseThrow(() -> new ApplicationException(LectureExceptionCode.LECTURE_NOT_FOUND));
    }
}
