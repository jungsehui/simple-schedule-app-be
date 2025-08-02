package com.example.simplescheduleapp.fcm.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "fcm_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcm_id")
    private Long id;

    private Long memberId;
    private String fcmToken;

    public FcmToken(Long memberId, String fcmToken) {
        this.memberId = memberId;
        this.fcmToken = fcmToken;
    }
}
