package com.example.simplescheduleapp.student.presentation;

import com.example.simplescheduleapp.student.application.StudentService;
import com.example.simplescheduleapp.student.application.command.StudentSignUpCommand;
import com.example.simplescheduleapp.student.presentation.request.StudentSignUpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RequiredArgsConstructor
@RestController
public class StudentController {

    private final StudentService studentService;

    @PostMapping("/students")
    public ResponseEntity<Void> signUpStudent(@RequestBody StudentSignUpRequest studentSignUpRequest) {
        StudentSignUpCommand studentSignUpCommand = studentSignUpRequest.toCommand();
        Long id = studentService.signUpStudent(studentSignUpCommand);
        return ResponseEntity.created(URI.create("/students/" + id)).build();
    }
}
