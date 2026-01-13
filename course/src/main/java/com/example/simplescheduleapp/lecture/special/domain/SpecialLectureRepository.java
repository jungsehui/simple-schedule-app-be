package com.example.simplescheduleapp.lecture.special.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureExceptionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialLectureRepository extends JpaRepository<SpecialLecture, Long> {

    default SpecialLecture getById(Long id) {
        return findById(id).orElseThrow(() -> new ApplicationException(SpecialLectureExceptionCode.SPECIAL_LECTURE_NOT_FOUND));
    }
}
