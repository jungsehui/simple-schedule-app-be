package com.example.simplescheduleapp.member.infrastructure.persistence;

import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@code MemberRepository} 포트의 JPA 어댑터. 도메인은 포트에만 의존하고,
 * Spring Data 세부는 여기에 격리된다. (ADR-0002 Stage 2)
 */
@Repository
@RequiredArgsConstructor
public class MemberRepositoryAdapter implements MemberRepository {

    private final MemberJpaRepository jpaRepository;

    @Override
    public Member save(Member member) {
        return jpaRepository.save(member);
    }

    @Override
    public Optional<Member> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Member> findByUsername(String username) {
        return jpaRepository.findByUsername(username);
    }
}
