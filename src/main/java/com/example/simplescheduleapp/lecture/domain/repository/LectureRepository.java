package com.example.simplescheduleapp.lecture.domain.repository;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.domain.entity.Lecture;
import com.example.simplescheduleapp.tutor.domain.entity.Tutor;
import com.example.simplescheduleapp.tutor.exception.TutorExceptionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {

    default Lecture getById(Long id) {
        return findById(id).orElseThrow(() -> new ApplicationException(TutorExceptionCode.TUTOR_NOT_FOUND));
    }

    @Query("SELECT l FROM Lecture l WHERE l.title LIKE %:keyword% OR l.memo LIKE %:keyword%")
    List<Lecture> findByKeyword(String keyword);

    List<Lecture> findAllByTutorId(Long id);
}
