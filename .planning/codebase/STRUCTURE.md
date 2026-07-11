# Codebase Structure

**Analysis Date:** 2026-07-02
**Analysis Basis:** Branch `feature/query-performance-tuning`, tip commit `3e4f654` (570 tracked files, 270 Java files: `common` 55, `course` 110, `notification` 68, `playground` 37, `ngrinder` 0). Branch `chore/ai-infra-cicd` points at the same commit. All paths are repository-relative and do **not** exist on `main`, which is a 12-file skeleton whose stale root `src/` (package `com.example.simplescheduleappback`) was removed when the project split into modules — never build on it.

## Directory Layout

```
ssa/  (rootProject.name = "simple-schedule-app")
├── settings.gradle           # includes (grouped by comment): INFRA common; BOOT course, notification; TEST playground, ngrinder
├── build.gradle              # root: Boot 3.4.3 / dep-mgmt 1.1.7 'apply false'; subprojects{}: java-library, Java 21 toolchain, Lombok, JUnit platform
├── docker-compose.yml        # course_db (mysql:8.0, 3306), notification_db (mysql:8.0, 3307), redis:7.2 ("sse-redis", 6379), cp-zookeeper/cp-kafka 7.3.2 (9092)
├── STUDY.md                  # root-level study notes (45 lines)
├── common/                   # Shared library (java-library + java-test-fixtures; bootJar disabled)
│   ├── build.gradle
│   └── src/
│       ├── main/java/com/example/simplescheduleapp/common/
│       │   ├── aop/                  # AopForTransaction (lock-wraps-transaction helper)
│       │   ├── auth/                 # Token, TokenService, TokenProperty, BearerTokenExtractor, Auth, AuthArgumentResolver, AuthConfig, TokenExceptionCode
│       │   ├── config/               # AsyncConfig (@EnableAsync), CorsConfig, JpaConfig (@EnableJpaAuditing)
│       │   ├── domain/               # BaseDomain, SoftDeletedDomain (@MappedSuperclass)
│       │   ├── event/                # DomainEvent, DomainEventRepository, EventStatus + exception/, mapper/, outbox/, producer/
│       │   ├── exception/            # ApplicationException, ExceptionCode, CommonExceptionHandler, InternalServerExceptionCode + response/
│       │   └── kafka/                # KafkaLectureEventMessage, LectureEventType + consumer/(+idempotency/), deadletter/, producer/, topic/
│       ├── main/resources/           # application-common-{local,test}.yml (FCM credential json is untracked/gitignored)
│       └── testFixtures/java/com/example/simplescheduleapp/support/
│                                     # ApplicationTest, ApplicationWithKafkaTest, CommonExceptionTest, MockTestSupport, MonkeySupport, UnitTest
├── course/                   # Boot server :8080 → course_db; Kafka group ssa-course-server-group
│   ├── build.gradle          # :common, redisson-spring-boot-starter, mysql, h2, ...
│   └── src/
│       ├── main/java/com/example/simplescheduleapp/
│       │   ├── CourseApplication.java
│       │   ├── config/               # RedisTemplateConfig, RedissonConfig
│       │   ├── consultation/         # domain/ application/ presentation/
│       │   ├── event/                # LectureEnrollment{Requested,Accepted,Rejected,Canceled}Event, LectureUpdatedEvent + mapper/CourseDomainEventMapper
│       │   ├── lecture/
│       │   │   ├── general/          # domain/(+service/) application/(+command/) presentation/(+request/,response/) exception/
│       │   │   └── special/          # same shape; capacity-limited lectures with Redis-gated enrollment
│       │   ├── member/               # login: domain/(Password, service/MemberRegister) application/ presentation/(+request/,response/) exception/
│       │   ├── parent/               # domain/(+service/) application/(+command/) presentation/(+request/)
│       │   ├── redis/                # aop/(RedissonDistributedLockAop, CustomSpringELParser), lock/(RedissonDistributedLock, SimpleRedisLock)
│       │   ├── schedule/domain/      # Schedule.java — JOINED-inheritance base of Lecture/SpecialLecture/Consultation
│       │   ├── student/              # domain/(+service/) application/(+command/) presentation/(+request/,response/)
│       │   └── tutor/                # domain/(+service/) application/(+command/) presentation/(+request/,response/)
│       ├── main/resources/           # application.yml + application-{local,test,stage,prod}.yml
│       └── test/java/com/example/simplescheduleapp/   # lecture/application/, member/{application,domain/entity,presentation}/
├── notification/             # Boot server :8081 → notification_db; Kafka group ssa-notification-server-group
│   ├── build.gradle          # :common, spring-data-redis, firebase-admin, spring-retry, spring-aop, ...
│   └── src/
│       ├── main/java/com/example/simplescheduleapp/
│       │   ├── NotificationApplication.java
│       │   ├── config/               # RedisTemplateConfig, ThreadPoolConfig
│       │   ├── fcm/                  # application/ config/ domain/(+service/) exception/ presentation/ utils/
│       │   ├── kafka/consumer/       # NotificationKafkaConsumer (single listener → strategy dispatch)
│       │   ├── notification/
│       │   │   ├── application/      # NotificationDispatcher, NotificationFacade + event/
│       │   │   ├── client/           # CourseClient (RestClient → course /internal) + config/(ClientConfig, ClientProperties), response/
│       │   │   ├── config/           # RetryConfig, SchedulerConfig
│       │   │   ├── domain/           # FailedNotification, NotificationType
│       │   │   ├── exception/
│       │   │   ├── schedule/         # NotificationRetryScheduler, NotificationRetryService
│       │   │   └── strategy/         # NotificationStrategy, NotificationStrategyFactory + lecture/*Strategy (5 impls)
│       │   ├── redis/                # cache/ publisher/ subscriber/ topic/ — SSE fan-out via Redis pub-sub
│       │   └── sse/                  # application/(SseConnectionService, SseConnectionPublisher) cache/(SseEmitterRepository) config/ exception/ presentation/(+response/)
│       ├── main/resources/           # application.yml + application-{local,test,stage,prod}.yml; client.course-server-internal-url
│       └── test/java/com/example/simplescheduleapp/   # fcm/, kafka/{consumer,event,infra}/, notification/schedule/, sse/
├── playground/               # Experiments & load-test assets — NOT part of course/notification runtime
│   ├── build.gradle
│   ├── docker-compose-*.yml  # thread-pool/virtual-thread block/non-block, single-thread, influxdb, ngrinder, special-lecture concurrent-enrollment scenarios
│   ├── k6-scripts/           # k6 load scripts (*.js)
│   ├── grafana-dashboard/ grafana-provisioning/   # metrics dashboards for the benchmarks
│   ├── ngrinder-controller/  # nGrinder controller runtime data (war libs, repos, script templates)
│   ├── MacOsPerformanceMetric.bash
│   └── src/
│       ├── main/java/com/example/playground/
│       │   ├── async/{config,event,singlethread,threadpool,virtualthread}/   # execution-model benchmarks
│       │   ├── common/domain/
│       │   └── healthcheck/
│       └── test/
│           ├── java/com/example/playground/requiresnewdeadlock/   # deadlock repro tests (@Disabled by default)
│           └── resources/application-requires-new-deadlock-{occur,not-occur}.yml
└── ngrinder/                 # Load-test module placeholder
    └── build.gradle          # groovy plugin + org.ngrinder:ngrinder-core:3.5.9; no source files at this tip
```

