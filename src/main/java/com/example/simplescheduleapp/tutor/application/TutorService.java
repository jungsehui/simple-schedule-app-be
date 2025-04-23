package com.example.simplescheduleapp.tutor.application;

import com.example.simplescheduleapp.tutor.application.command.TutorSignUpCommand;
import com.example.simplescheduleapp.tutor.domain.entity.Tutor;
import com.example.simplescheduleapp.tutor.domain.service.TutorRegister;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TutorService {

    private final TutorRegister tutorRegister;

    public Long signUpTutor(TutorSignUpCommand tutorSignUpCommand) {
        Tutor tutor = tutorSignUpCommand.toTutor();
        Tutor registereddTutor = tutorRegister.register(tutor);
        return registereddTutor.getId();
    }
}
