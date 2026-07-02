# course 모듈 (수강/강의 API 서버, :8080)

## 도메인 패키지 (각각 presentation/application/domain/exception 레이어)
`consultation`, `lecture.general`, `lecture.special`, `member`, `parent`, `schedule`, `student`, `tutor`
공통 인프라: `config`, `event`(이벤트 발행), `redis`(Redisson 락)

## 핵심 흐름
- 일반 수강신청: `LectureEnrollmentController` → `LectureEnrollmentService` → `PendingLectureEnrollment`(대기) → 강사 수락/거절(`/enrollments/accept|reject`) → Kafka `COURSE_EVENT_TOPIC` 발행 → notification 소비
- 특강 수강신청: `SpecialLectureEnrollmentController` — **4단계 동시성 방어** 구조. 락·검증 순서 임의 변경 금지
- 서버 간 API: `LectureEnrollmentInternalController` `/internal/**` — notification 전용, 외부 노출 금지(nginx 403)

## 규칙
- 동시성: Redisson 분산락 + DB 락 조합. 락 획득 순서 변경 전 `test/select-lock-deadlock` 브랜치의 데드락 사례 확인
- Kafka 발행은 `DomainEvent` 엔티티로 기록 후 발행 (uuid로 멱등성 추적, produceSuccess/produceFail 상태 전이)
- 조회 서비스는 `@Transactional(readOnly = true)`
- 테스트: `ApplicationWithKafkaTest`(EmbeddedKafka) 상속 패턴, Fixture Monkey `giveMeBuilder` 사용

## 알려진 부채 (수정 시 확인)
- `course/src/test/.../TutorLectureEnrollmentServiceTest.java` — 제거된 `CourseEventMessage`를 참조해 **컴파일 불가** (KafkaLectureEventMessage로의 마이그레이션 미완). 소유자 결정 필요
- 기본 프로파일에 root/1234 자격증명 하드코딩 — 로컬 전용, 운영은 `application-prod.yml` env 주입 사용