Uncommitted on the `chore/ai-infra-cicd` working tree (not yet in git): `.github/workflows/deploy.yml`, `deploy/nginx/`, `course/Dockerfile`, `notification/Dockerfile`. Tracked `.github/` currently contains only `PULL_REQUEST_TEMPLATE.md`.

## Directory Purposes

**`common/` (module):**
- Purpose: Shared library consumed by both boot servers via `implementation project(':common')`; not deployable on its own.
- Contains: JWT auth, base JPA classes, exception framework, transactional-outbox + Kafka event publishing, Kafka consumer plumbing (idempotency filter, consume history, dead-letter), shared test fixtures (`java-test-fixtures`).
- Key files: `common/src/main/java/com/example/simplescheduleapp/common/exception/ApplicationException.java`, `common/src/main/java/com/example/simplescheduleapp/common/event/outbox/EventRecorder.java`, `common/src/main/java/com/example/simplescheduleapp/common/kafka/consumer/KafkaIdempotencyFilter.java`, `common/src/testFixtures/java/com/example/simplescheduleapp/support/ApplicationTest.java`.

**`course/` (module):**
- Purpose: Primary business boot server — people (member/tutor/student/parent), lectures (general + special), enrollment workflow, consultations, schedules; publishes all Kafka events.
- Contains: One package per bounded context, each split `domain/`, `application/` (+`command/`), `presentation/` (+`request/`, `response/`), `exception/`; plus `event/` (DomainEvent subtypes + mapper), `redis/` (locks), `config/`.
- Key files: `course/src/main/java/com/example/simplescheduleapp/CourseApplication.java`, `course/src/main/java/com/example/simplescheduleapp/lecture/general/application/LectureEnrollmentService.java`, `course/src/main/java/com/example/simplescheduleapp/lecture/special/application/RedisSpecialLectureEnrollmentService.java`, `course/src/main/java/com/example/simplescheduleapp/lecture/general/presentation/LectureEnrollmentInternalController.java`.

