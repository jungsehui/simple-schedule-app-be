package com.example.simplescheduleapp.kafka.producer;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.InternalServerExceptionCode;
import com.example.simplescheduleapp.common.kafka.producer.KafkaProducer;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.kafka.topic.RequestLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.lecture.domain.PendingLectureEnrollment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RequestLectureEnrollmentTopicProducer {

    public static final String REQUEST_LECTURE_ENROLLMENT_TOPIC = KafkaTopics.REQUEST_LECTURE_ENROLLMENT_TOPIC;

    private final KafkaProducer<RequestLectureEnrollmentTopicMessage> kafkaProducer;

    public void produce(PendingLectureEnrollment pending, Lecture lecture) {
        try {
            RequestLectureEnrollmentTopicMessage message = RequestLectureEnrollmentTopicMessage.of(pending, lecture);
            log.info("Try to produce request lecture enrollment topic. pending ID: {}, lecture ID: {}",
                    pending.getId(), lecture.getId());
            kafkaProducer.produce(REQUEST_LECTURE_ENROLLMENT_TOPIC, message);
            log.info("Successfully produce request lecture enrollment topic. pending ID: {}, lecture ID: {}",
                    pending.getId(), lecture.getId());
        } catch (Exception e) {
            log.error("Unexpected exception producing request lecture enrollment topic. pending ID: {}, exception message: {}",
                    pending.getId(), e.getMessage(), e);
            throw new ApplicationException(InternalServerExceptionCode.UNKNOWN_EXCEPTION);
        }
    }
}
