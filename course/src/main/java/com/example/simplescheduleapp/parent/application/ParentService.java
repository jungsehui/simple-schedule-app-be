package com.example.simplescheduleapp.parent.application;

import com.example.simplescheduleapp.parent.application.command.ParentSignUpCommand;
import com.example.simplescheduleapp.parent.domain.Parent;
import com.example.simplescheduleapp.parent.domain.service.ParentRegister;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ParentService {

    private final ParentRegister parentRegister;

    public Long signUpParent(ParentSignUpCommand parentSignUpCommand) {
        Parent parent = parentSignUpCommand.toParent();
        Parent registeredParent = parentRegister.register(parent);
        return registeredParent.getId();
    }
}
