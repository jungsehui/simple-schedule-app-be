package com.example.simplescheduleapp.kafka.producer;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.InternalServerExceptionCode;
import com.example.simplescheduleapp.common.kafka.producer.KafkaProducer;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.kafka.topic.CancelLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.lecture.domain.Lecture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class CancelLectureEnrollmentTopicProducer {

    public static final String CANCEL_LECTURE_ENROLLMENT_TOPIC = KafkaTopics.CANCEL_LECTURE_ENROLLMENT_TOPIC;

    private final KafkaProducer<CancelLectureEnrollmentTopicMessage> kafkaProducer;

    public void produce(Long studentId, Lecture lecture) {
        try {
            CancelLectureEnrollmentTopicMessage message = CancelLectureEnrollmentTopicMessage.of(studentId, lecture);
            log.info("Try to produce cancel lecture enrollment topic. student ID: {}, lecture ID: {}",
                    studentId, lecture.getId());
            kafkaProducer.produce(CANCEL_LECTURE_ENROLLMENT_TOPIC, message);
            log.info("Successfully produce cancel lecture enrollment topic. student ID: {}, lecture ID: {}",
                    studentId, lecture.getId());
        } catch (Exception e) {
            log.error("Unexpected exception producing cancel lecture enrollment topic. student ID: {}, exception message: {}",
                    studentId, e.getMessage(), e);
            throw new ApplicationException(InternalServerExceptionCode.UNKNOWN_EXCEPTION);
        }
    }
}
