package com.example.simplescheduleapp.tutor.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.PublicEndpoint;
import com.example.simplescheduleapp.common.auth.RequireRole;
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

    /** 회원가입은 아직 계정이 없는 요청이므로 토큰을 요구할 수 없다 (ADR-0005 화이트리스트). */
    @PublicEndpoint
    @PostMapping("/tutors")
    public ResponseEntity<LoginResponse> signUpTutor(
            @RequestBody @Valid TutorSignUpRequest request
    ) {
        TutorSignUpCommand command = request.toCommand();
        Long id = tutorService.signUpTutor(command);
        Token token = tokenService.createToken(id, Role.TUTOR);
        return ResponseEntity.ok(new LoginResponse(id, token.accessToken(), Role.TUTOR.name()));
    }

    // 소유권(이 강의의 강사인가)은 서비스가 애그리거트에 위임해 검증한다 — ADR-0005.
    @RequireRole(Role.TUTOR)
    @PostMapping("/enrollments/accept")
    public ResponseEntity<LectureEnrollmentAcceptedResponse> acceptEnrollment(
            @Auth Long memberId,
            @RequestBody PendingAcceptRequest request
    ) {
        PendingAcceptCommand command = request.toCommand(memberId);
        Long lectureEnrollmentId = lectureEnrollmentService.acceptEnrollment(command);
        return ResponseEntity.ok(new LectureEnrollmentAcceptedResponse(lectureEnrollmentId));
    }

    @RequireRole(Role.TUTOR)
    @PostMapping("/enrollments/reject")
    public ResponseEntity<Void> rejectEnrollment(
            @Auth Long memberId,
            @RequestBody PendingRejectRequest request
    ) {
        PendingRejectCommand command = request.toCommand(memberId);
        lectureEnrollmentService.rejectEnrollment(command);
        return ResponseEntity.ok().build();
    }
}
