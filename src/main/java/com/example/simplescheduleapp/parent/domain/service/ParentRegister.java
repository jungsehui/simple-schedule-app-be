package com.example.simplescheduleappback.parent.domain.service;

import com.example.simplescheduleappback.common.exception.ApplicationException;
import com.example.simplescheduleappback.member.domain.service.MemberRegister;
import com.example.simplescheduleappback.member.exception.MemberExceptionCode;
import com.example.simplescheduleappback.parent.domain.entity.Parent;
import com.example.simplescheduleappback.parent.domain.repository.ParentRepository;
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
