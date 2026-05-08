---
description: 모든 md 문서를 최신 코드 상태와 동기화하고 HANDOFF.md를 갱신해 새 세션에서 작업 이어가기 가능하게 만듦
argument-hint: [--check-only]
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
---

세션 종료 직전 또는 컨텍스트 압축 직전에 호출. 다음을 보장한다:

1. 코드와 문서가 일치 (drift 없음)
2. HANDOFF.md가 진행 중인 작업/완료/미완료를 명시
3. 새 Claude 세션이 5분 안에 컨텍스트 잡고 작업 이어갈 수 있음

## 절차

### 1. 코드 ↔ 문서 drift 검사

다음 파일들이 일관되는지 grep으로 검사:

```bash
# 도메인 모델 필드 vs DATABASE.md 컬럼
diff <(grep -E '^\s+val [a-z]' src/main/kotlin/com/geekchat/server/domain/model/Message.kt) \
     <(grep -A 5 'CREATE TABLE message' docs/DATABASE.md)

# REST 엔드포인트 vs API.md
grep -rE '@(Get|Post|Patch|Delete)Mapping' src/main/kotlin/.../web/ | wc -l
grep -E '^### [0-9]+\.[0-9]+ \[' docs/API.md | wc -l

# WS 이벤트 vs WEBSOCKET.md
grep -E '"type" to "[a-z_]+"' src/main/kotlin/.../websocket/ChatEventListener.kt
grep -E '^### 4\.[0-9]+ ' docs/WEBSOCKET.md
```

drift 발견 → 사용자에게 어느 쪽이 정답인지 물어보고 다른 쪽 갱신.

### 2. 문서 갱신 대상 식별

이번 세션에 변경된 파일들의 도메인 영역 매핑:

| 변경된 코드 | 갱신할 문서 |
|---|---|
| `domain/model/*.kt` 필드 추가/제거 | `docs/DATABASE.md` 컬럼, `CLAUDE.md` 도메인 용어집 |
| `domain/event/ChatEvent.kt` 이벤트 추가 | `docs/WEBSOCKET.md` outbound 이벤트 |
| `adapter/in/web/*Controller.kt` 엔드포인트 추가/수정 | `docs/API.md` |
| `adapter/in/websocket/ChatWebSocketHandler.kt` inbound 추가 | `docs/WEBSOCKET.md` 3절 |
| `domain/error/ChatError.kt` 새 에러 | `docs/API.md` HTTP 매핑 표 |
| `application/service/*.kt` 비즈니스 로직 변경 | `docs/ARCHITECTURE.md` 데이터플로우 (필요 시) |
| `build.gradle.kts` 의존성 추가 | `CLAUDE.md` 기술 스택 표 |
| `Dockerfile`, `docker-compose.yml` | `docs/DEPLOYMENT.md` |

### 3. HANDOFF.md 갱신 (필수)

`HANDOFF.md`는 워크스페이스 루트(`~/Work/geek-chat/HANDOFF.md`)에 둔다.
다음 섹션을 매번 갱신:

```markdown
# HANDOFF — 다음 세션 진입점

마지막 갱신: <YYYY-MM-DD HH:MM>
세션 ID: <git short-sha or N/A>

## 1. 프로젝트 상태 한 줄
> 한 줄로 현재 상황 요약.

## 2. 마지막 commit
- `<sha>` <subject>
- ...

## 3. 미커밋/진행 중
- 어떤 파일/기능이 작업 중인지
- "왜 멈췄는지" (시간/대기/막힘 등)

## 4. 다음 세션 우선순위 Top 3
1. ...
2. ...
3. ...

## 5. 알려진 문제 / Watchpoint
- ...

## 6. 5분 컨텍스트 잡기 순서
1. 이 파일
2. server-v2/CLAUDE.md
3. server-v2/.claude/RULES.md
4. server-v2/docs/PRD-M2.md (남은 M2 기능)
5. server-v2/docs/API.md (현재 엔드포인트)

## 7. 자주 막히는 곳
- ...
```

### 4. 검증

- [ ] `git status` — 변경사항 확인
- [ ] `./gradlew test` — 전체 통과
- [ ] `grep -r "TODO" docs/ CLAUDE.md HANDOFF.md` — 작성 미완료 표시 없음
- [ ] HANDOFF.md 마지막 갱신 시각 = 오늘
- [ ] 변경된 코드 파일 수 == 갱신된 문서 영역 수

### 5. commit

`docs: refresh context — sync md files + HANDOFF.md` 단일 commit.
또는 `chore: refresh context (HANDOFF + docs)`.

## --check-only 모드

`--check-only` 플래그가 있으면 검증만 하고 수정 안 함. drift 발견 시 exit 1.
사용처: Stop hook에서 자동 호출 (사용자가 commit 전 갱신했는지 검사).

## 자동화 (선택)

`.claude/settings.json`의 Stop hook에 추가:
```json
{
  "Stop": [
    { "command": "./gradlew test" },
    {
      "command": "bash $CLAUDE_PROJECT_DIR/.claude/hooks/check-stale-docs.sh",
      "blocking": false
    }
  ]
}
```

`check-stale-docs.sh`는 git diff로 코드 변경 vs docs 변경 비율을 보고
문서가 누락되어 보이면 stderr로 경고만 출력 (block X).

## 사용 예

```
/refresh-context
```
→ 모든 md 갱신 + HANDOFF.md 갱신 + commit 후보 표시.

```
/refresh-context --check-only
```
→ drift 검사만. 갱신 필요 항목 리스트 출력.
