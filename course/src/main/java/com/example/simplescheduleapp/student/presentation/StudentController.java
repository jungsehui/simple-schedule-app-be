package com.example.simplescheduleapp.student.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.MemberRole;
import com.example.simplescheduleapp.common.auth.RequireRole;
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
        Token token = tokenService.createToken(id, MemberRole.STUDENT);
        return ResponseEntity.ok(new LoginResponse(id, token.accessToken()));
    }

    @RequireRole(MemberRole.STUDENT)
    @PostMapping("/lectures/{lectureId}/enrollments")
    public ResponseEntity<LectureEnrollmentResponse> requestLectureEnrollment(
            @Auth Long memberId,
            @PathVariable Long lectureId
    ) {
        LectureEnrollmentCreateCommand command = LectureEnrollmentCreateCommand.of(memberId, lectureId);
        Long pendingId = lectureEnrollmentService.requestEnrollment(command);
        return ResponseEntity.ok(new LectureEnrollmentResponse(pendingId, lectureId));
    }

    @RequireRole(MemberRole.STUDENT)
    @DeleteMapping("/lectures/{lectureId}/pending-enrollments")
    public ResponseEntity<Void> cancelPendingEnrollment(
            @Auth Long memberId,
            @PathVariable Long lectureId
    ) {
        PendingLectureEnrollmentCancelCommand command = PendingLectureEnrollmentCancelCommand.of(memberId, lectureId);
        lectureEnrollmentService.cancelPendingLectureEnrollment(command);
        return ResponseEntity.noContent().build();
    }

    @RequireRole(MemberRole.STUDENT)
    @DeleteMapping("/lectures/{lectureId}/enrollments")
    public ResponseEntity<Void> cancelEnrollment(
            @Auth Long memberId,
            @PathVariable Long lectureId
    ) {
        LectureEnrollmentCancelCommand command = LectureEnrollmentCancelCommand.of(memberId, lectureId);
        lectureEnrollmentService.cancelLectureEnrollment(command);
        return ResponseEntity.noContent().build();
    }
}
