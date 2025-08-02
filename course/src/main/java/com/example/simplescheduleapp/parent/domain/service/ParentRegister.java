package com.example.simplescheduleapp.parent.domain.service;

import com.example.simplescheduleapp.member.domain.service.MemberRegister;
import com.example.simplescheduleapp.parent.domain.Parent;
import com.example.simplescheduleapp.parent.domain.ParentRepository;
import org.springframework.stereotype.Component;

@Component
public class ParentRegister extends MemberRegister<Parent, ParentRepository> {

    public ParentRegister(ParentRepository parentRepository) {
        super(parentRepository);
    }
}
