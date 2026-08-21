# Coding Conventions

**Analysis Date:** 2026-07-02

> **Source branch:** This analysis is based on branch **`chore/ai-infra-cicd`** (built directly on **`feature/query-performance-tuning`** — the active development line, 270 Java files). The repo's `main` branch contains only the original single-module skeleton and is NOT representative. All paths below are repository-relative on that branch. Modules declared in `settings.gradle.kts`: `common` (INFRA library), `course` and `notification` (bounded-context libraries, no own bootJar), `app` (BOOT — the sole executable module, ADR-0003 Stage 2), `playground` and `ngrinder` (load-/experiment-testing, excluded from CI).

## Naming Patterns

**Packages:**
- Root package: `com.example.simplescheduleapp` (the `main`-branch skeleton uses `com.example.simplescheduleappback` — do not follow it).
- Feature-based (not layer-based) top-level packages: `lecture`, `member`, `student`, `tutor`, `parent`, `consultation`, `schedule`, `event`, `redis`, `config` (course module); `notification`, `fcm`, `sse`, `kafka`, `redis`, `config` (notification module); `common.{exception,domain,auth,aop,event,kafka}` (common module).
- Each feature package is internally layered: `domain` → `application` (+ `application/command`) → `presentation` (+ `presentation/request`, `presentation/response`), plus `exception`.
- Example: `course/src/main/java/com/example/simplescheduleapp/lecture/general/{domain,application,application/command,presentation,presentation/request,presentation/response,exception}`.
- Sub-domain split: `lecture/general/...` (regular lectures) vs `lecture/special/...` (limited-capacity "special lecture" with Redis-based concurrency defense).

**Files/Classes:**
- Entities: plain noun — `Lecture.java`, `Tutor.java`, `SpecialLectureEnrollment.java`, `PendingLectureEnrollment.java`.
- Repositories: `<Entity>Repository.java`, Spring Data JPA interfaces — `course/src/main/java/com/example/simplescheduleapp/lecture/general/domain/LectureRepository.java`.
- Services: `<Feature>Service.java` — `LectureService.java`, `RedisSpecialLectureEnrollmentService.java`.
- Commands (application-layer input DTOs): `<Entity><Action>Command.java` — `LectureCreateCommand.java`, `SpecialLectureEnrollmentCreateCommand.java` — Java `record`s under `application/command`.
- Web DTOs: `<Entity><Action>Request.java` / `...Response.java` under `presentation/request` and `presentation/response`, also `record`s. Requests expose `toCommand(...)`; Responses expose static `from(...)`.
- Exception codes: `<Feature>ExceptionCode.java`, an `enum` implementing `common/src/main/java/com/example/simplescheduleapp/common/exception/ExceptionCode.java`.
- Controllers: `<Feature>Controller.java` under `presentation` (plus `...InternalController.java` for server-to-server endpoints, e.g. `course/src/main/java/com/example/simplescheduleapp/lecture/general/presentation/LectureEnrollmentInternalController.java`).

**Methods:** Production method/field names are English camelCase (`createLecture`, `enrolledCount`, `validateTutorAuthority`). Test method names are **Korean full sentences with underscores** (see TESTING.md).

## Code Style

**Formatting:**
- No `.editorconfig`, Checkstyle, Spotless, or PMD anywhere in the repo. Formatting is IDE convention only (IntelliJ).
- 4-space indentation in Java source; tabs in `build.gradle` files.

**Linting:** None configured. Verification is compilation + tests only (`./gradlew build`).

## Import Organization

**Observed order (loose convention, not tool-enforced):**
1. Project imports (`com.example.simplescheduleapp...`)
2. Third-party (Lombok, Spring, Jakarta, Redisson, Kafka, Firebase)
3. JDK (`java.time.*`, `java.util.*`)
4. `static` imports last (test statics: `org.assertj.core.api.Assertions.assertThat`, `org.mockito.BDDMockito.given`; shared constants like `static com.example.simplescheduleapp.support.ApplicationWithKafkaTest.PORT`).

Wildcard imports appear occasionally (`jakarta.persistence.*`, `org.junit.jupiter.api.*`) — tolerated, not banned.

**Path Aliases:** Not applicable (plain Java packages).

## Lombok Usage

Lombok is declared once in the **root `build.gradle` `subprojects` block** (`compileOnly`/`annotationProcessor` for main AND test source sets); `common/build.gradle` additionally declares `testFixturesCompileOnly`/`testFixturesAnnotationProcessor` for the testFixtures source set. Module build files do not redeclare it.

