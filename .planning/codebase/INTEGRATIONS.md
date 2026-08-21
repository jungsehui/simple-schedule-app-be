# External Integrations

**Analysis Date:** 2026-07-02

> **Branch note:** This analysis is based on branch `feature/query-performance-tuning` (develop + 24 commits; a superset of `develop`). The `main` branch is only a skeleton and does NOT reflect the real codebase. The working tree analyzed also includes CI/CD infrastructure additions from `chore/ai-infra-cicd` (built on top of `feature/query-performance-tuning`): `.github/workflows/`, `course/Dockerfile`, `notification/Dockerfile`, `deploy/`.

## Service Topology

**As of ADR-0003 Stage 2 (single-JVM modular monolith), `course` and `notification` are bounded-context library modules with no own bootJar — they no longer run as independent Spring Boot services.** `:app` assembles both into one process on port 8080; `:8081` no longer exists.

| Module | Port | Database | Kafka consumer group | Purpose |
|---|---|---|---|---|
| `app` | 8080 | Local MySQL `localhost:3306/ssa` (single schema) / prod Supabase PostgreSQL (schema `ssa`) | `ssa-server-group` | Single JVM assembling `course` (lectures, enrollments, members, schedules) + `notification` (SSE streams + FCM push) |
| `playground` | 8082 | MySQL `localhost:3308/test` (test profile) | `test-ssa-group` | Performance experiments only, not deployed |

In production (`deploy/docker-compose.prod.yml`), only nginx (port 80) is exposed. `deploy/nginx/conf.d/ssa.conf` routes everything except `/chat/**` (GeekChat, a separate JVM process) to a single `ssa_backend` upstream (`app:8080`) — including `/sse-stream` and `/fcm/` — and returns 403 for `/internal/`. There is no `notification:8081` upstream.

## APIs & External Services

**Push Notifications — Firebase Cloud Messaging:**
- SDK: `com.google.firebase:firebase-admin:9.2.0` (`notification/build.gradle`)
- Bootstrap: `notification/src/main/java/com/example/simplescheduleapp/fcm/config/FcmConfig.java` — builds `FirebaseApp`/`FirebaseMessaging` beans from `GoogleCredentials.fromStream(new ClassPathResource(FCM_KEY_JSON).getInputStream())` scoped to `fcm.key.url` (`https://www.googleapis.com/auth/firebase.messaging`)
- Auth: service-account JSON `ssa-fcm-firebase-adminsdk-fbsvc-cabee70300.json` referenced by `fcm.key.json` in `notification/src/main/resources/application.yml`; the file is **gitignored** (first entry in `.gitignore`) and not committed
- Prod plan: JSON volume-mounted to `/app/config/fcm-service-account.json` with `FCM_KEY_JSON` env var (`deploy/docker-compose.prod.yml`); a `notification/Dockerfile` comment notes `FcmConfig` must switch from `ClassPathResource` to Spring's Resource abstraction for this to work
- Sending pipeline: `notification/src/main/java/com/example/simplescheduleapp/fcm/application/FcmService.java`, `fcm/domain/service/FcmMessageSender.java`, `fcm/domain/service/FcmApiFutureCallback.java` (async callback handling); token registration API in `fcm/presentation/FcmController.java`; tokens persisted via `fcm/domain/FcmToken.java` + `FcmTokenRepository.java`
- Resilience: Spring Retry + AOP (`spring-retry`, `spring-boot-starter-aop` in `notification/build.gradle`)

**Inter-service HTTP (internal API, notification → course):**
- Client: `notification/src/main/java/com/example/simplescheduleapp/notification/client/CourseClient.java` — Spring `RestClient` with 4xx/5xx handlers mapping to `ApplicationException(InternalServerExceptionCode.EXTERNAL_API_ERROR)`
- Config: `client.course-server-internal-url: http://localhost:8080/internal` (`notification/src/main/resources/application.yml`); prod value `http://course:8080/internal` via `CLIENT_COURSE_SERVER_INTERNAL_URL` env (`deploy/docker-compose.prod.yml`); wiring in `notification/.../notification/client/config/ClientConfig.java` + `ClientProperties.java`
- Server side: `course/src/main/java/com/example/simplescheduleapp/lecture/general/presentation/LectureEnrollmentInternalController.java` — `GET /internal/lectures/{lectureId}/student-ids`
- Exposure: `/internal/` is blocked at the nginx edge (`deploy/nginx/conf.d/ssa.conf` returns 403)

