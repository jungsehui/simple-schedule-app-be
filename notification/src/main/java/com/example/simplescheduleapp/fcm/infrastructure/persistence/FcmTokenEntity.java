package com.example.simplescheduleapp.fcm.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@code FcmToken} 도메인의 JPA 영속 모델 (ADR-0004).
 */
@Table(name = "fcm_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class FcmTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcm_id")
    private Long id;

    private Long memberId;
    private String fcmToken;

    public FcmTokenEntity(Long id, Long memberId, String fcmToken) {
        this.id = id;
        this.memberId = memberId;
        this.fcmToken = fcmToken;
    }
}
