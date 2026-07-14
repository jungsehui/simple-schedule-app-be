package com.example.simplescheduleapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * SSA 단일 JVM 진입점 (ADR-0003 Stage 2).
 * <p>
 * course(수강/강의/회원)와 notification(SSE/FCM) 바운디드 컨텍스트를 하나의 Spring 컨텍스트로
 * 조합한다. 기존 CourseApplication/NotificationApplication을 대체하는 유일한 부팅 클래스.
 * <p>
 * 두 컨텍스트 간 통신: Kafka(course 발행 → 같은 JVM notification 리스너가 브로커 경유 자기소비),
 * 수강생 조회는 in-process 어댑터(HTTP /internal 자기호출 제거). {@code @EnableScheduling}/
 * {@code @EnableRetry}는 common의 {@code SchedulingRetryConfig}가 제공한다.
 * <p>
 * 루트 패키지 {@code com.example.simplescheduleapp}에 위치 — course/notification/common을 모두
 * 컴포넌트 스캔한다.
 */
@ConfigurationPropertiesScan
@SpringBootApplication
public class SsaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsaApplication.class, args);
    }
}