## Data Storage

**Databases:**
- Single shared database for the `:app` JVM (ADR-0003 Stage 2 — `course` and `notification` no longer have their own database):
  - `app` → local MySQL 8.0 `jdbc:mysql://localhost:3306/ssa` / prod Supabase PostgreSQL schema `ssa` (`app/src/main/resources/application.yml`, `application-prod.yml`)
  - The root `docker-compose.yml` still starts separate `course_mysql`/`notification_mysql` containers (3306/3307) from the pre-Stage-2 topology, but `app`'s local config only connects to the `ssa` schema on 3306; `deploy/docker-compose.prod.yml` has no MySQL containers at all (Supabase is externally managed)
  - Hikari: `maximum-pool-size: 21` (local) / `15` (prod)
  - JPA: `hibernate.ddl-auto: update`; `spring.jpa.open-in-view: false` is set repo-wide to prevent connection exhaustion from long-lived SSE Emitters (`app/src/main/resources/application.yml`)
- H2 in-memory for tests — `course/src/main/resources/application-test.yml` (`jdbc:h2:mem:testdb`, `H2Dialect`); `runtimeOnly com.h2database:h2` in `course`, `notification`, `playground`

**Caching / Distributed Locking (Redis):**
- Redis 7.2 — `spring.data.redis.host: localhost`, `port: 6379` in both `course` and `notification`; local container `sse-redis` in root `docker-compose.yml`; prod Redis is password-protected with `maxmemory 256mb` / `allkeys-lru` (`deploy/docker-compose.prod.yml`)
- `course` uses Redisson 3.29.0 for distributed locks:
  - `course/src/main/java/com/example/simplescheduleapp/config/RedissonConfig.java`, `config/RedisTemplateConfig.java`
  - Lock implementations: `course/src/main/java/com/example/simplescheduleapp/redis/lock/RedissonDistributedLock.java`, `redis/lock/SimpleRedisLock.java`
  - Annotation-driven AOP: `redis/aop/RedissonDistributedLockAop.java` + `redis/aop/CustomSpringELParser.java` (SpEL-resolved lock keys)
  - Applied to limited-seat enrollment: `course/src/main/java/com/example/simplescheduleapp/lecture/special/application/RedisSpecialLectureEnrollmentService.java`, `lecture/special/application/SpecialLectureRedisClient.java`
- `notification` uses Redis for SSE-related client state: `notification/src/main/java/com/example/simplescheduleapp/sse/infrastructure/redis/RedisClientManager.java` (moved from the now-removed `redis/cache/` package)

**File Storage:**
- Not used (no S3/GCS/blob clients)

## Messaging / Event Streaming (Kafka)

- Broker: Confluent `cp-kafka:7.3.2` + `cp-zookeeper:7.3.2` (root `docker-compose.yml` for local — `localhost:9092`, internal `kafka:29092`; prod uses `kafka:29092` only per `deploy/docker-compose.prod.yml`); test profile expects `localhost:49092` (`common/src/main/resources/application-common-test.yml`), tests use `@EmbeddedKafka` via `spring-kafka-test`
- Shared Kafka infrastructure lives in `common`:
  - Producer: `common/src/main/java/com/example/simplescheduleapp/common/kafka/producer/KafkaProducer.java`, `KafkaProducerConfig.java`
  - Consumer: `common/.../common/kafka/consumer/KafkaConsumerConfig.java`, `KafkaConsumerProperty.java`
  - Idempotent consumption: `common/.../common/kafka/consumer/KafkaIdempotencyFilter.java`, `KafkaMessageConsumeHistory.java` (+ `KafkaMessageProcessConsumeHistoryRepository.java`), `consumer/idempotency/ConsumeHistoryIdempotencyService.java`, `IdempotencyService.java`
  - Dead-letter handling: `common/.../common/kafka/deadletter/DeadLetter.java`, `DeadLetterRecorder.java`, `DeadLetterRepository.java`
  - Topics/messages: `common/.../common/kafka/topic/KafkaTopics.java`, `KafkaDomainEventMessage.java`, `KafkaLectureEventMessage.java`, `LectureEventType.java`
