# Testing Patterns

**Analysis Date:** 2026-07-02

> **Source branch:** This analysis is based on branch **`chore/ai-infra-cicd`** (built directly on **`feature/query-performance-tuning`** — the active development line). The repo's `main` branch has only the empty skeleton test. All paths below are repository-relative on that branch. Test code lives in `course/src/test`, `notification/src/test`, `playground/src/test`, and the shared **`common/src/testFixtures`** source set. Note: `common` has NO `src/test` directory of its own on this branch — it only publishes test fixtures.

## Test Framework

**Runner:**
- JUnit 5 (Jupiter) — `useJUnitPlatform()` configured once in the root `build.gradle` `subprojects` block, inherited by every module:
  ```groovy
  testImplementation 'org.springframework.boot:spring-boot-starter-test'
  testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
  ```

**Assertion Library:**
- AssertJ (`assertThat` / `assertThatThrownBy`) via `spring-boot-starter-test` — the dominant style. Plain JUnit `assertThrows`/`assertDoesNotThrow` appears in a few pure entity tests (`course/src/test/java/com/example/simplescheduleapp/member/domain/entity/MemberTest.java`).
- Awaitility (`org.awaitility.Awaitility.await()`) for polling assertions on async flows (Kafka consume → notification dispatch) — used in `notification/src/test/java/com/example/simplescheduleapp/notification/NotificationIntegrationTest.java`. It arrives **transitively** (not declared in any `build.gradle`); if it ever needs pinning, add it explicitly to `notification/build.gradle`.

**Test Data:**
- Naver **Fixture Monkey 1.1.11** (`fixture-monkey-starter` + `fixture-monkey-jakarta-validation`) declared as `testFixturesApi` in `common/build.gradle`, so every module's tests get it via `testImplementation(testFixtures(project(':common')))`.
- **spring-kafka-test** (`@EmbeddedKafka`) also declared as `testFixturesApi` in `common/build.gradle`.

**Run Commands:**
```bash
./gradlew :common:test :course:test :notification:test   # the three CI-relevant modules
./gradlew :course:test                                    # single module
./gradlew test                                            # ALL modules (also runs playground's deadlock tests)
./gradlew test --tests "*LectureServiceTest"              # single test class
```
CI (`.github/workflows/ci.yml`) runs `./gradlew :common:build :course:build :notification:build --parallel` on JDK 21 for PRs/pushes to `main`/`develop` — `playground` and `ngrinder` are deliberately excluded (load-test only). Tests need no external services: H2 + `@EmbeddedKafka` cover the DB and broker.

## Test File Organization

**Location:** Standard `**/src/test/java/...` per module. Test packages loosely mirror main packages but may flatten sub-domains — e.g. tests for `lecture/general/application` classes live in `course/src/test/java/com/example/simplescheduleapp/lecture/application/`.

**Naming:** `<ClassUnderTest>Test.java`; scenario-split classes use descriptive suffixes: `FcmServiceSuccessTest.java` / `FcmServiceFailureTest.java` (split by success/failure), `LectureUpdatedNotificationForLoopTest.java` / `...ParallelStreamTest.java` / `...ExecutorTest.java` (split by implementation strategy being benchmarked, sharing `AbstractNotificationPerformanceTest`).

**Module test inventory:**
```
common/src/testFixtures/java/com/example/simplescheduleapp/support/     <- 6 shared base classes (below)
course/src/test/java/com/example/simplescheduleapp/
  lecture/application/    LectureServiceTest, StudentLectureEnrollmentServiceTest, TutorLectureEnrollmentServiceTest
  member/application/     MemberServiceTest
  member/domain/entity/   MemberTest, PasswordTest
  member/presentation/    MemberControllerTest
notification/src/test/java/com/example/simplescheduleapp/
  fcm/application/        FcmServiceSuccessTest, FcmServiceFailureTest
  kafka/consumer/         AbstractNotificationPerformanceTest + ForLoop/ParallelStream/Executor subclasses
  kafka/event/outbox/     EventRecorderTest, EventRecordListenerTest
  kafka/event/producer/   KafkaEventProducerTest
  kafka/infra/consumer/   KafkaIdempotencyFilterTest
  kafka/infra/deadletter/ DeadLetterRecorderTest
  notification/           NotificationIntegrationTest, schedule/NotificationRetrySchedulerFailureTest
  sse/                    SseConnectionPublisherTest, cache/{RedisClientManagerTest, SseEmitterRepositoryTest}, event/RedisSseMessagePublisherTest
playground/src/test/java/com/example/playground/requiresnewdeadlock/    <- deadlock reproduction tests
```

## Shared Test Base Classes (`common/src/testFixtures/java/com/example/simplescheduleapp/support/`)

