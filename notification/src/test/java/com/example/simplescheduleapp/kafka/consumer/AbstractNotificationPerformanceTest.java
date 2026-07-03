package com.example.simplescheduleapp.kafka.consumer;

import com.example.simplescheduleapp.NotificationApplication;
import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.common.kafka.topic.KafkaTopics;
import com.example.simplescheduleapp.notification.application.NotificationDispatcher;
import com.example.simplescheduleapp.notification.client.CourseClient;
import com.example.simplescheduleapp.notification.client.response.GetEnrolledStudentInfosResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import static com.example.simplescheduleapp.support.ApplicationWithKafkaTest.PORT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.doAnswer;
import static org.mockito.BDDMockito.given;

@EmbeddedKafka(
        topics = {KafkaTopics.COURSE_EVENT_TOPIC}, // 사용할 토픽 이름을 여기에 명시
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:" + PORT
        },
        ports = {PORT}
)
// 처리량/타이밍(Thread.sleep, 500명 팬아웃)을 측정하는 성능 테스트 — 정확성 검증이 아니므로
// CI 기본 test 태스크에서는 제외(@Tag("slow")). 고정 포트 EmbeddedKafka 경합도 함께 회피.
@Tag("slow")
@SpringBootTest(classes = NotificationApplication.class)
public abstract class AbstractNotificationPerformanceTest {

    // 테스트 상수
    protected static final int NUM_STUDENTS = 500;             // 알림 보낼 학생 수
    protected static final int DELAY_PER_NOTIFICATION_MS = 10; // 알림 1건당 지연 시간 (ms)

    @Autowired
    protected KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    protected NotificationDispatcher notificationDispatcher;

    @MockitoBean
    protected CourseClient courseClient;

    protected StopWatch stopWatch;
    protected CountDownLatch latch;

    @BeforeEach
    void setUp() {
        // 1. 테스트 도구 초기화
        stopWatch = new StopWatch();
        latch = new CountDownLatch(NUM_STUDENTS);

        // 2. Mock 객체 설정: courseClient는 항상 500명의 학생 ID 리스트를 반환
        List<Long> studentIds = LongStream.range(1, NUM_STUDENTS + 1).boxed().toList();
        GetEnrolledStudentInfosResponse response = new GetEnrolledStudentInfosResponse("강의 제목", "메모", studentIds);
        given(courseClient.getEnrolledStudentInfosByLectureId(anyLong())).willReturn(response);

        // 3. Mock 객체 설정: notificationService는 호출될 때마다 10ms 지연시키고 Latch를 감소시킴
        doAnswer(invocation -> {
            TimeUnit.MILLISECONDS.sleep(DELAY_PER_NOTIFICATION_MS);
            latch.countDown();
            return null;
        }).when(notificationDispatcher).dispatchPushNotification(any());
    }

    protected void produceMessage() {
        // 실제 컨슈머가 역직렬화하는 타입으로 발행. LECTURE_UPDATED는 courseClient로 수강생을 조회해
        // NUM_STUDENTS 명 전원에게 브로드캐스트하므로 성능 시나리오(500명 팬아웃)와 일치한다.
        KafkaLectureEventMessage message = KafkaLectureEventMessage.create(
                "perf-test-uuid", LectureEventType.LECTURE_UPDATED, 1L, null, 1L, "강의 제목", "강의가 수정되었습니다");
        kafkaTemplate.send(KafkaTopics.COURSE_EVENT_TOPIC, message);
    }
}
