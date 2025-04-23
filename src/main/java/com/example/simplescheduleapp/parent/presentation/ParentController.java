package com.example.simplescheduleapp.parent.presentation;

import com.example.simplescheduleapp.parent.application.ParentService;
import com.example.simplescheduleapp.parent.application.command.ParentSignUpCommand;
import com.example.simplescheduleapp.parent.presentation.request.ParentSignUpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RequiredArgsConstructor
@RestController
public class ParentController {

    private final ParentService parentService;

    @PostMapping("/parents")
    public ResponseEntity<Void> signUpParent( @RequestBody ParentSignUpRequest parentSignUpRequest) {
        ParentSignUpCommand parentSignUpCommand = parentSignUpRequest.toCommand();
        Long id =parentService.signUpParent(parentSignUpCommand);
        return ResponseEntity.created(URI.create("/parents/" + id)).build();
    }
}