**`notification/` (module):**
- Purpose: Notification-delivery boot server — consumes Kafka events, chooses SSE (connected) vs FCM (push) delivery, retries failures, exposes FCM-token and SSE endpoints, calls `course` internally over REST.
- Contains: `fcm/`, `sse/`, `kafka/consumer/`, `notification/` (dispatch + client + strategy + retry), `redis/` (pub-sub fan-out), `config/`.
- Key files: `notification/src/main/java/com/example/simplescheduleapp/NotificationApplication.java`, `notification/src/main/java/com/example/simplescheduleapp/kafka/consumer/NotificationKafkaConsumer.java`, `notification/src/main/java/com/example/simplescheduleapp/notification/strategy/NotificationStrategyFactory.java`, `notification/src/main/java/com/example/simplescheduleapp/notification/client/CourseClient.java`.

**`playground/` (module):**
- Purpose: Isolated experimentation sandbox — reproduces the `REQUIRES_NEW` deadlock, benchmarks async models (single-thread vs thread-pool vs virtual-thread), and stores load-test infrastructure (k6, grafana, nGrinder controller data, scenario docker-composes). Treat as documentation-via-code.
- Key files: `playground/src/test/java/com/example/playground/requiresnewdeadlock/RequiresNewDeadLockOccurTest.java`, `RequiresNewDeadLockNotOccurTest.java` (both `@Disabled`), `playground/k6-scripts/`, `playground/docker-compose-special-lecture-enrollment-concurrent-test.yml`.

**`ngrinder/` (module):**
- Purpose: Reserved for nGrinder Groovy load-test scripts (`org.ngrinder:ngrinder-core:3.5.9`, `groovy` plugin).
- Contains: Only `ngrinder/build.gradle` at this tip — zero source files. nGrinder controller runtime assets live under `playground/ngrinder-controller/` instead.

## Key File Locations

**Entry Points:**
- `course/src/main/java/com/example/simplescheduleapp/CourseApplication.java`: course server (8080).
- `notification/src/main/java/com/example/simplescheduleapp/NotificationApplication.java`: notification server (8081).

**Configuration:**
- `settings.gradle`: module list; `build.gradle` (root): shared plugin versions + `subprojects {}` (Java 21, Lombok, `java-library`, JUnit platform).
- `course/build.gradle`, `notification/build.gradle`, `common/build.gradle`, `playground/build.gradle`, `ngrinder/build.gradle`: per-module dependencies.
- `course/src/main/resources/application.yml` (+ `-local`, `-test`, `-stage`, `-prod`): port 8080, `course_db`, Hikari max 21, Kafka group `ssa-course-server-group`, `cors.allowed-origins`.
- `notification/src/main/resources/application.yml` (+ same suffixes): port 8081, `notification_db`, `open-in-view: false`, Kafka group `ssa-notification-server-group`, `fcm.key.json`, `client.course-server-internal-url: http://localhost:8080/internal`.
- `common/src/main/resources/application-common-{local,test}.yml`: shared config fragments (activated via `spring.profiles.group`).
- `docker-compose.yml` (root): local infra with healthchecks on both DBs and Redis.

