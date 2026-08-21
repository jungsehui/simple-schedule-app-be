# Codebase Concerns

**Analysis Date:** 2026-07-02

**Analysis basis:** Branch `feature/query-performance-tuning` (tip `3e4f654`, identical commit to `chore/ai-infra-cicd`) — the active line with 270 Java files across `course` (102 main-source files), `notification` (47), `common` (49), plus `playground`/`ngrinder` load-test modules. The repo's `main` branch (`dc31c8f`, 2025-05-02) is a boilerplate skeleton and does NOT contain the application; all findings below are from the active branch. Stack: Spring Boot 3.4.3, Java 21, MySQL 8.0, Redis 7.2, Kafka (cp-kafka 7.3.2 + Zookeeper). File paths are repository-relative.

## Security Considerations

**No authentication or authorization on any REST endpoint (Severity: Critical):**
- Risk: A complete Bearer-token mechanism exists — `common/src/main/java/com/example/simplescheduleapp/common/auth/Auth.java`, `AuthArgumentResolver.java`, `AuthConfig.java` — but `@Auth` has **zero usages** in any controller. There is no `SecurityFilterChain` class and no Spring Security dependency in any `build.gradle`. Every endpoint identifies the acting user via plain request input: `@RequestParam Long tutorId` (`course/src/main/java/com/example/simplescheduleapp/lecture/general/presentation/LectureController.java:28,59`, `course/src/main/java/com/example/simplescheduleapp/lecture/special/presentation/SpecialLectureController.java:28`), `@RequestParam Long studentId` (`course/src/main/java/com/example/simplescheduleapp/lecture/special/presentation/SpecialLectureEnrollmentController.java:22`, `course/src/main/java/com/example/simplescheduleapp/student/presentation/StudentController.java:39,49,59`), `@RequestParam Long memberId` (`notification/src/main/java/com/example/simplescheduleapp/fcm/presentation/FcmController.java:18`), and `@PathVariable Long memberId` on the SSE stream (`notification/src/main/java/com/example/simplescheduleapp/sse/presentation/SseController.java:19-20` — any caller can subscribe to any member's notification stream).
- Files: all `*Controller.java` under `course/src/main/java/**/presentation/` and `notification/src/main/java/**/presentation/`
- Current mitigation: None in-app. `deploy/nginx/conf.d/ssa.conf` adds rate limiting (20r/s per IP) but no auth.
- Recommendations: Wire `@Auth Long memberId` (resolved by the existing `AuthArgumentResolver` via `TokenService`) into every mutating/sensitive endpoint, replacing client-supplied identity params; add authorization checks (does this member own this resource) in the application layer; add regression tests asserting mismatched-identity requests are rejected.

**Hardcoded credentials committed to git (Severity: High):**
- Risk: `course/src/main/resources/application.yml:16-17` and `notification/src/main/resources/application.yml:16-17` connect as MySQL `root`/`1234`. `common/src/main/resources/application-common-local.yml:9` contains `token.secretKey` (base64 of a readable placeholder string) — the same value is repeated in 7 yml files including `course/src/main/resources/application-test.yml`, `course/src/test/resources/application.yml`, `notification/src/test/resources/application.yml`, `common/src/main/resources/application-common-test.yml`. Root `docker-compose.yml:9,29` defaults `MYSQL_ROOT_PASSWORD` to `1234`.
- Files: `course/src/main/resources/application.yml`, `notification/src/main/resources/application.yml`, `common/src/main/resources/application-common-local.yml`, `docker-compose.yml`
- Current mitigation: `deploy/.env.example` establishes an env-var pattern for prod (non-root `course_app`/`notification_app` DB users, `JWT_SECRET`, `REDIS_PASSWORD`, no committed values); `deploy/README.md` explicitly lists these credentials as burned and mandates rotation before deploy (including SSH password and WireGuard keys exposed in chat). The FCM service-account JSON is gitignored (`.gitignore:2`) and confirmed NOT tracked in git.
- Recommendations: Perform the rotations `deploy/README.md` mandates (JWT secret, DB passwords, FCM key, SSH/WireGuard) — they remain in git history forever. Remove the JWT secret default from `application-common-local.yml` in favor of a required env var; add a secret-scanning CI step.

**`/internal/**` API has no application-level protection (Severity: Medium):**
- **Superseded framing:** this finding predates ADR-0003 Stage 2. `notification` is no longer a separate service calling this endpoint over HTTP (see the superseded note in ARCHITECTURE.md); a shared-secret guard, `course/src/main/java/com/example/simplescheduleapp/config/InternalApiKeyFilter.java`, now exists on this path (fail-open when `internal.api.key` is unset — current prod value not verified from the repo). Whether the original risk still applies needs re-auditing, not assumed from this stale text.
- Risk: `course/src/main/java/com/example/simplescheduleapp/lecture/general/presentation/LectureEnrollmentInternalController.java:24-31` (`GET /internal/lectures/{lectureId}/student-ids`) exposes enrolled-student IDs with no auth token, no shared secret, no network restriction in the app. The notification module called it via `client.course-server-internal-url` (`notification/src/main/resources/application.yml:65`) before ADR-0003 Stage 2 — that config key and file no longer exist.
- Files: `course/src/main/java/com/example/simplescheduleapp/lecture/general/presentation/LectureEnrollmentInternalController.java`, `deploy/nginx/conf.d/ssa.conf`
- Current mitigation: In prod, nginx returns 403 for `location /internal/` (`deploy/nginx/conf.d/ssa.conf`) and the course container is not port-published. But locally (`docker-compose.yml` + direct app run on 8080) `/internal` is fully open, and any container inside `app_network` can call it.
- Recommendations: Add defense-in-depth: a shared-secret header or mTLS between services, or at minimum bind internal endpoints to a separate port not proxied by nginx.

**Input validation nearly absent (Severity: Medium):**
- Risk: `@Valid` appears in only 3 controllers (`course/src/main/java/com/example/simplescheduleapp/student/presentation/StudentController.java`, `tutor/presentation/TutorController.java`, `parent/presentation/ParentController.java`). Lecture creation/update, enrollment requests, consultation, member, and FCM token registration accept unvalidated request bodies and params.
- Files: `course/src/main/java/com/example/simplescheduleapp/lecture/general/presentation/LectureController.java`, `lecture/general/presentation/LectureEnrollmentController.java`, `consultation/presentation/ConsultationController.java`, `member/presentation/MemberController.java`, `notification/src/main/java/com/example/simplescheduleapp/fcm/presentation/FcmController.java`
- Recommendations: Add Bean Validation annotations to request DTOs and `@Valid` at all controller entry points; domain entities validate some invariants (capacity) but not input shape (negative capacity, blank titles, end-before-start times).

## Known Bugs

**FcmConfig cannot load the FCM key the prod deploy mounts — notification service will crash-loop in prod (Severity: Critical):**
- **Fixed since this was written:** `FcmConfig.java` now injects `ResourceLoader` and resolves `FCM_KEY_JSON` via a `loadCredentialResource()` helper that prepends `file:`/`classpath:` based on whether the value is an absolute path, matching this entry's own "Fix approach" below — and the exception is now rethrown with its cause preserved. Also, "notification service" is 3-process-era language: the FCM bootstrap now runs inside the single `:app` JVM (`app/Dockerfile`), not a separate `notification` image (`deploy/docker-compose.prod.yml` no longer builds one). Re-verify against current `FcmConfig.java` before treating this as an open bug.
- Symptoms: `notification/src/main/java/com/example/simplescheduleapp/fcm/config/FcmConfig.java:38` loads the credential via `new ClassPathResource(FCM_KEY_JSON)`. `deploy/docker-compose.prod.yml` mounts the key as a filesystem file at `/app/config/fcm-service-account.json` and sets `FCM_KEY_JSON: /app/config/fcm-service-account.json`. `ClassPathResource` cannot resolve an absolute filesystem path — `getInputStream()` throws `IOException`, which `FcmConfig` rethrows as `RuntimeException` (also discarding the stack trace, `FcmConfig.java:43-46`), failing `FirebaseApp` bean creation and aborting startup.
- Files: `notification/src/main/java/com/example/simplescheduleapp/fcm/config/FcmConfig.java`, `deploy/docker-compose.prod.yml`
- Trigger: Any prod/stage deployment of the notification image as configured.
- Workaround: None without code change — the key cannot be mounted externally today; it only works when the JSON sits on the classpath (`src/main/resources`), which the deploy docs correctly forbid.
- Fix approach: Inject `ResourceLoader` (or use `DefaultResourceLoader`) and reference the key with a `file:`/`classpath:` prefix, or branch on path style with `FileSystemResource`. Preserve the exception cause when rethrowing.

**SSE heartbeat cleanup relies on the wrong exception type (Severity: Medium):**
- Symptoms: `notification/src/main/java/com/example/simplescheduleapp/sse/application/SseConnectionService.java:86-98` schedules a heartbeat every 10s and cleans up only on `IOException`. `SseEmitter.send()` on an already-completed emitter throws `IllegalStateException`, which escapes the task — the scheduler then suppresses future executions as a side effect, so cleanup happens by accident (with an unhandled exception), not by design. The returned `ScheduledFuture` is never retained, so the heartbeat cannot be cancelled from the `onCompletion`/`onTimeout`/`onError` callbacks (`SseConnectionService.java:55-65`).
- Files: `notification/src/main/java/com/example/simplescheduleapp/sse/application/SseConnectionService.java`
- Trigger: Any normal SSE completion (client disconnect, timeout) followed by the next heartbeat tick.
- Fix approach: Keep the `ScheduledFuture` per member and cancel it in `clearSseConnectionResource`; catch `Exception` (not only `IOException`) inside the heartbeat task.

**`Optional<List<...>>` repository returns make error paths unreachable (Severity: Low):**
- Symptoms: `course/src/main/java/com/example/simplescheduleapp/lecture/general/domain/LectureEnrollmentRepository.java:24` declares `Optional<List<LectureEnrollment>> findAllByLectureId(...)`. Spring Data returns an empty list (never an empty Optional), so `getAllByLectureId`'s `orElseThrow` (`LectureEnrollmentRepository.java:19-22`) can never throw, and the `== null` check in `LectureEnrollmentService.getLectureEnrollments` (`course/src/main/java/com/example/simplescheduleapp/lecture/general/application/LectureEnrollmentService.java:50`) is dead code.
- Fix approach: Return `List<LectureEnrollment>` directly; drop the dead null/empty handling.

**Dangling commented-out call to a deleted method (Severity: Low):**
- Symptoms: `course/src/main/java/com/example/simplescheduleapp/lecture/special/presentation/SpecialLectureEnrollmentController.java:28` contains `// redisSpecialLectureEnrollmentService.enrollSpecialLectureEnrollmentKafka(command);` — that method no longer exists anywhere in the codebase. Leftover from an abandoned Kafka-based enrollment path.
- Fix approach: Delete the comment; document the chosen enrollment strategy (see Fragile Areas).

## Tech Debt

**Empty prod/stage configs while base config carries unsafe defaults (Severity: Critical):**
- Issue: `course/src/main/resources/application-prod.yml`, `application-stage.yml`, `notification/src/main/resources/application-prod.yml`, `application-stage.yml` are all **0 bytes**. `deploy/docker-compose.prod.yml` and both Dockerfiles set `SPRING_PROFILES_ACTIVE=prod`, so prod runs on the base `application.yml` values with env-var overrides for datasource/kafka/redis/token/cors only. Nothing overrides `spring.jpa.hibernate.ddl-auto: update` (`course/src/main/resources/application.yml:27`, `notification/src/main/resources/application.yml:27`), `show-sql: true` (both, line 28), or notification's `spring.sql.init.mode: always` (`notification/src/main/resources/application.yml:35-37` — currently a no-op since no `data.sql` exists, but a trap: any future seed script would execute on every prod boot). The `spring.profiles.group` only defines `local: common-local`; no `application-common-prod.yml` exists.
- Files: `course/src/main/resources/application-prod.yml`, `notification/src/main/resources/application-prod.yml`, `course/src/main/resources/application.yml`, `notification/src/main/resources/application.yml`
- Impact: Production would run Hibernate schema auto-migration and full SQL logging; a schema mismatch is silently "fixed" by Hibernate instead of failing fast.
- Fix approach: Populate prod ymls with `ddl-auto: validate`, `show-sql: false`, `sql.init.mode: never`; add a startup assertion failing if `ddl-auto` is `create`/`update` outside `local`/`test`.

**No schema migration tooling — schema owned by `ddl-auto: update` (Severity: High):**
- Issue: No Flyway or Liquibase in any `build.gradle`; no `db/migration` directory. All DDL — including the unique constraints the code's correctness depends on (`uk_lecture_student` in `course/src/main/java/com/example/simplescheduleapp/lecture/general/domain/LectureEnrollment.java:11-13`, `uk_special_lecture_student` in `lecture/special/domain/SpecialLectureEnrollment.java:11-13`, `uk_pending_lecture_student` in `lecture/general/domain/PendingLectureEnrollment.java:17-19`) — exists only as JPA annotations. The `catch (DataIntegrityViolationException)` idempotency handling (`LectureEnrollmentService.java:84-87`, `RedisSpecialLectureEnrollmentService.java:38-39`) silently stops working if a constraint is missing, and `ddl-auto: update` does not reliably add constraints to pre-existing tables.
- Files: `build.gradle`, `course/build.gradle`, `notification/build.gradle`
- Impact: No reviewable, versioned, roll-back-able schema history; environment drift is undetectable.
- Fix approach: Introduce Flyway with a baseline migration generated from the current entities; switch `ddl-auto` to `validate`.

**Branch topology: all real work sits on an unmerged feature line (Severity: High):**
- Issue: `main` is a skeleton 245 commits behind this branch. `develop` (`6301e65`, 2025-09-14) is 24 commits behind `feature/query-performance-tuning`/`chore/ai-infra-cicd` (same tip `3e4f654`). `feature/2-4-observability` (`59ff5be`) is a **sibling** line 29 commits ahead of `develop` with independent work — it and this branch have diverged from `develop` separately and will need reconciliation. The CI workflow (`.github/workflows/ci.yml`) only triggers on pushes/PRs targeting `main`/`develop`, so the active line gets no CI until merged.
- Files: `.github/workflows/ci.yml`
- Impact: Two long-lived diverged lines over the same codebase; merge conflicts and lost-work risk grow with every commit; `main` gives a false picture of the project.
- Fix approach: Merge this line to `develop` (then `main`) before further feature work; rebase or re-plan `feature/2-4-observability` on the merged result; consider adding feature-branch push triggers or required PR CI.

**Dead distributed-locking infrastructure (Severity: Medium):**
- Issue: `course/src/main/java/com/example/simplescheduleapp/redis/lock/RedissonDistributedLock.java`, `redis/lock/SimpleRedisLock.java`, and `redis/aop/RedissonDistributedLockAop.java` exist, but `@RedissonDistributedLock` has zero usage sites. The live concurrency strategy is the Redis atomic-decrement counter in `course/src/main/java/com/example/simplescheduleapp/lecture/special/application/SpecialLectureRedisClient.java`.
- Impact: Future readers cannot tell which locking strategy is canonical; the AOP also maps lock-acquisition failure to `InternalServerExceptionCode.UNKNOWN_EXCEPTION` (`RedissonDistributedLockAop.java:51`), a poor error signal if ever revived.
- Fix approach: Delete the unused lock classes (they remain in git history) or document why they're retained.

**Deploy packaging nits (Severity: Low):**
- Issue: `app/Dockerfile` copies `build/libs/*-SNAPSHOT.jar` (`app/Dockerfile:8`) — the glob breaks the moment versioning moves off `-SNAPSHOT` (`version = "0.0.1-SNAPSHOT"` in `buildSrc/src/main/kotlin/ssa.java-common.gradle.kts:14`, the shared convention plugin `app` applies via `ssa.spring-boot-app`). `.github/workflows/deploy.yml` offers a `stage` environment input, but `application-stage.yml` is empty and `deploy/docker-compose.prod.yml` is the only compose — "stage" deploys prod config.
- Files: `app/Dockerfile`, `.github/workflows/deploy.yml`
- Fix approach: Copy the jar by explicit name via a build arg; either implement a real stage config or remove the input option.

## Performance Bottlenecks

**EAGER `@ManyToOne` associations cause N+1 cascades (Severity: Medium):**
- Problem: No association in the codebase specifies `fetch = LAZY`; all `@ManyToOne` mappings use the JPA default (EAGER): `LectureEnrollment.lecture`/`.student` (`course/src/main/java/com/example/simplescheduleapp/lecture/general/domain/LectureEnrollment.java:24-30`), `Lecture.tutor` (`lecture/general/domain/Lecture.java:22-24`), `SpecialLecture.tutor` (`lecture/special/domain/SpecialLecture.java:22-24`). `LectureEnrollmentService.findStudentIdsByLectureId` (`course/src/main/java/com/example/simplescheduleapp/lecture/general/application/LectureEnrollmentService.java:100-108`) — the query behind the `/internal` API — hydrates full enrollment entities (each eagerly pulling Student, Lecture, and Lecture's Tutor) just to extract student IDs.
- Files: `course/src/main/java/com/example/simplescheduleapp/lecture/general/domain/LectureEnrollment.java`, `lecture/general/domain/LectureEnrollmentRepository.java`, `lecture/general/application/LectureEnrollmentService.java`
- Cause: Derived queries + EAGER defaults → one extra query per association per row.
- Improvement path: Set `fetch = FetchType.LAZY` on all `@ManyToOne`s; for the ID-only use case add `@Query("SELECT le.student.id FROM LectureEnrollment le WHERE le.lecture.id = :lectureId")`.

**Leading-wildcard LIKE search forces full table scans (Severity: Medium):**
- Problem: `course/src/main/java/com/example/simplescheduleapp/lecture/general/domain/LectureRepository.java:18` — `WHERE l.title LIKE %:keyword% OR l.memo LIKE %:keyword%`. MySQL cannot use a B-tree index with a leading `%`, and no index exists anyway (zero `@Index` declarations in the codebase; `ddl-auto` creates none beyond PK/FK/unique).
- Files: `course/src/main/java/com/example/simplescheduleapp/lecture/general/domain/LectureRepository.java`
- Improvement path: Add a MySQL `FULLTEXT` index on `title`/`memo` with `MATCH ... AGAINST` (requires the migration tooling above), or restrict to prefix search.

**No explicit secondary indexes on query columns (Severity: Low):**
- Problem: No `@Index` annotations anywhere. FK columns get implicit InnoDB indexes, but columns queried directly — e.g., `Schedule` time-range fields used by schedule lookups, `DomainEvent` status/timestamp used by the outbox relay (`common/src/main/java/com/example/simplescheduleapp/common/event/DomainEvent.java`) — have no declared indexes. (`KafkaMessageConsumeHistory`'s idempotency key is `unique = true`, so it is indexed.)
- Improvement path: Audit the outbox polling and schedule-range queries under load (ngrinder/k6 setups already exist under `playground/`) and add indexes via migrations.

## Fragile Areas

**General lecture enrollment can overbook — no concurrency guard (Severity: High):**
- Files: `course/src/main/java/com/example/simplescheduleapp/lecture/general/application/LectureEnrollmentService.java:72-88`, `course/src/main/java/com/example/simplescheduleapp/lecture/general/domain/Lecture.java:55-58,66-70`
- Why fragile: `acceptEnrollment` loads the `Lecture` without any lock, then `lecture.enroll(student)` does an in-memory check-and-increment of `enrolledCount`. There is no `@Version` field anywhere in the codebase and no pessimistic locking. Two concurrent accepts for different students both read the same `enrolledCount`, both pass `validateCanIncreaseEnrolledCount`, both commit → capacity exceeded plus a lost update on the counter. The unique constraint only blocks the *same* student enrolling twice. The special-lecture path received a Redis counter guard; this general path received nothing.
- Safe modification: Add `@Version` to `Schedule`/`Lecture` (optimistic, with retry) or a `PESSIMISTIC_WRITE` lock on the lecture row in `acceptEnrollment`; add a parallel-execution test proving capacity holds.
- Test coverage: `course/src/test/java/com/example/simplescheduleapp/lecture/application/TutorLectureEnrollmentServiceTest.java` and `StudentLectureEnrollmentServiceTest.java` exist but include no concurrent-accept scenario.

**Redis capacity counters can be evicted or lost — special-lecture enrollment breaks silently (Severity: High):**
- Files: `course/src/main/java/com/example/simplescheduleapp/lecture/special/application/SpecialLectureRedisClient.java`, `deploy/docker-compose.prod.yml`
- Why fragile: Capacity for special lectures lives solely in Redis keys `special_lecture:{id}:available`, initialized once at lecture creation (`SpecialLectureRedisClient.java:20-23`) with no rehydration path. The prod Redis runs with `--maxmemory 256mb --maxmemory-policy allkeys-lru` (`deploy/docker-compose.prod.yml`, redis service) — under memory pressure Redis may **evict a capacity counter**; the next `DECR` on the missing key yields `-1`, which the code reads as "full" (`SpecialLectureRedisClient.java:30-42`), rejecting all enrollments for that lecture (the compensating `INCR` then pins it at 0 — permanently "full"). A Redis restart (no AOF/RDB tuning configured) wipes all counters the same way.
- Safe modification: Use `noeviction` for this data (or a dedicated Redis instance/DB for correctness-critical keys), and add lazy rehydration: on missing key, rebuild `capacity - count(enrollments)` from MySQL before decrementing.
- Test coverage: No test covers counter-missing or Redis-restart scenarios.

**Redis-first enrollment with catch-based compensation (Severity: Medium):**
- Files: `course/src/main/java/com/example/simplescheduleapp/lecture/special/application/RedisSpecialLectureEnrollmentService.java`
- Why fragile: The flow decrements Redis, then writes to MySQL inside the same `@Transactional` method, compensating Redis in a `catch` block (`RedisSpecialLectureEnrollmentService.java:22-46`). A process crash between the decrement and the DB commit leaks a capacity slot with no reconciliation job. `GenerationType.IDENTITY` forces immediate INSERT so constraint violations do surface inside the `try`, but commit-time failures after the method body returns bypass the catch entirely. The outer `@Transactional` also holds a DB connection across Redis round-trips.
- Safe modification: Settle the strategy before touching this path — it already has three abandoned alternates (Redisson AOP, `SimpleRedisLock`, the deleted Kafka variant). A periodic reconciliation job (Redis counter vs. `COUNT(*)` of enrollments) would bound the damage of any leak.

**SSE connection registry: one emitter per member, silent overwrite (Severity: Medium):**
- Files: `notification/src/main/java/com/example/simplescheduleapp/sse/application/SseEmitterRegistry.java` (renamed from `SseEmitterRepository`, moved out of the now-removed `sse/cache/` package), `notification/src/main/java/com/example/simplescheduleapp/sse/application/SseConnectionService.java`
- Why fragile: `SseEmitterRegistry` maps `memberId → SseEmitter` (`SseEmitterRegistry.java:27`, `save()`). A second connection from the same member (e.g., second browser tab) overwrites the first without completing it — the orphaned emitter stays open up to the 1-hour timeout (`SseConnectionService.java` — `DEFAULT_TIMEOUT`) holding a servlet async context, its heartbeat still firing, while notifications reach only the newest connection. Cross-instance routing exists via Redis pub/sub (`notification/src/main/java/com/example/simplescheduleapp/sse/infrastructure/redis/RedisSseMessagePublisher.java`, `RedisSseMessageSubscriber.java` — the root-level `redis/{publisher,subscriber}/` package no longer exists), but the map itself is unbounded in-memory state.
- Safe modification: On `save`, complete any existing emitter for that member (or support a list per member); pair with the heartbeat-cancellation fix under Known Bugs.
- Test coverage: `notification/src/test/java/com/example/simplescheduleapp/sse/application/SseEmitterRegistryTest.java` exists but does not cover the overwrite scenario.

## Scaling Limits

**SSE emitters are per-instance in-memory state:**
- Current capacity: Bounded by servlet async contexts and heap on a single notification instance; 1-hour emitter timeout; heartbeat every 10s per connection (`notification/src/main/java/com/example/simplescheduleapp/sse/application/SseConnectionService.java`).
- Limit: Horizontal scaling of `notification` works only because Redis pub/sub routes per-member channels, but each instance still holds every local emitter; nginx `proxy_read_timeout 1h` on `/sse-stream/` (`deploy/nginx/conf.d/ssa.conf`) pins connections.
- Scaling path: Load-test connection churn; consider per-member connection caps.

**Kafka consumer concurrency is 1:**
- Current capacity: `factory.setConcurrency(1)` in `common/src/main/java/com/example/simplescheduleapp/common/kafka/consumer/KafkaConsumerConfig.java:39`; single topic (`KafkaTopics.COURSE_EVENT_TOPIC`), `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1`, single broker.
- Limit: Notification throughput is serialized; a slow FCM/SSE handler backs up the whole topic. (Error handling itself is solid: 2 retries + `DeadLetterRecorder` DLQ, manual-immediate acks, `KafkaIdempotencyFilter` dedupe — `KafkaConsumerConfig.java:41-48`.)
- Scaling path: Partition the topic by lectureId/memberId and raise concurrency; the retry scheduler (`notification/src/main/java/com/example/simplescheduleapp/notification/infrastructure/NotificationRetryScheduler.java`, moved out of the now-removed `notification/schedule/` package) already handles failed sends.

**Prod host is a single 4 vCPU / 8GB box running 6 containers:**
- Current capacity: mem limits in `deploy/docker-compose.prod.yml` total ~5.0GB across nginx/app/geekchat/Redis/Kafka/Zookeeper (prod DB is external Supabase PostgreSQL, not a container).
- Limit: No headroom for a second app replica.
- Scaling path: Documented in `deploy/README.md`; acceptable for current scale, revisit before prod load testing.

## Dependencies at Risk

**Kafka on Zookeeper (cp-kafka 7.3.2) (Severity: Low):**
- Risk: Zookeeper-based Kafka is deprecated upstream (KRaft is the default in Kafka 4.x); confluent 7.3.x is an older image line.
- Files: `docker-compose.yml`, `deploy/docker-compose.prod.yml`
- Migration plan: Move to a KRaft single-node setup when convenient; no application-code impact expected.

**No Spring Boot Actuator (Severity: Medium):**
- Risk: A gap rather than a risky dependency: no `actuator` dependency anywhere, so no health endpoints — `deploy/docker-compose.prod.yml` defines healthchecks for MySQL/Redis but none for the app containers, and nginx has no upstream health probing. Deploys can't verify app readiness. (Observability work appears to live on the diverged sibling branch `feature/2-4-observability`.)
- Migration plan: Add `spring-boot-starter-actuator`, expose `/actuator/health` internally, wire compose healthchecks and a post-deploy smoke check into `.github/workflows/deploy.yml`.

## Missing Critical Features

**Authentication enforcement (Severity: Critical):**
- Problem: See Security Considerations — the mechanism exists (`common/src/main/java/com/example/simplescheduleapp/common/auth/`) but nothing uses it.
- Blocks: Any real deployment; every user-facing feature is exploitable via IDOR until this lands.

**Schema migrations (Severity: High):**
- Problem: See Tech Debt — no Flyway/Liquibase.
- Blocks: Safe production schema evolution, the FULLTEXT/secondary indexes needed for search performance, and reliable unique-constraint provisioning.

**Post-deploy verification (Severity: Medium):**
- Problem: `.github/workflows/deploy.yml` ends after `docker compose up -d` with no health/smoke check; combined with the FcmConfig startup bug, a broken deploy would go unnoticed until users report it.
- Blocks: Confident unattended deploys.

## Test Coverage Gaps

**`common` module: 0 test files for 49 source files (Severity: High):**
- What's not tested directly in-module: JWT `TokenService`/`AuthArgumentResolver` (`common/src/main/java/com/example/simplescheduleapp/common/auth/`), `CommonExceptionHandler`, outbox `DomainEvent` lifecycle. Some common Kafka classes are exercised indirectly by notification-module tests (`notification/src/test/java/com/example/simplescheduleapp/kafka/infra/consumer/KafkaIdempotencyFilterTest.java`, `kafka/infra/deadletter/DeadLetterRecorderTest.java`, `kafka/event/outbox/EventRecorderTest.java`).
- Risk: Token validation — the future security boundary — has zero tests.
- Priority: High (add alongside the auth-enforcement work).

**`course` module: 7 test files for 102 source files; entire domains untested (Severity: High):**
- What's not tested: The whole `lecture/special/` package (Redis counter flow, compensation), `consultation/`, `tutor/`, `parent/`, `schedule/` — no test directories exist for any of them (`course/src/test/java/com/example/simplescheduleapp/` contains only `lecture/application/` and `member/`).
- Files: `course/src/test/java/com/example/simplescheduleapp/`
- Risk: The most concurrency-sensitive code in the system (special-lecture enrollment) has no co-located tests; concurrency experiments live only in `playground/src/test/java/com/example/playground/requiresnewdeadlock/` against toy entities.
- Priority: High.

**No concurrency test for general-lecture overbooking (Severity: High):**
- What's not tested: Parallel `acceptEnrollment` calls against one lecture's capacity (see Fragile Areas).
- Files: `course/src/main/java/com/example/simplescheduleapp/lecture/general/application/LectureEnrollmentService.java`
- Risk: The race ships silently; a fix could also regress silently.
- Priority: High — write the failing test first, then add the locking fix.

**No tests for Redis counter loss/rehydration or SSE duplicate connections (Severity: Medium):**
- What's not tested: Missing-key behavior in `SpecialLectureRedisClient`; emitter-overwrite behavior in `SseEmitterRegistry`.
- Files: `course/src/main/java/com/example/simplescheduleapp/lecture/special/application/SpecialLectureRedisClient.java`, `notification/src/test/java/com/example/simplescheduleapp/sse/application/SseEmitterRegistryTest.java`
- Priority: Medium.

---

*Concerns audit: 2026-07-02 — branch `feature/query-performance-tuning` @ `3e4f654`*
