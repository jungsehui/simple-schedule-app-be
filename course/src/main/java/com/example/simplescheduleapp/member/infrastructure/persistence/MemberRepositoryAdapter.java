package com.example.simplescheduleapp.member.infrastructure.persistence;

import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@code MemberRepository} 포트의 JPA 어댑터. 도메인은 포트에만 의존하고,
 * Spring Data 세부는 여기에 격리된다. (ADR-0002 Stage 2 / ADR-0004)
 *
 * <p>조회는 다형이다: {@code MemberMapper}가 구체 서브타입 엔티티를 대응하는 도메인
 * 서브타입(Student/Tutor/Parent)으로 디스패치해 {@code getRole()}을 정확히 보존한다.
 * {@code getById}/{@code getByUsername}(예외 번역)은 포트의 default 메서드를 그대로 사용한다.
 */
@Repository
@RequiredArgsConstructor
public class MemberRepositoryAdapter implements MemberRepository {

    private final MemberJpaRepository jpaRepository;

    @Override
    public Member save(Member member) {
        return MemberMapper.toDomain(jpaRepository.save(MemberMapper.toEntity(member)));
    }

    @Override
    public Optional<Member> findById(Long id) {
        return jpaRepository.findById(id).map(MemberMapper::toDomain);
    }

    @Override
    public Optional<Member> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(MemberMapper::toDomain);
    }
}
