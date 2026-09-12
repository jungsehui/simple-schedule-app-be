# 특강 계약 정정과 취소 기능 — 실행 계획

**Goal:** 특강 신청의 에러 계약을 바로잡고(만석·종료·내부 노출), 취소 경로를 만든다.

**Tech Stack:** Spring Boot 4.1 / Java 21 / Gradle 멀티모듈 / Redis(Redisson) / PostgreSQL(운영)·H2(테스트)

**Base:** `3c4fe5a` (develop = main, 2026-09-04 릴리스 직후)

---

## 무엇을 고치는가

전부 실측으로 확정한 것이다(`origin/main` 소스 대조).

| | 문제 | 근거 |
|---|---|---|
| **A** | 신청 경로에 **시간 검증이 0건**이다. 끝난 특강도 신청을 받는다 | `RedisSpecialLectureEnrollmentService`, `LockedSpecialLectureEnroller`에 시간 비교 없음. `SpecialLectureService`의 시간 사용은 **생성 시** 강사 일정 충돌 검사뿐 |
| **B** | 만석이 **400**이다. 409여야 한다 | `LectureExceptionCode:14` `CAPACITY_EXCEEDED(BAD_REQUEST, "L4", ...)`. 400은 "입력을 고쳐 재시도"라는 **틀린 행동 지시**다 |
| **C** | 특강이 **일반 강의 네임스페이스 코드**를 반환한다 | `SpecialLectureRedisClient:67`이 `LectureExceptionCode.CAPACITY_EXCEEDED`를 던진다 |
| **E** | 사용자 메시지에 **내부 인프라명이 노출**된다 | `SpecialLectureExceptionCode:11` `"Redis에서 특별 강의 정원 정보를 찾을 수 없습니다."` |
| **D** | **취소 경로가 없다**. 오신청이 영구고 좌석도 반환 불가 | 특강 매핑 3개(생성·목록·신청), `@DeleteMapping` 0건. 일반 강의는 `DELETE /lectures/{id}/enrollments` 있음 |

**기각된 주장(재조사 금지):** "만석도 `SLE001`/500이 나간다"는 보고가 있었으나 **거짓**이다. 만석은 `SpecialLectureRedisClient:67`에서 `L4`로 던져지고, 그 호출은 `RedisSpecialLectureEnrollmentService:62`로 **`try` 블록 바깥**이라 `mapToApplicationException`을 거치지 않는다. B는 "코드 부재"가 아니라 **"있는 코드의 상태 코드 오류"**다.

---

## A의 검증 위치 — 성능 설계와 부딪히는 지점

**결론은 아래 "채택" 절에 있다.** 세 안을 먼저 적는 이유는 기각된 안의 대가를 남겨 두기 위해서다.

현재 신청 흐름은 이렇다.

```
RedisSpecialLectureEnrollmentService:62   Redis DECR   ← try 블록 바깥, DB 접근 전
:64~76                                     try { DB 저장 } catch { Redis 보상 }
```

Redis를 먼저 두는 것이 **1차 방어선의 존재 이유**다. 정원 100명에 1만 명이 몰려도 9,900명은 DB에 닿지 않는다.

시간 검증을 어디 둘지에 따라 결과가 갈린다.

### 안 1 — 서비스 계층(DB 로드 후)에 둔다 · 최소 변경

`SpecialLectureEnrollmentService`는 이미 특강을 로드한다. 거기서 `endTime`을 보고 던지면 기존 catch가 Redis 좌석을 되돌린다. **새 DB 읽기 0건.**

**문제:** 종료 후 1일이 지나면 Redis 키가 만료돼(TTL = `endTime + 1일`) **`:62`에서 `SL002`가 먼저 터진다.** 시간 검증에 도달하지 못한다. 즉 끝난 특강의 응답이 종료 시점에 따라 갈린다.

| 종료 후 | 응답 |
|---|---|
| ~1일 이내 | 새 "종료됨" 코드 (정상) |
| 1일 초과 | `SL002` (엉뚱함) |

### 안 2 — Redis 호출 전에 특강을 로드해 검증한다 · 정확

응답이 항상 일관된다. **대신 모든 신청이 DB를 한 번 읽는다.** PK 조회라 싸지만, 1차 방어선의 설계 목적(DB 부하 99% 감소)을 정면으로 훼손한다.

### 안 3 — `endTime`을 Redis에 함께 저장하고 Lua에서 검사한다 · 권장

`initializeSpecialLecture`가 정원과 함께 `endTime`도 넣고, 감소 스크립트가 시간을 먼저 본다. **DB 부하 0, 응답 일관.** 대신 Lua 스크립트와 `AtomicCounter` 포트 확장이 필요해 작업량이 가장 크다.

> 처음에는 "기존 키에 `endTime`이 없어 재초기화가 필요하다"고 적었으나 **틀렸다.** 아래 키 부재 판별 경로가 레거시 키를 그대로 흡수한다.

