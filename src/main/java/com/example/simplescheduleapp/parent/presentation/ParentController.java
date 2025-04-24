package com.example.simplescheduleapp.parent.presentation;

import com.example.simplescheduleapp.common.auth.Token;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.member.presentation.response.LoginResponse;
import com.example.simplescheduleapp.parent.application.ParentService;
import com.example.simplescheduleapp.parent.application.command.ParentSignUpCommand;
import com.example.simplescheduleapp.parent.presentation.request.ParentSignUpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class ParentController {

    private final ParentService parentService;
    private final TokenService tokenService;

    @PostMapping("/parents")
    public ResponseEntity<LoginResponse> signUpParent(
            @RequestBody ParentSignUpRequest parentSignUpRequest
    ) {
        ParentSignUpCommand parentSignUpCommand = parentSignUpRequest.toCommand();
        Long id = parentService.signUpParent(parentSignUpCommand);
        Token token = tokenService.createToken(id);
        return ResponseEntity.ok(new LoginResponse(id, token.accessToken()));
    }
}
