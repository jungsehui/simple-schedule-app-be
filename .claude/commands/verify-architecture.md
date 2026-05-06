---
description: 헥사고날 import 방향 위반 검사 (도메인에 JPA/Spring import 등)
allowed-tools: Bash, Grep, Read
---

헥사고날 아키텍처 규칙 위반 여부를 검사한다.

## 검사 항목

1. **도메인 레이어 의존성 체크**
   ```bash
   bash .claude/hooks/check-domain-imports.sh
   ```
   `src/main/kotlin/com/geekchat/server/domain/`의 모든 .kt 파일에서:
   - `import jakarta.persistence.*` → 위반
   - `import org.springframework.*` → 위반
   - `import com.fasterxml.jackson.*` (단순 직렬화 OK일 수 있음 — 케이스별 검토)

2. **adapter/in이 다른 adapter import 안 하는지**:
   ```bash
   grep -r "import com.geekchat.server.adapter.out" src/main/kotlin/com/geekchat/server/adapter/in/
   ```
   결과 있으면 위반 (포트 거치도록 리팩토링).

3. **adapter/out이 다른 adapter import 안 하는지**:
   ```bash
   grep -r "import com.geekchat.server.adapter.in" src/main/kotlin/com/geekchat/server/adapter/out/
   ```

4. **service가 adapter import 안 하는지**:
   ```bash
   grep -r "import com.geekchat.server.adapter" src/main/kotlin/com/geekchat/server/application/service/
   ```
   결과 있으면 위반 (포트 인터페이스만 사용해야).

5. **port 인터페이스가 adapter import 안 하는지**:
   ```bash
   grep -r "import com.geekchat.server.adapter" src/main/kotlin/com/geekchat/server/application/port/
   ```

## 결과 해석

- 모두 통과: 좋음. 헥사고날 의존성 방향 OK.
- 위반 있음: 리포트하고 리팩토링 제안.

## 향후

ArchUnit 테스트로 컴파일 시간에 자동 검증하도록 마이그레이션 권장:
- 의존성: `testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")`
- 위치: `src/test/kotlin/.../architecture/HexagonalArchitectureTest.kt`
- 테스트가 `./gradlew test`에 포함되어 매번 검증됨.
