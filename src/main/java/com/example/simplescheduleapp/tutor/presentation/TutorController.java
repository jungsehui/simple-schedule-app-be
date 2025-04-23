package com.example.simplescheduleappback.tutor.presentation;

import com.example.simplescheduleappback.common.auth.Token;
import com.example.simplescheduleappback.common.auth.TokenService;
import com.example.simplescheduleappback.member.presentation.response.LoginResponse;
import com.example.simplescheduleappback.tutor.application.TutorService;
import com.example.simplescheduleappback.tutor.application.command.TutorSignUpCommand;
import com.example.simplescheduleappback.tutor.presentation.request.TutorSignUpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RequiredArgsConstructor
@RestController
public class TutorController {

    private final TutorService tutorService;
    private final TokenService tokenService;

    @PostMapping("/tutors")
    public ResponseEntity<LoginResponse> signUpTutor(
            @RequestBody TutorSignUpRequest tutorSignUpRequest
    ) {
        TutorSignUpCommand tutorSignUpCommand = tutorSignUpRequest.toCommand();
        Long id = tutorService.signUpTutor(tutorSignUpCommand);
        Token token = tokenService.createToken(id);
        return ResponseEntity.ok(new LoginResponse(id, token.accessToken()));
    }
}
