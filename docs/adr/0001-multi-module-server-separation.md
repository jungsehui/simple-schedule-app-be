# ADR-0001: 멀티모듈 서버 분리 (common / course / notification)

- 상태: 채택됨 (feature/server-separation → develop 머지 완료)
- 날짜: 2025-08 (기록일: 2026-07-02)

## 맥락
단일 Spring Boot 앱에서 수강신청 트래픽(동시성 제어)과 알림 전송(SSE 롱커넥션, FCM 외부 API 지연)이 서로 자원을 간섭했다. 부하 특성이 다른 두 책임을 분리해 독립 배포·확장이 필요했다.

## 결정
1. Gradle 멀티모듈로 분리: `common`(공유 라이브러리), `course`(:8080), `notification`(:8081), `playground`/`ngrinder`(부하 테스트 전용)
2. 모듈 간 통신: 비동기는 Kafka(`COURSE_EVENT_TOPIC`), 동기 최소한만 course `/internal/**` REST
3. DB 분리: course_db(3306), notification_db(3307) — 모듈 간 테이블 직접 참조 금지
4. 이벤트 신뢰성: `DomainEvent` 엔티티로 발행 이력·상태(uuid, EventStatus) 영속화

## 결과
- (+) 알림 장애가 수강신청 경로에 전파되지 않음, 모듈별 독립 배포 가능
- (+) common의 testFixtures로 테스트 인프라 공유
- (-) Kafka 메시지 스키마가 사실상 공용 API — 변경 시 양쪽 호환성 관리 필요
- (-) 리팩토링 시 모듈 경계를 넘는 참조 정리 비용 (예: `CourseEventMessage` 제거 후 테스트 미정리 사례)

## AI 에이전트 지침
모듈 경계를 넘는 변경(공유 DTO, Kafka 메시지, /internal API)은 반드시 `ssa-architecture-guardian` 리뷰를 거칠 것.
