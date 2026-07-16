package com.example.simplescheduleapp.fcm.infrastructure.persistence;

import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@code FcmTokenRepository} 포트의 JPA 어댑터 (ADR-0004).
 * <p>{@code getByMemberId}(예외 번역)는 포트의 default 메서드를 그대로 사용한다.
 */
@Repository
@RequiredArgsConstructor
public class FcmTokenRepositoryAdapter implements FcmTokenRepository {

    private final FcmTokenJpaRepository jpaRepository;

    @Override
    public FcmToken save(FcmToken fcmToken) {
        return FcmTokenMapper.toDomain(jpaRepository.save(FcmTokenMapper.toEntity(fcmToken)));
    }

    @Override
    public Optional<FcmToken> findByMemberId(Long memberId) {
        return jpaRepository.findByMemberId(memberId).map(FcmTokenMapper::toDomain);
    }
}
