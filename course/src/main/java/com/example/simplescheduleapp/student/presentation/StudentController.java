package com.example.simplescheduleapp.student.presentation;

import com.example.simplescheduleapp.common.auth.Token;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.lecture.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.application.command.LectureEnrollmentCancelCommand;
import com.example.simplescheduleapp.lecture.application.command.LectureEnrollmentCreateCommand;
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
            @RequestBody @Valid StudentSignUpRequest studentSignUpRequest
    ) {
        StudentSignUpCommand studentSignUpCommand = studentSignUpRequest.toCommand();
        Long id = studentService.signUpStudent(studentSignUpCommand);
        Token token = tokenService.createToken(id);
        return ResponseEntity.ok(new LoginResponse(id, token.accessToken()));
    }

    @PostMapping("/lectures/{lectureId}/enrollments")
    public ResponseEntity<LectureEnrollmentResponse> requestLectureEnrollment(
            @PathVariable Long lectureId,
            @RequestParam Long studentId
    ) {
        LectureEnrollmentCreateCommand command = LectureEnrollmentCreateCommand.of(studentId, lectureId);
        Long pendingId = lectureEnrollmentService.requestEnrollment(command);
        return ResponseEntity.ok(new LectureEnrollmentResponse(pendingId));
    }

    @DeleteMapping("/lectures/{lectureId}/enrollments")
    public ResponseEntity<Void> cancelEnrollment(
            @RequestParam Long studentId,
            @PathVariable Long lectureId
    ) {
        LectureEnrollmentCancelCommand command = LectureEnrollmentCancelCommand.of(studentId, lectureId);
        lectureEnrollmentService.cancelEnrollment(command);
        return ResponseEntity.noContent().build();
    }
}
