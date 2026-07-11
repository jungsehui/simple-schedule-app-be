package com.example.simplescheduleapp.common.event;

import com.example.simplescheduleapp.common.domain.SoftDeletedDomain;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_DATE_IS_NULL;

@SQLRestriction(DELETED_DATE_IS_NULL)
@SQLDelete(sql = "UPDATE domain_event SET deleted_date = CURRENT_TIMESTAMP WHERE id = ?")
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
    private EventStatus status;

    @Column(nullable = false)
    private Long targetDomainId; // 강의 ID 등의 Aggregate Root(이벤트 주체)에 대한 정보를 담을 것

    private String failReason;

    // 무애노테이션(기본 매핑) — 신규 jakarta.persistence 사용은 아키텍처 래칫 신규 위반이 됨
    private int retryCount = 0;

    protected DomainEvent(Long targetDomainId) {
        this.uuid = UUID.randomUUID().toString();
        this.status = EventStatus.INIT;
        this.targetDomainId = targetDomainId;
    }

    protected DomainEvent(String uuid, EventStatus status, Long targetDomainId) {
        this.uuid = uuid;
        this.status = status;
        this.targetDomainId = targetDomainId;
    }

    public void regenerateUuid() {
        this.uuid = UUID.randomUUID().toString();
    }

    public void produceSuccess() {
        this.status = EventStatus.PRODUCE_SUCCESS;
    }

    public void produceFail(Throwable e) {
        this.status = EventStatus.PRODUCE_FAIL;
        this.failReason = e.getMessage();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void markDead() {
        this.status = EventStatus.DEAD;
    }

    public abstract String getTopic();
}
