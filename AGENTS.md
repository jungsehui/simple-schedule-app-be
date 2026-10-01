# AGENTS.md

이 저장소의 AI 에이전트 공통 가이드입니다. 상세 내용은 [CLAUDE.md](./CLAUDE.md)를 단일 소스로 사용하세요.

## 핵심 요약

- **스택**: Spring Boot 4.1.0, Java 21, Gradle 멀티모듈 모듈러 모놀리스 (실행 모듈은 `app` 하나, `common`/`course`/`notification`은 라이브러리, `geekchat`은 컴포지트 빌드)
- **빌드·테스트**: `./gradlew buildAll` (CI와 동일)
- **레이어 규칙**: 헥사고날. 의존은 안쪽(`domain`)으로만
- **모듈 간 통신**: Kafka 이벤트(비동기) / `app`의 인프로세스 어댑터(동기). course `/internal/**`는 외부 노출 금지
- **시크릿 금지**: 하드코딩 금지, 운영 값은 환경변수 주입
- **테스트가 스펙**: 테스트 실패 시 프로덕션 코드를 고친다. 테스트 파일 임의 수정 금지.
- **커밋 전 필수**: 위 gradle 빌드·테스트 전체 통과

## 참고 문서

- `.planning/codebase/` — 코드베이스 분석 문서 7종
- `common/CLAUDE.md`, `course/CLAUDE.md`, `notification/CLAUDE.md`, `geekchat/CLAUDE.md` — 모듈별 가이드
- `docs/adr/` — 아키텍처 결정 기록
