package com.example.simplescheduleapp.lecture.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.exception.LectureExceptionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {

    default Lecture getByLectureId(Long lectureId) {
        return findById(lectureId).orElseThrow(() -> new ApplicationException(LectureExceptionCode.LECTURE_NOT_FOUND));
    }

    @Query("SELECT l FROM Lecture l WHERE l.title LIKE %:keyword% OR l.memo LIKE %:keyword%")
    List<Lecture> findByKeyword(String keyword);

    List<Lecture> findAllByTutorId(Long id);
}
