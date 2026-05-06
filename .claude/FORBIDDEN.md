# FORBIDDEN — 절대 하면 안 되는 것들

이 규칙들은 **자동 hook + 코드 리뷰**로 강제된다. 위반 시 작업을 즉시 중단하고 우회 방법(아래)을 따른다.

## 🚫 1. 도메인 레이어에 JPA / Spring import 금지

### 위반 예시
```kotlin
// ❌ src/main/kotlin/com/geekchat/server/domain/model/User.kt
import jakarta.persistence.Entity   // 절대 금지
import org.springframework.stereotype.Component  // 절대 금지
```

### 이유
- 헥사고날 아키텍처의 핵심: 도메인은 프레임워크/인프라를 모른다
- JPA가 들어오면 도메인 모델이 영속 라이프사이클에 묶임 → 테스트 어려워지고 hexagonal 의미 사라짐

### 대안
- JPA 엔티티는 `adapter/out/persistence/entity/` 에 별도 정의
- `toDomain()` / `fromDomain()` 매핑으로 변환

### 자동 차단
`PreToolUse:Edit/Write` hook이 `.claude/hooks/check-domain-imports.sh`로 검사.

---

## 🚫 2. v1 NestJS 코드 수정 금지

### 경로
- `~/Work/geek-chat/geek-chat-server/` (NestJS 백엔드 v1)
- `~/Work/geek-chat/geek-chat-web/` (Expo Web 프론트 v1)

### 이유
- v1은 운영 중 또는 참조 전용. v2 마이그레이션 기준 코드.
- v1을 건드리면 두 버전이 동기화 안 되어 디버깅 지옥.

### 대안
- v1 로직 참조는 OK (read-only). OAuth 흐름은 v1 `auth.controller.ts`이 v2 포팅 기준.
- 변경이 정말 필요하면: 별도 작업으로 분리, 사용자 명시 승인 필요.

### 자동 차단
`PreToolUse:Edit/Write` hook이 path 검사.

---

## 🚫 3. 테스트 없이 커밋 금지

### 위반 시그널
- 새 도메인 메서드 / 서비스 메서드 / 엔드포인트 추가했는데 테스트 없음
- `./gradlew test` 실패한 상태로 commit

### 이유
- 91 → 105 tests로 성장한 안전망. 한 번 깨지면 회복 비용 큼.
- 테스트가 곧 스펙 (CLAUDE.md 정책).

### 대안
- 단위 테스트: MockK 패턴 (`AuthServiceTest.kt` 참조)
- 통합 테스트: `@SpringBootTest @ActiveProfiles("test", "dev")` (`AuthIntegrationTest.kt` 참조)
- round-trip 매핑 테스트: 도메인↔JPA 검증 (`RoundTripMappingTest.kt` 참조)

### 자동 차단
- `Stop` hook (`./gradlew test`) 실패 시 사용자에게 알림. 그래도 commit하려면 명시적 우회.

---

## 🚫 4. 서비스 레이어에서 예외 던지기 금지

### 위반 예시
```kotlin
@Service
class FooService {
    fun doX(): Foo {
        if (badInput) throw IllegalArgumentException("...")  // ❌
    }
}
```

### 이유
- 모든 서비스 메서드는 `Either<ChatError, T>` 반환 (ROP 패턴)
- 컨트롤러에서 `fold`로 분기 → ResponseEntity로 변환

### 대안
```kotlin
fun doX(): Either<ChatError, Foo> {
    if (badInput) return Either.Left(ChatError.InvalidUsername())
    // ...
    return Either.Right(foo)
}
```

새 에러 타입은 `/add-chat-error` 슬래시 커맨드로 추가.

---

## 🚫 5. 위험한 bash 명령 금지

| 명령 | 이유 |
|---|---|
| `rm -rf /` 또는 `rm -rf ~` | 시스템 파괴 |
| `git push --force` (main/master) | 협업 히스토리 파괴 |
| `git reset --hard` (확인 없이) | 로컬 변경사항 사라짐 |
| `./gradlew --no-verify ...` | hook 우회 |
| `docker compose down -v` (확인 없이) | DB 데이터 삭제 |

### 자동 차단
`PreToolUse:Bash` hook이 패턴 검사.

### 우회
정말 필요하면 사용자에게 물어보고 명시적 승인 받기.

---

## 🚫 6. 시크릿 코드/문서/로그에 하드코딩 금지

| 항목 | 어디에 |
|---|---|
| `JWT_SECRET` | `.env.docker` (gitignored) 또는 호스트 env var |
| OAuth client_secret | 동상 |
| DB 비밀번호 | 동상 |
| 메시지 content | 로그에 출력 시 `// TODO: Remove content from log before production` 주석 필수 |

### 이유
- git history는 영구. 한 번 들어가면 `git filter-branch`로도 완전 제거 어려움.
- 로그는 프로덕션에서 외부 로그 시스템(Datadog, CloudWatch 등)으로 흘러갈 수 있음.

### 자동 차단
- 코드 리뷰 (PreCommit hook 추가 검토)
- `.gitignore`에 `.env*` 등록되어 있음

---

## 🚫 7. 빌드 깨진 채로 commit/push 금지

```bash
# ❌
./gradlew test  # FAILED
git commit -m "WIP"   # 깨진 상태로 commit

# ✅
./gradlew test  # PASSED
git commit -m "feat: ..."
```

### 자동 차단
`Stop` hook이 `./gradlew test` 실행. 실패 시 사용자에게 표시.

---

## 위반 발견 시 절차

1. **즉시 멈춘다**. 더 이상 변경하지 않는다.
2. 위반 사항을 명확히 보고: 어느 파일, 어느 줄, 어떤 규칙.
3. 위반 원인 분석: 잘못된 패턴인지, 우회가 필요한지, 정말 예외 케이스인지.
4. 우회가 정말 필요하면 사용자에게 명시적으로 승인 요청.
5. 승인 후에만 진행.

## 예외 인정 케이스

이런 경우는 위 규칙에서 제외:
- v1 코드 read (Read tool로 참조만): OK
- 도메인 모델에 `kotlin.*`, `java.*` import: OK (표준 라이브러리)
- 테스트 코드에 JPA/Spring import: OK (테스트는 통합 가능)
- `infrastructure/` 레이어에 모든 import: OK (인프라는 자유)