**Annotations actually used (verified across all 270 files):**
- `@Getter` (~47 files) — entities, exception-code enums. **No `@Setter` anywhere**; state changes go through named business methods.
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` — every JPA entity and mapped superclass (satisfies Hibernate's no-arg requirement without exposing public no-arg construction). Example: `course/src/main/java/com/example/simplescheduleapp/lecture/general/domain/Lecture.java`.
- `@RequiredArgsConstructor` (~76 files) — services, controllers, AOP components: constructor injection of `final` fields. **No field injection in production code.**
- `@Slf4j` — anything that logs (`LectureService`, `RedissonDistributedLockAop`, `CommonExceptionHandler`).
- **Never used:** `@Data`, `@Builder`, `@Value`, `@Setter`, `@EqualsAndHashCode`, `@ToString`. Immutable DTOs are Java `record`s instead of Lombok-generated classes.

## DTO / Entity Patterns

**Entities (JPA, `domain` package) — rich domain model:**
Business rules and invariant validation live inside the entity; violations throw `ApplicationException` with a feature `ExceptionCode`:
```java
// course/src/main/java/com/example/simplescheduleapp/lecture/general/domain/Lecture.java
@DiscriminatorValue("LECTURE")
@Table(name = "lecture")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class Lecture extends Schedule {
    ...
    public LectureEnrollment enroll(Student student) {
        increaseEnrolledCount();               // validates capacity internally
        return new LectureEnrollment(this, student);
    }
    private void validateCanIncreaseEnrolledCount() {
        if (enrolledCount >= capacity) {
            throw new ApplicationException(LectureExceptionCode.CAPACITY_EXCEEDED);
        }
    }
}
```
- Inheritance for shared schedule behavior: `Lecture extends Schedule` with `@Inheritance(strategy = InheritanceType.JOINED)` + `@DiscriminatorColumn(name = "type")` on `course/src/main/java/com/example/simplescheduleapp/schedule/domain/Schedule.java`.
- **No `@Version`/optimistic locking exists on this branch** (removed on the query-performance-tuning line; special-lecture concurrency is handled via Redis counter + DB unique constraint — see Transaction Patterns).
- Soft delete: `Schedule` uses `@SQLDelete(sql = "UPDATE schedule SET deleted_date = CURRENT_TIMESTAMP WHERE schedule_id = ?")` + `@SQLRestriction(DELETED_DATE_IS_NULL)` (constant from `common/src/main/java/com/example/simplescheduleapp/common/SqlRestrictionClause.java`) and extends `common/src/main/java/com/example/simplescheduleapp/common/persistence/SoftDeletedDomain.java`.
- Auditing: `common/src/main/java/com/example/simplescheduleapp/common/persistence/BaseDomain.java` is a `@MappedSuperclass` with `@CreatedDate`/`@LastModifiedDate` + `AuditingEntityListener`. `SoftDeletedDomain extends BaseDomain` adds `deletedDate`.

**Commands (`application/command`, records):**
```java
// course/src/main/java/com/example/simplescheduleapp/lecture/general/application/command/LectureCreateCommand.java
public record LectureCreateCommand(Long memberId, String title, LocalDateTime startTime,
                                   LocalDateTime endTime, String memo, int capacity) {
    public Schedule toSchedule() { return new Schedule(title, startTime, endTime, memo); }
}
```

**Request → Command → Entity → Response flow (controller pattern):**
```java
// course/src/main/java/com/example/simplescheduleapp/lecture/general/presentation/LectureController.java
@PostMapping("/lectures")
public ResponseEntity<LectureCreateResponse> createLecture(@RequestParam Long tutorId,
        @RequestBody LectureCreateRequest lectureCreateRequest) {
    LectureCreateCommand command = lectureCreateRequest.toCommand(tutorId); // inject identity
    Lecture savedLecture = lectureService.createLecture(command);
    URI location = URI.create("/lectures/" + savedLecture.getId());
    return ResponseEntity.created(location).body(LectureCreateResponse.from(savedLecture));
}
```
- Requests carry `toCommand(Long ...)` injecting identity values not present in the HTTP payload.
- Responses are records with a static `from(Entity)` factory (`course/src/main/java/com/example/simplescheduleapp/lecture/general/presentation/response/LectureCreateResponse.java`).
- Services return entities/lists directly; controllers do the Response conversion. No generic `Result`/envelope type.

**Repositories:**
- Spring Data JPA interfaces extending `JpaRepository<Entity, Long>`, annotated `@Repository`.
- `getBy...` **default methods** wrap `findBy...().orElseThrow(...)` to centralize not-found mapping:
```java
// course/src/main/java/com/example/simplescheduleapp/lecture/general/domain/LectureRepository.java
default Lecture getByLectureId(Long lectureId) {
    return findById(lectureId).orElseThrow(() -> new ApplicationException(LectureExceptionCode.LECTURE_NOT_FOUND));
}
@Query("SELECT l FROM Lecture l WHERE l.title LIKE %:keyword% OR l.memo LIKE %:keyword%")
List<Lecture> findByKeyword(String keyword);
```
- Follow this: services call `getBy...` and never handle `Optional` themselves.

## Exception Handling

**Core types (`common/src/main/java/com/example/simplescheduleapp/common/exception/`):**
- `ExceptionCode` — interface: `getHttpStatus()`, `getCode()`, `getMessage()`.
- `ApplicationException extends RuntimeException` — the single business exception type, carries an `ExceptionCode`; message auto-formatted from the code.
- Feature enums implement `ExceptionCode` with a per-feature letter prefix and Korean messages:
```java
// course/src/main/java/com/example/simplescheduleapp/lecture/general/exception/LectureExceptionCode.java
LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "L0", "해당 강의가 없습니다."),
CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "L4", "수강 정원이 초과되었습니다."),
```
- `InternalServerExceptionCode` — shared fallbacks (`UNKNOWN_EXCEPTION`, `INVALID_INPUT_VALUE`).

**Global handler — `common/src/main/java/com/example/simplescheduleapp/common/exception/CommonExceptionHandler.java` (`@ControllerAdvice`), three handlers:**
1. `ApplicationException` → status from `code.getHttpStatus()`; logs `error` (with stack trace) for 5xx codes, `warn` otherwise; body `ExceptionResponse.from(code)`.
2. `MethodArgumentNotValidException` → collects bean-validation field errors into a `Map<String,String>`, returns 400 with `MethodArgumentExceptionResponse.from(InternalServerExceptionCode.INVALID_INPUT_VALUE, errors)`.
3. `Exception` (catch-all) → logs full stack trace, returns generic 500 `UNKNOWN_EXCEPTION` (never leaks internals).

**Convention for a new failure case:** add an enum constant to the feature's `*ExceptionCode` and `throw new ApplicationException(YourCode.YOUR_CASE)`. Do NOT create exception subclasses or throw raw `RuntimeException` in business logic.

**Infrastructure exception re-mapping + compensation (canonical example):**
```java
// course/src/main/java/com/example/simplescheduleapp/lecture/special/application/RedisSpecialLectureEnrollmentService.java
} catch (Exception e) {
    specialLectureRedisClient.compensateSpecialLectureEnrollment(command.specialLectureId());
    throw switch (e) {                                   // Java 21 pattern-matching switch
        case DataIntegrityViolationException ex -> new ApplicationException(SpecialLectureEnrollmentExceptionCode.ALREADY_ENROLLED);
        case ApplicationException ex -> ex;
        default -> new ApplicationException(SpecialLectureEnrollmentExceptionCode.SPECIAL_LECTURE_ENROLLMENT_FAILED);
    };
}
```
Duplicate enrollment surfaces as `DataIntegrityViolationException` from the DB **unique constraint** (not a pre-check query) and is translated; any DB failure triggers Redis compensation first.

## Transaction Patterns

- `@Transactional` (Spring's `org.springframework.transaction.annotation.Transactional`) is applied at the **service method** level, only on mutating methods (`LectureService.updateLecture`, `LectureEnrollmentService.acceptEnrollment`). Read methods are left plain — `readOnly = true` is not used anywhere; do not blanket-annotate service classes.
- `Propagation.REQUIRES_NEW` appears in exactly two production places:
  - `common/src/main/java/com/example/simplescheduleapp/common/aop/AopForTransaction.java` — a generic `@Component` running a `ProceedingJoinPoint` in its own new transaction, used by the distributed-lock AOP so the lock is released only after the inner transaction commits.
  - `notification/src/main/java/com/example/simplescheduleapp/notification/application/NotificationRetryService.java` (moved out of the now-removed `notification/schedule/` package) — retry batches isolated per attempt.
- **Self-invocation caveat:** Spring AOP is proxy-based; `@Transactional`/`@Aspect` advice does not apply to same-bean calls. That is why `AopForTransaction` is a separate bean invoked by `course/src/main/java/com/example/simplescheduleapp/redis/aop/RedissonDistributedLockAop.java` (an `@Aspect` around `@RedissonDistributedLock`-annotated methods: Redisson **FairLock**, `tryLock(waitTime, leaseTime)`, SpEL lock key via `CustomSpringELParser`, unlock in `finally` guarded by `isHeldByCurrentThread()`). Note: this annotation infra exists but currently has no active production call sites — the special-lecture path uses the Redis-counter + unique-constraint approach instead.
- **Transactional event listeners (outbox pattern, `common/src/main/java/com/example/simplescheduleapp/common/event/`):** domain services publish `DomainEvent` subclasses via `ApplicationEventPublisher` (see `LectureService.updateLecture` publishing `LectureUpdatedEvent`); `outbox/EventRecordListener` persists the event with `@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)` (atomic with the business tx), and `producer/EventProducerListener` publishes to Kafka with `phase = TransactionPhase.AFTER_COMMIT`. Follow this split when adding new domain events — never publish to Kafka inside the business transaction.
- Kafka consumer idempotency/dead-letter infra lives in `common/src/main/java/com/example/simplescheduleapp/common/kafka/consumer/` (`KafkaIdempotencyFilter`, `idempotency/ConsumeHistoryIdempotencyService`) and `common/src/main/java/com/example/simplescheduleapp/common/kafka/deadletter/` (`DeadLetterRecorder`).

## Comments & Documentation

- Comments are Korean and explain **why** (e.g. the compensation rationale block in `RedisSpecialLectureEnrollmentService`, the `// 49092 포트로 실행중인 작업이 없어야 함` port note in `common/src/testFixtures/java/com/example/simplescheduleapp/support/ApplicationWithKafkaTest.java`).
- Tests use `// given` / `// when` / `// then` section comments consistently.
- Config YAML files carry inline Korean comments for non-obvious settings.
- No Javadoc requirement; add block comments only where behavior is non-obvious (concurrency, AOP, transaction phases).

