package com.example.simplescheduleapp.notification.application.strategy;

/**
 * 알림 전략을 고르는 이벤트 종류. 유스케이스 계층이 소유하는 어휘다.
 *
 * <p>{@code common.kafka.LectureEventType}(전송 계층 타입)과 값이 같지만 별개다.
 * 두 어휘의 변환은 인바운드 어댑터({@code NotificationKafkaConsumer})가 책임지며,
 * 그 변환은 {@code default} 없는 switch 식이라 common 쪽에 상수가 추가되면
 * 런타임 예외가 아니라 <b>컴파일 오류</b>로 드러난다.
 */
public enum NotificationEventType {

    ENROLLMENT_REQUESTED, // 수강 신청 요청
    ENROLLMENT_ACCEPTED,  // 수강 신청 승낙
    ENROLLMENT_REJECTED,  // 수강 신청 거절
    ENROLLMENT_CANCELED,  // 수강 신청 취소
    LECTURE_UPDATED,      // 강의 내용 수정
    ;
}
