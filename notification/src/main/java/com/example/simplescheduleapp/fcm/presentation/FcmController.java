package com.example.simplescheduleapp.fcm.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
import com.example.simplescheduleapp.common.auth.AuthIdentities;
import com.example.simplescheduleapp.fcm.application.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class FcmController {

    private final FcmService fcmService;

    // Phase 3a 듀얼리드: 토큰 식별자 우선, memberId 파라미터는 레거시 폴백 (3b에서 제거 예정)
    @GetMapping(value = "/fcm/token")
    public void addFcmToken(
            @Auth(required = false) Long authMemberId,
            @RequestParam(required = false) Long memberId,
            @RequestHeader("FCM-TOKEN") String fcmToken
    ){
        fcmService.addFcmToken(AuthIdentities.resolve(authMemberId, memberId), fcmToken);
    }
}