**Core Logic:**
- `course/src/main/java/com/example/simplescheduleapp/lecture/`: lecture + enrollment domain (general and special).
- `notification/src/main/java/com/example/simplescheduleapp/notification/`: dispatch, strategies, retry, course client.
- `common/src/main/java/com/example/simplescheduleapp/common/event/` and `common/kafka/`: cross-service event/outbox/consumer framework.

**Testing:**
- `course/src/test/java/com/example/simplescheduleapp/`: `lecture/application/`, `member/{application,domain/entity,presentation}/` tests.
- `notification/src/test/java/com/example/simplescheduleapp/`: `fcm/application/`, `kafka/{consumer,event/{mock,outbox,producer},infra/{consumer,deadletter}}/`, `notification/schedule/`, `sse/{application,cache,event}/` tests.
- `common/src/testFixtures/java/com/example/simplescheduleapp/support/`: `ApplicationTest`, `ApplicationWithKafkaTest`, `CommonExceptionTest`, `MockTestSupport`, `MonkeySupport` (Fixture Monkey), `UnitTest` — consumed by both suites via `testImplementation(testFixtures(project(':common')))`.
- `playground/src/test/java/com/example/playground/requiresnewdeadlock/`: manual deadlock repro.

## Naming Conventions

**Files:**
- Entities: singular noun — `Lecture.java`, `Tutor.java`, `SpecialLectureEnrollment.java`, `FailedNotification.java`.
- Repositories: `{Entity}Repository.java` — Spring Data interfaces with default "getOrThrow"-style methods that raise `ApplicationException` instead of returning `Optional` to callers.
- Application services: `{Feature}Service.java` in `application/`; domain services in `domain/service/` (`MemberRegister`, `TutorRegister`, `PendingLectureEnrollmentService`).
- Commands: `{Action}Command.java` records in `application/command/` (e.g. `SpecialLectureEnrollmentCreateCommand`).
- Requests/Responses: `{Action}Request.java` / `{Action}Response.java` records in `presentation/request|response/`, bridged by `toCommand()` and static `from()`/`of()`.
- Exception codes: `{Domain}ExceptionCode.java` enum implementing `ExceptionCode`, one per bounded context.
- Events: `{Entity}{PastTenseAction}Event.java` (`LectureEnrollmentRequestedEvent`, `LectureUpdatedEvent`) in `course/src/main/java/com/example/simplescheduleapp/event/`.
- Strategies: `{Action}{Entity}Strategy.java` (`AcceptEnrollmentStrategy`) under `notification/.../notification/strategy/lecture/`.
- Internal (service-to-service) controllers: `{Feature}InternalController.java` mapped under `/internal/**`.
- Tests: `{ClassUnderTest}Test.java`; scenario-suffixed suites where useful (`FcmServiceFailureTest`, `RequiresNewDeadLockOccurTest`).

**Directories / Packages:**
- Package-by-feature at module top level, then package-by-layer within each feature (`domain`, `application`, `presentation`, `exception`, plus `application/command`, `presentation/request`, `presentation/response`, `domain/service`).
- Base package for both boot modules: `com.example.simplescheduleapp` (singular "app", no "back"); `common` uses `com.example.simplescheduleapp.common`. The skeleton package `com.example.simplescheduleappback` exists only on `main` and is dead.

## Where to Add New Code

