# Technology Stack

**Analysis Date:** 2026-07-02

> **Branch note:** This analysis is based on branch `feature/query-performance-tuning` (develop + 24 commits; a superset of `develop`). The `main` branch is only a skeleton and does NOT reflect the real codebase. The working tree analyzed also includes CI/CD infrastructure additions from `chore/ai-infra-cicd` (built on top of `feature/query-performance-tuning`): `.github/workflows/`, `course/Dockerfile`, `notification/Dockerfile`, `deploy/`.

## Languages

**Primary:**
- Java 21 — all application modules (`java.toolchain.languageVersion = JavaLanguageVersion.of(21)` in root `build.gradle` `subprojects` block and repeated in each module's `build.gradle`). 270 `.java` files across `common/`, `course/`, `notification/`, `playground/`.

**Secondary:**
- Groovy — `ngrinder` module load-test scripting (`ngrinder/build.gradle` applies the `groovy` plugin, depends on `org.apache.groovy:groovy`); Gradle build scripts are Groovy DSL (not Kotlin DSL).

## Runtime

**Environment:**
- JVM 21 (Gradle toolchain-pinned; CI uses Temurin 21 via `actions/setup-java@v4` in `.github/workflows/ci.yml`)
- Production containers: `eclipse-temurin:21-jre-jammy` base image with `-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC` (`course/Dockerfile`, `notification/Dockerfile`)

**Package Manager / Build Tool:**
- Gradle 8.11.1 via wrapper (`gradle/wrapper/gradle-wrapper.properties`)
- Lockfile: none (versions resolved via Spring Boot BOM / `io.spring.dependency-management`)

## Multi-Module Layout

`settings.gradle` (root project name `simple-schedule-app`) wires five modules:

| Module | Role | Boot app? |
|---|---|---|
| `common` | Shared library (web/JPA/validation/Kafka/JWT + test fixtures) | No (`bootJar` disabled, plain `jar` enabled in `common/build.gradle`) |
| `course` | Course/lecture/member bounded-context library — no own port, assembled into `:app` (ADR-0003 Stage 2) | No (`bootJar` disabled; `:app` is the sole bootable module) |
| `notification` | Notification bounded-context library: SSE + FCM — no own port, assembled into `:app` (ADR-0003 Stage 2; `:8081` no longer exists) | No (`bootJar` disabled; `:app` is the sole bootable module) |
| `playground` | Performance/load-test sandbox (port 8082, standalone — no `common` dependency) | Yes |
| `ngrinder` | nGrinder load-test scripts (Groovy) — `ngrinder/build.gradle` only, **no `src/` directory exists** | N/A |

Root `build.gradle` declares plugin versions with `apply false` and applies `java`, `java-library`, `org.springframework.boot`, `io.spring.dependency-management` plus shared Lombok/test dependencies to all subprojects. `course` and `notification` depend on `common` via `implementation project(':common')` and `testImplementation(testFixtures(project(':common')))`.

## Frameworks

**Core:**
- Spring Boot 3.4.3 — root `build.gradle` (`id 'org.springframework.boot' version "3.4.3" apply false`); same version declared in `course/build.gradle`, `notification/build.gradle`, `playground/build.gradle`, `ngrinder/build.gradle`
- Spring Dependency Management plugin 1.1.7
- Spring Boot Starter Web — exposed as `api` from `common/build.gradle`, propagates to `course`/`notification`
- Spring Boot Starter Data JPA (`api` from `common`) — Hibernate ORM
- Spring Boot Starter Validation (`api` from `common`)
- Spring Kafka (`api` from `common`); Apache Kafka client 3.7.1 (`org.apache.kafka:kafka-clients:3.7.1` in `common/build.gradle`)
- Spring Boot Starter Data Redis (`notification/build.gradle`)
- Spring Retry (`org.springframework.retry:spring-retry`) + Spring Boot Starter AOP (`notification/build.gradle`) — retryable FCM/notification flows
- Redisson 3.29.0 (`org.redisson:redisson-spring-boot-starter:3.29.0` in `course/build.gradle`) — distributed locks
- No Spring Security starter in any module — auth is custom JWT (see Key Dependencies)

**Testing:**
- Spring Boot Starter Test (JUnit 5, Mockito, AssertJ) — applied to all subprojects from root `build.gradle`; `useJUnitPlatform()` everywhere
- `spring-kafka-test` — `@EmbeddedKafka` support (`testFixturesApi` in `common/build.gradle`; also `testImplementation` in `playground/build.gradle`)
- Fixture Monkey 1.1.11 (Naver) — `fixture-monkey-starter`, `fixture-monkey-jakarta-validation` exposed as `testFixturesApi` from `common/build.gradle`
- `java-test-fixtures` Gradle plugin (`common/build.gradle`) — shared test support classes in `common/src/testFixtures/java/com/example/simplescheduleapp/support/`: `UnitTest.java`, `ApplicationTest.java`, `ApplicationWithKafkaTest.java`, `MockTestSupport.java`, `MonkeySupport.java`, `CommonExceptionTest.java`
- H2 (`runtimeOnly 'com.h2database:h2'` in `course`, `notification`, `playground`) — in-memory DB for tests (`course/src/main/resources/application-test.yml`: `jdbc:h2:mem:testdb`, `ddl-auto: create`)

**Build/Dev:**
- Lombok — `compileOnly` + `annotationProcessor` (and test variants) for all subprojects (root `build.gradle`); `testFixturesCompileOnly`/`testFixturesAnnotationProcessor` in `common/build.gradle`
- Spring Boot Configuration Processor — `annotationProcessor` in `course/build.gradle` and `notification/build.gradle` (autocompletion for `@ConfigurationProperties` classes such as `common/src/main/java/com/example/simplescheduleapp/common/auth/TokenProperty.java`)

## Key Dependencies

**Critical:**
- `io.jsonwebtoken:jjwt-api:0.12.5` (+ `jjwt-impl`, `jjwt-jackson` as `runtimeOnly`) in `common/build.gradle` — JWT issuing/parsing in `common/src/main/java/com/example/simplescheduleapp/common/auth/TokenService.java`
- `org.redisson:redisson-spring-boot-starter:3.29.0` (`course`) — distributed lock AOP (`course/src/main/java/com/example/simplescheduleapp/redis/aop/RedissonDistributedLockAop.java`)
- `com.google.firebase:firebase-admin:9.2.0` (`notification`) — FCM push (`notification/src/main/java/com/example/simplescheduleapp/fcm/config/FcmConfig.java`)
- `org.apache.kafka:kafka-clients:3.7.1` + `spring-kafka` (`common`) — event bus between services

**Infrastructure:**
- `com.mysql:mysql-connector-j` (`runtimeOnly` in `course`, `notification`, `playground`) — MySQL driver, version via Boot BOM
- HikariCP (via Boot) — tuned in `app/src/main/resources/application.yml` (local, `maximum-pool-size: 21`) and `app/src/main/resources/application-prod.yml` (prod, `maximum-pool-size: 15`); `course/src/main/resources/application.yml` and `notification/src/main/resources/application.yml` no longer exist (ADR-0003 Stage 2 — runtime config moved to `:app`)
- `org.ngrinder:ngrinder-core:3.5.9` (`ngrinder/build.gradle`) — load-test scripting

## Configuration

**Profiles:** `local` (default active), `stage`, `test`, `prod`.
- `course/src/main/resources/application.yml` and `notification/src/main/resources/application.yml` no longer exist — each module keeps only `application-test.yml` (ADR-0003 Stage 2). Base runtime config lives in `app/src/main/resources/application.yml`: `server.port: 8080`, MySQL `localhost:3306/ssa` (single schema), Redis `localhost:6379`, Kafka consumer group `ssa-server-group`, `cors.allowed-origins: http://localhost:3000`, `spring.jpa.open-in-view: false`, `fcm.key.json`/`fcm.key.url`; profile group `local` → includes `common-local`
- `common/src/main/resources/application-common-local.yml` / `application-common-test.yml` — shared Kafka bootstrap servers (`localhost:9092` local, `localhost:49092` test) and JWT `token.secretKey` / `token.accessTokenExpirationMillis` (86400000 ms = 24h)
- `course/src/main/resources/application-test.yml` — H2, Flyway disabled, test Kafka group `ssa-course-server-group-test`
- `notification/src/main/resources/application-test.yml` — **empty file** (inherits from `common-test` via profile group)
- `application-stage.yml` and `application-prod.yml` in both `course` and `notification` are **currently empty (0 bytes)** — prod config is injected via environment variables in `deploy/docker-compose.prod.yml` (e.g., `SPRING_DATASOURCE_URL`, `TOKEN_SECRETKEY`)
- `playground/src/main/resources/application-test.yml` — port 8082, MySQL `localhost:3308/test`, Kafka group `test-ssa-group`

**Secrets:**
- FCM service-account JSON (`ssa-fcm-firebase-adminsdk-fbsvc-cabee70300.json`) is **gitignored** (first entry in `.gitignore`) and loaded at runtime via `ClassPathResource` in `notification/src/main/java/com/example/simplescheduleapp/fcm/config/FcmConfig.java`; for prod it is volume-mounted to `/app/config/fcm-service-account.json` per `deploy/docker-compose.prod.yml` (note: a `notification/Dockerfile` comment flags that `FcmConfig` must move from `ClassPathResource` to Spring's Resource abstraction for the mount to work)
- Prod secrets come from a server-side `.env` file (template: `deploy/.env.example` — `GHCR_OWNER`, `IMAGE_TAG`, `MYSQL_ROOT_PASSWORD`, `COURSE_DB_USER/PASSWORD`, `NOTIFICATION_DB_USER/PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, `JWT_ACCESS_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`)
- Local dev DB password and a test JWT secret are hardcoded in local/test yml files (presence noted; see CONCERNS.md)

**Build:**
- Root `build.gradle`, `settings.gradle`, per-module `build.gradle` files
- `docker-compose.yml` (repo root) — local dev infrastructure
- `course/Dockerfile`, `notification/Dockerfile` — runtime-only images (jar built in CI via `./gradlew :<module>:bootJar`; Docker build context must be repo root)

## Platform Requirements

**Development (via `docker-compose.yml` at repo root):**
- MySQL 8.0 × 2 — `course_db` on host port 3306, `notification_db` on host port 3307
- Redis 7.2 (container `sse-redis`) on 6379
- Kafka + Zookeeper — Confluent images `cp-kafka:7.3.2` / `cp-zookeeper:7.3.2`; broker on `localhost:9092` (internal listener `kafka:29092`)
- JDK 21 (Gradle toolchain auto-provisions if missing)
- Load-testing extras under `playground/`: `docker-compose-ngrinder.yml`, `docker-compose-influxdb.yml`, k6 scripts (`playground/k6-scripts/`), Grafana dashboards (`playground/grafana-dashboard/`, `playground/grafana-provisioning/`), and thread-model comparison compose files (`docker-compose-thread-pool-{block,non-block}.yml`, `docker-compose-virtual-thread-{block,non-block}.yml`, etc.)

**Production:**
- Docker images pushed to GHCR (`ghcr.io/<owner>/ssa-course`, `ghcr.io/<owner>/ssa-notification`) by `.github/workflows/deploy.yml`
- Single self-hosted server (vCPU 4 / RAM 8GB per `deploy/docker-compose.prod.yml` comments) running the full stack via `deploy/docker-compose.prod.yml`: nginx 1.27-alpine front (port 80), both app containers, MySQL × 2, Redis 7.2 (password-protected, `allkeys-lru`), Kafka + Zookeeper — all with per-container memory limits
- Deploy access over WireGuard VPN + SSH (`deploy` job in `.github/workflows/deploy.yml`)
- CI: `.github/workflows/ci.yml` runs `./gradlew :common:build :course:build :notification:build --parallel` on push/PR to `main`/`develop`; tests are H2/`@EmbeddedKafka`-based so no external services are needed; `playground`/`ngrinder` are excluded from CI

---

*Stack analysis: 2026-07-02*
