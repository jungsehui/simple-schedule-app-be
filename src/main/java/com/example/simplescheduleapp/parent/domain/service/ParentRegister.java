package com.example.simplescheduleapp.parent.domain.service;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.domain.service.MemberRegister;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import com.example.simplescheduleapp.parent.domain.entity.Parent;
import com.example.simplescheduleapp.parent.domain.repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ParentRegister implements MemberRegister<Parent> {

    private final ParentRepository parentRepository;

    @Override
    public Parent register(Parent parent) {
        try {
            return parentRepository.save(parent);
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(MemberExceptionCode.DUPLICATED_USERNAME_PHONE);
        }
    }
}
