package com.example.simplescheduleapp.kafka.consumer;

import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.kafka.topic.EnrollSpecialLectureEnrollmentTopicMessage;
import com.example.simplescheduleapp.special.domain.SpecialLecture;
import com.example.simplescheduleapp.special.domain.SpecialLectureEnrollment;
import com.example.simplescheduleapp.special.domain.SpecialLectureEnrollmentRepository;
import com.example.simplescheduleapp.special.domain.SpecialLectureRepository;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaConsumer {

    private final SpecialLectureRepository specialLectureRepository;
    private final StudentRepository studentRepository;
    private final SpecialLectureEnrollmentRepository specialLectureEnrollmentRepository;

    @KafkaListener(
            topics = KafkaTopics.ENROLL_SPECIAL_LECTURE_ENROLLMENT_SUCCESS_TOPIC,
            containerFactory = CourseKafkaConsumerConfig.ENROLL_SPECIAL_LECTURE_ENROLLMENT_SUCCESS_CONTAINER_FACTORY
    )
    @Transactional
    public void consumeLectureEnrollmentRequest(
            EnrollSpecialLectureEnrollmentTopicMessage message,
            Acknowledgment ack,
            @Header(KafkaHeaders.OFFSET) int offset
    ) {
        log.info("Try to consume enroll special lecture enrollment success topic. id: {}, offset: {}", message.specialLectureId(), offset);

        Long specialLectureId = message.specialLectureId();
        Long studentId = message.studentId();

        log.info("Writing to DB -> specialLectureId: {}, studentId: {}", specialLectureId, studentId);

        try {
            SpecialLecture specialLecture = specialLectureRepository.getById(specialLectureId);
            Student student = studentRepository.getById(studentId);
            SpecialLectureEnrollment specialLectureEnrollment = specialLecture.enroll(student);

            specialLectureEnrollmentRepository.save(specialLectureEnrollment);
            specialLectureRepository.save(specialLecture);
        } catch (DataIntegrityViolationException e) {
            log.warn("Already enrolled student detected in DB writer: {}", studentId);
        } catch (Exception e) {
            log.error("Error while writing enrollment to DB.", e);
        }

        ack.acknowledge();
        log.info("Successfully consume enroll special lecture enrollment success topic. id :{}, offset: {}", message.specialLectureId(), offset);
    }
}
