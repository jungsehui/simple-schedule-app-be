package com.example.simplescheduleapp.member.infrastructure.persistence;

import com.example.simplescheduleapp.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA 리포지토리 — {@link MemberRepositoryAdapter}가 이 인터페이스로
 * {@code MemberRepository} 포트를 구현한다. Spring Data는 이 infrastructure 계층에만 존재한다.
 */
public interface MemberJpaRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUsername(String username);
}
