package com.example.simplescheduleapp.tutor.application;

import com.example.simplescheduleapp.tutor.application.command.TutorSignUpCommand;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.domain.service.TutorRegister;
import com.example.simplescheduleapp.member.application.port.out.AccountRegistrationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TutorService {

    private final TutorRegister tutorRegister;
    private final AccountRegistrationPort accountRegistrationPort;

    // 회원과 account를 한 트랜잭션에서 만든다. 어느 쪽이 실패해도 둘 다 롤백된다 (ADR-0006 P1)
    @Transactional
    public Long signUpTutor(TutorSignUpCommand command) {
        Tutor tutor = command.toTutor();
        Tutor registeredTutor = tutorRegister.register(tutor);
        accountRegistrationPort.register(registeredTutor);
        return registeredTutor.getId();
    }
}
