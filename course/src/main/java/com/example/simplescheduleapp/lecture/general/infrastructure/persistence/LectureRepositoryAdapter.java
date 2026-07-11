package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.lecture.general.domain.LectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LectureRepositoryAdapter implements LectureRepository {

    private final LectureJpaRepository jpaRepository;

    @Override
    public Lecture save(Lecture lecture) {
        return jpaRepository.save(lecture);
    }

    @Override
    public Optional<Lecture> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Lecture> findByKeyword(String keyword) {
        return jpaRepository.findByKeyword(keyword);
    }

    @Override
    public List<Lecture> findAllByTutorId(Long id) {
        return jpaRepository.findAllByTutorId(id);
    }
}
