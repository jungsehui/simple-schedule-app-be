# ADR-0003 Stage 5 검증 — 브리프 §2 충돌 대조 (중간 보고)

> 오너 승인 방향: 백지 설계가 아니라 **승인된 설계(ADR-0003 Stage 5)를 브리프 제약과 대조해 검증**.
> 이 문서는 그 대조의 1차 결과다. 근거는 전부 `origin/develop` 소스와 오너 DB 실측이다.

## 결론 먼저

**브리프가 "구조 이슈로 생존"이라 분류한 4개 중 3개는 이미 ADR-0003에서 결정돼 있다.**
진짜로 열려 있는 것은 **역할 모델(§2-4)** 과 **username 유니크 정책** 둘이다.

| 브리프 충돌 | ADR-0003의 답 | 상태 |
|---|---|---|
| §2-1 식별자 Long vs UUID | **결정 2** — Long(member_id 승계) + `account.external_uuid` | ✅ 결정됨. FK 검증 통과(아래) |
| §2-2 username 정규식 | (ADR 미언급) | 🟡 현재 데이터 0건. 제약은 생존 |
| §2-3 비밀번호 필수/선택 | **결정 4** — DelegatingPasswordEncoder(`{sha256}`) + 로그인 시 BCrypt(12) lazy re-hash | ✅ 결정됨. **이미 구현·동작 중**(실측 bcrypt 1 / legacy 6) |
| §2-4 역할 모델 | **결정 3**이 claims를 `sub`+`memberId`+`role`+`admin`으로 정함 (부분) | 🔴 **미해결** — 아래 |
| §2-5 인증 스택 이원화 | **결정 3** — Spring Security(GC) 채택. 전환기 `securityMatcher("/chat/**")` 체인 + SSA permitAll 체인 + RoleInterceptor 존치 → Phase 3b에 `@PreAuthorize` 수렴 | ✅ 결정됨 |
| §2-6 시크릿 2개 | **결정 3** — JWT HS512 **단일 신규 시크릿** | ✅ 결정됨 |
| §2-7 스키마 분리 | **결정 9** — geekchat는 `geekchat` 스키마 **유지** | ✅ 결정됨 |
| 신규: username 충돌 3쌍 | (해당 없음) | 🔴 **미해결** |

**즉 브리프는 이미 답이 있는 질문을 여러 개 다시 물었다.** 브리프가 나쁜 게 아니라, 브리프를 쓴 시점에 ADR-0003 Stage 5를 안 본 것이다. 검증의 첫 성과가 이것이다 — 설계 범위가 절반 이하로 줄었다.

## §2-1 검증 — "양측 FK 이관 0건"은 성립한다. 다만 그 의미를 정확히 읽어야 한다

ADR 결정 2의 근거가 *"이중 키면 양측 FK 이관 0건"*이다. 실측으로 확인했다.

**geekchat에서 `UserJpaEntity`를 FK로 참조하는 엔티티 5개** (`origin/develop`):

| 엔티티 | FK 컬럼 |
|---|---|
| `RefreshTokenJpaEntity` | `user_id` |
| `ChatRoomMemberJpaEntity` | `user_id` |
| `UserProviderJpaEntity` | `user_id` |
| `MessageJpaEntity` | **`sender_id`** |
| `InviteLinkJpaEntity` | **`creator_id`** |

> **검색 패턴 주의.** `user_id|userId`만으로 찾으면 3개만 걸리고 `sender_id`·`creator_id` 둘을 놓친다.
> 오케스트레이터의 1차 목록이 그 3개였고, `sender|creator|author|owner`를 더해서야 5개가 나왔다.
> 이 저장소의 렌즈 그대로다 — **검사가 무언가를 잡는 것과 잡으려는 것을 잡는 것은 다르다.**

**판정: 성립한다.** 5개 FK가 전부 `users.id`(varchar UUID)를 가리키는데, 이중 키 설계에서 `users` 테이블은 **그대로 남고** `account.external_uuid`가 그것을 참조로 잇는다. SSA 쪽 FK도 `member.member_id`(Long)를 그대로 가리킨다. 양쪽 어느 FK도 안 건드린다.

**다만 그 대가를 명시해야 한다.** "FK 이관 0건"이 성립하는 이유는 **두 사용자 테이블이 모두 살아남기 때문**이다. 즉 ADR Stage 5는 데이터를 *합치는* 설계가 아니라 **정체성 해석을 잇는** 설계다.

그리고 이게 **브리프 §5 권고와 같은 설계다** — 브리프는 *"1단계는 합치기가 아니라 잇기를 검토하라"*를 새 제안으로 썼는데, ADR이 이미 그 결론이다. 두 문서가 독립적으로 같은 답에 도달한 셈이라 그 방향의 신뢰도가 높다.

