# 후속 부채 일괄 상환

## Context

PR #48~#52로 notification Clean Architecture·오류 처리·웹 갭이 끝났고, 그 과정에서 **고치지 않고 근거와 함께 미룬 항목들**이 남았다. 이 계획은 그것들을 갚는다.

**실측 근거** (`origin/develop` = `e0420f9`, 2026-08-14 확인):

| 항목 | 실측 |
|---|---|
| `NotificationKafkaConsumer.toEventType` | `switch (type)` 앞에 null 가드 없음 → null `type`이면 NPE |
| enum 매핑 검증 | `LectureEventType` ↔ `NotificationEventType` 크기 대조 테스트 없음 (검출이 단방향) |
| `NotificationStrategy.supports()` | 프로덕션 호출자 **0**. 유일한 호출자가 `AcceptEnrollmentStrategyTest:39-40` |
| 옛 디렉터리 테스트 | 3개 — `notification/schedule/NotificationRetrySchedulerFailureTest`, `sse/cache/RedisClientManagerTest`, `sse/event/RedisSseMessagePublisherTest` |
| `archunit.properties` | 3모듈 전부 `allowStoreCreation=true` |
| `app` 모듈 | 테스트가 `SsaApplicationContextTest` 하나, ArchUnit 스토어·규칙 **없음** |
| `course` `HexagonalRulesTest` | 규칙 3개뿐 — `classes_should_live_in_a_layer`도 `application → infrastructure`도 없음 |
| notification freeze | `application → infrastructure` **5건**, 전부 `FcmService` → FCM 인프라 |

## Global Constraints

1. **동작 보존이 기본.** **이 계획에는 프로덕션 동작을 바꾸는 Task가 하나도 없다.** 전부 테스트 추가·이동·규칙 추가·문서 수정이다. 동작을 바꾸는 작업(FCM 비동기 재설계, 대기 목록 조회 신설)은 각각 `fcm-async-redesign.md`, `pending-enrollment-listing.md`로 분리했다 — 되돌릴 일이 생겼을 때 여섯 개 커밋 묶음에서 원인을 가려내지 않기 위해서다.
2. **테스트는 스펙이다.** 기존 테스트의 assert를 약화시키지 마라. 협력자 API가 바뀌어 테스트가 따라가는 것(리팩터 종속 적응)만 허용한다.
3. **ArchUnit 규칙 텍스트를 건드리면 스토어 UUID가 새로 생기고 옛 기준선이 고아가 된다.** 규칙을 수정할 때는 `stored.rules` 전후 diff를 증거로 남기고, 리셋된 기준선 값을 보고하라. 기존 규칙의 `.because(...)` 문자열을 무심코 바꾸지 마라.
4. **freeze 규칙을 추가하면 반드시 반증하라.** 기준선에 없는 새 위반을 주입해 빨간불을 확인하고 원복한다. 모든 것을 동결해 통과하는 규칙과 실제로 동작하는 규칙은 겉보기가 같다.
5. **각 Task는 독립 revert 가능해야 한다.** Task 경계에서 빌드가 green이어야 한다.
6. 빌드 검증은 `./gradlew :common:build :course:build :notification:build :app:build`.
7. 작업 디렉터리는 `/Users/jsh14/Work/simple-schedule-app/simple-schedule-app-be/.wt-ssa-analysis` (worktree). 사용자 기본 체크아웃 `ssa/`가 아니다.

---

## Task 1: Kafka 어댑터 null 가드 + enum 매핑 양방향 검출

**목표:** null `type` 페이로드가 NPE 대신 도메인 예외로 실패하게 하고, enum 매핑이 한쪽으로만 검출되던 구멍을 막는다.

**파일:** `notification/src/main/java/com/example/simplescheduleapp/notification/infrastructure/NotificationKafkaConsumer.java` + 테스트

**하는 일:**

