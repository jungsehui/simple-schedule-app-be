package com.example.simplescheduleapp.common.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 도메인 이벤트 — 순수 도메인 모델 (ADR-0004 / ADR-0002 §4).
 *
 * <p>JPA/프레임워크 의존 0. 아웃박스 영속(SINGLE_TABLE 상속·discriminator·소프트삭제)은
 * {@code common.outbox.persistence}의 {@code DomainEventEntity}가 담당하고, 변환은 각 서브타입이
 * 제공하는 {@code DomainEventPersistenceMapper}가 한다. Kafka 메시지는 또 다른 타입
 * ({@code KafkaLectureEventMessage})으로, 셋을 매퍼로 연결한다.
 *
 * <p>발행 상태(status/retryCount/failReason)는 아웃박스 전달 보증을 위한 상태다.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public abstract class DomainEvent {

    private Long id;
    private String uuid;
    private EventStatus status;
    private Long targetDomainId; // 강의 ID 등 Aggregate Root(이벤트 주체) 식별자
    private String failReason;
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

    /** DB 복원용 — 영속 매퍼 전용. */
    protected DomainEvent(Long id, String uuid, EventStatus status, Long targetDomainId,
                          String failReason, int retryCount) {
        this.id = id;
        this.uuid = uuid;
        this.status = status;
        this.targetDomainId = targetDomainId;
        this.failReason = failReason;
        this.retryCount = retryCount;
    }

    /**
     * 아웃박스에 기록되며 부여된 식별자를 전파한다 — {@code EventRecorder} 전용.
     *
     * <p><b>★ 이 전파가 끊기면 전달 보증이 조용히 깨진다.</b> 순수화 이전에는 이 클래스가 곧 JPA
     * 엔티티라 {@code save()}가 인스턴스에 직접 id를 심었고, BEFORE_COMMIT에 기록된 그 인스턴스가
     * AFTER_COMMIT 리스너로 그대로 전달돼 id를 갖고 있었다. 이제는 매퍼가 만든 <i>다른</i> 인스턴스에
     * id가 담기므로, 원본에 다시 실어주지 않으면 발행 후 {@code save()}가 UPDATE가 아닌 INSERT가 되어
     * <b>중복 행</b>이 생기고 원본 행은 INIT으로 남아 릴레이가 <b>무한 재발행</b>한다
     * (at-least-once → 중복 배달). {@code EventRecorderTest}가 이를 가드한다.
     */
    public void assignId(Long id) {
        this.id = id;
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