Deliberate inheritance chain — **extend the most specific applicable base** instead of wiring `@ExtendWith`/`@SpringBootTest` by hand:

```
MonkeySupport                 protected FixtureMonkey `sut` field, no framework wiring
 └─ MockTestSupport           + @ExtendWith(MockitoExtension.class)
     └─ CommonExceptionTest   + exceptionTest(Executable, ExceptionCode) helper
         ├─ UnitTest                  plain unit tests (Mockito, no Spring context)
         ├─ ApplicationTest           + @SpringBootTest (full context, H2)
         └─ ApplicationWithKafkaTest  + @EmbeddedKafka on port 49092 + @SpringBootTest
                                      + waitingConsumeTopicSync/Async helpers
```

- `MonkeySupport.sut` is a preconfigured `FixtureMonkey` (`FailoverIntrospector` over `FieldReflectionArbitraryIntrospector` → `ConstructorPropertiesArbitraryIntrospector`, `JakartaValidationPlugin`, `defaultNotNull(true)`). Never instantiate your own `FixtureMonkey`.
- `CommonExceptionTest.exceptionTest(executable, expectedCode)` asserts an `ApplicationException` with a specific `ExceptionCode`. It currently has **no call sites** on this branch — tests use AssertJ `assertThatThrownBy(...).extracting("code")` instead — but it remains the available shorthand.
- `ApplicationWithKafkaTest.PORT = 49092` is the single embedded-Kafka port constant. Any new Kafka test must reuse it (extend the base, or `import static ...ApplicationWithKafkaTest.PORT` into your own `@EmbeddedKafka` declaration as `NotificationIntegrationTest` and `AbstractNotificationPerformanceTest` do). Nothing else may listen on 49092 while tests run.
- **Multi-app-class caveat (notification module):** notification tests re-annotate `@SpringBootTest(classes = NotificationApplication.class)` on the subclass even when extending `ApplicationTest`, to pin the configuration class (e.g. `notification/src/test/java/com/example/simplescheduleapp/kafka/infra/consumer/KafkaIdempotencyFilterTest.java`). Course tests rely on the inherited bare `@SpringBootTest` (component scan finds `CourseApplication`).

## Test Configuration (H2, profiles)

- Each server module's `src/test/resources/application.yml` sets `spring.profiles.active: test` and defines a **profile group** `test → common-test`, plus `spring.jpa.hibernate.ddl-auto: create` (fresh schema per run — never rely on data surviving between test classes).
- The H2 datasource lives in `course/src/main/resources/application-test.yml` (activated by that profile):
  ```yaml
  spring:
    datasource:
      url: jdbc:h2:mem:testdb
      driver-class-name: org.h2.Driver
      username: sa
      password:
    jpa:
      database-platform: org.hibernate.dialect.H2Dialect
      hibernate:
        ddl-auto: create
    flyway:
      enabled: false
  ```
- `common/src/main/resources/application-common-test.yml` points Kafka producer/consumer `bootstrap-servers` at `localhost:49092` — matching the `@EmbeddedKafka` port, which is why Kafka-integrated tests work with zero extra config.
- H2 is `runtimeOnly 'com.h2database:h2'` in `course/build.gradle` and `notification/build.gradle`; MySQL (`com.mysql:mysql-connector-j`) is production-only.
- `playground/src/test/resources/application-requires-new-deadlock-{occur,not-occur}.yml` are per-scenario profiles for the deadlock tests.

## Test Structure

**Given/When/Then with Korean method names:**
```java
// course/src/test/java/com/example/simplescheduleapp/lecture/application/LectureServiceTest.java
class LectureServiceTest extends ApplicationTest {

    @Autowired LectureService lectureService;
    @Autowired LectureRepository lectureRepository;
    @Autowired TutorRepository tutorRepository;

    Tutor tutor = new Tutor("jungsehui", "Password123!", "정세희", 25, "01023420594", 3);

    @Test
    void 강의_생성_요청이_들어오면_강의를_저장한다() {
        // given
        Tutor savedTutor = tutorRepository.save(tutor);
        LectureCreateCommand command = new LectureCreateCommand(savedTutor.getId(), "테스트 제목", now, now.plusHours(1), "테스트 메모", 5);
        // when
        Lecture lecture = lectureService.createLecture(command);
        // then
        Lecture createdLecture = lectureRepository.getByLectureId(lecture.getId());
        assertThat(createdLecture.getTitle()).isEqualTo("테스트 제목");
    }
}
```
- Method names: Korean full sentences, `_` word separator. Classes with non-ASCII names add `@SuppressWarnings("NonAsciiCharacters")` and `@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)`.
- `// given` / `// when` / `// then` comments delimit every test — follow this in new tests.
- Richer classes add Korean `@DisplayName` (class-level style `"EventRecorder 은(는)"`, `"알림 시스템 통합 테스트"`) and `@Nested` groups (`NotificationIntegrationTest` is the only current `@Nested` user).
- **Rollback via `@Transactional` on the test class** for repository-touching Spring tests: `EventRecorderTest`, `KafkaIdempotencyFilterTest` (`notification/src/test/java/com/example/simplescheduleapp/kafka/...`). Others rely on the fresh `ddl-auto: create` schema.
- `@BeforeEach` is used to `reset(...)` shared `@SpyBean`/`@MockitoBean` singletons between methods (`NotificationIntegrationTest.setUp()`); no `@AfterAll` cleanup patterns.

