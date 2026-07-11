---
description: 새 REST 엔드포인트를 헥사고날 전체 스택에 걸쳐 스캐폴딩
argument-hint: <METHOD> <PATH> <description>
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
---

새 REST 엔드포인트를 추가한다. 인자로 받은 정보:
- METHOD: GET / POST / PATCH / DELETE
- PATH: `/api/...`
- description: 무엇을 하는 엔드포인트인지

## 절차 (모든 단계 수행 + 빌드 + 테스트 통과까지)

1. **DTO 정의** — `adapter/in/web/dto/<Domain>Dto.kt`에 Request/Response 데이터 클래스 추가. Bean Validation (`@field:NotBlank`, `@field:Pattern`) 적용.

2. **포트 인터페이스** (필요 시) — `application/port/out/<Domain>Repository.kt`에 메서드 추가.

3. **Spring Data + 어댑터** (필요 시) — `adapter/out/persistence/repository/SpringData<Domain>Repository.kt` + `adapter/out/persistence/adapter/<Domain>RepositoryAdapter.kt`에 구현 추가.

4. **서비스 메서드** — `application/service/<Domain>Service.kt`에 메서드 추가.
   - `Either<ChatError, T>` 반환
   - `@Transactional` 명시
   - 검증 → 본 작업 → 이벤트 발행 (필요 시) 순서

5. **컨트롤러 메서드** — `adapter/in/web/<Domain>Controller.kt`에 메서드 추가.
   - `@AuthenticationPrincipal userId: String` (인증 필요 시)
   - `service(...).fold(onLeft = { it.toResponseEntity() }, onRight = { ResponseEntity.ok(...) })`

6. **에러 매핑** — 새 ChatError 타입 생성 시 `ChatErrorMapping.kt`의 sealed when에 추가 (sealed class 컴파일러가 강제).

7. **단위 테스트** — `src/test/.../application/service/<Domain>ServiceTest.kt`에 성공 / 실패 케이스 각 1개 이상.

8. **통합 테스트** — `src/test/.../integration/<Domain>IntegrationTest.kt`에 MockMvc 기반 테스트.

9. **문서 업데이트** — `docs/API.md`에 엔드포인트 추가 (method, path, auth, request, response, error codes, curl 예시).

10. **빌드 검증** — `./gradlew test` 통과 확인.

## 컨벤션 참조
- 에러 처리: `Either<ChatError, T>` (예외 던지지 X)
- 절대 규칙 + 코드 패턴: `.claude/RULES.md`
