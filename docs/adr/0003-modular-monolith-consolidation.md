# ADR-0003: 단일 JVM 모듈러 모놀리스 통합 (SSA + GeekChat)

- 상태: 승인 (2026-07-11)
- 결정자: jungsehui + Claude (표본: linkareer-migration-spring, geek-chat-server-v2)

## 배경

모노레포에 두 개의 독립 Gradle 빌드(SSA: Java 21/Boot 3.4.3/Groovy, GeekChat: Kotlin 2.3/Boot 4.1/Modulith)와
세 개의 JVM 프로세스(course/notification/geekchat)가 공존한다. 운영 단순화·비용 절감·도메인 응집을 위해
**하나의 Gradle 빌드, 하나의 bootable 앱, 하나의 CI/CD**로 통합한다. 품질 기준은 표본 두 프로젝트의
하우스스타일(순수 도메인, 포트/어댑터, 컨텍스트 경계 명문화, 빌드 그래프로 순수성 강제)이다.

## 결정

| # | 항목 | 결정 | 근거 |
|---|---|---|---|
| 1 | Boot 버전 | **4.1.0으로 통일 (SSA 상향)** | GeekChat이 Boot4 전용 기능(Modulith 2.1, Spring AI 2.0, Jackson 3, webmvc-test)에 실결합. 3.4 OSS 지원 종료. 하향은 이중 비용 |
| 2 | 정준 ID | **Long(BIGINT, member_id 승계) + `account.external_uuid`(GC UUID 보존)** | Phase 3a 계약(JWT memberId, SSE 경로)에 Long이 박제. 이중 키면 양측 FK 이관 0건 |
| 3 | 인증 스택 | **Spring Security(GC) 채택.** JWT HS512 단일 신규 시크릿, claims=`sub`+`memberId`(호환)+`role`+`admin`, access 15m / refresh 14d 회전 | refresh 회전·OAuth·BCrypt·@PreAuthorize 기구현. 전환기: `securityMatcher("/chat/**")` 체인 + SSA permitAll 체인 + RoleInterceptor 존치 → Phase 3b에 @PreAuthorize 수렴 |
| 4 | 비밀번호 | DelegatingPasswordEncoder(`{sha256}` 매처) + 로그인 성공 시 BCrypt(12) lazy re-hash | SSA 무염 SHA-256은 일괄 변환 불가. Phase 3b에서 잔존 계정 강제 재설정 |
| 5 | 모듈 트리 | 루트 `settings.gradle.kts` + build-logic 승격(컨벤션 플러그인 `ssa.java-library`/`ssa.kotlin-library`/`ssa.spring-boot-app`), bootable은 **`:app` 하나**. `:platform`(구 SSA common), `:course`, `:notification`, `:chat:{shared,user,auth,room,core,websocket,ai}`, `:tools:{playground,ngrinder}`, `libs.versions.toml` 단일 카탈로그 | `common` 이름 충돌 해소 + 표본의 "빌드 그래프 = 1차 순수성 방어선" 패턴 |
| 6 | 경계 강제 | ArchUnit freeze 래칫 유지 + Modulith `@ApplicationModule`(초기 OPEN) + ModularityTests 병행. OPEN→CLOSED·패키지 루트 통일은 래칫 백로그 | 이중 표준은 과도기 비용으로 수용 |
| 7 | Kafka | **완전 삭제.** 아웃박스(EventRecorder→EventProducerListener)는 유지, `EventProducer` 구현만 ApplicationEventPublisher 인프로세스로 교체. NotificationKafkaConsumer→AFTER_COMMIT `@Async` 리스너(전략 5개 재사용). 최종 Modulith Event Publication Registry로 재발행 표준화 | 단일 JVM에서 브로커는 순수 오버헤드. 메모리 5.6G→3.5G |
| 8 | 통신 규약 | 컨텍스트 간 단방향 도메인 이벤트(과거형 네이밍, `aggregateId="Type:id"`). notification은 "아무도 import하지 않는" 격리 모듈(이벤트 유입만) | linkareer outbox 규약 + GC websocket 격리 패턴 결합 |
| 9 | DB | 단일 Supabase PG + 당분간 스키마 3개(search_path 브리지). **Flyway를 Stage 0 선도입**(ddl-auto:update 폐기 경로) | 스키마 1개 통합은 마지막 선택 단계 |
| 10 | 순서 원칙 | 빌드 통합 → **Kafka 제거(SSA JVM 통합과 동일 단계)** → Boot 4.1 상향(독립 격리) → GeekChat 흡수 → 정체성 통합 | 프레임워크 업그레이드 리스크와 앱 통합 리스크를 절대 섞지 않음 |