- Transactional outbox pattern in `common/src/main/java/com/example/simplescheduleapp/common/event/`: `DomainEvent.java` + `DomainEventRepository.java` + `EventStatus.java` (persisted events), `outbox/EventRecorder.java` + `outbox/EventRecordListener.java` (record within transaction), `producer/KafkaEventProducer.java` + `producer/EventProducerListener.java` + `producer/EventAsyncTaskExecutorConfig.java` (async publish after commit), `mapper/DomainEventMapper.java` + `DomainEventMapperFactory.java`
- Producers: `course` publishes lecture-enrollment lifecycle events (`course/src/main/java/com/example/simplescheduleapp/event/` — `LectureEnrollmentRequestedEvent`, `LectureEnrollmentAcceptedEvent`, `LectureEnrollmentRejectedEvent`, `LectureEnrollmentCanceledEvent`, `LectureUpdatedEvent`, `mapper/CourseDomainEventMapper.java`)
- Consumer: `notification/src/main/java/com/example/simplescheduleapp/notification/infrastructure/NotificationKafkaConsumer.java` — consumes course events to create notifications (SSE + FCM)

## Real-time / Streaming to Clients (SSE)

- `notification` serves Server-Sent Events: `notification/src/main/java/com/example/simplescheduleapp/sse/presentation/SseController.java`, `sse/application/SseConnectionService.java`, `sse/application/SseEmitterRegistry.java` (renamed from `SseEmitterRepository`, moved out of the now-removed `sse/cache/` package)
- nginx is tuned for it (`deploy/nginx/conf.d/ssa.conf` `/sse-stream` block: `proxy_buffering off`, `proxy_read_timeout 1h`, HTTP/1.1 keep-alive)
- `spring.jpa.open-in-view: false` in `app/src/main/resources/application.yml` guards the connection pool against long-lived emitters (`notification/src/main/resources/application.yml` no longer exists, ADR-0003 Stage 2)

## Authentication & Identity

- Custom JWT auth implemented in `common` (no Spring Security, no external IdP):
  - `common/src/main/java/com/example/simplescheduleapp/common/auth/TokenService.java` — jjwt 0.12.5 issue/verify
  - `common/.../common/auth/Auth.java` (annotation) + `AuthArgumentResolver.java` + `AuthConfig.java` — controller argument resolution
  - `common/.../common/auth/BearerTokenExtractor.java`, `Token.java`, `TokenProperty.java` (`token.secretKey`, `token.accessTokenExpirationMillis` = 86400000 ms), `TokenExceptionCode.java`
- Secrets: test/local secret in `common/src/main/resources/application-common-{local,test}.yml`; prod via `JWT_SECRET` env (`deploy/.env.example`, `deploy/docker-compose.prod.yml`)
- CORS: `common/src/main/java/com/example/simplescheduleapp/common/config/CorsConfig.java`; `cors.allowed-origins: http://localhost:3000` locally (frontend app), `CORS_ALLOWED_ORIGINS` env in prod

## Monitoring & Observability

**Error Tracking:** None (no Sentry/Rollbar/APM)

**Metrics:** No Actuator/Micrometer wired into `course`/`notification` (`spring-boot-starter-actuator` absent from module `build.gradle` files). Grafana/InfluxDB exist only for load-test experiments under `playground/` (`playground/docker-compose-influxdb.yml`, `playground/grafana-provisioning/`)

