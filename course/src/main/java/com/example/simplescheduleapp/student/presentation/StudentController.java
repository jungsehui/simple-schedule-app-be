package com.example.simplescheduleapp.student.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.PublicEndpoint;
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

/**
 * 수강생 API.
 *
 * <p>{@code studentId} 쿼리 파라미터는 하위호환을 위해 시그니처에 남아 있지만 <b>값을 쓰지 않는다</b>
 * — 식별자는 토큰에서만 온다(ADR-0005). 클라이언트가 남의 ID를 넣어도 자기 자원만 다룬다.
 */
@RequiredArgsConstructor
@RestController
public class StudentController {

    private final StudentService studentService;
    private final TokenService tokenService;
    private final LectureEnrollmentService lectureEnrollmentService;

    /** 회원가입은 아직 계정이 없는 요청이므로 토큰을 요구할 수 없다 (ADR-0005 화이트리스트). */
    @PublicEndpoint
    @PostMapping("/students")
    public ResponseEntity<LoginResponse> signUpStudent(
            @RequestBody @Valid StudentSignUpRequest request
    ) {
        StudentSignUpCommand command = request.toCommand();
        Long id = studentService.signUpStudent(command);
        Token token = tokenService.createToken(id, Role.STUDENT);
        return ResponseEntity.ok(new LoginResponse(id, token.accessToken(), Role.STUDENT.name()));
    }

    @RequireRole(Role.STUDENT)
    @PostMapping("/lectures/{lectureId}/enrollments")
    public ResponseEntity<LectureEnrollmentResponse> requestLectureEnrollment(
            @Auth Long memberId,
            @RequestParam(required = false) Long studentId, // 레거시 — 수용하되 무시
            @PathVariable Long lectureId
    ) {
        LectureEnrollmentCreateCommand command = LectureEnrollmentCreateCommand.of(memberId, lectureId);
        Long pendingId = lectureEnrollmentService.requestEnrollment(command);
        return ResponseEntity.ok(new LectureEnrollmentResponse(pendingId, lectureId));
    }

    @RequireRole(Role.STUDENT)
    @DeleteMapping("/lectures/{lectureId}/pending-enrollments")
    public ResponseEntity<Void> cancelPendingEnrollment(
            @Auth Long memberId,
            @RequestParam(required = false) Long studentId, // 레거시 — 수용하되 무시
            @PathVariable Long lectureId
    ) {
        PendingLectureEnrollmentCancelCommand command = PendingLectureEnrollmentCancelCommand.of(memberId, lectureId);
        lectureEnrollmentService.cancelPendingLectureEnrollment(command);
        return ResponseEntity.noContent().build();
    }

    @RequireRole(Role.STUDENT)
    @DeleteMapping("/lectures/{lectureId}/enrollments")
    public ResponseEntity<Void> cancelEnrollment(
            @Auth Long memberId,
            @RequestParam(required = false) Long studentId, // 레거시 — 수용하되 무시
            @PathVariable Long lectureId
    ) {
        LectureEnrollmentCancelCommand command = LectureEnrollmentCancelCommand.of(memberId, lectureId);
        lectureEnrollmentService.cancelLectureEnrollment(command);
        return ResponseEntity.noContent().build();
    }
}
