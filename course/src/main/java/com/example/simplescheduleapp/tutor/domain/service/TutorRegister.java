package com.example.simplescheduleapp.tutor.domain.service;

import com.example.simplescheduleapp.member.domain.service.MemberRegister;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import org.springframework.stereotype.Component;

@Component
public class TutorRegister extends MemberRegister<Tutor> {

    public TutorRegister(TutorRepository memberRepository) {
        super(memberRepository);
    }
}
