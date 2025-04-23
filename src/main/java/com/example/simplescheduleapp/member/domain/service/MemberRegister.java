package com.example.simplescheduleapp.member.domain.service;

import com.example.simplescheduleapp.member.domain.entity.Member;

public interface MemberRegister<T extends Member> {

    T register(T member);
}