## Mocking

**Framework:** Mockito — BDDMockito style (`given(...).willReturn(...)`, `then(mock).should()`) preferred; `when/thenReturn` appears in the controller slice test.

**Spring-context test with bean overrides (dominant pattern — real service, mocked repositories):**
```java
// course/src/test/java/com/example/simplescheduleapp/lecture/application/StudentLectureEnrollmentServiceTest.java
class StudentLectureEnrollmentServiceTest extends ApplicationTest {

    @Autowired  private LectureEnrollmentService lectureEnrollmentService; // real bean
    @MockitoBean private LectureRepository lectureRepository;              // replaced in context
    @MockitoBean private LectureEnrollmentRepository lectureEnrollmentRepository;

    @Test
    void 수강신청_조회_시_결과가_없으면_예외를_던진다() {
        given(lectureEnrollmentRepository.getAllByLectureId(500L)).willReturn(List.of());
        assertThatThrownBy(() -> lectureEnrollmentService.getLectureEnrollments(500L))
                .isInstanceOf(ApplicationException.class)
                .extracting("code")
                .isEqualTo(LectureEnrollmentExceptionCode.LECTURE_ENROLLMENT_NOT_FOUND);
    }
}
```
- `@MockitoBean` (Spring Boot 3.4's replacement for deprecated `@MockBean`) is the standard — 10 test files use it; `@MockBean` is never used.
- `@SpyBean` wraps real beans to test actual branching while verifying calls — only in `NotificationIntegrationTest` (`NotificationDispatcher`, `SseConnectionPublisher`, `FcmService` spied; `FirebaseMessaging`, `RedisClientManager`, repositories fully mocked with the comment "외부 의존성은 완전히 Mocking").
- Plain `mock(Class)` objects are used inline for entities whose behavior is stubbed (`TutorLectureEnrollmentServiceTest`: `mock(Lecture.class)` + `given(lecture.enroll(student)).willReturn(enrollment)`).
- `MockedStatic` + `ArgumentCaptor` for Firebase static APIs (`ApiFutures`) in `notification/src/test/java/com/example/simplescheduleapp/fcm/application/FcmServiceSuccessTest.java` / `FcmServiceFailureTest.java`.
- Controller slice: `@WebMvcTest(MemberController.class)` + `MockMvc` + `@MockitoBean` for service/token beans — `course/src/test/java/com/example/simplescheduleapp/member/presentation/MemberControllerTest.java` (`mockMvc.perform(post("/login")...).andExpect(jsonPath("$.accessToken").value(...))`).

**Pure unit tests (no Spring):** entity tests construct objects directly and assert invariants — `MemberTest`, `PasswordTest` (`course/src/test/java/com/example/simplescheduleapp/member/domain/entity/`); `EventRecordListenerTest` extends `UnitTest` for Mockito-only listener logic.

## Fixtures and Factories

**Fixture Monkey via inherited `sut`:**
```java
Lecture lecture = sut.giveMeBuilder(Lecture.class)
        .set("id", lectureId)
        .sample();

PendingLectureEnrollment pending = sut.giveMeBuilder(PendingLectureEnrollment.class)
        .set("lectureId", 1L)
        .set("studentId", studentId)
        .sample();
```
- Reach for Fixture Monkey when an entity has many irrelevant fields or protected constructors; hand-construct via public constructors for simple cases (`new Tutor("jungsehui", "Password123!", "정세희", 25, "01023420594", 3)`).
- No separate `fixtures/` directory; fixture building is inlined per test. Reusable **test doubles** for the event system live in `notification/src/test/java/com/example/simplescheduleapp/kafka/event/mock/` (`TestDomainEvent`, `TestEventService`).

## Coverage

**Requirements:** None enforced. No JaCoCo (or any coverage plugin) in any `build.gradle`. CI gates on build+test success only (`.github/workflows/ci.yml`); failed-test reports are uploaded as artifacts (`**/build/reports/tests/test/`).

## Test Types

**Unit Tests:** extend `UnitTest`/`MockTestSupport` or nothing at all (entity tests). Fast, no Spring context. Examples: `MemberTest`, `PasswordTest`, `EventRecordListenerTest`.

**Integration Tests (majority):** extend `ApplicationTest` (full context + H2) — `LectureServiceTest`, `EventRecorderTest`, `KafkaIdempotencyFilterTest`, `FcmServiceSuccessTest`, SSE tests. Or `ApplicationWithKafkaTest` for embedded-Kafka flows — `course/src/test/java/com/example/simplescheduleapp/lecture/application/TutorLectureEnrollmentServiceTest.java` (the canonical produce-and-verify example):
```java
// when
lectureEnrollmentService.acceptEnrollment(command);
// then — consume the real Kafka record from the embedded broker
ConsumerRecords<String, String> records = waitingConsumeTopicSync(KafkaTopics.COURSE_EVENT_TOPIC);
assertThat(records.count()).isEqualTo(1);
CourseEventMessage message = objectMapper.readValue(records.iterator().next().value(), CourseEventMessage.class);
assertThat(message.senderId()).isEqualTo(tutorId);
```
`waitingConsumeTopicSync/Async(topic)` (in `ApplicationWithKafkaTest`) spins up a throwaway `KafkaConsumer` against the embedded broker and polls up to 10 s — use it to inspect the raw produced record instead of mocking `KafkaTemplate`.

**Controller Slice:** `@WebMvcTest` — `MemberControllerTest` only.

**Concurrency / Deadlock Tests (`playground/src/test/java/com/example/playground/requiresnewdeadlock/`):**
- `RequiresNewDeadLockOccurTest` — reproduces a `REQUIRES_NEW` connection-pool deadlock with `ExecutorService` + `CountDownLatch` firing 2 concurrent event publications. It is **`@Disabled` by default** with the comment `// 데드락 확인 시에만 풀기` ("enable only when checking for deadlock") because it intentionally exhausts the pool — never remove `@Disabled` in CI runs.
- `RequiresNewDeadLockNotOccurTest` — the fixed-configuration counterpart, enabled and green.
- Both pin scenario config via `@ActiveProfiles("requires-new-deadlock-occur"/"...-not-occur")` mapping to the YAMLs in `playground/src/test/resources/`.

**Performance-Comparison Tests (`notification/src/test/java/com/example/simplescheduleapp/kafka/consumer/`):**
- `AbstractNotificationPerformanceTest` (abstract, `@EmbeddedKafka` on shared PORT) publishes a lecture event fanning out to `NUM_STUDENTS = 500` mocked notifications with `DELAY_PER_NOTIFICATION_MS = 10`, measuring with `StopWatch` + `CountDownLatch`; subclasses (`...ForLoopTest`, `...ParallelStreamTest`, `...ExecutorTest`) compare dispatch strategies. Treat these as benchmarks, not correctness gates.

**Load Tests (outside JUnit):** k6 scripts in `playground/k6-scripts/` — `special-lecture-enrollment-concurrent-test.js`, `special-lecture-enrollment-concurrent-immediately-test.js` (the special-lecture concurrency defense is verified end-to-end here, NOT in JUnit on this branch), plus async-strategy scripts (`virtual-thread-*.js`, `thread-pool-*.js`, `single-thread.js`). nGrinder assets in `ngrinder/` and `playground/ngrinder-controller/`. These require a running server + external tooling; they are not part of `./gradlew test`.

**E2E Tests:** Not used (no Selenium/Playwright/Cypress).

## Common Patterns

**Async/eventual-consistency assertions (Kafka → SSE/FCM dispatch):**
```java
// notification/src/test/java/com/example/simplescheduleapp/notification/NotificationIntegrationTest.java
kafkaTemplate.send(KafkaTopics.COURSE_EVENT_TOPIC, message);

await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
    verify(sseConnectionPublisher, times(1)).publishSseNotification(any(NotificationRequest.class));
    verify(fcmService, never()).sendFcmNotification(any());
});
```
Always use Awaitility `await().untilAsserted(...)` after publishing — never `Thread.sleep` — since consumption happens on another thread.

**Error/exception testing (current idiom):**
```java
assertThatThrownBy(() -> lectureEnrollmentService.getLectureEnrollments(id))
        .isInstanceOf(ApplicationException.class)
        .extracting("code")
        .isEqualTo(LectureEnrollmentExceptionCode.LECTURE_ENROLLMENT_NOT_FOUND);
```
(The inherited `exceptionTest(executable, code)` helper from `CommonExceptionTest` is available as a shorthand but currently unused.)

**Interaction verification:** `then(mock).should().save(entity)` / `verify(mock, times(n))` / `verify(mock, never())` — used heavily to assert exact collaborator calls, especially around Kafka publication and notification fallback branching.

---

*Testing analysis: 2026-07-02*
