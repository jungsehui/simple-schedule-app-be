# SSA Backend — HANDOFF

> 마지막 갱신: 2026-07-17 · 브랜치 `develop` (`5987d3d`)
> 작업 트리: worktree `simple-schedule-app-be/.wt-ssa-analysis` · inner repo `simple-schedule-app-be/ssa` (main, `2c6a7b0`로 최신화됨)

## -3. ✅ ADR-0004 전면 도메인 순수화 완료 (PR #24, `5987d3d`, 2026-07-17)
ADR-0002가 걸어둔 Stage 3 동결을 **오너 결정으로 해제**하고 전면 순수화 진행. 도메인=순수 POJO, JPA는 `infrastructure/persistence/`(Entity+Mapper+JpaRepository+RepositoryAdapter)에 격리.

- **래칫 해소: course 177→0, notification 38→0, common 54→5.** 잔여 5줄은 `DomainEventExceptionCode`의 HttpStatus 참조 — 프로젝트 전반 컨벤션이라 의도적 잔존.
- Phase A(교차참조 객체→ID) → Phase B(Member·Schedule JOINED 계층, 수강등록 3종, FailedNotification, FcmToken) → common(`common.domain`→`common.persistence` 개명, 이벤트 인프라 `common.outbox` 분리, DomainEvent 3분리).
- **순수화가 드러낸 결함 2건 수정**: ① `created_date` 유실(`7f7eafc`) — 순수 도메인엔 감사 필드가 없어 매퍼가 만든 새 엔티티로 UPDATE하면 null로 덮임 ② **아웃박스 id 전파**(`df1c77a`) — 분리 후 매퍼가 만든 *다른 인스턴스*에 id가 담기므로 전파 안 하면 발행 후 save()가 INSERT가 되어 중복 배달. `EventRecorder.assignId` + `EventRecorderTest`가 가드.
- **별건 결함**: bcrypt 전환(`f6ae465`, 무염 SHA-256→bcrypt, 점진 마이그레이션이라 기존 사용자 무중단) · SSE 하트비트 누수(`ea35036`) · notification↔fcm 순환 제거(`bf23bcc`).
- **CI 구멍 수정(`971cb2f`)**: `buildAll`이 `:common:test`를 빠뜨려 **ArchUnit 래칫이 CI에서 한 번도 안 돌고 있었다**. 즉 순수화가 되돌아가도 CI는 초록. `:common:build` 추가로 해소 — CI 로그에 `> Task :common:test` 최초 확인.
- 검증: 로컬 118 테스트 green, PR CI green(8m6s, headSha=971cb2f). **스키마 변경 없음**(discriminator·컬럼 전부 보존).