## 🔴 미해결 1 — 역할 모델 (§2-4)

결정 3이 claims를 `sub`+`memberId`+`role`+`admin`으로 정했다. `admin`이 **별도 boolean claim**인 것이 힌트다 — geekchat의 ADMIN을 role 값이 아니라 플래그로 다루겠다는 뜻으로 읽힌다.

**그런데 ADR이 명시적으로 답하지 않은 것이 남는다:**

- SSA는 `Member`가 **abstract + JOINED 상속**이고 Student/Tutor/Parent가 **타입**이다. geekchat `User`는 플랫 `data class`다
- 통합 후 geekchat 사용자는 **어느 서브타입에도 안 맞는다.** STUDENT도 TUTOR도 PARENT도 아니다
- `role` claim에 무엇이 들어가는가? geekchat 사용자에게 `role: "USER"`가 들어가면 SSA의 `Role.valueOf("USER")`가 **`IllegalArgumentException` → 포괄 handleException → 500**이다

**이건 설계가 답해야 할 진짜 질문이고, ADR에 답이 없다.**

### 파생 — 1단계 완료 조건의 순서 제약

`extractRole()`의 500 위험은 **시크릿을 통일하는 순간 발화한다.** 그 전에는 서명 검증에서 먼저 끊겨 이 경로에 도달조차 안 한다.

> **따라서 "타 시스템 토큰이 500이 아니라 의도된 4xx로 거부되는가"라는 테스트는
> 시크릿 통일과 반드시 같은 커밋에 들어가야 한다.** 그 전에 쓰면 통과하지만 아무것도 검증하지 않는다.

판별 질문 형식으로: *"내가 막으려는 바로 그 사고에서 이 검사가 실패하는가?"* — 시크릿 통일 전에 쓴 테스트는 **아니오**다.

## 🔴 미해결 2 — username 유니크 정책

데이터 3쌍 정리는 오너 확인 1회면 끝난다(같은 사람이면 병합, 아니면 개명). **설계 질문은 그게 아니라 통합 스키마의 제약이다:**

- `account`에 username 유니크를 거는가? 그럼 geekchat의 nullable username은 어떻게 되는가(실측: `username: NULLABLE`, `nickname: NOT NULL`)
- 두 정규식 중 무엇을 통합 규칙으로 삼는가? 좁은 쪽(geekchat)을 택하면 기존 SSA 계정이 불합격하고, 넓은 쪽(SSA)을 택하면 geekchat 기존 값이 불합격한다
- 실측상 현재 7/7 양방향 호환이지만 **그건 전 계정이 오너 테스트 계정이기 때문**이다. 실사용자 유입 즉시 부활한다

## ADR 근거 재확인 — 오히려 강해졌다

결정 2가 Long을 택한 근거가 *"Phase 3a 계약(JWT memberId, SSE 경로)에 Long이 박제"*다.

**그 근거는 설계 시점보다 지금 더 강하다.** 이후 3b-partial로 가면서 `GET /me/lectures`·`GET /me/schedules` 같은 **토큰 전용 조회가 새로 생겼고**, 이들은 경로 변수 없이 토큰의 `memberId`로만 주체를 정한다. 즉 `memberId: Long`에 묶인 표면이 넓어졌다.

설계 재확인이 근거를 약화시키는 게 아니라 강화한 경우다.

## 다음 단계

1. **역할 모델 결정** — 브레인스토밍 대상. 옵션과 트레이드오프를 펼쳐야 한다
2. **username 유니크 정책 결정** — 위 세 질문
3. 그 둘이 정해지면 `writing-plans`로 Stage 5 실행 계획

**착수 전 확인 필요:** ADR-0003 결정 3이 "Spring Security 채택 + RoleInterceptor 존치"인데, 그 사이 ADR-0005(기본 차단 게이트)가 들어왔다. 두 결정이 어떻게 만나는지는 아직 안 봤다 — 다음 검증 항목이다.

---

# 검증 2차 — ADR-0003 결정 3 vs ADR-0005 정합 (2026-08-26)

## 실측

| 확인 | 결과 |
|---|---|
| SSA(`app`/`course`/`notification`/`common`)에 Spring Security 의존 | **0건** — 현재 SSA는 Spring Security를 쓰지 않는다 |
| geekchat 모듈의 Spring Security 의존 | 6개 모듈 전부 |
| ADR-0003 결정 3 날짜 | 2026-07-11 (결정 9는 07-13 확정) |
| ADR-0005 날짜 | **2026-07-30 — 더 나중이고, 이미 구현·배포됐다** |

