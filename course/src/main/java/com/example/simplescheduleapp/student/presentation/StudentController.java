package com.example.simplescheduleapp.student.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.AuthIdentities;
import com.example.simplescheduleapp.common.auth.RequireRole;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.common.auth.Token;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.lecture.general.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.general.application.command.LectureEnrollmentCancelCommand;
import com.example.simplescheduleapp.lecture.general.application.command.LectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.lecture.general.application.command.PendingLectureEnrollmentCancelCommand;
import com.example.simplescheduleapp.member.presentation.response.LoginResponse;
import com.example.simplescheduleapp.student.application.StudentService;
import com.example.simplescheduleapp.student.application.command.StudentSignUpCommand;
import com.example.simplescheduleapp.student.presentation.request.StudentSignUpRequest;
import com.example.simplescheduleapp.student.presentation.response.LectureEnrollmentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class StudentController {

    private final StudentService studentService;
    private final TokenService tokenService;
    private final LectureEnrollmentService lectureEnrollmentService;

    @PostMapping("/students")
    public ResponseEntity<LoginResponse> signUpStudent(
            @RequestBody @Valid StudentSignUpRequest request
    ) {
        StudentSignUpCommand command = request.toCommand();
        Long id = studentService.signUpStudent(command);
        Token token = tokenService.createToken(id, Role.STUDENT);
        return ResponseEntity.ok(new LoginResponse(id, token.accessToken(), Role.STUDENT.name()));
    }

    // Phase 3a 듀얼리드: 토큰 식별자 우선, studentId 파라미터는 레거시 폴백 (3b에서 제거 예정)
    @RequireRole(Role.STUDENT)
    @PostMapping("/lectures/{lectureId}/enrollments")
    public ResponseEntity<LectureEnrollmentResponse> requestLectureEnrollment(
            @Auth(required = false) Long memberId,
            @RequestParam(required = false) Long studentId,
            @PathVariable Long lectureId
    ) {
        LectureEnrollmentCreateCommand command = LectureEnrollmentCreateCommand.of(AuthIdentities.resolve(memberId, studentId), lectureId);
        Long pendingId = lectureEnrollmentService.requestEnrollment(command);
        return ResponseEntity.ok(new LectureEnrollmentResponse(pendingId, lectureId));
    }

    @RequireRole(Role.STUDENT)
    @DeleteMapping("/lectures/{lectureId}/pending-enrollments")
    public ResponseEntity<Void> cancelPendingEnrollment(
            @Auth(required = false) Long memberId,
            @RequestParam(required = false) Long studentId,
            @PathVariable Long lectureId
    ) {
        PendingLectureEnrollmentCancelCommand command = PendingLectureEnrollmentCancelCommand.of(AuthIdentities.resolve(memberId, studentId), lectureId);
        lectureEnrollmentService.cancelPendingLectureEnrollment(command);
        return ResponseEntity.noContent().build();
    }

    @RequireRole(Role.STUDENT)
    @DeleteMapping("/lectures/{lectureId}/enrollments")
    public ResponseEntity<Void> cancelEnrollment(
            @Auth(required = false) Long memberId,
            @RequestParam(required = false) Long studentId,
            @PathVariable Long lectureId
    ) {
        LectureEnrollmentCancelCommand command = LectureEnrollmentCancelCommand.of(AuthIdentities.resolve(memberId, studentId), lectureId);
        lectureEnrollmentService.cancelLectureEnrollment(command);
        return ResponseEntity.noContent().build();
    }
}
