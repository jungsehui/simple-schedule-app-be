package com.example.simplescheduleapp.common.event.producer;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.DomainEventRepository;
import com.example.simplescheduleapp.common.event.mapper.DomainEventMapperFactory;
import com.example.simplescheduleapp.common.event.mapper.DomainEventMapper;
import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.InternalServerExceptionCode;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.producer.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaEventProducer implements EventProducer {

    private final DomainEventMapperFactory domainEventMapperFactory;
    private final KafkaProducer<KafkaLectureEventMessage> kafkaProducer;
    private final DomainEventRepository domainEventRepository;

    @Transactional
    @Override
    public void produce(DomainEvent domainEvent) {
        String topic = domainEvent.getTopic();
        Long eventId = domainEvent.getId();

        try {
            DomainEventMapper mapper = domainEventMapperFactory.getMapper(domainEvent);
            KafkaLectureEventMessage message = mapper.mapToMessage(domainEvent);

            log.info("Try to produce kafka topic. topic: {}, eventId: {}", topic, eventId);

            kafkaProducer.produce(topic, message);

            log.info("Successfully produce kafka topic. topic: {}, eventId: {}", topic, eventId);

            // 아웃박스에 발행 성공 상태 업데이트
            domainEvent.produceSuccess();
            domainEventRepository.save(domainEvent);
            log.info("Successfully update domainEvent state to produce success. id: {}", domainEvent.getId());
        } catch (Exception e) {
            log.error("Unexpected exception while produce kafka topic: {}, message: {}", topic, e.getMessage(), e);

            // 아웃박스에 발행 실패 상태 업데이트
            domainEvent.produceFail(e);
            domainEventRepository.save(domainEvent);
            log.info("Successfully update domainEvent state to produce fail. id: {}", domainEvent.getId());

            // 필요시 트랜잭션 롤백 등을 위해 상위로 예외 전파
            throw new ApplicationException(InternalServerExceptionCode.UNKNOWN_EXCEPTION);
        }
    }
}
