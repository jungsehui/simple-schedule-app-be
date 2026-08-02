package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.lecture.general.domain.LectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * {@code LectureRepository} 포트의 JPA 어댑터 (ADR-0004).
 * <p>Spring Data/JPA 세부와 도메인↔엔티티 매핑을 여기에 격리한다.
 * 매퍼가 version을 왕복시키므로 save 시 낙관적 락(merge의 {@code WHERE version = ?})이 유지된다.
 */
@Repository
@RequiredArgsConstructor
public class LectureRepositoryAdapter implements LectureRepository {

    private final LectureJpaRepository jpaRepository;

    @Override
    public Lecture save(Lecture lecture) {
        return LectureMapper.toDomain(jpaRepository.save(LectureMapper.toEntity(lecture)));
    }

    @Override
    public Optional<Lecture> findById(Long id) {
        return jpaRepository.findById(id).map(LectureMapper::toDomain);
    }

    @Override
    public List<Lecture> findByKeyword(String keyword) {
        return jpaRepository.findByKeyword(keyword).stream().map(LectureMapper::toDomain).toList();
    }

    @Override
    public List<Lecture> findAllByTutorId(Long id) {
        return jpaRepository.findAllByTutorId(id).stream().map(LectureMapper::toDomain).toList();
    }
}
