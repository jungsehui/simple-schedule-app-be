package com.example.simplescheduleapp.kafka.event.mock;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.mapper.DomainEventMapper;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 테스트 전용 DomainEventMapper.
 *
 * <p>notification 모듈은 프로덕션에서 도메인 이벤트를 발행하지 않으므로 {@code DomainEventMapper}
 * 구현이 없다(발행 경로는 course의 {@code CourseDomainEventMapper}). 그러나
 * {@code KafkaEventProducerTest}는 common 모듈의 {@code KafkaEventProducer} 발행 성공 경로를
 * 검증하며, 이를 위해선 {@link TestDomainEvent}를 매핑할 협력자가 필요하다. 이 매퍼가 그 협력자다.
 */
@Profile("test")
@Component
public class TestDomainEventMapper implements DomainEventMapper {

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof TestDomainEvent;
    }

    @Override
    public KafkaLectureEventMessage mapToMessage(DomainEvent event) {
        return KafkaLectureEventMessage.create(
                event.getUuid(),
                LectureEventType.LECTURE_UPDATED,
                event.getTargetDomainId(),
                null,
                1L,
                "테스트 강의",
                "테스트 이벤트");
    }
}
