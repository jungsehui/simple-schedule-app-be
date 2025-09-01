package com.example.simplescheduleapp.common.event;

import com.example.simplescheduleapp.common.domain.SoftDeletedDomain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_AT_IS_NULL;

@SQLRestriction(DELETED_AT_IS_NULL)
@SQLDelete(sql = "UPDATE domain_event SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "event_type")
@Table(name = "domain_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public abstract class DomainEvent extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String uuid;

    @Enumerated(EnumType.STRING)
    private EventState state;

    @Column(nullable = false)
    private Long targetDomainId; // 강의 ID 등

    private String failReason;

    protected DomainEvent(Long targetDomainId) {
        this.uuid = UUID.randomUUID().toString();
        this.state = EventState.INIT;
        this.targetDomainId = targetDomainId;
    }

    protected DomainEvent(String uuid, EventState state, Long targetDomainId) {
        this.uuid = uuid;
        this.state = state;
        this.targetDomainId = targetDomainId;
    }

    public void regenerateUuid() {
        this.uuid = UUID.randomUUID().toString();
    }

    public void produceSuccess() {
        this.state = EventState.PRODUCE_SUCCESS;
    }

    public void produceFail(Throwable e) {
        this.state = EventState.PRODUCE_FAIL;
        this.failReason = e.getMessage();
    }

    public abstract String getTopic();
}