## -2. 🏗️ 모듈러 모놀리스 진행 (ADR-0003) — Stage 0·1·2 완료
- **Stage 0**(`1668e41`): Flyway baseline(prod), 관용 파서(memberId↔sub), git hooks(pre-push=buildAll).
- **Stage 1**(PR #21): 빌드·CI 단일화 — buildSrc 컨벤션 3종 + `libs.versions.toml` + `.kts` + `includeBuild("geekchat")`+`buildAll` + CI 1잡. 루트 Gradle **8.14.5**.
- **Stage 2**(PR #22, `92464a3`): **course+notification → 단일 JVM `:app`** (2 프로세스=app+geekchat). course/notification=라이브러리, 통합 main `SsaApplication`. Kafka 자기소비 유지. **단일 `ssa` 스키마**(공유 인프라 테이블 단일 카피, 프리픽스 없음 — 사용자 결정). /internal HTTP→`InProcessEnrolledStudentsAdapter`(:app). 검증: 통합 컨텍스트 테스트 + 로컬 Postgres prod 부팅(Flyway V1·18테이블·Kafka 배선). 아키텍처 리뷰 위반 0.
- 통합 빌드: `./gradlew buildAll` = `:app`(course+notification) + geekchat(:app). 배포 이미지 = ssa-app + ssa-geekchat(2종).
- **다음 = Stage 3**(SSA Boot 3.4→4.1 정렬). 이후 Stage 4(geekchat 흡수=단일 JVM 완성) → Stage 5(account 통합=member+user).
- 후속 개선(비블로커): Flyway 정착 후 ddl-auto→validate, `app.integration` ArchUnit 가드, Kafka KRaft 전환(zookeeper 제거, −512m).

## ⚠️ Stage 2 배포 전 수동 준비 (다음 배포 시 필수)
1. Supabase: `CREATE SCHEMA IF NOT EXISTS ssa;` (기존 ssa_course/ssa_notification은 폐기 — pre-Flyway 테스트 데이터)
2. 서버 `/opt/ssa/.env`: `COURSE_DB_*`/`NOTIFICATION_DB_*` → **단일 `SSA_DB_URL`/`SSA_DB_USER`/`SSA_DB_PASSWORD`** (currentSchema=ssa). deploy/.env.example 참고.
3. 이미지가 ssa-course/ssa-notification(2) → **ssa-app(1)**로 바뀜. compose는 app 서비스 하나. 첫 배포 시 구 course/notification 컨테이너는 `--remove-orphans`로 정리됨.
4. GEEKCHAT_DDL_AUTO=validate 유지(변경 없음).

## -1. 🚀 운영 배포 LIVE (2026-07-11, main `2c6a7b0`)
- **3 프로세스 전부 가동·검증 완료**: course(actuator UP)·notification(FCM 200)·geekchat(healthy) + nginx/kafka/redis/zookeeper. Supabase 테이블 15/5/7 생성.
- 배포 경로: main push → Deploy 워크플로(이미지 3종→GHCR) → WG 터널 → SSH → compose. **주의: CI가 노트북과 같은 WG 피어 키 사용 — 배포 중 노트북 터널 Deactivate 필수** (가드가 90초 내 감지·실패, `gh run rerun <id> --failed`로 재시도).
- 트러블슈팅 이력(재발 방지): ① JWT_SECRET은 **base64url**(`openssl rand -base64 64 | tr '+/' '-_'`) ② RedissonConfig password 반영 픽스(f8f5bc8) ③ FCM 키 파일은 컨테이너 유저가 읽도록 644 ④ GHCR pull은 CI GITHUB_TOKEN만 가능(gh CLI 토큰은 read:packages 없음) ⑤ Supabase는 **aws-1** 세션 풀러.
- 서버 상태 파일: `/opt/ssa/.env`(GEEKCHAT_DDL_AUTO=validate 전환됨), `/opt/ssa/secrets/fcm-service-account.json`.
- 남은 개선: CI 전용 WG 피어 발급(DokaDev) 또는 서버측 pull 기반 자동 배포(read:packages PAT) — 터널 토글 불필요화.

## 0. 최신: GeekChat 모노레포 + Supabase PG + 자동 배포 (PR #17)
- **GeekChat 편입**: geek-chat/server-v2 → `geekchat/` (subtree, 히스토리 보존). 한 레포 · 두 Gradle 빌드(Kotlin/Boot4 vs Java/Boot3.4). `common` 모듈명 충돌은 빌드 분리로 회피.
- **DB**: MySQL 컨테이너 제거 → **Supabase PostgreSQL 단일 외부 DB**, 스키마 격리(`ssa_course`/`ssa_notification`/`geekchat`). course prod PG 기동·DDL·네이티브 쿼리 로컬 PG16 검증 완료. ⚠ Supabase 접속은 **세션 풀러 호스트**(aws-0-ap-northeast-2.pooler.supabase.com:5432, user=postgres.&lt;ref&gt;) 사용 — direct 호스트는 IPv6 전용.
- **CD**: `deploy.yml`이 **main push에 자동 트리거** (develop→main PR 병합 = 배포). 이미지 3종(ssa-course/-notification/-geekchat) → GHCR → GH Actions가 WireGuard 터널로 서버 SSH 배포. nginx가 `/chat/`(REST)·`/chat/ws`(WebSocket)를 geekchat(context-path=/chat)로 라우팅.
- **배포 전 수동 준비 (1회)**: ① Supabase SQL Editor에서 스키마 3개 CREATE ② 서버 `/opt/ssa/.env`를 `deploy/.env.example` 기준으로 갱신(시크릿 전부 신규 발급 — 채팅에 노출된 Supabase 비밀번호·WG 키는 로테이션) ③ GitHub Secrets(WG_* 6 + DEPLOY_* 4) 등록 ④ 첫 배포만 `GEEKCHAT_DDL_AUTO=update` → validate 복귀 ⑤ 서버 SSH 접근 확인(shjung@10.64.212.20, WG 터널 경유).
- 잔여: notification PG 기동 스모크(FCM 키 필요), 서버 MySQL 기존 데이터 이관(범위 외, 빈 DB 시작), geekchat OAuth 콜백 URL 실도메인 반영.

## 1. 지금 상태 한 줄 요약 (develop 기준)
원격 29개 브랜치 전수 조사 → **유효 작업 전부 develop에 통합 완료**. develop = `a2dd683`, 3모듈 clean 빌드·테스트 green, CI green. PR #17(모노레포+PG+CD)이 develop 대기 중.

## 2. 이번 세션 통합 내역 (2026-07-10)
### 1차: 열린 PR 3개 병합
- **PR #15** input-validation (`dd076a2`) · **PR #14** observability+4단계 동시성 방어 (`337bc82`, main→develop 재타깃, 충돌 3건 해소) · **PR #13** 헥사고날 기반 (`031751c`) · 래칫 확장 `Schedule.version` 2건(151→153, `bc87f98`)

### 2차: 잔여 작업 브랜치 5종 적응 통합 (**PR #16**, `73e6c6e`)
병렬 분석 에이전트 7개가 실코드 기준으로 판정 후 순차 통합. 유닛별 전체 빌드 green 확인 후 커밋.
- `ab4fdb2` **Outbox Relay** (stage/1-2): 적응 — produce() @Transactional 제거(실패 상태 롤백 결함), retryCount 선영속(DEAD 도달 보장), @Column 미사용(래칫 위반 0)
- `82edda6` **스케줄 충돌 감지** (stage/2-1): 적응 — ScheduleRepository 3분할, ScheduleConflictValidator 순수 자바 + `config.ScheduleConfig` @Bean. 신규 409 SC0/SC1
- `fc9debb` **낙관적 락 재시도** (stage/1-3): 적응 — Lecture.@Version 드롭(Schedule 루트와 중복 → Hibernate 기동 실패 방지), @Retryable 3개 경로 유지
- `2363aa9` **Redis Lua 원자화** (feature/1-5): obs AtomicCounter 구조로 재구현 — 키부재(-2)/소진(-1) 구분, INCR-back 제거, TTL=종료+1일(과거면 30일). 신규 404 SL002
- `c3fb8e3` **인증 Phase 3a** (feature/1-1): 원안 3b 하드컷을 계약 게이트에 맞춰 **듀얼리드로 각색** — @RequireRole+RoleInterceptor(무토큰 통과, 불일치 403 T6), AuthIdentities(토큰 우선, 둘 다 없으면 401 T7), accept/reject 소유권 검증(토큰 제시 시만)

### 스킵 (사유 확정)
- `feature/sse` fix: develop에서 이관+소프트삭제로 해소 — 적용 시 재신청 차단 회귀
- `stage/1-4`: PR #15와 패치 동일(git cherry "-")
- `test/select-lock-deadlock`: 프로덕션 코드 160줄 삭제하는 실험 스냅샷

## 3. 검증 상태
- 로컬: `./gradlew clean :common:build :course:build :notification:build` → BUILD SUCCESSFUL (develop `73e6c6e`, 31 tasks)
- CI: PR #16 pass(2m51s) · develop push CI 감시 중(문서 커밋 포함 재확인 예정)
- 테스트 신규 20건: relay 3 · 충돌검증 3 · 재시도계약 1 · RedisClient 8(기존 5 갱신 — 사용자 승인) · RoleInterceptor 5 · AuthIdentities 3 · 소유권 1 (기존 accept/reject 2건 시그니처 갱신 — 사용자 승인)
- ArchUnit 래칫: course 153 유지, 신규 위반 0 (2-1의 신규 리포지토리는 3분할로 무위반 통합)

## 4. 계약 변경 (API-CONTRACT.md 반영 완료 — FE/오케스트레이터 공지 필요)
전부 additive (기존 클라이언트 무영향):
- 식별자 파라미터(tutorId/studentId/memberId) optional화 — `Authorization: Bearer` 시 토큰이 우선
- 403 T6 (토큰 제시+role 불일치) · 401 T7 (식별자 전무) · 400 L2 (소유권 위반, 기존 코드 신규 표면) · 409 SC0/SC1 (스케줄 충돌) · 404 SL002 (Redis 정원 키 부재)
- SSE는 경로 변수 폴백 유지 (EventSource 헤더 제약 — 3b 전 SSE 토큰 전송 방식 결정 필요)

## 5. 다음 할 일 (우선순위)
1. **인증 Phase 3b** (오케스트레이터 게이트): Android Phase 2 완료 후 — @Auth required 전환, 레거시 파라미터 제거, RoleInterceptor 무토큰 401. 코드에 `Phase 3a`/`3b에서 전환` 주석으로 전환 지점 전부 표시됨.
2. ~~Stage 2 나머지~~ / ~~Stage 3 도메인 JPA 제거~~ → **ADR-0004로 완료**(위 -3절). `DomainEventRepository`도 포트+어댑터로 분리됨(`c342c8d`).
3. **ADR-0003 Stage 4**: geekchat 흡수(단일 JVM 완성) → Stage 5(account 통합=member+user).
4. **운영 후속**: outbox DEAD 이벤트 메트릭/알림, domain_event.status 인덱스, 멀티 인스턴스 시 relay 중복 실행 가드(ShedLock), Redis 정원 키 백필 절차(SL002 대량 발생 대비), @Retryable 소진 시 409 매핑(@Recover).
5. **브랜치 청소** (승인 필요): 통합 완료된 원격 브랜치 삭제 — feature/1-1~1-5, 2-1, 2-4, stage/*, 병합된 PR 브랜치들.

## 6. 주의/함정
- ArchUnit freeze 갱신은 같은 커밋에 · common 테스트 DoNotIncludeJars 금지 · PR 재타깃만으론 CI 안 돎(close/reopen)
- **래칫을 새 모듈에 붙이면 `./gradlew buildAll --dry-run | grep :<모듈>:test`로 CI 포함 여부 확인 필수** — 규칙이 작동해도 CI에서 안 돌면 무의미하다(2026-07-17 common이 정확히 이 상태였음, `971cb2f`)
- **순수 도메인 ↔ 엔티티 매핑 시 id·감사필드 전파 확인** — 매퍼가 만드는 건 *다른 인스턴스*다. id 미전파=INSERT 중복, createdDate 미전파=null 덮어쓰기. 새 애그리거트 순수화 시 왕복 테스트 필수
- `ci.yml`의 `pull_request: branches:`는 **base 기준 필터** — base가 목록 밖이면 CI가 아예 안 돈다(PR #24가 `integration/*` base라 24개 커밋 동안 CI 0회)
- 낙관적 락 @Version은 **Schedule 루트에만** — 자식 엔티티에 추가하면 Hibernate 기동 실패
- KafkaEventProducer.produce()에 @Transactional 금지 — 실패 상태 저장이 롤백됨
- Redis 정원 키 완전 소실(플러시) 시 특강 신청 전부 404 SL002 — 재적재 절차 필요
- 보안: 시크릿 로테이션(JWT/DB/WireGuard/SSH/FCM) 여전히 미완 — `docs/SECURITY.md`
- NotificationIntegrationTest 1건 @Disabled · slow 태그는 `-PincludeSlow`

## 7. 검증 명령
```
cd simple-schedule-app-be/.wt-ssa-analysis
./gradlew buildAll                                          # CI와 동일한 단일 엔트리포인트
wc -l */src/test/archunit-violations/*                      # 래칫 진행도 (course 0 / notification 0 / common 5)
./gradlew buildAll --dry-run | grep ":.*:test"              # CI가 실제로 도는 test 태스크 확인
```
로컬 인프라: `docker compose up -d` (MySQL 3306/3307, Redis 6379, Kafka 9092).

## 8. 문서 지도
- `docs/adr/0001` 멀티모듈 · `docs/adr/0002` 헥사고날 로드맵 · `docs/architecture/hexagonal-guidelines.md`
- `docs/SECURITY.md` 보안 · `.planning/codebase/` 분석 7종 · 루트/모듈 `AGENTS.md`
- `orchestration/API-CONTRACT.md` 클라이언트 계약 단일 기준 (2026-07-10 Phase 3a 갱신됨)
