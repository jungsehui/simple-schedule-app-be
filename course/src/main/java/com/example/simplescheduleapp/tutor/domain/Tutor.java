package com.example.simplescheduleapp.tutor.domain;

import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.Password;
import lombok.Getter;

/**
 * 강사 — 순수 도메인 모델 (ADR-0004).
 *
 * <p>JPA/프레임워크 의존 0. 테이블(tutor)·discriminator("TUTOR")·조인 컬럼 등 영속 관심사는
 * {@code infrastructure/persistence}의 {@code TutorEntity}가 담당한다.
 */
@Getter
public class Tutor extends Member {

    private final int careerPeriod;

    /** 신규 가입용 — 평문 비밀번호를 해싱해 보관한다. */
    public Tutor(String username, String password, String name, int age, String phoneNumber, int careerPeriod) {
        super(username, password, name, age, phoneNumber);
        this.careerPeriod = careerPeriod;
    }

    /** DB 복원용 재구성 생성자 — 매퍼 전용. */
    public Tutor(Long id, String username, Password password, String name, int age, String phoneNumber, int careerPeriod) {
        super(id, username, password, name, age, phoneNumber);
        this.careerPeriod = careerPeriod;
    }

    @Override
    public Role getRole() {
        return Role.TUTOR;
    }
}
