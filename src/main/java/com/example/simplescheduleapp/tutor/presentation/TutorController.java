package com.example.simplescheduleapp.tutor.presentation;

import com.example.simplescheduleapp.common.auth.Token;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.lecture.presentation.request.PendingCheckRequest;
import com.example.simplescheduleapp.member.presentation.response.LoginResponse;
import com.example.simplescheduleapp.tutor.application.TutorService;
import com.example.simplescheduleapp.tutor.application.command.TutorSignUpCommand;
import com.example.simplescheduleapp.tutor.presentation.request.TutorSignUpRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class TutorController {

    private final TutorService tutorService;
    private final TokenService tokenService;

    @PostMapping("/tutors")
    public ResponseEntity<LoginResponse> signUpTutor(
            @RequestBody @Valid TutorSignUpRequest tutorSignUpRequest
    ) {
        TutorSignUpCommand tutorSignUpCommand = tutorSignUpRequest.toCommand();
        Long id = tutorService.signUpTutor(tutorSignUpCommand);
        Token token = tokenService.createToken(id);
        return ResponseEntity.ok(new LoginResponse(id, token.accessToken()));
    }

    @PostMapping("/enrollments/accept")
    public ResponseEntity<Long> acceptEnrollment(
            @RequestBody PendingCheckRequest request
    ) {
        Long lectureEnrollmentId = tutorService.acceptEnrollment(request.pendingId());
        return ResponseEntity.ok(lectureEnrollmentId);
    }

    @PostMapping("/enrollments/reject")
    public ResponseEntity<Void> rejectEnrollment(
            @RequestBody PendingCheckRequest request
    ) {
        tutorService.rejectEnrollment(request.pendingId());
        return ResponseEntity.ok().build();
    }
}
