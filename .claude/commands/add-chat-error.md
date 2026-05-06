---
description: ChatError sealed class에 새 에러 타입 + HTTP 매핑 추가
argument-hint: <ErrorName> <category> <httpStatus>
allowed-tools: Read, Edit, Bash, Grep
---

새 에러 타입을 ChatError sealed class에 추가한다.

## 절차

1. **`domain/error/ChatError.kt`** 에 새 data class 추가:
   ```kotlin
   data class <ErrorName>(
       val context: String,  // 필요한 컨텍스트
       override val message: String = "..."
   ) : ChatError()
   ```
   - 카테고리(Auth/User/Room/Message/WebSocket/InviteLink/OAuth/Generic)에 맞춰 위치 선정
   - 메시지는 사용자에게 보여줄 텍스트 (한국어 OK, 그러나 sensitive info 노출 X)

2. **`adapter/in/web/dto/ChatErrorMapping.kt`** 의 `when` 절에 매핑 추가:
   ```kotlin
   is ChatError.<ErrorName> -> HttpStatus.<STATUS>
   ```
   - 401: 인증 실패
   - 403: 권한 부족 (인증은 됐는데 권한 없음)
   - 404: 리소스 없음
   - 409: 충돌 (이미 존재)
   - 410: 사라짐 (만료)
   - 400: 잘못된 입력
   - 502: 외부 서비스 실패
   - 500: 내부 오류

3. **컴파일 검증** — sealed class `when`은 모든 분기를 강제하므로 컴파일러가 누락을 잡는다.
   ```bash
   ./gradlew compileKotlin
   ```

4. **단위 테스트 추가** — 해당 서비스에서 새 에러를 반환하는 케이스의 테스트.

5. **문서 업데이트** — `docs/API.md`의 "HTTP Status 매핑" 표에 추가.

## 체크리스트
- [ ] sealed class `when` 컴파일러 통과
- [ ] `./gradlew test` 통과
- [ ] `docs/API.md` 업데이트
