package com.example.playground.async.event;

import com.example.playground.common.domain.SoftDeletedDomain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "event_type")
@Table(name = "test_domain_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public abstract class TestDomainEvent extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(unique = true)
    protected String uuid;

    protected Long requestId;
    protected LocalDateTime initializedDate; // 객체 생성 시점
    protected LocalDateTime processedDate;   // 실제 처리된 시점

    protected long initializedDateToCreatedDateDuration;   // 객체 생성 시점 - DB 기록 시점
    protected long createdDateToProcessedDateDuration;     // DB 기록 시점 - 처리 완료 시점
    protected long initializedDateToProcessedDateDuration; // 객체 생성 시점 - 처리 완료 시점

    @Enumerated(EnumType.STRING)
    protected TestEventState state;

    public TestDomainEvent(Long requestId) {
        this.initializedDate = LocalDateTime.now();
        this.uuid = UUID.randomUUID().toString();
        this.state = TestEventState.INIT;
        this.requestId = requestId;
    }

    public void publishSuccess() {
        this.state = TestEventState.PRODUCE_SUCCESS;
        this.processedDate = LocalDateTime.now();
        log.info("이벤트 객체 생성 시점: {}, DB 기록 시점: {}, 처리 완료 시점: {}", initializedDate, getCreatedDate(), processedDate);
        initializedDateToCreatedDateDuration = Duration.between(initializedDate, getCreatedDate()).toMillis();
        createdDateToProcessedDateDuration = Duration.between(getCreatedDate(), processedDate).toMillis();
        initializedDateToProcessedDateDuration = Duration.between(initializedDate, processedDate).toMillis();
        log.info("객체 생성 시점 - DB 기록 시점: {}ms", initializedDateToCreatedDateDuration);
        log.info("DB 기록 시점 - 처리 완료 시점: {}ms", createdDateToProcessedDateDuration);
        log.info("객체 생성 시점 - 처리 완료 시점: {}ms", initializedDateToProcessedDateDuration);
    }

    public void regenerateUuid() {
        this.uuid = UUID.randomUUID().toString();
    }

    public void publishFail() {
        this.state = TestEventState.PRODUCE_FAIL;
    }
}
