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
| 7 | Kafka·Redis | **둘 다 유지 (사용자 결정 2026-07-11).** 단일 JVM `:app`이 Kafka에 발행·구독하는 자기소비 구조 — 브로커 경유의 내구성(재소비·버퍼링)과 기존 멱등/DLQ 계층, 학습 가치를 보존. 앱 프로세스 개수와 브로커 유지 여부는 무관함을 명시. 메모리 절약은 **KRaft 모드 전환**(Zookeeper 제거, −512m)과 힙 상한 축소로 달성. EmbeddedKafka 테스트 재작성 불필요 → Stage 2의 테스트 승인 게이트 소멸 | 초안의 "완전 삭제"는 메모리 최적화 제안이었으나 기각. Modulith Registry 전환은 장기 선택지로만 보존 |
| 8 | 통신 규약 | 컨텍스트 간 단방향 도메인 이벤트(과거형 네이밍, `aggregateId="Type:id"`). notification은 "아무도 import하지 않는" 격리 모듈(이벤트 유입만) | linkareer outbox 규약 + GC websocket 격리 패턴 결합 |
| 9 | DB 스키마 (2026-07-13 확정) | **course+notification → 단일 `ssa` 스키마** (단일 JVM=단일 DataSource=단일 스키마, 아웃박스 원자성이 강제). `domain_event`/`kafka_message_consume_history`/`dead_letter`는 중복이 아니라 **공유 인프라 테이블**(현재는 각 스키마에 빈 shell) → 단일 카피. **프리픽스 불필요**(테이블명 충돌 0건 + Producer/Consumer 역할 비고정 → 이벤트/멱등/DLQ는 공유 인프라). 모듈 경계는 코드(Gradle+ArchUnit+패키지), DB 스키마 아님. **geekchat는 `geekchat` 스키마 유지**(Stage 4 흡수 시 결정). 기존 ssa_course/ssa_notification(pre-Flyway) 폐기. | 단일 스키마, 프리픽스 없음, geekchat만 분리 |
| 10 | 순서 원칙 | 빌드 통합 → **Kafka 제거(SSA JVM 통합과 동일 단계)** → Boot 4.1 상향(독립 격리) → GeekChat 흡수 → 정체성 통합 | 프레임워크 업그레이드 리스크와 앱 통합 리스크를 절대 섞지 않음 |

## 단계 (모든 단계는 빌드+테스트+배포 가능 상태로 종료)

