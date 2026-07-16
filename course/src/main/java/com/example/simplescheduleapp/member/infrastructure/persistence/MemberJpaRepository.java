package com.example.simplescheduleapp.member.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA 리포지토리 — {@link MemberRepositoryAdapter}가 이 인터페이스로
 * {@code MemberRepository} 포트를 구현한다. Spring Data는 이 infrastructure 계층에만 존재한다.
 *
 * <p>{@code MemberEntity}는 JOINED 상속의 루트라 조회 결과가 다형이다(Hibernate가 구체
 * 서브타입 엔티티를 반환). 도메인 서브타입으로의 변환은 {@code MemberMapper}가 담당한다. (ADR-0004)
 */
public interface MemberJpaRepository extends JpaRepository<MemberEntity, Long> {

    Optional<MemberEntity> findByUsername(String username);
}
