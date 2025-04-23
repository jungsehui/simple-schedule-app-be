package com.example.simplescheduleappback.parent.application;

import com.example.simplescheduleappback.parent.application.command.ParentSignUpCommand;
import com.example.simplescheduleappback.parent.domain.entity.Parent;
import com.example.simplescheduleappback.parent.domain.repository.ParentRepository;
import com.example.simplescheduleappback.parent.domain.service.ParentRegister;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ParentService {

    private final ParentRegister parentRegister;
    private final ParentRepository parentRepository;

    public Long signUpParent(ParentSignUpCommand parentSignUpCommand) {
        Parent parent = parentSignUpCommand.toParent();
        Parent registeredParent = parentRegister.register(parent);
        return registeredParent.getId();
    }
}
