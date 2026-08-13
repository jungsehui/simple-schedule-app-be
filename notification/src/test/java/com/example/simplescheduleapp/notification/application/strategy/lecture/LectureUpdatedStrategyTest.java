package com.example.simplescheduleapp.notification.application.strategy.lecture;

import com.example.simplescheduleapp.notification.application.NotificationFacade;
import com.example.simplescheduleapp.notification.application.port.out.EnrolledStudentsPort;
import com.example.simplescheduleapp.notification.application.port.out.GetEnrolledStudentInfosResponse;
import com.example.simplescheduleapp.notification.application.strategy.NotificationCommand;
import com.example.simplescheduleapp.notification.application.strategy.NotificationEventType;
import com.example.simplescheduleapp.support.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 강의 수정 전략 단위 테스트.
 *
 * <p>{@code common.kafka} import 없이 커맨드만으로 검증한다. 이 전략은 커맨드의
 * {@code lectureId}로 수강생을 조회한 뒤 전원에게 팬아웃하므로 {@code lectureId} 사용처이기도 하다.
 */
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LectureUpdatedStrategyTest extends UnitTest {

    @InjectMocks
    private LectureUpdatedStrategy lectureUpdatedStrategy;

    @Mock
    private NotificationFacade notificationFacade;

    @Mock
    private EnrolledStudentsPort courseClient;

    @Test
    @DisplayName("LECTURE_UPDATED 이벤트를 지원한다")
    void 지원_이벤트_종류() {
        assertThat(lectureUpdatedStrategy.getSupportType()).isEqualTo(NotificationEventType.LECTURE_UPDATED);
    }

    @Test
    @DisplayName("커맨드의 lectureId로 수강생을 조회해 강사 이름으로 전원에게 보낸다")
    void 수강생_전원에게_팬아웃한다() {
        NotificationCommand command = new NotificationCommand(100L, null, 42L, "자료구조 특강", "3주차 휴강");
        given(courseClient.getEnrolledStudentInfosByLectureId(100L))
                .willReturn(new GetEnrolledStudentInfosResponse("자료구조 특강", "메모", List.of(1L, 2L, 3L)));

        lectureUpdatedStrategy.handle(command);

        verify(courseClient).getEnrolledStudentInfosByLectureId(100L);
        verify(notificationFacade).sendNotification(
                42L,                     // sender: tutorId
                List.of(1L, 2L, 3L),     // target: 조회된 수강생 전원
                "자료구조 특강",
                "강의 내용이 수정되었습니다: 3주차 휴강"
        );
    }
}
