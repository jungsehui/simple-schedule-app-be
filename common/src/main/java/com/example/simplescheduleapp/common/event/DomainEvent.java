package com.example.simplescheduleapp.common.event;

import com.example.simplescheduleapp.common.entity.SoftDeletedEntity;
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

    protected DomainEvent(Long targetDomainId) {
        this.uuid = UUID.randomUUID().toString();
        this.eventState = EventState.INIT;
        this.targetDomainId = targetDomainId;
    }

    protected DomainEvent(String uuid, EventState eventState, Long targetDomainId) {
        this.uuid = uuid;
        this.eventState = eventState;
        this.targetDomainId = targetDomainId;
    }

    public void regenerateUuid() {
        this.uuid = UUID.randomUUID().toString();
    }

    public void produceSuccess() {
        this.eventState = EventState.PRODUCE_SUCCESS;
    }

    public void produceFail(Throwable e) {
        this.eventState = EventState.PRODUCE_FAIL;
        this.failReason = e.getMessage();
    }

    public abstract String getTopic();
}