즉 결정 3의 "Spring Security 채택"은 **SSA에 대해 아직 미구현이고 Stage 5의 미래 상태**다. ADR-0005는 그 사이에 들어와 **인터셉터 기반 기본 차단**으로 구현됐다.

## 판정 — 전환기는 정합한다. 종착점이 충돌한다

### 전환기: 문제없다

결정 3의 *"SSA permitAll 체인 + RoleInterceptor 존치"*는 **Security 레벨에서 통과시키고 실제 게이트는 인터셉터가 잡는다**는 뜻으로 읽히고, 그건 ADR-0005와 공존한다. Spring Security 필터 체인이 `permitAll`이어도 그 뒤 `HandlerInterceptor`는 그대로 돈다.

**다만 결정 3의 전환기 서술은 한 군데 낡았다.** ADR-0005가 신설한 `AuthenticationInterceptor`(기본 차단 게이트)를 모른다 — 결정 3이 쓰인 시점에 없던 클래스다. 실제 게이트는 이제 **인터셉터 두 개이고 순서가 계약**이다(`AuthConfig`: 인증 → 인가). Stage 5 계획서는 `RoleInterceptor`가 아니라 **이 둘**을 다뤄야 한다.

### 🔴 종착점: 충돌한다

결정 3의 종착점이 *"Phase 3b에 `@PreAuthorize` 수렴"*이다. **`@PreAuthorize`는 메서드에 붙이는 옵트인이다** — 안 붙은 엔드포인트는 제한되지 않는다.

**그건 fail-open이고, ADR-0005는 정확히 그 fail-open 때문에 존재한다.** ADR-0005 배경에 실측이 있다: *"12개 컨트롤러 중 7개에 `@RequireRole`이 없다. 그중 `GET /lectures/{lectureId}/enrollments`는 수강생 명단이 무인증으로 노출된다."*

`@RequireRole`이 옵트인이라 7개가 방치됐는데, `@PreAuthorize`로 갈아타면 **같은 구조를 이름만 바꿔 되풀이한다.** ADR-0005 결정 1의 문장이 그대로 적용된다: *"기본을 차단으로 뒤집으면 그 실수가 구조적으로 불가능해진다."*

## 해소 방법은 있다 — 명시가 필요할 뿐

Spring Security로 가면서도 기본 차단을 유지할 수 있다:

```
http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/login", "/students", "/tutors", "/parents").permitAll()
        .requestMatchers("/internal/**").permitAll()   // InternalApiKeyFilter가 담당
        .anyRequest().authenticated())                 // ← 기본 차단
```

`anyRequest().authenticated()`가 ADR-0005의 화이트리스트와 **같은 성질**이다. `@PreAuthorize`는 그 위에서 **역할·소유권만** 다루면 되고, 인증 게이트를 대체하지 않는다.

**따라서 결정 3의 "SSA permitAll 체인"은 전환기 서술로만 유효하고, 종착 상태가 되면 안 된다.**

## Stage 5 계획서에 넣을 제약

1. Spring Security 도입 시 SSA 체인은 **`anyRequest().authenticated()`**로 끝나야 한다. `permitAll`은 전환기 한정이다
2. 공개 엔드포인트 목록은 ADR-0005의 화이트리스트와 **한 곳에서** 관리해야 한다. 두 곳(`@PublicEndpoint`와 Security 설정)에 나뉘면 어긋난다 — 이 저장소가 반복해서 겪은 형태다
3. `@PreAuthorize` 수렴은 **인가(역할·소유권)에 한정**한다. 인증 게이트를 옮기는 것이 아니다
4. 인터셉터 제거 시점에 **ADR-0005의 회귀 테스트가 살아 있어야 한다** — `AuthGateIntegrationTest`가 "보호 엔드포인트를 토큰 없이 부르면 401"을 고정하는데, 게이트 구현을 바꿔도 이 테스트는 통과해야 한다. 통과하지 않으면 기본 차단이 깨진 것이다

> 판별 질문 적용: *"인터셉터를 Security로 갈아탄 뒤, 새 엔드포인트에 애노테이션을 빠뜨렸을 때 이 검사가 실패하는가?"*
> `@PreAuthorize`만으로는 **아니오**다. `anyRequest().authenticated()`가 있어야 **예**가 된다.

## ADR 처리

이건 ADR-0003을 **개정할 사안**이다(결정 3의 종착점 서술). 다만 Stage 5 계획서에서 제약으로 명시하고, 실제 구현 시 ADR-0003에 정정 항목을 추가하는 편이 순서상 맞다 — 지금 고치면 아직 안 한 설계를 미리 확정하는 셈이다.
