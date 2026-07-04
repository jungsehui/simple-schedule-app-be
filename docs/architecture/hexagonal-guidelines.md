# 헥사고날 아키텍처 규약 (SSA)

> 근거: [ADR-0002](../adr/0002-hexagonal-architecture-adoption.md). 집행: 각 모듈 `HexagonalRulesTest`(ArchUnit, CI 상시).
> 신규 코드는 이 규약을 따라야 하며, 위반은 ArchUnit이 차단한다. 기존 부채는 `src/test/archunit-violations/`에 동결되어 있다.

## 1. 목표 패키지 구조 (바운디드 컨텍스트 단위)

```
{context}/                        예: lecture/general, fcm, sse
├── domain/                       # 순수 자바 — 엔티티·VO·도메인 서비스·도메인 예외
│   └── (repository 포트)         # Stage 2부터: 순수 인터페이스 (Spring Data 아님)
├── application/
│   ├── port/
│   │   ├── in/                   # 유스케이스 인터페이스 (XxxUseCase) — presentation이 호출
│   │   └── out/                  # 아웃바운드 포트 (XxxPort) — 외부세계 추상화
│   ├── command/                  # 유스케이스 입력 모델 (기존 관례 유지)
│   └── XxxService                # 유스케이스 구현: port.in 구현, port.out 호출
├── presentation/                 # 인바운드 어댑터 — REST 컨트롤러, request/response DTO
└── infrastructure/               # 아웃바운드 어댑터 — JPA·Kafka·Redis·Firebase·HTTP 구현
```

## 2. 계층별 허용 규칙

| 계층 | 허용 | 금지 |
|---|---|---|
| `domain` | 순수 자바(JDK), 같은 컨텍스트 domain, common 순수 타입, Lombok(한시 허용) | **Spring·JPA·Kafka·Firebase·Redisson·jjwt·Jackson**, application/presentation/config 참조 |
| `application` | domain, 자신의 port, common 순수 타입, `@Service`·`@Transactional`(한시 허용) | **presentation 참조**, 외부 기술 SDK 직접 사용(포트 경유) |
| `presentation` | application(port.in), 요청/응답 DTO 소유, Spring Web | 도메인 엔티티를 응답으로 직접 노출(DTO 변환 필수) |
| `infrastructure` | 모든 외부 기술, application의 port.out 구현, domain 참조 | 비즈니스 규칙 보유(위임만) |

## 3. 포트 규약

- **명명**: 인바운드 `XxxUseCase`, 아웃바운드 `XxxPort`. 어댑터는 기술명 접두 `FirebasePushSender`, `CourseHttpClient`처럼 "무엇으로 구현했는지"가 드러나게.
- **포트는 도메인 언어로 말한다**: 시그니처에 외부 기술 타입(`Message`, `ApiFuture`, `ConsumerRecord`, `ResponseEntity`) 금지. 비동기가 필요하면 `CompletableFuture`(JDK)로.
- **포트 남용 금지**: 포트는 **외부 기술 경계에만** 둔다. 같은 프로세스 내 application→domain 호출에 포트를 끼우지 않는다(과설계).
- **소유권**: port 인터페이스와 그 입출력 타입은 application(또는 domain)이 소유한다. 어댑터의 DTO가 포트 시그니처에 새어 나오면 안 된다.

## 4. 래칫 운영 (FreezingArchRule)

1. 아키텍처를 개선하면 관련 freeze 스토어 라인이 줄어든다 → **갱신된 스토어를 같은 커밋에 포함**한다.
2. 스토어에 라인이 **늘어나는** 변경은 금지 — 불가피하면 `ssa-architecture-guardian` 리뷰와 함께 사유를 커밋 메시지에 명시.
3. 진행도 확인: `wc -l */src/test/archunit-violations/*` (라인 수 = 남은 부채).
4. 스토어 기준선(2026-07-04 설치 시점): course 177 / common 54 / notification 38 (레이어 방향 위반은 course·notification 모두 0 — 이미 준수).

## 5. 리포지토리 포트화 패턴 (Stage 2)

도메인 패키지의 `interface XxxRepository extends JpaRepository`는 Spring Data(외부 기술)가
도메인에 침투한 형태다. 아래 3분할로 분리한다 (본보기: `member`):

```
{context}/domain/XxxRepository                 # 포트: 순수 인터페이스 (save/findXxx + getXxx 도메인예외 규약)
{context}/infrastructure/persistence/
    ├── XxxJpaRepository extends JpaRepository  # Spring Data (findByXxx 파생쿼리)
    └── XxxRepositoryAdapter implements XxxRepository  # @Repository, JPA에 위임
```

- application/domain은 **포트에만** 의존 — 사용처 코드 변경 없음(같은 메서드 시그니처).
- `getById`/`getByUsername`처럼 "없으면 도메인 예외" 규약은 포트의 `default` 메서드로(순수 자바).
- 애그리거트마다 반복. 남은 대상: course 도메인 리포지토리 10개 + `MemberRegister<T, R extends JpaRepository>`
  제네릭 베이스(student/tutor/parent 등록이 공유 — 별도 소단위로 진행).

## 6. 본보기 (Stage 1 pilot — notification)

- `EnrolledStudentsPort`(application/port/out) ← `CourseClient`(HTTP 어댑터)가 구현.
  전략(`LectureUpdatedStrategy`)은 포트에만 의존한다 — course 서버 통신 수단이 바뀌어도 전략은 불변.
- `FcmMessageSender`·`FcmApiFutureCallback`은 도메인 패키지에서 `fcm.infrastructure`로 이동 —
  Firebase SDK는 이제 어댑터 계층에만 존재한다.

새 컨텍스트/기능을 만들 때 이 본보기 구조를 복제할 것.
