# .claude/ — AI 세션 진입점

새 Claude/Codex/Gemini 세션은 이 순서로 읽는다:

1. **`../CLAUDE.md`** — 프로젝트 개요 (기술 스택, 디렉토리 맵, 도메인 용어)
2. **`./RULES.md`** — 절대 금지 규칙 + 코드 패턴
3. 작업 명확하면 곧장 `./commands/`의 슬래시 커맨드 사용

## 파일

| 경로 | 역할 |
|---|---|
| `settings.json` | hooks (PreToolUse + Stop). Claude Code 런타임이 자동 적용 |
| `RULES.md` | 절대 규칙 + 코드 패턴 + 헥사고날 import 방향 + 커밋 컨벤션 |
| `commands/add-rest-endpoint.md` | REST API 신규 추가 (전체 스택 스캐폴딩) |
| `commands/add-domain-model.md` | 도메인 모델 + JPA + round-trip 테스트 |
| `commands/deploy-check.md` | 배포 전 사전 점검 |
| `hooks/check-domain-imports.sh` | `domain/`에 JPA/Spring import 차단 + v1 코드 수정 차단 |
| `hooks/check-bash-safety.sh` | `rm -rf /`, `git push --force` 등 위험 명령 차단 |

## 자동 차단 (settings.json)

| 트리거 | 차단 조건 |
|---|---|
| `PreToolUse:Bash` | rm -rf 시스템 / git push --force main / git reset --hard / --no-verify / docker compose down -v |
| `PreToolUse:Edit\|Write\|MultiEdit` | domain/에 JPA/Spring import / v1 디렉토리(`~/Work/geek-chat/geek-chat-server`) 수정 |
| `Stop` | `./gradlew test` 자동 실행 |

비활성화: `OMC_SKIP_HOOKS=PreToolUse,Stop` 또는 `DISABLE_OMC=true` env.

## 자세한 운영 절차

- 빌드/실행/테스트: `../CLAUDE.md` §7
- API 카탈로그: `../docs/API.md`
- WebSocket 이벤트: `../docs/WEBSOCKET.md`
- 배포 런북: `../docs/DEPLOYMENT.md`
