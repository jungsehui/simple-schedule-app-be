package com.example.simplescheduleapp.parent.presentation;

import com.example.simplescheduleapp.common.auth.PublicEndpoint;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.common.auth.Token;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.member.presentation.response.LoginResponse;
import com.example.simplescheduleapp.parent.application.ParentService;
import com.example.simplescheduleapp.parent.application.command.ParentSignUpCommand;
import com.example.simplescheduleapp.parent.presentation.request.ParentSignUpRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 회원가입은 아직 계정이 없는 요청이므로 토큰을 요구할 수 없다 (ADR-0005 화이트리스트). */
@PublicEndpoint
@RequiredArgsConstructor
@RestController
public class ParentController {

    private final ParentService parentService;
    private final TokenService tokenService;

    @PostMapping("/parents")
    public ResponseEntity<LoginResponse> signUpParent(
            @RequestBody @Valid ParentSignUpRequest request
    ) {
        ParentSignUpCommand command = request.toCommand();
        Long id = parentService.signUpParent(command);
        Token token = tokenService.createToken(id, Role.PARENT);
        return ResponseEntity.ok(new LoginResponse(id, token.accessToken(), Role.PARENT.name()));
    }
}