> **진행 상태**: Stage 0 ✅(`1668e41`), Stage 1 ✅(PR #21, `668172d`) — develop 병합·CI green. 다음 = Stage 2.

| Stage | 규모 | 범위 | 게이트 |
|---|---|---|---|
| 0 기준선·안전장치 ✅ | M | 깨진 테스트 부채 해소(NotificationIntegrationTest @MockitoBean 재설계), Flyway baseline(prod), 상호 claim 관용 파서(sub↔memberId), git hooks(pre-push=buildAll) | 3모듈 빌드 green, 테스트 부채 0 — 완료 |
| 1 빌드·CI 통합 ✅ | M | buildSrc 컨벤션 3종(`ssa.java-common/library/spring-boot-app`), `libs.versions.toml`, settings/build→kts, `includeBuild("geekchat")`+`buildAll`, CI 1잡. **모듈명·패키지·산출물 불변**(:platform 재배치는 Stage 4로 연기) | `./gradlew buildAll` green, bootJar 산출물 불변, CI 단일 잡 — 완료 |
| 2 SSA 단일 JVM (Kafka 유지) | L | course+notification→`:app`(Boot 3.4 유지, Kafka 발행·구독 경로 무변경 — 자기소비), /internal REST→직접 호출, Kafka **KRaft 단일노드 전환**(Zookeeper 제거)+힙 상한 축소 | 전체 그린+부팅 스모크(EmbeddedKafka 테스트 그대로 통과), 특강→알림 E2E, 2-프로세스 배포 |
| 3 Boot 4.1 정렬 | M | SSA 모듈 3.4→4.1, Gradle 통일, Redisson 상향, spring-retry 처리, fasterxml→tools.jackson(2파일), Hibernate 7 회귀 | 전체 그린 + 동일 토폴로지 배포, 직렬화 회귀 없음 |
| 4 GeekChat 흡수 | L | `:app`이 `:chat:*` 포함, context-path 제거→`/chat` prefix 명시, WS `/chat/ws`, HealthController→actuator, Security 이중 체인 선고정, 단일 DataSource+search_path(교차 테이블명 검사) | 전체+Modularity+ArchUnit 그린, SSA 무인증 접근성 회귀 테스트, 1 app+redis+nginx 배포 |
| 5 정체성 통합 | L | Flyway로 `account`(Long id, external_uuid) 생성·백필, 듀얼라이트, 통합 claim JWT, /login 응답 불변 | 레거시 클라이언트 무영향, 양측 로그인·refresh·OAuth E2E, 백필 정합성, 단계별 롤백 가능 |
| 6 래칫·정리 (Phase 3b 게이트) | M | 레거시 파라미터 제거·무토큰 401, RoleInterceptor→@PreAuthorize, SHA-256 강제 재설정, outbox→Modulith Registry 검토, 패키지 루트 통일 | 항목별 독립 배포·롤백 |

## 실행 노트 (학습)
- **컴포지트 빌드는 단일 Gradle 버전**을 강제한다. `includeBuild("geekchat")` 하려면 루트 래퍼를 geekchat의 8.14.5로 올려야 했다(Boot 4.1 플러그인이 Gradle 8.14+ 요구). Boot 3.4는 8.14와 호환이라 안전 — Stage 3의 "Gradle 통일"이 Stage 1에서 선반영됨.
- **buildSrc vs build-logic**: geekchat이 `build-logic`(name="build-logic")을 쓰므로, SSA는 이름 충돌을 피해 `buildSrc`를 택함(루트 빌드에만 적용, geekchat 격리).
- `gradle.includedBuild("...")`의 키는 rootProject.name이 아니라 **디렉터리명**(`geekchat`).
- Boot 플러그인이 buildSrc 클래스패스에 오르면 모듈 `plugins{}`에서 **버전 명시 금지**(unknown version 충돌) → playground/ngrinder도 컨벤션화로 해결.

## 삭제 대상 (누적)
Zookeeper(KRaft 전환 시) · /internal REST·InternalApiKeyFilter · geekchat 독립 빌드/Dockerfile/CI 특례 ·
HealthController·context-path · (선택) 단일 JVM에서 잉여가 되는 Redis SSE pub/sub 팬아웃 ·
(3b 후) 무염 SHA-256·RoleInterceptor·레거시 파라미터. **Kafka·Redis 본체와 멱등/DLQ 계층은 유지.**

## 주요 리스크
1. **Security 전역 잠금 사고**: starter-security 유입 순간 SSA 전 엔드포인트 기본 잠김 → Stage 4 전 접근성 회귀 테스트 선고정.
2. Boot 3.4→4.1 메이저 업(Jackson3/Hibernate7) + spring-kafka 4.0 호환 확인 → Stage 3 독립 배포 격리.
3. KRaft 전환 시 브로커 데이터 포맷 변경 → 신규 볼륨으로 시작(토픽은 재생성, 아웃박스가 재발행 보장).
4. search_path 3스키마 오연결 → 교차 테이블명 사전 검사 + Flyway 명시 관리.
5. 단일 장애점(WS 폭주→course 전파) → Tomcat/async 익스큐터 분리, SSE·WS 재연결 정책.

## 관련
- ADR-0001(멀티모듈), ADR-0002(헥사고날 래칫) — 본 ADR은 0002의 래칫을 유지·확장한다.
- 표본: `/Users/jsh14/Work/linkareer-migration-spring`(공유 ArchUnit 팩토리, 레이어별 공유 모듈, @UseCase, outbox 규약), `/Users/jsh14/Work/geek-chat/geek-chat-server-v2`(순수 도메인, Either, 모듈 격리, ModularityTests).