### 채택: 안 3 + 키 부재 시 DB 판별 (2026-09-04 확정)

안 1은 우리가 방금 고친 문제와 같은 모양이다. 하나의 코드(`SL002`)가 두 원인("정원 정보 없음" = 버그, "신청 기간 지남" = 정상)을 덮어 클라이언트가 취할 행동이 정해지지 않는다(렌즈 2). 안 2는 이 엔드포인트의 존재 이유를 깎는다.

**안 3이 안전한 이유:** `endTime`이 생성 후 바뀌지 않아 Redis 사본이 DB와 어긋날 경로가 없다. 그리고 Lua가 DECR **전에** 거부하므로 **보상 경로가 아예 생기지 않는다**(안 1은 DECR 후 거부라 보상이 필요하다).

**보강 — `KEY_NOT_FOUND`일 때만 DB로 판별한다.** 안 3만으로는 종료 1일 후 키가 만료돼 "키 없음 = 종료 or 버그"가 다시 겹친다. 키 부재일 때만 특강을 로드해 가른다.

| DB 상태 | 응답 |
|---|---|
| 존재 + 종료됨 | 종료 코드 (정상 상황) |
| 존재 + 미종료 | `SL002` — **진짜 정원 정보 유실이다. 버그이고 알람 대상이다** |
| 부재 | `SLE002` (특강 없음) |

DB 읽기가 **비정상 경로에만** 생기고 핫 패스는 DB 0을 유지한다. 그리고 `SL002`가 단일 원인 코드가 된다.

**기존 키를 재초기화하지 않는다.** 레거시 키(`endTime` 필드 없음)는 위 DB 판별 경로로 흘려보내면 된다. 마이그레이션 절차 자체가 사라진다.

### ⚠️ 안 3의 전제는 지금 지켜지고 있지 않다 — 가드가 필요하다

"`endTime`은 생성 후 불변"이 안 3의 안전성을 떠받치는데, **그건 구조적 보장이 아니라 우연이다.**

```
SpecialLecture.java:56   public void update(Long tutorId, Schedule schedule, int capacity)
SpecialLecture.java:58       updateSchedule(schedule);      ← startTime/endTime을 바꾼다
```

**프로덕션 호출자는 0건이다**(양성 대조: 같은 검색이 일반 강의의 `LectureService:62` 호출자를 잡는다). 하지만 메서드는 살아 있고, **특강 수정 PUT을 붙일 때 가장 먼저 부를 이름**이다. 그 순간 Redis의 `endTime` 사본이 조용히 낡는다. **아무것도 실패하지 않고 신청 가능 기간만 틀려진다.**

`MemberRepository.getByUsername`과 같은 함정(고아가 된 매력적 이름)이다. 삭제는 하지 않는다 — 그건 결함이 아니라 아직 배선되지 않은 기능이다. 대신 **ArchUnit 규칙으로 호출을 막아** 배선하려는 사람이 반드시 Redis 사본 갱신을 같이 처리하게 만든다. 규칙의 `.because(...)`가 그 이유를 전달한다.

위반 0건이 정상이므로 `freeze()`를 쓰지 않는다. **반증 필수**: `SpecialLecture` 외 프로덕션 클래스에 호출을 주입해 규칙이 실패하는지 확인하고 원복한다.

---

## 배치 구성 판단

### A+B+C+E와 D는 **한 브랜치**로 간다

**근거: 클라이언트 통지 횟수.** 넷은 에러 계약을 바꾸고 D는 신규 엔드포인트인데, **D도 클라이언트 작업을 만든다**(취소 버튼 UI). 나누면 통지 2회, 클라이언트 배포 2회다. 지금 웹·안드로이드가 신청 UX를 만드는 중이라 **한 번에 주는 편이 싸다.**

또 D의 설계가 A에 의존한다: **종료된 특강의 취소를 허용할지**가 A의 시간 기준과 같은 축이다. 따로 하면 두 번 결정하게 된다.

### `GET /lectures/{id}`는 **분리해서 먼저** 낸다

`LectureCreateResponse`(쓰기 응답)를 읽기에 재사용해 `enrolledCount`가 없다. `LectureResponse` 전환은 **필드 추가(additive)**다.

**GATE에 묶지 않는 이유:** GATE는 **행동이 역전되는 변경**을 위한 장치다(에러 코드 전환, 검증 규칙 확대). 클라이언트가 서버보다 먼저 반영하면 사고가 나기 때문이다. 응답 필드 추가는 클라이언트가 미지 필드를 무시하므로 **역전이 없다.** 같은 GATE에 묶으면 additive 변경이 불필요하게 지연된다.

