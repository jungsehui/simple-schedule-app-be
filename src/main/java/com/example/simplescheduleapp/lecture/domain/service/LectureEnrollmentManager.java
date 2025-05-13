package com.example.simplescheduleapp.lecture.domain.service;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.domain.*;
import com.example.simplescheduleapp.lecture.exception.LectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.lecture.exception.LectureExceptionCode;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import com.example.simplescheduleapp.notification.application.NotificationService;
import com.example.simplescheduleapp.notification.message.NotificationMessage;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;
import com.example.simplescheduleapp.tutor.domain.service.TutorRegister;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LectureEnrollmentRegister {

    private final NotificationService notificationService;
    private final LectureEnrollmentRepository lectureEnrollmentRepository;
    private final PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository;

    public LectureEnrollment enrollStudentToLecture(PendingLectureEnrollment pending, Lecture lecture, Student student) {
        // 학생이 이미 수강 등록한 상태라면 기등록 에러
        if (lectureEnrollmentRepository.existsByLectureAndStudent(lecture, student)) {
            throw new ApplicationException(LectureExceptionCode.ALREADY_ENROLLED);
        }

        pending.accept();
        pendingLectureEnrollmentRepository.save(pending);
        LectureEnrollment savedEnrollment = lectureEnrollmentRepository.save(new LectureEnrollment(lecture, student));
        lecture.increaseEnrolledCount();

        Long studentId = student.getId();
        String message = "'" + lecture.getTitle() + "' 강의 수강신청이 수락되었습니다.";
        notificationService.sendPushNotification(new NotificationMessage(studentId, lecture.getTitle(), message));

        return savedEnrollment;
    }


    public void cancelEnrollment(Long enrollmentId) {
        LectureEnrollment enrollment = lectureEnrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ApplicationException(LectureEnrollmentExceptionCode.LECTURE_ENROLLMENT_NOT_FOUND));

        // 1. (선택 사항) 취소 권한 검증 (예: 학생 본인인지, 강사인지)

        // 2. 등록 정보 삭제
        lectureEnrollmentRepository.delete(enrollment);

        // 3. (선택 사항) 알림 발행 등 후처리 로직 추가
    }
}
