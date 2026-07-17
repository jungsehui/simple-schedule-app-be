package com.example.simplescheduleapp.parent.infrastructure.persistence;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import com.example.simplescheduleapp.parent.domain.Parent;
import com.example.simplescheduleapp.parent.domain.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@code ParentRepository} 포트의 JPA 어댑터. 도메인 ↔ 엔티티 매핑을 담당하고,
 * 유니크 제약 위반을 도메인 예외로 번역. (ADR-0002 Stage 2 / ADR-0004)
 */
@Repository
@RequiredArgsConstructor
public class ParentRepositoryAdapter implements ParentRepository {

    private final ParentJpaRepository jpaRepository;

    @Override
    public Parent save(Parent member) {
        try {
            return ParentMapper.toDomain(jpaRepository.save(ParentMapper.toEntity(member)));
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(MemberExceptionCode.DUPLICATED_USERNAME_PHONE);
        }
    }

    @Override
    public Optional<Parent> findById(Long id) {
        return jpaRepository.findById(id).map(ParentMapper::toDomain);
    }
}
