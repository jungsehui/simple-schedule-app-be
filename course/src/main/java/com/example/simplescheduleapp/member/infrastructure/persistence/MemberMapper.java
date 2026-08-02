package com.example.simplescheduleapp.member.infrastructure.persistence;

import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.parent.domain.Parent;
import com.example.simplescheduleapp.parent.infrastructure.persistence.ParentEntity;
import com.example.simplescheduleapp.parent.infrastructure.persistence.ParentMapper;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.infrastructure.persistence.StudentEntity;
import com.example.simplescheduleapp.student.infrastructure.persistence.StudentMapper;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import com.example.simplescheduleapp.tutor.infrastructure.persistence.TutorEntity;
import com.example.simplescheduleapp.tutor.infrastructure.persistence.TutorMapper;

/**
 * 순수 도메인 {@code Member} ↔ JPA {@code MemberEntity} 변환 (ADR-0004).
 *
 * <p>회원은 JOINED 상속(discriminator "role")이라 <b>다형 디스패치</b>가 핵심이다:
 * {@code MemberJpaRepository}가 다형 조회를 하면 Hibernate가 구체 서브타입 엔티티
 * (StudentEntity/TutorEntity/ParentEntity)를 반환하므로, 이를 대응하는 구체 도메인
 * 서브타입으로 변환해야 {@code Member.getRole()}이 정확해진다(로그인이 이에 의존).
 * 실제 필드 변환은 각 컨텍스트의 매퍼에 위임한다.
 */
final class MemberMapper {

    private MemberMapper() {
    }

    static Member toDomain(MemberEntity entity) {
        return switch (entity) {
            case StudentEntity student -> StudentMapper.toDomain(student);
            case TutorEntity tutor -> TutorMapper.toDomain(tutor);
            case ParentEntity parent -> ParentMapper.toDomain(parent);
            default -> throw new IllegalStateException(
                    "알 수 없는 회원 서브타입 엔티티: " + entity.getClass().getName());
        };
    }

    static MemberEntity toEntity(Member domain) {
        return switch (domain) {
            case Student student -> StudentMapper.toEntity(student);
            case Tutor tutor -> TutorMapper.toEntity(tutor);
            case Parent parent -> ParentMapper.toEntity(parent);
            default -> throw new IllegalStateException(
                    "알 수 없는 회원 서브타입 도메인: " + domain.getClass().getName());
        };
    }
}
