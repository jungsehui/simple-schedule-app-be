package com.example.simplescheduleapp.student.infrastructure.persistence;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@code StudentRepository} 포트의 JPA 어댑터. 도메인 ↔ 엔티티 매핑을 담당하고, 유니크 제약
 * 위반(Spring의 {@link DataIntegrityViolationException})을 도메인 예외로 번역하는 책임을 진다 —
 * 덕분에 {@code MemberRegister}는 Spring에 의존하지 않는다. (ADR-0002 Stage 2 / ADR-0004)
 */
@Repository
@RequiredArgsConstructor
public class StudentRepositoryAdapter implements StudentRepository {

    private final StudentJpaRepository jpaRepository;

    @Override
    public Student save(Student member) {
        try {
            return StudentMapper.toDomain(jpaRepository.save(StudentMapper.toEntity(member)));
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(MemberExceptionCode.DUPLICATED_USERNAME_PHONE);
        }
    }

    @Override
    public Optional<Student> findById(Long id) {
        return jpaRepository.findById(id).map(StudentMapper::toDomain);
    }
}
