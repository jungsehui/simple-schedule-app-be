package com.example.simplescheduleapp.common.event;

import com.example.simplescheduleapp.common.entity.SoftDeletedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_AT_IS_NULL;

@SQLRestriction(DELETED_AT_IS_NULL)
@SQLDelete(sql = "UPDATE domain_event SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "event_type")
@Table(name = "domain_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public abstract class DomainEvent extends SoftDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String uuid;

    @Enumerated(value = EnumType.STRING)
    private EventState eventState;

    @Column(nullable = false)
    private Long targetDomainId;

    private String failReason;
}
