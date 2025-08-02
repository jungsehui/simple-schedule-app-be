package com.example.simplescheduleapp.fcm.domain;

import com.example.simplescheduleapp.member.domain.Member;
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

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    private String fcmToken;

    public FcmToken(Member member, String fcmToken) {
        this.member = member;
        this.fcmToken = fcmToken;
    }
}
