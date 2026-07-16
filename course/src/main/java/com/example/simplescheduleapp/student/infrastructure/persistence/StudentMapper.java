package com.example.simplescheduleapp.student.infrastructure.persistence;

import com.example.simplescheduleapp.member.domain.Password;
import com.example.simplescheduleapp.student.domain.Student;

/**
 * 순수 도메인 {@code Student} ↔ JPA {@code StudentEntity} 변환 (ADR-0004).
 *
 * <p>템플릿(FcmTokenMapper)과 달리 public이다: 회원 계층이 JOINED 상속이라 다형 조회
 * ({@code MemberRepository.findByUsername})를 처리하는 {@code MemberMapper}가 구체 서브타입
 * 변환을 이 매퍼에 위임해야 하기 때문이다(중복 매핑 방지).
 */
public final class StudentMapper {

    private StudentMapper() {
    }

    public static Student toDomain(StudentEntity entity) {
        return new Student(
                entity.getId(),
                entity.getUsername(),
                new Password(entity.getPassword()),
                entity.getName(),
                entity.getAge(),
                entity.getPhoneNumber(),
                entity.getSchool()
        );
    }

    public static StudentEntity toEntity(Student domain) {
        return new StudentEntity(
                domain.getId(),
                domain.getUsername(),
                domain.getPassword().getHashedPassword(),
                domain.getName(),
                domain.getAge(),
                domain.getPhoneNumber(),
                domain.getSchool()
        );
    }
}