**Logging:**
- SLF4J + Logback (Boot default), Lombok `@Slf4j` throughout
- Request-scoped correlation: `common/src/main/java/com/example/simplescheduleapp/common/filter/MdcLoggingFilter.java` (MDC logging filter)
- nginx access log with request/upstream timing (`deploy/nginx/nginx.conf` `log_format main ... rt=$request_time uct=$upstream_connect_time`)
- No centralized log aggregation

## CI/CD & Deployment

**CI:** `.github/workflows/ci.yml` — on push/PR to `main`/`develop`: JDK 21 (Temurin), Gradle build cache, `./gradlew :common:build :course:build :notification:build --parallel`; test reports uploaded as artifacts on failure; `playground`/`ngrinder` excluded

**CD:** `.github/workflows/deploy.yml` (manual `workflow_dispatch`, env choice `prod`/`stage`):
1. Matrix build (`course`, `notification`) → `./gradlew :<module>:bootJar` → Docker image (`course/Dockerfile`, `notification/Dockerfile`, non-root `spring` user) → push to GHCR (`ghcr.io/<owner>/ssa-<module>:<sha>` and `:latest`)
2. Deploy job: brings up a WireGuard tunnel (secrets `WG_PRIVATE_KEY`, `WG_ADDRESS`, `WG_PEER_PUBLIC_KEY`, `WG_PRESHARED_KEY`, `WG_ENDPOINT`, `WG_ALLOWED_IPS`), SSHes to the server (`DEPLOY_SSH_KEY`, `DEPLOY_SSH_KNOWN_HOSTS`, `DEPLOY_HOST`, `DEPLOY_USER`), syncs `deploy/docker-compose.prod.yml` + `deploy/nginx/` to `/opt/ssa/`, then `docker compose -f docker-compose.prod.yml --env-file .env pull && up -d`

**Hosting:** single self-hosted server behind WireGuard; nginx reverse proxy (`deploy/nginx/nginx.conf` — rate limit 20 r/s + burst 40, gzip, security headers, `server_tokens off`) is the only exposed service (port 80; 443/TLS marked TODO in `deploy/docker-compose.prod.yml`)

## Environment Configuration

**Local (yml-based, root `docker-compose.yml` supplies infra):**
- `spring.datasource.*` for the single `:app` JVM; Redis `localhost:6379`; Kafka `localhost:9092` (via `common/src/main/resources/application-common-local.yml`); `fcm.key.json`/`fcm.key.url`; `token.secretKey`/`token.accessTokenExpirationMillis`; `cors.allowed-origins`

**Production (env vars via `deploy/docker-compose.prod.yml` + server-side `.env`; template `deploy/.env.example`):**
- `GHCR_OWNER`, `IMAGE_TAG`, `MYSQL_ROOT_PASSWORD`, `COURSE_DB_USER`, `COURSE_DB_PASSWORD`, `NOTIFICATION_DB_USER`, `NOTIFICATION_DB_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, `JWT_ACCESS_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`
- FCM service-account JSON lives only on the server at `deploy/secrets/fcm-service-account.json` (mounted read-only into the notification container)

**Secrets location:**
- FCM JSON: gitignored, classpath-loaded locally, volume-mounted in prod
- CI/CD secrets: GitHub Actions Secrets (WireGuard + SSH + registry creds)
- No Vault/cloud secrets manager

## Webhooks & Callbacks

**Incoming:** None

**Outgoing:** None beyond FCM push delivery and the internal notification→course HTTP call described above

## Load-Testing Tooling (non-production)

- `ngrinder/` — Groovy nGrinder scripts module (`ngrinder-core:3.5.9`); currently `build.gradle` only, no `src/`
- `playground/` — experiment harness: k6 scripts (`playground/k6-scripts/`), Grafana dashboards/provisioning, InfluxDB and nGrinder compose files, thread-model comparison compose files, `playground/MacOsPerformanceMetric.bash`; `playground/ngrinder-controller/` is an nGrinder controller runtime data directory (checked in, kept by a `.gitignore` exception)

---

*Integration audit: 2026-07-02*