1. `toEventType`의 **switch 앞에** null 가드를 넣는다. `type == null`이면 `ApplicationException(NotificationTypeExceptionCode.NOTIFICATION_TYPE_NOT_FOUND)`.
2. `LectureEventType` ↔ `NotificationEventType` **크기 대조 테스트**를 추가한다 (`assertThat(NotificationEventType.values()).hasSameSizeAs(LectureEventType.values())`).

**⚠ 절대 하지 말 것:** `switch`에 `default -> throw`를 추가하지 마라. `default`가 없다는 것이 `NotificationEventType` 재선언의 **유일한 근거**다 — `common`에 상수가 추가되면 런타임이 아니라 컴파일 오류로 먼저 드러나는 성질이 사라진다. 가드는 반드시 switch **바깥 앞쪽**에 둔다.

**왜 크기 대조인가:** 현재 검출은 단방향이다. `common.kafka.LectureEventType`에 상수를 추가하면 switch가 컴파일 오류를 낸다. 그런데 `NotificationEventType`에만 추가하면 **아무 신호도 없다**. 크기 대조 한 줄이 반대 방향을 막는다.

**완료 조건:**
- null `type` 페이로드가 `ApplicationException`으로 실패하는 테스트가 있고, 가드를 제거하면 그 테스트가 빨간불이 되는 것을 확인(반증)
- 크기 대조 테스트가 있고, `NotificationEventType`에 상수를 하나 임시 추가하면 빨간불이 되는 것을 확인 후 원복(반증)
- switch에 `default` 절이 **없음**을 확인해 보고
- 4모듈 빌드 green

---

## Task 2: `supports()` 죽은 코드 제거

**목표:** 프로덕션이 아무도 부르지 않는 메서드를 걷어낸다.

**파일:** `notification/.../application/strategy/NotificationStrategy.java` + `NotificationStrategyFactory` + 전략 테스트

**실측:** `supports()`의 유일한 호출자는 `AcceptEnrollmentStrategyTest:39-40`이다. 팩토리는 `getSupportType()`으로 고른다.

**하는 일:**
1. `NotificationStrategy.supports()` 기본 메서드를 제거한다.
2. `AcceptEnrollmentStrategyTest`에서 `supports()`를 검증하던 부분을 **`getSupportType()` 검증으로 바꾼다** — 검증 의도(이 전략이 어떤 이벤트를 담당하는가)는 유지하고 호출 대상만 바꾼다. 검증을 삭제하지 마라.

**먼저 확인할 것:** 제거 전에 `grep -rn "supports(" notification/src`로 호출자가 정말 그 테스트뿐인지 다시 확인하라. 하나라도 프로덕션 호출자가 있으면 **BLOCKED로 보고**하고 제거하지 마라.

**완료 조건:** 4모듈 빌드 green. 전략 담당 이벤트를 검증하는 테스트가 여전히 존재.

---

## Task 3: 테스트 디렉터리 정합

**목표:** 프로덕션이 이동한 뒤 옛 디렉터리에 남은 테스트 3개를 대응 위치로 옮긴다.

| 현재 | 이동 후 |
|---|---|
| `notification/src/test/.../notification/schedule/NotificationRetrySchedulerFailureTest.java` | `notification/src/test/.../notification/infrastructure/NotificationRetrySchedulerFailureTest.java` |
| `notification/src/test/.../sse/cache/RedisClientManagerTest.java` | `notification/src/test/.../sse/infrastructure/redis/RedisClientManagerTest.java` |
| `notification/src/test/.../sse/event/RedisSseMessagePublisherTest.java` | `notification/src/test/.../sse/infrastructure/redis/RedisSseMessagePublisherTest.java` |

**하는 일:** `git mv` + `package` 선언 수정. **테스트 본문(assert)은 한 줄도 바꾸지 마라.** import가 깨지면 그것만 고친다. 이동 후 빈 디렉터리는 정리한다.

**완료 조건:** 4모듈 빌드 green. `git diff -M` 검토 시 rename + package 선언 외 변경이 없어야 한다. 이동한 테스트 3개가 실제로 실행되었다는 증거(테스트 결과 XML의 tests 수)를 보고하라 — package와 디렉터리가 어긋나면 조용히 실행에서 빠질 수 있다.

