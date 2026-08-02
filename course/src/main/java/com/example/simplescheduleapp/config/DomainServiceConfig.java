package com.example.simplescheduleapp.config;

import com.example.simplescheduleapp.lecture.general.domain.PendingLectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.general.domain.service.PendingLectureEnrollmentService;
import com.example.simplescheduleapp.parent.domain.ParentRepository;
import com.example.simplescheduleapp.parent.domain.service.ParentRegister;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import com.example.simplescheduleapp.student.domain.service.StudentRegister;
import com.example.simplescheduleapp.tutor.domain.TutorRepository;
import com.example.simplescheduleapp.tutor.domain.service.TutorRegister;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 순수 도메인 서비스의 수동 빈 등록 — 도메인 계층의 Spring 무의존 유지 (ADR-0002 / ADR-0004).
 *
 * <p>{@code config.ScheduleConfig}(ScheduleConflictValidator)와 동일한 관례를 따른다:
 * 도메인 서비스는 {@code @Component} 없이 순수 자바로 두고, 빈 등록만 여기서 담당한다.
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public TutorRegister tutorRegister(TutorRepository tutorRepository) {
        return new TutorRegister(tutorRepository);
    }

    @Bean
    public StudentRegister studentRegister(StudentRepository studentRepository) {
        return new StudentRegister(studentRepository);
    }

    @Bean
    public ParentRegister parentRegister(ParentRepository parentRepository) {
        return new ParentRegister(parentRepository);
    }

    @Bean
    public PendingLectureEnrollmentService pendingLectureEnrollmentService(
            PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository) {
        return new PendingLectureEnrollmentService(pendingLectureEnrollmentRepository);
    }
}
