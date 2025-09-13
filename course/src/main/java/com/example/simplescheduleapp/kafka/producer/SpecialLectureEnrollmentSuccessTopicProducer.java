package com.example.simplescheduleapp.kafka.producer;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.InternalServerExceptionCode;
import com.example.simplescheduleapp.common.kafka.producer.KafkaProducer;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.kafka.topic.EnrollSpecialLectureEnrollmentTopicMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpecialLectureEnrollmentSuccessTopicProducer {

    public static final String SPECIAL_LECTURE_ENROLLMENT_SUCCESS_TOPIC = KafkaTopics.ENROLL_SPECIAL_LECTURE_ENROLLMENT_SUCCESS_TOPIC;

    private final KafkaProducer<EnrollSpecialLectureEnrollmentTopicMessage> kafkaProducer;

    public void produce(Long specialLectureId, Long studentId) {
        try {
            EnrollSpecialLectureEnrollmentTopicMessage message = EnrollSpecialLectureEnrollmentTopicMessage.of(specialLectureId, studentId);
            log.info("Try to produce special lecture enrollment topic. special lecture ID: {}, student ID: {}",
                    specialLectureId, studentId);
            kafkaProducer.produce(SPECIAL_LECTURE_ENROLLMENT_SUCCESS_TOPIC, message);
            log.info("Successfully produce special lecture enrollment topic. special lecture ID: {}, student ID: {}",
                    specialLectureId, studentId);
        } catch (Exception e) {
            log.error("Unexpected exception producing special lecture enrollment topic. special lecture ID: {}, student ID: {}",
                    specialLectureId, e.getMessage(), e);
            throw new ApplicationException(InternalServerExceptionCode.UNKNOWN_EXCEPTION);
        }
    }
}
