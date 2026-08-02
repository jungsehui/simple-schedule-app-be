package com.example.simplescheduleapp.fcm.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface FcmTokenJpaRepository extends JpaRepository<FcmTokenEntity, Long> {

    Optional<FcmTokenEntity> findByMemberId(Long memberId);
}
