package com.example.simplescheduleapp.tutor.presentation;

import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.common.auth.Token;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.lecture.general.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.general.application.command.PendingAcceptCommand;
import com.example.simplescheduleapp.lecture.general.application.command.PendingRejectCommand;
import com.example.simplescheduleapp.tutor.presentation.request.PendingAcceptRequest;
import com.example.simplescheduleapp.member.presentation.response.LoginResponse;
import com.example.simplescheduleapp.tutor.application.TutorService;
import com.example.simplescheduleapp.tutor.application.command.TutorSignUpCommand;
import com.example.simplescheduleapp.tutor.presentation.request.PendingRejectRequest;
import com.example.simplescheduleapp.tutor.presentation.request.TutorSignUpRequest;
import com.example.simplescheduleapp.tutor.presentation.response.LectureEnrollmentAcceptedResponse;
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
    private final LectureEnrollmentService lectureEnrollmentService;

    @PostMapping("/tutors")
    public ResponseEntity<LoginResponse> signUpTutor(
            @RequestBody @Valid TutorSignUpRequest request
    ) {
        TutorSignUpCommand command = request.toCommand();
        Long id = tutorService.signUpTutor(command);
        Token token = tokenService.createToken(id, Role.TUTOR);
        return ResponseEntity.ok(new LoginResponse(id, token.accessToken(), Role.TUTOR.name()));
    }

    @PostMapping("/enrollments/accept")
    public ResponseEntity<LectureEnrollmentAcceptedResponse> acceptEnrollment(
            @RequestBody PendingAcceptRequest request
    ) {
        PendingAcceptCommand command = request.toCommand();
        Long lectureEnrollmentId = lectureEnrollmentService.acceptEnrollment(command);
        return ResponseEntity.ok(new LectureEnrollmentAcceptedResponse(lectureEnrollmentId));
    }

    @PostMapping("/enrollments/reject")
    public ResponseEntity<Void> rejectEnrollment(
            @RequestBody PendingRejectRequest request
    ) {
        PendingRejectCommand command = request.toCommand();
        lectureEnrollmentService.rejectEnrollment(command);
        return ResponseEntity.ok().build();
    }
}
