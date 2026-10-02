# Architecture

<!-- STALE-SNAPSHOT banner -->
> [!WARNING]
> **스냅샷 문서입니다. 현재 코드와 다를 수 있습니다.**
> 본문은 2026-07-02 `feature/query-performance-tuning` 분석이 바탕이고, 2026-08에 ADR-0003 Stage 2와 3a(단일 JVM `app`, Spring Boot 4.1.0, PostgreSQL 운영 DB)를 일부만 보정했습니다.
> 그래서 옛 구조 서술과 새 구조 서술이 섞여 있을 수 있습니다.
> 현재 사실의 정본은 루트 `AGENTS.md`와 `docs/adr/`입니다. 본문과 정본이 다르면 정본을 따르고, 이 문서는 다음 재분석 때 다시 생성합니다.

**Analysis Date:** 2026-07-02
**Analysis Basis:** Branch `feature/query-performance-tuning`, tip commit `3e4f654` (570 tracked files, 270 Java files). Branch `chore/ai-infra-cicd` currently points at the **same commit** — no CI/CD commits exist in git yet (a working tree on that branch carries uncommitted `.github/workflows/deploy.yml`, `deploy/nginx/`, and per-module `Dockerfile` scaffolding). All paths below are repository-relative and resolve only on this branch (or `chore/ai-infra-cicd`), **not** on `main`.

## Branch Topology

**`main` is a skeleton — do not navigate it.** It has only 12 files: a bare Spring Boot starter under `src/main/java/com/example/simplescheduleappback/` (note the different, dead package name) plus gradle wrapper. It was never fast-forwarded past its initial commits.

| Branch | Relationship | Content |
|---|---|---|
| `main` | root, 3 commits | Skeleton only (12 files, package `com.example.simplescheduleappback`) |
| `develop` | integration branch | Full multi-module implementation (560 tracked files); common ancestor of both active lines |
| `feature/query-performance-tuning` | `develop` + 24 commits (tip `3e4f654`) | **The active branch documented here** — Kafka/outbox restructuring, notification retry/strategy rework, Redis fixes, deadlock tests |
| `chore/ai-infra-cicd` | same commit as `feature/query-performance-tuning` | CI/CD work in progress; only uncommitted scaffold so far |
| `feature/2-4-observability` | `develop` + separate ~250-commit line | Observability work (Actuator, MDC, structured logging; special-lecture enrollment defense history); **not merged** into `develop` or this branch |
| `feature/server-separation`, `feature/kafka`, `feature/sse`, `feature/special-lecture`, `feature/lecture*`, `feature/schedule`, `feature/tutor`, `test/select-lock-deadlock` | historical | Merged into or superseded by `develop`; do not build on them |

Verify with: `git log --oneline develop..feature/query-performance-tuning` (24 commits) and `git merge-base --is-ancestor develop feature/query-performance-tuning` (true).

## Pattern Overview

**Overall:** Layered architecture (Presentation → Application → Domain) inside package-by-feature bounded contexts. As of ADR-0003 Stage 2 (single-JVM modular monolith), `course` and `notification` are **bounded-context library modules with no own bootJar** — they are assembled by the `:app` module into **one Spring Boot process on port 8080** (`:8081` no longer exists), alongside a shared library module (`common`), integrated asynchronously via **Kafka with a transactional outbox**.