---

## Task 4: 래칫 실효성 — 스토어 리셋 방지 + course/app 규칙 확대

**목표:** 규칙이 조용히 무력화되는 두 경로를 막는다.

### 4-1. 스토어 리셋을 **실제로 잡는** 검사 추가

**실측:** 3모듈 전부 `freeze.store.default.allowStoreCreation=true`. 이 설정 때문에 **규칙 텍스트(`.because` 문자열 포함)만 바꿔도 새 UUID 스토어가 생기고 옛 기준선이 고아가 되며 CI는 초록**이다.

`false`로 끄면 새 규칙을 추가할 때마다 수동 개입이 필요해지므로 끄지 않는다. 대신 **증상을 잡는 검사**를 넣는다:

> `archunit-violations/`의 파일 수(`stored.rules` 제외)와 `stored.rules`의 엔트리 수가 **일치**해야 한다.

고아 스토어가 생기는 순간 이 검사가 실패한다. 고아 발생이 곧 기준선 리셋의 증상이므로, 이것이 실제로 발화하는 유일한 자동 방어다. (Task 3 리뷰에서 사람이 손으로 하던 대조를 자동화하는 것이다.)

**어디에 둘까:** 세 모듈 각각의 `HexagonalRulesTest` 옆에 두는 것이 자연스럽다. 다만 **세 모듈에 같은 코드를 세 번 복사하지는 마라** — `common`의 testFixtures에 헬퍼를 두고 각 모듈이 한 줄로 호출하는 편이 낫다. 판단해서 근거를 보고하라.

**주의:** `stored.rules`는 `UUID=규칙텍스트` 형태의 properties 파일이다. 파싱 방식을 먼저 실물로 확인하고 쓰라(줄 수 세기는 규칙 텍스트에 개행이 있으면 틀린다).

**추가로:** 세 `archunit.properties`에 위험을 설명하는 주석을 남긴다. **주석은 방지가 아니라 문서화다** — 위 검사가 실제 방어이고, 주석은 왜 실패했는지 다음 사람이 읽을 자료다.

**반증 필수:** 스토어 파일을 하나 임시로 추가(또는 `stored.rules`에서 한 줄 제거)해 검사가 **실제로 빨간불**이 되는지 확인하고 원복하라.

### 4-2. `course`에 레이어·방향 규칙 추가

`course/src/test/.../architecture/HexagonalRulesTest.java`에 **기존 3개 규칙은 텍스트를 포함해 그대로 두고** 두 규칙을 추가한다.

**⚠ 컴파일 함정:** 이 파일은 현재 `noClasses`만 static import 하고 있다(8행). `classes()`를 쓰려면 **import를 함께 추가**해야 한다:

```java
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
```

추가할 규칙:

```java
@ArchTest
static final ArchRule classes_should_live_in_a_layer = freeze(
        classes().that().resideOutsideOfPackages(/* 아래 지침대로 결정 */)
                .should().resideInAnyPackage("..domain..", "..application..", "..infrastructure..", "..presentation..", "..config..")
                .because("레이어 밖 클래스는 의존성 규칙 검사를 통째로 우회한다"));

@ArchTest
static final ArchRule application_should_not_depend_on_infrastructure = freeze(
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("유스케이스는 포트에만 의존한다 — 구현 교체가 코어를 흔들면 안 된다"));
```

**제외 목록을 추측으로 채우지 마라.** 먼저 `course`에서 레이어 밖 클래스를 **실제로 열거**하고, 그 결과를 보고 제외 목록을 쓴다. notification은 `..architecture..`, `..exception..` 둘을 제외했다 — course도 같은지 확인하라. `@AnalyzeClasses`에 이미 `DoNotIncludeTests`가 걸려 있으므로 테스트 전용 패키지는 애초에 대상이 아닐 수 있다. **투기적 제외는 편의가 아니라 규칙에 뚫은 구멍이다.**

