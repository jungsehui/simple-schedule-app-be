package com.example.simplescheduleapp.member.infrastructure.persistence;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.application.port.out.AccountRegistrationPort;
import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * {@link AccountRegistrationPort}의 JPA 어댑터.
 *
 * <p>id를 직접 지정하는 엔티티라 Spring Data {@code save}(merge 경로, 선조회 발생) 대신 {@code persist}를 쓰고,
 * 곧바로 flush해 유니크 위반을 이 자리에서 도메인 예외로 번역한다. flush를 커밋 시점으로 미루면
 * 위반이 트랜잭션 밖에서 터져 500이 된다.
 */
@Repository
@RequiredArgsConstructor
public class AccountRegistrationAdapter implements AccountRegistrationPort {

    private final EntityManager entityManager;

    @Override
    public void register(Member member) {
        try {
            entityManager.persist(AccountEntity.activeMember(member.getId(), member.getUsername(), Instant.now()));
            entityManager.flush();
        } catch (ConstraintViolationException e) {
            throw new ApplicationException(MemberExceptionCode.DUPLICATED_USERNAME_PHONE);
        }
    }
}
