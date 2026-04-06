package com.example.simplescheduleapp.member.presentation;

import com.example.simplescheduleapp.common.auth.Token;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.member.application.MemberService;
import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.presentation.request.LoginRequest;
import com.example.simplescheduleapp.member.presentation.response.LoginResponse;
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
            @RequestBody LoginRequest request
    ) {
        Member member = memberService.login(request.username(), request.password());
        Token token = tokenService.createToken(member.getId(), member.getMemberRole());
        return ResponseEntity.ok(new LoginResponse(member.getId(), token.accessToken()));
    }
}