**기준선을 손으로 검산하라.** 나온 숫자가 그럴듯해 보인다고 그냥 동결하지 마라 — 열거한 클래스 수와 기준선 줄 수가 대응하는지 대조하고 보고하라. 기준선이 예상보다 **작게** 나오면 부채가 적은 게 아니라 **규칙 텍스트가 잘못된 것**이다. 잘못 동결된 규칙은 규칙이 없는 것보다 나쁘다.

**위반을 고치지 마라.** 이 Task의 목적은 숫자를 박는 것이지 갚는 것이 아니다. 기준선 숫자를 커밋 메시지와 보고서에 기록하라.

### 4-3. `app` 모듈 ArchUnit 커버리지

**실측:** `app/src/test`에 `SsaApplicationContextTest` 하나뿐, ArchUnit 없음. `app`은 `course`+`notification`을 합쳐 부트하는 조립 모듈이다.

**먼저 판단하라:** `app`에 자체 규칙을 두는 것이 값을 하는가? `app`의 프로덕션 코드가 실질적으로 부트 클래스와 설정뿐이라면 규칙은 공허하다. `app/src/main`의 실제 클래스를 세어 보고 **값을 못 한다고 판단되면 규칙을 만들지 말고 그 근거를 보고하라.** 과설계를 피하는 것이 규칙을 늘리는 것보다 중요하다.

**완료 조건:** 4모듈 빌드 green. 추가한 규칙 각각에 대해 **반증** 수행(기준선에 없는 위반 주입 → 빨간불 → 원복). `stored.rules` 전후 diff와 고아 스토어 유무를 보고.

---

## 검증 (모든 Task 공통)

```bash
./gradlew :common:build :course:build :notification:build :app:build
```

freeze 잔여:
```bash
for m in common course notification; do
  echo -n "$m: "
  find $m/src/test/archunit-violations -type f ! -name stored.rules -exec cat {} \; 2>/dev/null | grep -c .
done
```

---

## Task 5: 문서 드리프트 정리 — `:8081` / 3-프로세스 서술

**목표:** 코드는 2026-08-02에 단일 JVM으로 바뀌었는데 문서가 옛 토폴로지를 서술하고 있다. **이걸 근거로 삼는 AI 세션이 오판한다** (안드로이드 세션이 실제로 제보했다).

**실측 대상** (`8081` 문자열 기준):

| 파일 | 건수 |
|---|---|
| `.planning/codebase/ARCHITECTURE.md` | 3 |
| `.planning/codebase/STRUCTURE.md` | 3 |
| `.planning/codebase/STACK.md` | 2 |
| `.planning/codebase/INTEGRATIONS.md` | 2 |
| `.claude/agents/ssa-code-analyzer.md` | 1 |
| `docs/adr/0001-multi-module-server-separation.md` | 1 |

**하는 일:**

- `.planning/codebase/*`와 `.claude/agents/ssa-code-analyzer.md`는 **현재 사실로 고친다**: 프로덕션은 `app` 단일 JVM(8080), `8081`은 존재하지 않음, nginx가 단일 오리진으로 경로 라우팅.
- **`docs/adr/0001`은 고치지 마라.** ADR은 그 시점의 결정 기록이라 사후 편집 대상이 아니다. 대신 상단에 "이 결정은 ADR-0003이 대체했다"는 취지의 **Superseded 표기**를 한 줄 추가한다. 기존 본문은 그대로 둔다.
- 이번 브랜치에서 바뀐 패키지 경로(`sse/cache` → `sse/infrastructure` 등)가 문서에 남아 있으면 함께 고친다. `.planning/codebase/STRUCTURE.md`는 특히 규칙이 실패시킬 경로를 지시하고 있어 우선순위가 높다.

**하지 말 것:** 문서를 새로 쓰지 마라. 틀린 사실만 고친다. 범위가 커지면 무엇을 남겼는지 보고하라.

**완료 조건:** `grep -rn "8081" --include='*.md' .planning .claude`가 0건(ADR 제외). 코드 변경 0줄. 고친 문장마다 근거가 현재 코드에 있어야 한다 — 추측으로 채우지 마라.
