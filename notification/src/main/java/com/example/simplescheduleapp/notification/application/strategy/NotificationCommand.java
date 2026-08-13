package com.example.simplescheduleapp.notification.application.strategy;

/**
 * 알림 전략의 입력 커맨드. 유스케이스 계층이 소유하는 타입이다.
 *
 * <p>전송 계층 DTO({@code common.kafka.KafkaLectureEventMessage})를 그대로 받던 자리를 대신한다.
 * 필드는 전략 5개가 실제로 읽는 값만 담는다(YAGNI):
 * <ul>
 *     <li>{@code lectureId}: LectureUpdated(수강생 조회)</li>
 *     <li>{@code studentId}: Accept/Cancel/Reject/Request</li>
 *     <li>{@code tutorId}: 전략 5개 전부</li>
 *     <li>{@code lectureTitle}, {@code details}: 전략 5개 전부</li>
 * </ul>
 * Kafka 메시지의 {@code uuid}는 어떤 전략도 읽지 않아 옮기지 않았다.
 * 이벤트 종류({@code type})는 전략 선택에만 쓰이므로 커맨드가 아니라
 * {@link NotificationStrategyFactory}의 인자로 전달된다.
 */
public record NotificationCommand(
        Long lectureId,
        Long studentId,
        Long tutorId,
        String lectureTitle,
        String details
) {
}
