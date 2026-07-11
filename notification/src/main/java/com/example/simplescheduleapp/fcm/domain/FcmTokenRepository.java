package com.example.simplescheduleapp.fcm.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.fcm.exception.FcmTokenExceptionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    default FcmToken getByMemberId(Long memberId) {
        return findByMemberId(memberId).orElseThrow(() -> new ApplicationException(FcmTokenExceptionCode.FCM_TOKEN_NOT_FOUND));
    }

    Optional<FcmToken> findByMemberId(Long memberId);
}
