package com.example.simplescheduleapp.member.presentation;

import com.example.simplescheduleapp.common.auth.PublicEndpoint;
import com.example.simplescheduleapp.common.auth.Token;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.member.application.LoginResult;
import com.example.simplescheduleapp.member.application.MemberService;
import com.example.simplescheduleapp.member.presentation.request.LoginRequest;
import com.example.simplescheduleapp.member.presentation.response.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 로그인은 토큰을 얻기 위한 입구이므로 토큰을 요구할 수 없다 (ADR-0005 화이트리스트). */
@PublicEndpoint
@RequiredArgsConstructor
@RestController
public class MemberController {

    private final MemberService memberService;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResult result = memberService.login(request.username(), request.password());
        Token token = tokenService.createToken(result.memberId(), result.role());
        return ResponseEntity.ok(new LoginResponse(result.memberId(), token.accessToken(), result.role().name()));
    }
}
