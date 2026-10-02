package com.example.simplescheduleapp.parent.application;

import com.example.simplescheduleapp.parent.application.command.ParentSignUpCommand;
import com.example.simplescheduleapp.parent.domain.Parent;
import com.example.simplescheduleapp.parent.domain.service.ParentRegister;
import com.example.simplescheduleapp.member.application.port.out.AccountRegistrationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ParentService {

    private final ParentRegister parentRegister;
    private final AccountRegistrationPort accountRegistrationPort;

    // 회원과 account를 한 트랜잭션에서 만든다. 어느 쪽이 실패해도 둘 다 롤백된다 (ADR-0006 P1)
    @Transactional
    public Long signUpParent(ParentSignUpCommand command) {
        Parent parent = command.toParent();
        Parent registeredParent = parentRegister.register(parent);
        accountRegistrationPort.register(registeredParent);
        return registeredParent.getId();
    }
}
