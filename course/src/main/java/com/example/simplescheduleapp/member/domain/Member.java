package com.example.simplescheduleapp.member.domain;

import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import lombok.Getter;

/**
 * 회원 — 순수 도메인 모델 (ADR-0004).
 *
 * <p>JPA/프레임워크 의존 0. 테이블(member)·JOINED 상속·discriminator("role")·소프트삭제 등
 * 영속 관심사는 {@code infrastructure/persistence}의 {@code MemberEntity}가 담당하고,
 * 변환은 매퍼가 한다. 감사(생성/수정 일시)·소프트삭제도 영속 관심사이므로 도메인은
 * {@code SoftDeletedDomain}을 상속하지 않는다.
 *
 * <p>구체 서브타입(Student/Tutor/Parent)이 자신의 역할을 부여하며, 이는 엔티티의
 * discriminator 값과 1:1로 대응한다.
 */
@Getter
public abstract class Member {

    private final Long id;
    private final String username;
    /** 레거시 해시가 로그인 시 bcrypt로 승급될 수 있어 가변이다({@link #login} 참고). */
    private Password password;
    private final String name;
    private final int age;
    private final String phoneNumber;

    /** 신규 가입용 — 평문 비밀번호를 해싱해 보관한다. username은 검증·정규화된다. */
    protected Member(String username, String password, String name, int age, String phoneNumber) {
        this(null, Username.of(username).value(), Password.hashPassword(password), name, age, phoneNumber);
    }

    /** DB 복원용 재구성 생성자 — 매퍼 전용(이미 해싱된 비밀번호를 그대로 받는다). */
    protected Member(Long id, String username, Password password, String name, int age, String phoneNumber) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
        this.age = age;
        this.phoneNumber = phoneNumber;
    }

    /**
     * 인증한다. 레거시(무염 SHA-256) 해시로 인증에 성공하면 <b>그 자리에서 bcrypt로 승급</b>한다 —
     * 기존 사용자를 끊지 않는 점진 마이그레이션(ADR-0004, 구조 리뷰 HIGH 보안 수정).
     *
     * @return 비밀번호가 승급되어 저장이 필요하면 {@code true}. 호출자(애플리케이션 계층)가
     *         승급된 경우에만 저장한다.
     */
    public boolean login(String plainPassword) {
        if (!this.password.match(plainPassword)) {
            throw new ApplicationException(MemberExceptionCode.INVALID_USERNAME_PASSWORD);
        }
        if (this.password.isLegacy()) {
            this.password = Password.hashPassword(plainPassword);
            return true;
        }
        return false;
    }

    /** 구체 서브타입(Student/Tutor/Parent)이 자신의 역할을 반환한다. discriminator와 대응. */
    public abstract Role getRole();
}
