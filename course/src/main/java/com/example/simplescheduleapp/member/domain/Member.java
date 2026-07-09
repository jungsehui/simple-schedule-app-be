package com.example.simplescheduleapp.member.domain;

import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.common.domain.SoftDeletedDomain;
import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_DATE_IS_NULL;

@SQLRestriction(DELETED_DATE_IS_NULL)
@SQLDelete(sql = "UPDATE member SET deleted_date = CURRENT_TIMESTAMP WHERE member_id = ?")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role")
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public abstract class Member extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    @Embedded
    private Password password;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "age", nullable = false)
    private int age;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    public Member(String username, String password, String name, int age, String phoneNumber) {
        this.username = username;
        this.password = Password.hashPassword(password);
        this.name = name;
        this.age = age;
        this.phoneNumber = phoneNumber;
    }

    public void login(String plainPassword) {
        boolean same = this.password.match(plainPassword);
        if (!same) {
            throw new ApplicationException(MemberExceptionCode.INVALID_USERNAME_PASSWORD);
        }
    }

    /** 구체 서브타입(Student/Tutor/Parent)이 자신의 역할을 반환한다. discriminator와 대응. */
    public abstract Role getRole();
}