**New domain feature in `course`:**
- Create `course/src/main/java/com/example/simplescheduleapp/{feature}/` with `domain/`, `application/`, `presentation/` sub-packages, using `lecture/general/` as the template shape.
- Add `{feature}/exception/{Feature}ExceptionCode.java` implementing `ExceptionCode` (`common/src/main/java/com/example/simplescheduleapp/common/exception/ExceptionCode.java`).
- Mirror the package under `course/src/test/java/com/example/simplescheduleapp/{feature}/`.

**New Kafka event / notification type:**
1. Add the enum constant to `LectureEventType` (`common/src/main/java/com/example/simplescheduleapp/common/kafka/LectureEventType.java`).
2. Add a `DomainEvent` subtype in `course/src/main/java/com/example/simplescheduleapp/event/` and handle it in `CourseDomainEventMapper` (`course/src/main/java/com/example/simplescheduleapp/event/mapper/CourseDomainEventMapper.java`).
3. Add a `NotificationStrategy` implementation under `notification/src/main/java/com/example/simplescheduleapp/notification/strategy/lecture/` implementing `getSupportType()` for the new enum — it auto-registers in `NotificationStrategyFactory` via list injection; no factory edits.

**New REST endpoint:**
- Extend the feature's existing `presentation/{Feature}Controller.java`, or add a new controller in that `presentation/` package; DTOs go in `presentation/request|response/` with `toCommand()`/`from()`.
- For service-to-service-only endpoints, follow `LectureEnrollmentInternalController` (`course/src/main/java/com/example/simplescheduleapp/lecture/general/presentation/LectureEnrollmentInternalController.java`): `Internal` suffix + `/internal/**` path, separate from the public controller. The consuming side belongs in `notification/src/main/java/com/example/simplescheduleapp/notification/client/` next to `CourseClient`.

**Shared cross-cutting utility:**
- Add under `common/src/main/java/com/example/simplescheduleapp/common/` — both boot modules pick it up automatically. Do not duplicate logic in `course` and `notification`.

**New distributed-lock use case:**
- Annotate the method with `@RedissonDistributedLock(key = "...")` (`course/src/main/java/com/example/simplescheduleapp/redis/lock/RedissonDistributedLock.java`); `RedissonDistributedLockAop` handles acquire/release. This mechanism exists only in `course`; if `notification` needs it, promote the `redis/aop` + `redis/lock` packages into `common` rather than copying.

**New test:**
- Integration: extend `ApplicationTest` or `ApplicationWithKafkaTest` (`common/src/testFixtures/java/com/example/simplescheduleapp/support/`). Unit: `UnitTest`/`MockTestSupport`. Test data: `MonkeySupport` (Fixture Monkey). Fixtures flow through `testImplementation(testFixtures(project(':common')))` in both boot modules' build files.

## Special Directories

**`playground/`:**
- Purpose: Non-production experiments + load-test infrastructure (k6, grafana provisioning, nGrinder controller data, scenario composes). Not a dependency of the product services.
- Generated: No (except `ngrinder-controller/` contents, which are tool runtime data). Committed: Yes.

**`ngrinder/`:**
- Purpose: Placeholder module for Groovy load scripts; build file only at this tip.
- Generated: No. Committed: Yes.

**`build/` (per module) and `.gradle/`:**
- Purpose: Gradle outputs and cache. Generated: Yes. Committed: No.

**`gradle/wrapper/`, `gradlew`, `gradlew.bat`:**
- Purpose: Gradle wrapper bootstrap. Committed: Yes.

**`common/src/main/resources/ssa-fcm-firebase-adminsdk-fbsvc-*.json`, `notification/src/main/resources/ssa-fcm-firebase-adminsdk-fbsvc-*.json`:**
- Purpose: Firebase service-account credentials for FCM (referenced by `fcm.key.json` in `notification/src/main/resources/application.yml`). Gitignored by filename in the root `.gitignore`.
- Generated: No (provisioned per environment). Committed: No — never read or commit their contents.

**`.omc/`:**
- Purpose: oh-my-claudecode session state committed on this branch (`docs: .omc/` is the tip commit). Tooling metadata, not application code.

---

*Structure analysis: 2026-07-02, branch `feature/query-performance-tuning` @ `3e4f654`*
