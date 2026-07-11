package com.example.simplescheduleapp.tutor.infrastructure.persistence;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@code TutorRepository} 포트의 JPA 어댑터. 유니크 제약 위반을 도메인 예외로 번역. (ADR-0002 Stage 2)
 */
@Repository
@RequiredArgsConstructor
public class TutorRepositoryAdapter implements TutorRepository {

    private final TutorJpaRepository jpaRepository;

    @Override
    public Tutor save(Tutor member) {
        try {
            return jpaRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(MemberExceptionCode.DUPLICATED_USERNAME_PHONE);
        }
    }

    @Override
    public Optional<Tutor> findById(Long id) {
        return jpaRepository.findById(id);
    }
}
