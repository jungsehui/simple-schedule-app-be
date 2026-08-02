package com.example.simplescheduleapp.student.domain;

import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.Password;
import lombok.Getter;

/**
 * 학생 — 순수 도메인 모델 (ADR-0004).
 *
 * <p>JPA/프레임워크 의존 0. 테이블(student)·discriminator("STUDENT")·조인 컬럼 등 영속 관심사는
 * {@code infrastructure/persistence}의 {@code StudentEntity}가 담당한다.
 */
@Getter
public class Student extends Member {

    private final String school;

    /** 신규 가입용 — 평문 비밀번호를 해싱해 보관한다. */
    public Student(String username, String password, String name, int age, String phoneNumber, String school) {
        super(username, password, name, age, phoneNumber);
        this.school = school;
    }

    /** DB 복원용 재구성 생성자 — 매퍼 전용. */
    public Student(Long id, String username, Password password, String name, int age, String phoneNumber, String school) {
        super(id, username, password, name, age, phoneNumber);
        this.school = school;
    }

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }
}
