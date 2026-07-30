package com.example.simplescheduleapp.fcm.presentation;

import com.example.simplescheduleapp.common.auth.Auth;
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

    /**
     * FCM 기기 토큰 등록. 역할 제한은 없으나 인증은 필요하다.
     *
     * <p>{@code memberId} 쿼리 파라미터는 하위호환으로 남기되 값을 쓰지 않는다 — 예전에는 이 값으로
     * 남의 계정에 기기 토큰을 붙일 수 있었다(푸시 탈취). 이제 토큰 식별자만 사용한다(ADR-0005).
     */
    @GetMapping(value = "/fcm/token")
    public void addFcmToken(
            @Auth Long authMemberId,
            @RequestParam(required = false) Long memberId, // 레거시 — 수용하되 무시
            @RequestHeader("FCM-TOKEN") String fcmToken
    ) {
        fcmService.addFcmToken(authMemberId, fcmToken);
    }
}