## Commit Convention (Korean, type-prefixed)

Verified over branch history — messages are `<type>: <Korean description>`:
```
feat:      new feature                 refactor:  most common type
fix:       bug fix                     test:      tests (e.g. "test: 데드락 테스트")
chore:     build/config                docs:      documentation
```
Real examples: `refactor: 유니크 제약 조건으로 도메인 서비스 해소 가능 여지 확인`, `fix: 재처리 카운트 버그 수정`, `chore: 빌드 수정`. Write commit subjects in Korean; keep code identifiers in English.

## Function/Method Design

**Size:** Service methods stay short (< ~25 lines); validation is delegated to entity methods, not inlined `if` chains in services.

**Parameters:** Once a method needs 3+ inputs, pass a `Command` record (`createLecture(LectureCreateCommand command)`), not positional primitives.

**Return values:** Domain methods return entities or `void`; services return entities/lists; controllers convert to Response records.

## Module Design

**Dependency direction:** `course` and `notification` each declare `implementation project(':common')` + `testImplementation(testFixtures(project(':common')))`. `common/build.gradle` exposes shared starters via **`api`** (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-kafka`) so they propagate to consumers; JWT (`jjwt` 0.12.5) is `implementation`/`runtimeOnly` (internal detail). `common` never depends on `course`/`notification`; **before ADR-0003 Stage 2 (superseded, commit `45c694b`)** the two servers communicated only via Kafka topics (`common/src/main/java/com/example/simplescheduleapp/common/kafka/topic/KafkaTopics.java`) plus an internal HTTP client (`CourseClient` → `LectureEnrollmentInternalController`) — `CourseClient` and its package are deleted; the equivalent synchronous call is now the in-process `app/src/main/java/com/example/simplescheduleapp/integration/InProcessEnrolledStudentsAdapter.java`. Never add a direct `course`↔`notification` project dependency (still current — the two remain separate Gradle modules even though `:app` assembles them into one JVM).

**common is a library:** `bootJar` disabled / `jar` enabled in `common/build.gradle`. `course`/`notification` are themselves libraries too now (`ssa.java-library` plugin, no own `bootJar` — ADR-0003 Stage 2); `app` is the sole runnable Spring Boot app (`app/src/main/java/com/example/simplescheduleapp/SsaApplication.java`). `CourseApplication.java`/`NotificationApplication.java` still exist but only as test-only bootstraps under `course/src/test/java/...`/`notification/src/test/java/...`, not under `src/main/java` as previously documented here.

**Toolchain:** Java 21 (`JavaLanguageVersion.of(21)` in every module), Spring Boot 3.4.3, Gradle wrapper committed (`gradlew`). Redisson 3.29.0 (course), Firebase Admin 9.2.0 + spring-retry + AOP starter (notification), MySQL connector for prod / H2 for tests (`runtimeOnly` in both server modules).

**CI/CD:** `.github/workflows/ci.yml` runs `./gradlew buildAll` (root `build.gradle.kts` task, `dependsOn(":common:build", ":course:build", ":notification:build", ":app:build")` plus the geekchat composite build; sequential, no `--parallel`) on JDK 21 (temurin) for PRs and pushes to `main`/`develop`; `playground`/`ngrinder` are deliberately excluded from CI (load-test only; tests run against H2/`@EmbeddedKafka`, so no external services are needed). `.github/workflows/deploy.yml` handles deployment; Dockerfiles at `app/Dockerfile` (course+notification, single JVM) and `geekchat/Dockerfile`, nginx config under `deploy/nginx/conf.d/`.

---

*Convention analysis: 2026-07-02*