**Key Characteristics:**
- Multi-module Gradle build (`settings.gradle.kts` groups them: INFRA = `common`; bounded-context libraries (no own bootJar) = `course`, `notification`; BOOT = `app` (the sole executable module, ADR-0003 Stage 2); TOOLS = `playground`, `ngrinder`). Java 21 toolchain, Spring Boot 3.4.3, Lombok applied to every subproject from the root `build.gradle`.
- Package-by-feature inside each bounded-context module: `course` has `com.example.simplescheduleapp.{member,tutor,student,parent,lecture.general,lecture.special,consultation,schedule,event,redis,config}`; `notification` has `com.example.simplescheduleapp.{fcm,sse,notification,config}` (the root-level `kafka/` and `redis/` packages no longer exist — see Data Flow / SSE emitter lifecycle below for their new locations).
- Each feature slice splits into `domain/`, `application/` (+ `command/`), `presentation/` (+ `request/`, `response/`), `exception/` sub-packages.
- Rich domain model — entities carry behavior (`Lecture.enroll()`, `Lecture.cancel()`, `SpecialLecture.enroll()`, `Schedule.validatePastTime()`), not anemic data holders.
- Single shared database for the `:app` JVM (local MySQL schema `ssa` on 3306; prod Supabase PostgreSQL schema `ssa` — ADR-0003 Stage 2). The root `docker-compose.yml` still starts separate `course_db`/`notification_db` MySQL containers (3306/3307) from the pre-Stage-2 topology, but `app`'s local config only connects to the `ssa` schema on 3306; the `notification_db` container is unused local infra left over from before the merge.
- Cross-service consistency via domain events + outbox + Kafka (single topic), never distributed transactions.
- Concurrency-heavy special-lecture enrollment uses a 4-layer defense (Redis atomic counter → transactional DB write → DB unique constraint → Redis compensating transaction); a general-purpose Redisson fair-lock AOP exists separately.

## Module Composition

| Module | Java files | Role |
|---|---|---|
| `common` | 55 | Shared library (`java-library` + `java-test-fixtures`; `bootJar` disabled). Auth/JWT, base entities, exception framework, outbox/event framework, Kafka producer+consumer plumbing, shared test fixtures |
| `course` | 110 | Bounded-context library, no own port — assembled into `:app` (ADR-0003 Stage 2). People (member/tutor/student/parent), lectures (general + special), enrollment, consultations, schedules. Publishes Kafka events |
| `notification` | 68 | Bounded-context library, no own port — assembled into `:app` (ADR-0003 Stage 2; `:8081` no longer exists). Consumes Kafka events; SSE + FCM delivery, retry |
| `playground` | 37 | Experiments only (async models, deadlock repro, k6/grafana/ngrinder-controller assets). Not wired into product runtime |
| `ngrinder` | 0 | `ngrinder/build.gradle` only (Groovy + `org.ngrinder:ngrinder-core:3.5.9`); no source files at this tip |

## Layers

**Presentation (`presentation/`):**
- Purpose: HTTP entry points — `@RestController` classes and request/response DTO records.
- Location: e.g. `course/src/main/java/com/example/simplescheduleapp/lecture/general/presentation/LectureController.java`, `.../presentation/request/LectureCreateRequest.java`, `.../presentation/response/LectureCreateResponse.java`
- Contains: Controllers; records with `toCommand()` / static `from()`/`of()` factory methods.
- Depends on: Application layer services + command objects.
- Used by: External HTTP clients; `LectureEnrollmentInternalController` was used by `notification` via `CourseClient` in the pre-ADR-0003 3-process topology (see the superseded note under Data Flow) — `notification` is no longer a separate service.

**Application (`application/`):**
- Purpose: Use-case orchestration — transaction boundaries, repository coordination, domain-event publication.
- Location: e.g. `course/src/main/java/com/example/simplescheduleapp/lecture/general/application/LectureEnrollmentService.java`, `course/src/main/java/com/example/simplescheduleapp/lecture/special/application/RedisSpecialLectureEnrollmentService.java`
- Contains: `@Service` + `@Transactional` classes; `command/` sub-package with input records (e.g. `SpecialLectureEnrollmentCreateCommand`).
- Depends on: Domain entities/repositories, `ApplicationEventPublisher`, Redis clients.
- Used by: Presentation layer.

**Domain (`domain/`):**
- Purpose: JPA entities with behavior, Spring Data repositories, small domain services for cross-entity invariants.
- Location: e.g. `course/src/main/java/com/example/simplescheduleapp/lecture/general/domain/Lecture.java`, `.../domain/LectureRepository.java`, `.../domain/service/PendingLectureEnrollmentService.java`, `course/src/main/java/com/example/simplescheduleapp/member/domain/service/MemberRegister.java`
- Contains: `@Entity` classes extending `BaseDomain`/`SoftDeletedDomain`, `JpaRepository` interfaces (default methods provide "getOrThrow" lookups that raise `ApplicationException`), value objects (e.g. `member/domain/Password.java`).
- Depends on: `common` module base classes and exception types only.
- Used by: Application layer.