## 단계 (모든 단계는 빌드+테스트+배포 가능 상태로 종료)

| Stage | 규모 | 범위 | 게이트 |
|---|---|---|---|
| 0 기준선·안전장치 | M | 깨진 테스트 부채 해소(⚠ 승인 게이트), Flyway baseline, 신규 HS512 시크릿 병행 검증, 상호 claim 관용 파서(sub↔memberId), git hooks | 양쪽 `./gradlew test` 그린, 구/신 토큰 검증, 3-이미지 배포 무변경 |
| 1 빌드·CI 통합 | M | build-logic 승격, 루트 kts 전환, 모듈 재배치(:platform 등), 컨벤션 플러그인 3종(모듈별 BOM 3.4/4.1 공존), 카탈로그, CI 1잡 | 단일 `./gradlew build` 그린, 단일 파이프라인 3-이미지 배포, 런타임 무변화 |
| 2 SSA 단일 JVM + Kafka 삭제 | L | course+notification→`:app`(Boot 3.4 유지), 인프로세스 EventProducer, 컨슈머→리스너 전환, DeadLetter 재배선, /internal REST→직접 호출, EmbeddedKafka 테스트 재작성(⚠ 승인 게이트) | 전체 그린+부팅 스모크, 특강→알림 E2E, 재발행 경로 확인, 2-프로세스 배포 |
| 3 Boot 4.1 정렬 | M | SSA 모듈 3.4→4.1, Gradle 통일, Redisson 상향, spring-retry 처리, fasterxml→tools.jackson(2파일), Hibernate 7 회귀 | 전체 그린 + 동일 토폴로지 배포, 직렬화 회귀 없음 |
| 4 GeekChat 흡수 | L | `:app`이 `:chat:*` 포함, context-path 제거→`/chat` prefix 명시, WS `/chat/ws`, HealthController→actuator, Security 이중 체인 선고정, 단일 DataSource+search_path(교차 테이블명 검사) | 전체+Modularity+ArchUnit 그린, SSA 무인증 접근성 회귀 테스트, 1 app+redis+nginx 배포 |
| 5 정체성 통합 | L | Flyway로 `account`(Long id, external_uuid) 생성·백필, 듀얼라이트, 통합 claim JWT, /login 응답 불변 | 레거시 클라이언트 무영향, 양측 로그인·refresh·OAuth E2E, 백필 정합성, 단계별 롤백 가능 |
| 6 래칫·정리 (Phase 3b 게이트) | M | 레거시 파라미터 제거·무토큰 401, RoleInterceptor→@PreAuthorize, SHA-256 강제 재설정, outbox→Modulith Registry 검토, 패키지 루트 통일 | 항목별 독립 배포·롤백 |

## 삭제 대상 (누적)
Kafka 15클래스+컨슈머+멱등 계층+EmbeddedKafka 인프라 · /internal REST·InternalApiKeyFilter · Redis SSE pub/sub 팬아웃 ·
geekchat 독립 빌드/Dockerfile/CI 특례 · HealthController·context-path · (3b 후) 무염 SHA-256·RoleInterceptor·레거시 파라미터.

## 주요 리스크
1. **Security 전역 잠금 사고**: starter-security 유입 순간 SSA 전 엔드포인트 기본 잠김 → Stage 4 전 접근성 회귀 테스트 선고정.
2. Boot 3.4→4.1 메이저 업(Jackson3/Hibernate7) → Kafka 선삭제로 표면 축소 + Stage 3 독립 배포 격리.
3. Kafka 버퍼링 상실 → 아웃박스 유지 + 재발행 스케줄러(기구현 OutboxRelay 활용)를 Stage 2 게이트에 포함.
4. search_path 3스키마 오연결 → 교차 테이블명 사전 검사 + Flyway 명시 관리.
5. 단일 장애점(WS 폭주→course 전파) → Tomcat/async 익스큐터 분리, SSE·WS 재연결 정책.

## 관련
- ADR-0001(멀티모듈), ADR-0002(헥사고날 래칫) — 본 ADR은 0002의 래칫을 유지·확장한다.
- 표본: `/Users/jsh14/Work/linkareer-migration-spring`(공유 ArchUnit 팩토리, 레이어별 공유 모듈, @UseCase, outbox 규약), `/Users/jsh14/Work/geek-chat/geek-chat-server-v2`(순수 도메인, Either, 모듈 격리, ModularityTests).
