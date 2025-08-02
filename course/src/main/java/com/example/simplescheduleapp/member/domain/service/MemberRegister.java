package com.example.simplescheduleapp.member.domain.service;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;

import static com.example.simplescheduleapp.member.exception.MemberExceptionCode.DUPLICATED_USERNAME_PHONE;

@RequiredArgsConstructor
public abstract class MemberRegister<T extends Member, R extends JpaRepository<T, ?>> {

    private final R memberRepository;

    public T register(T member) {
        try {
            return memberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(DUPLICATED_USERNAME_PHONE);
        }
    }
}
