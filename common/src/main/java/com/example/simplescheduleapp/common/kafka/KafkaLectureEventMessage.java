package com.example.simplescheduleapp.common.kafka;

public record KafkaLectureEventMessage(
        String uuid,
        LectureEventType type, // 어떤 이벤트가 발생했는지
        Long lectureId,        // Aggregate Root ID
        Long studentId,        // 관련된 학생
        Long tutorId,          // 관련된 강사
        String lectureTitle,   // 스냅샷 데이터 (제목 변경 대비)
        String details         // 부가 정보 (수정 내용 등)
) {

    public static KafkaLectureEventMessage create(
            String uuid,
            LectureEventType type,
            Long lectureId,
            Long studentId,
            Long tutorId,
            String lectureTitle,
            String details
    ) {
        return new KafkaLectureEventMessage(
                uuid,
                type,
                lectureId,
                studentId,
                tutorId,
                lectureTitle,
                details
        );
    }
}
