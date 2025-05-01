package com.example.simplescheduleapp.lecture.application.command;

public record TutorLectureEnrollmentCommand(
        Long tutorId,
        Long lectureId,
        String studentName,
        String studentPhoneNumber
) {


}
