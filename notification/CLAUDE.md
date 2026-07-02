# notification 모듈 (알림 서버, :8081)

## 패키지
`sse`(SSE 스트림), `fcm`(FCM 푸시), `kafka`(컨슈머), `notification`(알림 도메인), `config`, `redis`

## 핵심 흐름
- Kafka `COURSE_EVENT_TOPIC` 소비 (group: `ssa-notification-server-group`) → 알림 저장 → SSE 실시간 전송 + FCM 푸시
- SSE: `SseController` `/sse-stream/{memberId}` (`text/event-stream`) — nginx에서 `proxy_buffering off` 필수
- FCM: `FcmController` `/fcm/token`, `FcmConfig`가 서비스 계정 JSON 로딩
- course 동기 호출: `client.course-server-internal-url` 설정 → `/internal/**` REST

## 규칙
- `spring.jpa.open-in-view: false` 변경 금지 — SSE Emitter로 인한 커넥션 고갈 방지
- Emitter는 타임아웃·에러 시 반드시 제거(누수 방지). 등록/해제 코드 변경 시 QA 필수
- FCM 재시도는 spring-retry 사용. 실패 알림 유실 정책 변경 시 사용자 확인
- FCM 서비스 계정 JSON(`ssa-fcm-firebase-adminsdk-*.json`)은 gitignore 유지 — 절대 커밋 금지

## 알려진 부채 (수정 시 확인)
- `FcmConfig`가 `ClassPathResource` 사용 → 컨테이너 배포 시 외부 마운트 파일을 못 읽음. Spring `Resource` 추상화(`file:` 접두사)로 교체 필요 (deploy/README.md 4번)
- 기본 프로파일 `sql.init.mode: always` — 운영 프로파일에서는 `never`로 덮어씀