**Infrastructure / cross-cutting (`common` module + per-module `config/`, `redis/`, `fcm/`, `sse/`):**
- Purpose: Auth, exception handling, outbox + Kafka plumbing, Redis locks/pub-sub, FCM push, SSE connections.
- Location: `common/src/main/java/com/example/simplescheduleapp/common/**`, `course/src/main/java/com/example/simplescheduleapp/redis/**`, `notification/src/main/java/com/example/simplescheduleapp/{fcm,sse}/**` (`notification`'s root-level `redis/` package no longer exists; Redis pub-sub now lives under `sse/infrastructure/redis/`)
- Contains: `AuthArgumentResolver`, `CommonExceptionHandler`, `KafkaEventProducer`, `EventRecorder`/`EventRecordListener`, `RedissonDistributedLockAop`, FCM services, `SseConnectionService`.
- Used by: Both bounded-context modules (`course`, `notification`) via `implementation project(':common')`, assembled together into `:app`.

## Data Flow

**Standard request (e.g. create lecture):**

1. `LectureController` (`course/src/main/java/com/example/simplescheduleapp/lecture/general/presentation/LectureController.java`) receives the request; the request record's `toCommand()` builds a command.
2. The application service runs inside `@Transactional`, constructs/persists the entity via its repository.
3. Controller returns a response record built with `Response.from(entity)`.

**Cross-service notification flow (event-driven):**

1. `LectureEnrollmentService` (`course/src/main/java/com/example/simplescheduleapp/lecture/general/application/LectureEnrollmentService.java`) persists the business change, then publishes a `DomainEvent` subtype (e.g. `LectureEnrollmentRequestedEvent`, `course/src/main/java/com/example/simplescheduleapp/event/LectureEnrollmentRequestedEvent.java`) via Spring's in-process `ApplicationEventPublisher`.
2. `EventRecordListener.recordEvent()` (`common/src/main/java/com/example/simplescheduleapp/common/event/outbox/EventRecordListener.java`), `@TransactionalEventListener(phase = BEFORE_COMMIT)`, calls `EventRecorder.record()` to persist the event as an outbox row (`DomainEvent` entity) **in the same DB transaction** as the business write. `EventRecorder` retries UUID collisions up to `MAX_UUID_RETRY = 3` (`common/src/main/java/com/example/simplescheduleapp/common/event/outbox/EventRecorder.java`).
3. After commit, `EventProducerListener.publishEvent()` (`common/src/main/java/com/example/simplescheduleapp/common/event/producer/EventProducerListener.java`), `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async(EVENT_ASYNC_TASK_EXECUTOR)` (executor defined in `common/src/main/java/com/example/simplescheduleapp/common/event/producer/EventAsyncTaskExecutorConfig.java`), invokes `KafkaEventProducer.produce()`.
4. `KafkaEventProducer` (`common/src/main/java/com/example/simplescheduleapp/common/event/producer/KafkaEventProducer.java`) maps the outbox row to a `KafkaLectureEventMessage` via `DomainEventMapperFactory` → `CourseDomainEventMapper` (`course/src/main/java/com/example/simplescheduleapp/event/mapper/CourseDomainEventMapper.java`), publishes to `KafkaTopics.COURSE_EVENT_TOPIC` (`common/src/main/java/com/example/simplescheduleapp/common/kafka/topic/KafkaTopics.java` — literal `"COURSE_EVENT_TOPIC"`, single unified topic), then marks the outbox row `produceSuccess()`/`produceFail()`.
5. `NotificationKafkaConsumer.consumeNotificationEvent()` (`notification/src/main/java/com/example/simplescheduleapp/notification/infrastructure/NotificationKafkaConsumer.java`) listens with `containerFactory = KafkaConsumerConfig.LECTURE_EVENT_CONTAINER_FACTORY` and manual `Acknowledgment`. Both consumer groups (`ssa-notification-server-group`, `ssa-course-server-group`) previously set per module are gone — the single JVM now runs one group, `ssa-server-group` (`app/src/main/resources/application.yml`; `course/src/main/resources/application.yml` and `notification/src/main/resources/application.yml` no longer exist, ADR-0003 Stage 2).
6. Before the listener body, `KafkaIdempotencyFilter` (`common/src/main/java/com/example/simplescheduleapp/common/kafka/consumer/KafkaIdempotencyFilter.java`, a `RecordFilterStrategy`) skips duplicates via `IdempotencyService`/`ConsumeHistoryIdempotencyService` (`common/src/main/java/com/example/simplescheduleapp/common/kafka/consumer/idempotency/`), backed by `KafkaMessageConsumeHistory` + `KafkaMessageProcessConsumeHistoryRepository` (same `consumer/` package).
7. `NotificationStrategyFactory.getStrategy(message.type())` (`notification/src/main/java/com/example/simplescheduleapp/notification/application/strategy/NotificationStrategyFactory.java`) builds an `EnumMap<LectureEventType, NotificationStrategy>` from constructor-injected `List<NotificationStrategy>` and dispatches to one handler per event type under `notification/src/main/java/com/example/simplescheduleapp/notification/application/strategy/lecture/` (`RequestEnrollmentStrategy`, `AcceptEnrollmentStrategy`, `RejectEnrollmentStrategy`, `CancelEnrollmentStrategy`, `LectureUpdatedStrategy`). `LectureEventType` (`common/src/main/java/com/example/simplescheduleapp/common/kafka/LectureEventType.java`) enumerates `ENROLLMENT_REQUESTED`, `ENROLLMENT_ACCEPTED`, `ENROLLMENT_REJECTED`, `ENROLLMENT_CANCELED`, `LECTURE_UPDATED`.
8. Strategy → `NotificationFacade`/`NotificationDispatcher` (`notification/src/main/java/com/example/simplescheduleapp/notification/application/`) → real-time SSE push if the user is connected (`SseConnectionService`, Redis pub-sub fan-out via `notification/src/main/java/com/example/simplescheduleapp/sse/infrastructure/redis/RedisSseMessagePublisher.java` and `RedisSseMessageSubscriber.java`), else FCM push (`notification/src/main/java/com/example/simplescheduleapp/fcm/application/`). Failures are recorded as `FailedNotification` (`notification/src/main/java/com/example/simplescheduleapp/notification/domain/FailedNotification.java`) and resent by `NotificationRetryScheduler` (`notification/src/main/java/com/example/simplescheduleapp/notification/infrastructure/`) / `NotificationRetryService` (`notification/src/main/java/com/example/simplescheduleapp/notification/application/`).
9. On listener failure the message is not acknowledged; failures route to dead-letter recording (`common/src/main/java/com/example/simplescheduleapp/common/kafka/deadletter/DeadLetter.java`, `DeadLetterRecorder.java`, `DeadLetterRepository.java`).

**Synchronous server-to-server call (notification → course) — superseded, 3-process-era description kept for history:** As of ADR-0003 Stage 2 (commit `45c694b`), `course` and `notification` run in the same `:app` JVM and this HTTP round-trip no longer happens; `app/src/main/java/com/example/simplescheduleapp/integration/InProcessEnrolledStudentsAdapter.java` now implements `EnrolledStudentsPort` with a direct in-process call instead. The paragraphs below describe the pre-Stage-2 mechanism they replaced.

- `CourseClient.getEnrolledStudentInfosByLectureId()` (`notification/src/main/java/com/example/simplescheduleapp/notification/client/CourseClient.java`) issues `GET /lectures/{lectureId}/student-ids` on a Spring `RestClient` whose base URL comes from `client.course-server-internal-url` = `http://localhost:8080/internal` (`notification/src/main/resources/application.yml`, bound by `ClientProperties`/`ClientConfig` in `notification/src/main/java/com/example/simplescheduleapp/notification/client/config/`). 4xx/5xx responses are translated to `ApplicationException(InternalServerExceptionCode.EXTERNAL_API_ERROR)`.
- Server side: `LectureEnrollmentInternalController` (`course/src/main/java/com/example/simplescheduleapp/lecture/general/presentation/LectureEnrollmentInternalController.java`) maps `GET /internal/lectures/{lectureId}/student-ids` and returns `GetEnrolledStudentInfosResponse`. The `Internal` suffix + `/internal` path prefix is the convention for service-to-service endpoints, kept separate from the public `LectureEnrollmentController`.

**State Management:**
- Persistent state: single shared schema `ssa` for the `:app` JVM (JPA/Hibernate, `ddl-auto: update`; local MySQL, prod Supabase PostgreSQL — ADR-0003 Stage 2). `course` and `notification` entities coexist in this one schema with no table-name collisions.
- Ephemeral/coordination state: shared Redis (`redis:7.2`, container `sse-redis`, port 6379) — SSE emitter registry, SSE pub-sub fan-out, special-lecture capacity counters, Redisson locks.
- Integration bus: single Kafka topic `COURSE_EVENT_TOPIC` (Confluent `cp-kafka:7.3.2` + `cp-zookeeper:7.3.2` in root `docker-compose.yml`, broker on 9092); at-least-once delivery + consumer-side dedup table = effectively-once processing.
- `spring.jpa.open-in-view: false` is set explicitly, repo-wide, to prevent connection-pool exhaustion from long-lived SSE emitters (`app/src/main/resources/application.yml` — `course`/`notification` no longer have their own `application.yml`, ADR-0003 Stage 2). Hikari pool is capped at 21 (local) / 15 (prod) for the single JVM.

## Key Abstractions

**DomainEvent / Outbox (`common/src/main/java/com/example/simplescheduleapp/common/event/`):**
- Purpose: A business fact persisted transactionally with the change and reliably published to Kafka (transactional outbox).
- Examples: `DomainEvent.java`, `DomainEventRepository.java`, `EventStatus.java`, `outbox/EventRecorder.java`, `outbox/EventRecordListener.java`, `producer/EventProducerListener.java`, `producer/KafkaEventProducer.java`, `producer/EventProducer.java`.
- Pattern: Transactional Outbox via paired `@TransactionalEventListener` phases (BEFORE_COMMIT record, AFTER_COMMIT async publish).

**DomainEventMapper / DomainEventMapperFactory (`common/src/main/java/com/example/simplescheduleapp/common/event/mapper/`):**
- Purpose: Translates `DomainEvent` subtypes into the wire-format `KafkaLectureEventMessage` (`common/src/main/java/com/example/simplescheduleapp/common/kafka/KafkaLectureEventMessage.java`).
- Examples: `DomainEventMapperFactory.java`; concrete mapper `course/src/main/java/com/example/simplescheduleapp/event/mapper/CourseDomainEventMapper.java`.
- Pattern: Factory + Strategy; each producing module contributes its own mapper.

**NotificationStrategy / NotificationStrategyFactory (`notification/src/main/java/com/example/simplescheduleapp/notification/application/strategy/`):**
- Purpose: One handler per `LectureEventType` behind a single Kafka listener.
- Examples: `NotificationStrategy.java` (declares `getSupportType()` + `handle(message)`), `NotificationStrategyFactory.java`, `strategy/lecture/*.java` (5 implementations).
- Pattern: Strategy with Spring list injection into an `EnumMap` — add a new `@Component implements NotificationStrategy`; no factory edits needed. Unknown types raise `NotificationTypeExceptionCode.NOTIFICATION_TYPE_NOT_FOUND`.

**ExceptionCode / ApplicationException (`common/src/main/java/com/example/simplescheduleapp/common/exception/`):**
- Purpose: Uniform typed errors — HTTP status + short code + Korean message — rendered centrally.
- Examples: `ExceptionCode.java` (interface), `ApplicationException.java`, `CommonExceptionHandler.java` (`@ControllerAdvice`), `InternalServerExceptionCode.java`; per-domain enums such as `course/src/main/java/com/example/simplescheduleapp/lecture/general/exception/LectureExceptionCode.java` (codes `L0`–`L7`), `course/src/main/java/com/example/simplescheduleapp/lecture/special/exception/SpecialLectureEnrollmentExceptionCode.java`, `notification/src/main/java/com/example/simplescheduleapp/sse/exception/SseExceptionCode.java`.
- Pattern: Enum-per-bounded-context implementing the common interface.

**Schedule hierarchy (JPA JOINED inheritance — `Lecture`, `SpecialLecture`, and `Consultation` are ALL siblings under `Schedule`):**
- Purpose: `Schedule` (`course/src/main/java/com/example/simplescheduleapp/schedule/domain/Schedule.java`) is the shared base entity: `@Inheritance(strategy = InheritanceType.JOINED)`, `@DiscriminatorColumn(name = "type")`, table `schedule`, owns `title`/`startTime`/`endTime`/`memo` and `validatePastTime()`.
- Subclasses: `Lecture` (`@DiscriminatorValue("LECTURE")`, table `lecture`, adds `tutor`, `capacity`, `enrolledCount`; `course/src/main/java/com/example/simplescheduleapp/lecture/general/domain/Lecture.java`), `SpecialLecture` (`@DiscriminatorValue("SPECIAL_LECTURE")`, table `special_lecture`; `course/src/main/java/com/example/simplescheduleapp/lecture/special/domain/SpecialLecture.java`), `Consultation` (`@DiscriminatorValue("CONSULTATION")`; `course/src/main/java/com/example/simplescheduleapp/consultation/domain/Consultation.java`). `SpecialLecture` extends `Schedule` directly, not `Lecture`.
- Soft delete on the base: `@SQLRestriction(DELETED_DATE_IS_NULL)` + `@SQLDelete(... SET deleted_date = CURRENT_TIMESTAMP ...)` — deletes become UPDATEs and every query auto-filters deleted rows for the whole hierarchy.

**BaseDomain / SoftDeletedDomain (`common/src/main/java/com/example/simplescheduleapp/common/persistence/`):**
- Purpose: `@MappedSuperclass` auditing base (`BaseDomain`, enabled via `@EnableJpaAuditing` in `common/src/main/java/com/example/simplescheduleapp/common/config/JpaConfig.java`) and soft-delete timestamp (`SoftDeletedDomain extends BaseDomain`).

## Entry Points

**`:app` (single JVM, ADR-0003 Stage 2 — the only production entry point):**
- Location: `app/src/main/java/com/example/simplescheduleapp/SsaApplication.java`
- Triggers: Spring Boot main; port 8080; DB local MySQL `jdbc:mysql://localhost:3306/ssa` / prod Supabase PostgreSQL (single `ssa` schema); Kafka group `ssa-server-group`. `:8081` no longer exists.
- Responsibilities (assembled from the `course` and `notification` library modules): Member login + tutor/student/parent sign-up, lecture CRUD, general + special enrollment, consultations, publishing all domain events (`course`); consuming lecture events, SSE connect endpoint (`notification/src/main/java/com/example/simplescheduleapp/sse/presentation/SseController.java`), FCM token registration (`notification/src/main/java/com/example/simplescheduleapp/fcm/presentation/`), retry scheduling (`notification`).

**`course` / `notification` (bounded-context libraries, no own entry point):**
- `course/src/test/java/com/example/simplescheduleapp/CourseApplication.java` and `notification/src/test/java/com/example/simplescheduleapp/NotificationApplication.java` are test-only Spring Boot bootstraps for running each module's suite in isolation (`:8080`/`:8081` here are moot) — not production entry points.

**`playground` module (non-product):**
- Location: `playground/src/main/java/com/example/playground/`, tests under `playground/src/test/java/com/example/playground/`
- Responsibilities: async execution-model benchmarks (`async/{singlethread,threadpool,virtualthread}/`), `REQUIRES_NEW` deadlock repro tests, healthcheck; plus load-test assets (`playground/k6-scripts/`, `playground/grafana-dashboard/`, `playground/grafana-provisioning/`, `playground/ngrinder-controller/`, and many scenario `docker-compose-*.yml` files including two special-lecture concurrent-enrollment test composes).

**`ngrinder` module:** `ngrinder/build.gradle` only (no sources at this tip); nGrinder controller runtime assets actually live under `playground/ngrinder-controller/`.

## Concurrency Handling

**Special-lecture enrollment — 4-layer defense against overselling (history developed on `feature/2-4-observability`, implementation present here):**

1. **Redis atomic capacity gate** — `SpecialLectureRedisClient.enrollSpecialLectureEnrollment()` (`course/src/main/java/com/example/simplescheduleapp/lecture/special/application/SpecialLectureRedisClient.java`) atomically `DECR`s key `special_lecture:{id}:available` (initialized at lecture creation via `initializeSpecialLecture()`); a negative result is re-incremented and throws `LectureExceptionCode.CAPACITY_EXCEEDED`. Fast path, no DB round-trip on rejection.
2. **Transactional DB write** — `SpecialLectureEnrollmentService.enrollSpecialLectureEnrollment()` (`course/src/main/java/com/example/simplescheduleapp/lecture/special/application/SpecialLectureEnrollmentService.java`) persists the `SpecialLectureEnrollment` row inside `@Transactional`.
3. **DB unique constraint** — `SpecialLectureEnrollment` (`course/src/main/java/com/example/simplescheduleapp/lecture/special/domain/SpecialLectureEnrollment.java`) declares `@UniqueConstraint(name = "uk_special_lecture_student", columnNames = {"special_lecture_id", "student_id"})`, catching duplicate-enrollment races Redis cannot see.
4. **Redis compensating transaction** — `RedisSpecialLectureEnrollmentService.enrollSpecialLectureEnrollment()` (`course/src/main/java/com/example/simplescheduleapp/lecture/special/application/RedisSpecialLectureEnrollmentService.java`) wraps 1–2: on any DB failure it calls `compensateSpecialLectureEnrollment()` (re-`INCR`), then rethrows via a `switch` mapping — `DataIntegrityViolationException` → `ALREADY_ENROLLED`, `ApplicationException` passes through, anything else → `SPECIAL_LECTURE_ENROLLMENT_FAILED`.

SAGA-style compensation rather than 2PC — Redis and DB counts reconcile through the compensation step.

**General-purpose Redisson distributed lock (separate mechanism, `course` only):**
- `@RedissonDistributedLock` (`course/src/main/java/com/example/simplescheduleapp/redis/lock/RedissonDistributedLock.java`) + `RedissonDistributedLockAop` (`course/src/main/java/com/example/simplescheduleapp/redis/aop/RedissonDistributedLockAop.java`): `@Around` advice acquires a **fair lock** (`redissonClient.getFairLock("LOCK:" + spelKey)`) with SpEL dynamic keys (`CustomSpringELParser`, same `redis/aop` package), configurable `waitTime`/`leaseTime`, delegates to `AopForTransaction.proceed()` (`common/src/main/java/com/example/simplescheduleapp/common/aop/AopForTransaction.java`) so the lock brackets the whole transaction, restores the interrupt flag on `InterruptedException`, and unlocks safely in `finally` (guarded by `isLocked() && isHeldByCurrentThread()`). A simpler `SimpleRedisLock` (`course/src/main/java/com/example/simplescheduleapp/redis/lock/SimpleRedisLock.java`) exists as an alternative implementation. Redisson wiring: `course/src/main/java/com/example/simplescheduleapp/config/`.

**Kafka duplicate-delivery defense:**
- `KafkaIdempotencyFilter` (`common/src/main/java/com/example/simplescheduleapp/common/kafka/consumer/KafkaIdempotencyFilter.java`) filters records whose UUID was already processed (`ConsumeHistoryIdempotencyService`, history entity `KafkaMessageConsumeHistory`), so notification sends do not repeat on redelivery.

**SSE emitter lifecycle:**
- `SseEmitterRegistry` (`notification/src/main/java/com/example/simplescheduleapp/sse/application/SseEmitterRegistry.java` — renamed from `SseEmitterRepository`, moved out of the now-removed `sse/cache/` package) holds per-user emitters; `SseConnectionService`/`SseConnectionPublisher` (same `sse/application/` package) manage connect/send; `RedisMessageListenerConfig` (`notification/src/main/java/com/example/simplescheduleapp/sse/config/RedisMessageListenerConfig.java`) + `notification/src/main/java/com/example/simplescheduleapp/sse/infrastructure/redis/{RedisSseMessagePublisher,RedisSseMessageSubscriber,RedisChannels}.java` (the root-level `redis/{publisher,subscriber,topic}/` package no longer exists) fan out SSE messages across instances via Redis pub-sub.

**Deadlock repro (playground, documentation-via-code):**
- `RequiresNewDeadLockOccurTest` / `RequiresNewDeadLockNotOccurTest` (`playground/src/test/java/com/example/playground/requiresnewdeadlock/`) reproduce a MySQL row-lock deadlock from `Propagation.REQUIRES_NEW` nested transactions racing on one row (`ExecutorService` + `CountDownLatch`); `@Disabled` by default, toggled via `playground/src/test/resources/application-requires-new-deadlock-{occur,not-occur}.yml`. This finding is why production event publishing uses AFTER_COMMIT + `@Async` instead of `REQUIRES_NEW`.

## Error Handling

**Strategy:** Centralized `@ControllerAdvice` translating a typed exception hierarchy; services throw semantically-named `ApplicationException`s.

**Patterns:**
- `ApplicationException` (`common/src/main/java/com/example/simplescheduleapp/common/exception/ApplicationException.java`) carries an `ExceptionCode` (HTTP status + short code + Korean message).
- `CommonExceptionHandler` (`common/src/main/java/com/example/simplescheduleapp/common/exception/CommonExceptionHandler.java`) handles `ApplicationException` (log level driven by 5xx-ness), bean-validation `MethodArgumentNotValidException` (field-error map), and a catch-all; response shapes live in `common/src/main/java/com/example/simplescheduleapp/common/exception/response/`.
- `DataIntegrityViolationException` is caught at the service layer and mapped to domain codes (see the `switch` in `RedisSpecialLectureEnrollmentService`), never leaked to the generic handler.
- Kafka failures: unacked → dead-letter path (`common/src/main/java/com/example/simplescheduleapp/common/kafka/deadletter/`); downstream send failures → `FailedNotification` rows retried by `NotificationRetryScheduler` (retry/scheduler config in `notification/src/main/java/com/example/simplescheduleapp/notification/config/RetryConfig.java`, `SchedulerConfig.java`).
- `CourseClient` used to map any 4xx/5xx from the internal call to `InternalServerExceptionCode.EXTERNAL_API_ERROR` — this class is deleted (ADR-0003 Stage 2, see the superseded note under Data Flow); the in-process `InProcessEnrolledStudentsAdapter` has no equivalent 4xx/5xx translation because there is no HTTP call to translate.

## Cross-Cutting Concerns

**Logging:** SLF4J via Lombok `@Slf4j`; INFO try/success pairs with IDs (see `KafkaEventProducer`, `LectureEnrollmentInternalController`), ERROR/WARN with context on failure; Korean messages common in domain-level logs.

**Validation:** Bean Validation on request DTOs (`spring-boot-starter-validation` via `common`); domain invariants enforced in entity constructors/methods (`Schedule.validatePastTime()`, `Lecture.validateCanIncreaseEnrolledCount()`, `PendingLectureEnrollmentService`).

**Authentication:** Custom JWT bearer scheme in `common/src/main/java/com/example/simplescheduleapp/common/auth/` — `TokenService`/`TokenProperty`/`Token` (jjwt) issue/verify, `BearerTokenExtractor` parses the `Authorization` header, `@Auth Long memberId` parameters are resolved by `AuthArgumentResolver` registered in `AuthConfig`; `TokenExceptionCode` covers token errors. No Spring Security filter chain. Note: the `/internal/**` endpoints have no auth guard beyond network placement.

**CORS:** `CorsConfig` (`common/src/main/java/com/example/simplescheduleapp/common/config/CorsConfig.java`) driven by `cors.allowed-origins` (default `http://localhost:3000`).

**Async:** `@EnableAsync` in `common/src/main/java/com/example/simplescheduleapp/common/config/AsyncConfig.java`; dedicated event executor in `EventAsyncTaskExecutorConfig`; notification thread pool in `notification/src/main/java/com/example/simplescheduleapp/config/ThreadPoolConfig.java`.

---

*Architecture analysis: 2026-07-02, branch `feature/query-performance-tuning` @ `3e4f654`*
