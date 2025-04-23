package com.example.simplescheduleappback.member.domain.service;

import com.example.simplescheduleappback.member.domain.entity.Member;
import com.example.simplescheduleappback.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

public interface MemberRegister<T extends Member> {

    T register(T member);
}
