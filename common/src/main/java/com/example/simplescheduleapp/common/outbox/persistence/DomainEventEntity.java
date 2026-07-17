package com.example.simplescheduleapp.common.outbox.persistence;

import com.example.simplescheduleapp.common.event.EventStatus;
import com.example.simplescheduleapp.common.persistence.SoftDeletedDomain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_DATE_IS_NULL;

/**
 * 아웃박스 레코드 — {@code DomainEvent} 도메인의 JPA 영속 모델 (ADR-0004).
 *
 * <p>SINGLE_TABLE 상속·discriminator("event_type")·소프트삭제·감사 등 영속 관심사를 도메인 대신
 * 여기서 담당한다. 서브타입 엔티티는 각 컨텍스트(course 등)가 제공하며, 도메인↔엔티티 변환은
 * {@link DomainEventPersistenceMapper} 구현이 담당한다(common은 서브타입을 알지 못한다).
 *
 * <p>테이블·컬럼·discriminator는 순수화 이전과 동일하다 — 스키마 불변.
 */
@SQLRestriction(DELETED_DATE_IS_NULL)
@SQLDelete(sql = "UPDATE domain_event SET deleted_date = CURRENT_TIMESTAMP WHERE id = ?")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "event_type")
@Table(name = "domain_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public abstract class DomainEventEntity extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String uuid;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @Column(nullable = false)
    private Long targetDomainId;

    private String failReason;

    private int retryCount = 0;

    protected DomainEventEntity(Long id, String uuid, EventStatus status, Long targetDomainId,
                                String failReason, int retryCount) {
        this.id = id;
        this.uuid = uuid;
        this.status = status;
        this.targetDomainId = targetDomainId;
        this.failReason = failReason;
        this.retryCount = retryCount;
    }
}
