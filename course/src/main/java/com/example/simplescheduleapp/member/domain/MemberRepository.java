package com.example.simplescheduleapp.member.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    default Member getById(Long id) {
        return findById(id).orElseThrow(() -> new ApplicationException(MemberExceptionCode.MEMBER_NOT_FOUND));
    }

    default Member getByUsername(String username) {
        return findByUsername(username).orElseThrow(() -> new ApplicationException(MemberExceptionCode.INVALID_USERNAME_PASSWORD));
    }

    Optional<Member> findById(Long id);

    Optional<Member> findByUsername(String username);
}
