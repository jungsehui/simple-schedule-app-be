package com.example.simplescheduleapp.special.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.special.exception.SpecialLectureExceptionCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecialLectureRepository extends JpaRepository<SpecialLecture, Long> {

    default SpecialLecture getById(Long id) {
        return findById(id).orElseThrow(() -> new ApplicationException(SpecialLectureExceptionCode.SPECIAL_LECTURE_NOT_FOUND));
    }
}
