package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LectureJpaRepository extends JpaRepository<Lecture, Long> {

    @Query("SELECT l FROM Lecture l WHERE l.title LIKE %:keyword% OR l.memo LIKE %:keyword%")
    List<Lecture> findByKeyword(String keyword);

    List<Lecture> findAllByTutorId(Long id);
}
