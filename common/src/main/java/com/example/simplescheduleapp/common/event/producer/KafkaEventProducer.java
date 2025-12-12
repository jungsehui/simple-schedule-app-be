package com.example.simplescheduleapp.common.event.producer;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.DomainEventRepository;
import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.InternalServerExceptionCode;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.producer.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaEventProducer implements EventProducer {

    private final KafkaProducer<KafkaLectureEventMessage> kafkaProducer;
    private final DomainEventRepository domainEventRepository;

    @Override
    public void produce(DomainEvent domainEvent) {
        String topic = domainEvent.getTopic();
        Long eventId = domainEvent.getId();

        try {
            // 도메인 이벤트 스스로 메시지를 생성
            KafkaLectureEventMessage message = domainEvent.toMessage();

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
