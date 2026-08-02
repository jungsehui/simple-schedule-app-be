package com.example.simplescheduleapp.tutor.infrastructure.persistence;

import com.example.simplescheduleapp.member.domain.Password;
import com.example.simplescheduleapp.tutor.domain.Tutor;

/**
 * 순수 도메인 {@code Tutor} ↔ JPA {@code TutorEntity} 변환 (ADR-0004).
 *
 * <p>다형 조회를 처리하는 {@code MemberMapper}가 위임할 수 있도록 public이다
 * ({@code StudentMapper}와 동일한 이유).
 */
public final class TutorMapper {

    private TutorMapper() {
    }

    public static Tutor toDomain(TutorEntity entity) {
        return new Tutor(
                entity.getId(),
                entity.getUsername(),
                new Password(entity.getPassword()),
                entity.getName(),
                entity.getAge(),
                entity.getPhoneNumber(),
                entity.getCareerPeriod()
        );
    }

    public static TutorEntity toEntity(Tutor domain) {
        return new TutorEntity(
                domain.getId(),
                domain.getUsername(),
                domain.getPassword().getHashedPassword(),
                domain.getName(),
                domain.getAge(),
                domain.getPhoneNumber(),
                domain.getCareerPeriod()
        );
    }
}
