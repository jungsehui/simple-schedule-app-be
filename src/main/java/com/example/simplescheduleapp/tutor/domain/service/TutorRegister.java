package com.example.simplescheduleapp.tutor.domain.service;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.domain.service.MemberRegister;
import com.example.simplescheduleapp.tutor.domain.entity.Tutor;
import com.example.simplescheduleapp.tutor.domain.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import static com.example.simplescheduleapp.member.exception.MemberExceptionCode.DUPLICATED_USERNAME_PHONE;

@RequiredArgsConstructor
@Component
public class TutorRegister implements MemberRegister<Tutor> {

    private final TutorRepository tutorRepository;

    @Override
    public Tutor register(Tutor tutor) {
        try {
            return tutorRepository.save(tutor);
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(DUPLICATED_USERNAME_PHONE);
        }
    }
}
