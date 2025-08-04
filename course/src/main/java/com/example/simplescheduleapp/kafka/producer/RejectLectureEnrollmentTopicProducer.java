package com.example.simplescheduleapp.kafka.producer;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.InternalServerExceptionCode;
import com.example.simplescheduleapp.common.kafka.producer.KafkaProducer;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.kafka.topic.RejectLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.student.domain.Student;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RejectLectureEnrollmentTopicProducer {

    public static final String REJECT_LECTURE_ENROLLMENT_TOPIC = KafkaTopics.REJECT_LECTURE_ENROLLMENT_TOPIC;

    private final KafkaProducer<RejectLectureEnrollmentTopicMessage> kafkaProducer;

    public void produce(Lecture lecture, Student student) {
        try {
            RejectLectureEnrollmentTopicMessage message = RejectLectureEnrollmentTopicMessage.of(lecture, student);
            log.info("Try to produce reject lecture enrollment topic. lecture ID: {}, student ID: {}",
                    lecture.getId(), student.getId());
            kafkaProducer.produce(REJECT_LECTURE_ENROLLMENT_TOPIC, message);
            log.info("Successfully produce reject lecture enrollment topic. lecture ID: {}, student ID: {}",
                    lecture.getId(), student.getId());
        } catch (Exception e) {
            log.error("Unexpected exception producing reject lecture enrollment topic. lecture ID: {}, exception message: {}",
                    lecture.getId(), e.getMessage(), e);
            throw new ApplicationException(InternalServerExceptionCode.UNKNOWN_EXCEPTION);
        }
    }
}
