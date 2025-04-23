package com.example.simplescheduleappback.member.presentation;

import com.example.simplescheduleappback.common.auth.Token;
import com.example.simplescheduleappback.common.auth.TokenService;
import com.example.simplescheduleappback.member.application.MemberService;
import com.example.simplescheduleappback.member.presentation.request.LoginRequest;
import com.example.simplescheduleappback.member.presentation.response.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class MemberController {

    private final MemberService memberService;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest loginRequest
    ) {
        Long id = memberService.login(loginRequest.username(), loginRequest.password());
        Token token = tokenService.createToken(id);
        return ResponseEntity.ok(new LoginResponse(id, token.accessToken()));
    }
}
