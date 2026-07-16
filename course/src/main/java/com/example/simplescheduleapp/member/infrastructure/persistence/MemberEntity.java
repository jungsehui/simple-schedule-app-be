package com.example.simplescheduleapp.member.infrastructure.persistence;

import com.example.simplescheduleapp.common.domain.SoftDeletedDomain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_DATE_IS_NULL;

/**
 * {@code Member} 도메인 계층의 JPA 영속 모델 (ADR-0004).
 *
 * <p>테이블(member)·JOINED 상속·discriminator("role")·소프트삭제 SQL·감사 타임스탬프 등
 * 저장 관심사만 담당하고, 도메인 불변식/행위(login 등)는 순수 {@code Member}가 보유한다.
 * 매핑은 순수화 전 {@code Member} @Entity에서 그대로 이관된 것으로 운영 스키마는 불변이다.
 *
 * <p>비밀번호는 도메인의 {@code Password} VO 대신 해시 문자열을 그대로 {@code password}
 * 컬럼에 보유한다(컬럼 동일 — @Embedded 시절과 스키마가 같다). 변환은 매퍼가 한다.
 */
@SQLRestriction(DELETED_DATE_IS_NULL)
@SQLDelete(sql = "UPDATE member SET deleted_date = CURRENT_TIMESTAMP WHERE member_id = ?")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role")
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public abstract class MemberEntity extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "age", nullable = false)
    private int age;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    protected MemberEntity(Long id, String username, String password, String name, int age, String phoneNumber) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
        this.age = age;
        this.phoneNumber = phoneNumber;
    }
}