**따라서 통지는 총 2회다.** ①`GET /lectures/{id}` 필드 추가(게이트 없음, 즉시) ②특강 배치(GATE 뒤). 이걸 1회로 줄이려면 additive 변경을 GATE까지 붙잡아야 하는데, 그 대가가 이득보다 크다.

---

## File Structure

| 파일 | 책임 | 항목 |
|---|---|---|
| `lecture/special/exception/SpecialLectureEnrollmentExceptionCode.java` | `SLE004`(만석, 409) · `SLE005`(종료됨, 409) 신설 | B·C·A |
| `lecture/special/exception/SpecialLectureExceptionCode.java` | `SL002` 메시지에서 인프라명 제거 | E |
| `lecture/special/application/SpecialLectureRedisClient.java` | 만석 시 특강 전용 코드로 전환 | C |
| `lecture/special/application/SpecialLectureRedisClient.java` | 키 부재 시 DB 판별(종료 / 정원정보 유실 / 특강 없음) | A·E |
| `lecture/special/domain/SpecialLectureEnrollmentRepository.java` | `findBySpecialLectureIdAndStudentId`, **영향 행 수를 반환하는 delete** | D |
| `.../persistence/SpecialLectureEnrollmentJpaRepository.java` + `...Adapter.java` | 위 구현 | D |
| `lecture/special/application/SpecialLectureCancelService.java` (신규) | 취소 유스케이스 | D |
| `lecture/special/presentation/SpecialLectureEnrollmentController.java` | `DELETE /special-lectures/{id}/enrollments` | D |

추가로 안 3이 요구하는 것:

| 파일 | 책임 |
|---|---|
| `common/redis/counter/AtomicCounter.java` + Redisson 어댑터 | `endTime`을 함께 보는 조건부 감소(Lua) |
| `lecture/special/domain/SpecialLectureUpdateRuleTest.java` (신규 테스트) | `SpecialLecture.update` 호출 금지 ArchUnit 가드 |

**약 12~14 파일.** 오너 승인 게이트(3파일 이상) 대상이다.

---

## D의 설계 — 좌석 반환의 멱등성

이 계획에서 가장 틀리기 쉬운 부분이다.

**순서는 DB 먼저, Redis 나중이다.** 뒤집으면 안 된다.

```
1. DELETE ... WHERE special_lecture_id = ? AND student_id = ?   → 영향 행 수를 받는다
2. 영향 행 수가 1일 때만 Redis 좌석 반환
```

- **Redis를 먼저 올리면**: DB 삭제가 실패했을 때 좌석이 늘어나 **초과 판매**가 된다.
- **DB를 먼저 지우면**: Redis 증가가 실패해도 좌석이 하나 덜 팔린다. 안전한 방향이다.

**멱등성은 "영향 행 수"에서 나온다.** 조회 후 삭제(read-then-delete)로는 안 된다. 동시 취소 2건이 같은 행을 읽고 둘 다 좌석을 반환해 **정원을 넘긴다.** 삭제가 반환하는 행 수가 유일한 원자적 판정 근거다.

`compensateSpecialLectureEnrollment`를 **재사용하지 않는다.** 그건 DB 저장 실패 전용이고 상한이 없다(`incrementIfExists`). 취소 경로는 위 행 수 판정이 상한 역할을 한다.

**종료된 특강의 취소 = 허용한다 (2026-09-04 확정).** 좌석을 돌려받을 사람이 없어 실익은 없지만, 막으면 **"취소 못 하는 신청"이 다시 생긴다** — 이 계획이 없애려는 바로 그 상태다. 키가 이미 만료됐으면 좌석 반환을 생략하고 경고 로그만 남긴다(부재 키를 TTL 없이 부활시키지 않는다는 기존 원칙 그대로).

---

## 남기는 것

- **일반 강의 `L4`도 400이다.** 같은 오류지만 다른 계약이라 이 계획에서 건드리지 않는다. 특강 전용 코드를 신설하는 것이 `L4`의 상태 코드를 바꾸지 않고 특강만 정확하게 만드는 방법이다(그래서 C가 B의 부작용을 해소한다).
- **CD 스모크 확장** — 릴리스가 바꾼 동작을 최소 1개 검증하도록. 2026-09-04 릴리스에서 "옛 이미지가 떠 있어도 스모크는 초록"이 재확인됐다.

## 클라이언트 전이 규칙 (통지에 포함)

특강 만석 코드가 신설되면 클라이언트의 현행 `code == "L4"` 분기가 특강에서 구식이 된다. **전이 기간에는 `L4`와 신규 코드를 둘 다 만석으로 매핑**한다. 가산이라 게이트 전 배포에도 안전하고, 서버 배포 후 `L4`는 일반 강의 전용으로 수렴한다.

상태 코드가 아니라 `code` 필드로 분기시켜 둔 것이 여기서 값을 한다. 상태 코드 분기였으면 400 → 409 전이가 클라이언트를 깨뜨렸다.
