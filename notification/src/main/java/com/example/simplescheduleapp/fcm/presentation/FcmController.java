package com.example.simplescheduleapp.fcm.presentation;

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

    @GetMapping(value = "/fcm/token")
    public void addFcmToken(
            @RequestParam Long memberId,
            @RequestHeader("FCM-TOKEN") String fcmToken
    ){
        fcmService.addFcmToken(memberId, fcmToken);
    }
}
