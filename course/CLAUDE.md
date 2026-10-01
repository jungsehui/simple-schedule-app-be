# course 모듈 (수강/강의 바운디드 컨텍스트, 라이브러리)

자체 bootJar 없음. 실행은 `app` 모듈이 조합해서 한다(포트 8080).

## 도메인 패키지 (각각 presentation/application/domain/infrastructure/exception 레이어, ADR-0002 헥사고날)
`consultation`, `lecture.general`, `lecture.special`, `member`, `parent`, `schedule`, `student`, `tutor`
공통 인프라: `config`, `event`(이벤트 발행), `redis`(Redisson 락)

## 핵심 흐름
- 일반 수강신청: `LectureEnrollmentController` → `LectureEnrollmentService` → `PendingLectureEnrollment`(대기) → 강사 수락/거절(`/enrollments/accept|reject`) → Kafka `COURSE_EVENT_TOPIC` 발행 → notification 소비
- 특강 수강신청: `SpecialLectureEnrollmentController` — **4단계 동시성 방어** 구조. 락·검증 순서 임의 변경 금지
- 내부 API: `LectureEnrollmentInternalController` `/internal/**` — 외부 노출 금지(nginx 403). notification은 이제 HTTP가 아니라 `app`의 `InProcessEnrolledStudentsAdapter`로 같은 데이터를 조회한다

## 규칙
- 동시성: Redisson 분산락 + DB 락 조합. 락 획득 순서 변경 전 `test/select-lock-deadlock` 브랜치의 데드락 사례 확인
- Kafka 발행은 `DomainEvent` 엔티티로 기록 후 발행 (uuid로 멱등성 추적, produceSuccess/produceFail 상태 전이)
- 조회 서비스는 `@Transactional(readOnly = true)`
- 테스트: `ApplicationWithKafkaTest`(EmbeddedKafka) 상속 패턴, Fixture Monkey `giveMeBuilder` 사용

## 알려진 부채 (수정 시 확인)
- 기본 프로파일(`app/src/main/resources/application.yml`)의 DB 자격증명 기본값이 root/1234 — 로컬 전용, 운영은 `application-prod.yml` env 주입 사용
