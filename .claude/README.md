# .claude/ — AI 세션 컨트롤 센터

이 폴더는 Claude Code (그리고 Codex/Gemini) 세션이 GeekChat v2 프로젝트에서 작업할 때 참조하는 모든 메타 자산을 담는다.

## 새 세션이 처음 봐야 할 순서

1. **`/Users/jsh14/Work/geek-chat/geek-chat-server-v2/CLAUDE.md`** ← 프로젝트 전반 (기술 스택, 디렉토리 맵, 도메인 용어). 이게 메인 진입점.
2. **`.claude/FORBIDDEN.md`** ← **절대 하면 안 되는 것**들. 모든 변경 전 확인 필수.
3. **`.claude/CONVENTIONS.md`** ← 코드 스타일, 테스트 패턴, 커밋 규칙.
4. 작업이 명확하면 곧장 `.claude/commands/`의 슬래시 커맨드 사용.

## 파일/폴더 책임

| 경로 | 역할 |
|---|---|
| `settings.json` | hooks 정의 (Stop, PreToolUse, PostToolUse). Claude Code 런타임이 자동 적용 |
| `settings.local.json` | 개인 머신 전용 설정 (gitignore). 강한 차단/추가 hook 등 |
| `README.md` | 이 파일 |
| `FORBIDDEN.md` | 위반 시 즉시 멈춰야 할 절대 규칙 + 이유 + 대안 |
| `CONVENTIONS.md` | "이렇게 해라" — 코드/테스트/커밋 패턴 |
| `commands/` | 프로젝트 슬래시 커맨드 (`/add-rest-endpoint` 등). frontmatter + body |
| `hooks/` | shell 스크립트 (settings.json hook이 호출). 실패 시 exit 1로 차단 |
| `prompts/` | 자주 쓰는 프롬프트 템플릿 (스캐폴딩, 리뷰 등) |

## 슬래시 커맨드 빠른 참조

| 커맨드 | 용도 |
|---|---|
| `/add-rest-endpoint` | REST API 신규 추가 (도메인→포트→서비스→컨트롤러→DTO→테스트) |
| `/add-domain-model` | 새 도메인 모델 + JPA 엔티티 + round-trip 테스트 |
| `/add-chat-error` | ChatError sealed class에 새 에러 타입 + HTTP 매핑 추가 |
| `/add-ws-event` | WebSocket 이벤트 + 도메인 이벤트 + listener 추가 |
| `/verify-architecture` | 헥사고날 import 방향 위반 검사 |
| `/run-test-watch` | gradle test --continuous로 watch 모드 |
| `/deploy-check` | 배포 전 사전 점검 (테스트 + 환경변수 + Dockerfile) |

## 훅(hooks) 동작

| 훅 | 트리거 | 차단 조건 |
|---|---|---|
| `PreToolUse:Edit/Write` | 파일 수정 직전 | (1) `domain/` 경로에 JPA/Spring import 시도 (2) v1 코드(`~/Work/geek-chat/geek-chat-server/`) 수정 시도 |
| `PreToolUse:Bash` | bash 실행 직전 | `rm -rf /`, `git push --force`, `git reset --hard` 같은 위험 명령 |
| `Stop` | 세션 종료 직전 | `./gradlew test` — 실패 시 사용자에게 알림 |

훅을 일시 비활성화하려면 환경변수 `OMC_SKIP_HOOKS=PreToolUse,Stop` 또는 `DISABLE_OMC=true` 설정.

## 우선순위 (이 폴더 만들 때 가장 큰 ROI 3가지)

1. **`FORBIDDEN.md` + PreToolUse hook으로 도메인 import 차단** — 한 번 망가지면 ripple effect 큰 부분
2. **`commands/add-*` 5개** — 90% 작업이 이 패턴 반복이라 시간 절약 큼
3. **`Stop hook의 ./gradlew test`** — 실패한 채로 세션 종료 방지 (이미 있음)

나머지는 시간 나면 추가.
