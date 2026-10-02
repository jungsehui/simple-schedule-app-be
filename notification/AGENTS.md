# notification 모듈 (알림 바운디드 컨텍스트, 라이브러리)

자체 bootJar 없음. 실행은 `app` 모듈이 조합해서 한다(포트 8080).

## 패키지
`sse`(SSE 스트림), `fcm`(FCM 푸시), `kafka`(컨슈머), `notification`(알림 도메인), `config`, `redis`

## 핵심 흐름
- Kafka `COURSE_EVENT_TOPIC` 소비 (group: `ssa-server-group`, 같은 JVM이 발행하고 자기 소비) → 알림 저장 → SSE 실시간 전송 + FCM 푸시
- SSE: `SseController` `/sse-stream` (`text/event-stream`, 회원은 인증 토큰으로 식별) — nginx에서 `proxy_buffering off` 필수
- FCM: `FcmController` `/fcm/token`, `FcmConfig`가 서비스 계정 JSON 로딩
- course 동기 조회: `EnrolledStudentsPort`(아웃바운드 포트). 구현은 `app`의 `InProcessEnrolledStudentsAdapter`(HTTP `CourseClient`는 제거됨)

## 규칙
- `spring.jpa.open-in-view: false`(설정 위치: `app`의 `application*.yml`) 변경 금지 — SSE Emitter로 인한 커넥션 고갈 방지
- Emitter는 타임아웃·에러 시 반드시 제거(누수 방지). 등록/해제 코드 변경 시 QA 필수
- FCM 재시도는 실패 알림을 `FailedNotification`으로 저장하고 `NotificationRetryScheduler`가 10분마다 최대 3회 재처리한다(spring-retry 의존은 Boot 4에서 제거됨). 실패 알림 유실 정책 변경 시 사용자 확인
- FCM 서비스 계정 JSON(`ssa-fcm-firebase-adminsdk-*.json`)은 gitignore 유지 — 절대 커밋 금지

## 알려진 부채 (수정 시 확인)
- (해소됨) `FcmConfig`는 `ResourceLoader`로 `classpath:`/`file:`/절대경로를 모두 읽는다
- 스키마는 Flyway(`app/src/main/resources/db`)가 관리하고 `sql.init.mode`는 기본, 운영 프로파일 모두 `never`
