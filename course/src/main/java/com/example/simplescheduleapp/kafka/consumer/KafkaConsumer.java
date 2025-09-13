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
            // ★★ 이중 안전장치: DB에도 동시성 제어 로직(조건부 UPDATE)을 그대로 사용합니다.
            // Redis에서 통과했더라도, 만약의 경우를 대비해 DB에서도 정원을 다시 확인합니다.
            int updatedRows = specialLectureRepository.increaseSpecialLectureEnrollmentCount(specialLectureId);
            if (updatedRows == 0) {
                // 이 경우는 Redis의 데이터와 DB 데이터가 일시적으로 달랐던 특이 케이스입니다.
                // 정원이 마감되었으므로 에러 로그를 남기고 더 이상 진행하지 않습니다.
                log.error("DB capacity check failed for specialLectureId: {}", specialLectureId);
                // 필요하다면 Redis 카운터를 다시 감소시키는 보상 트랜잭션 로직을 넣을 수 있습니다.
                return;
            }

            // 자리가 최종적으로 확보되었으므로, 수강 신청 기록을 생성합니다.
            SpecialLecture specialLecture = specialLectureRepository.getById(specialLectureId);
            Student student = studentRepository.getById(studentId);
            SpecialLectureEnrollment specialLectureEnrollment = new SpecialLectureEnrollment(specialLecture, student);

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
