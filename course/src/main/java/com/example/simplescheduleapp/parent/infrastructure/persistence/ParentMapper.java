package com.example.simplescheduleapp.parent.infrastructure.persistence;

import com.example.simplescheduleapp.member.domain.Password;
import com.example.simplescheduleapp.parent.domain.Parent;

/**
 * 순수 도메인 {@code Parent} ↔ JPA {@code ParentEntity} 변환 (ADR-0004).
 *
 * <p>다형 조회를 처리하는 {@code MemberMapper}가 위임할 수 있도록 public이다
 * ({@code StudentMapper}와 동일한 이유).
 */
public final class ParentMapper {

    private ParentMapper() {
    }

    public static Parent toDomain(ParentEntity entity) {
        return new Parent(
                entity.getId(),
                entity.getUsername(),
                new Password(entity.getPassword()),
                entity.getName(),
                entity.getAge(),
                entity.getPhoneNumber(),
                entity.getChildrenNumber()
        );
    }

    public static ParentEntity toEntity(Parent domain) {
        return new ParentEntity(
                domain.getId(),
                domain.getUsername(),
                domain.getPassword().getHashedPassword(),
                domain.getName(),
                domain.getAge(),
                domain.getPhoneNumber(),
                domain.getChildrenNumber()
        );
    }
}
