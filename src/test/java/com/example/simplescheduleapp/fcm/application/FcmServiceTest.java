package com.example.simplescheduleapp.fcm.application;

import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.MemberRepository;
import com.example.simplescheduleapp.support.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class FcmServiceTest extends UnitTest {

    @InjectMocks
    FcmService fcmService;

    @Mock
    MemberRepository memberRepository;

    @Mock
    FcmTokenRepository fcmTokenRepository;

    @Test
    void FCM_토큰_저장_성공() {
        Member member = sut.giveMeOne(Member.class);
        String token = "test-fcm-token";

        given(memberRepository.getById(any())).willReturn(member);

        fcmService.addFcmToken(1L, token);

        then(fcmTokenRepository).should().save(any(FcmToken.class));
    }

    @Test
    void sendPushNotification() {
    }
}
