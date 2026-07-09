# 보안 노트 (SSA)

이번 작업에서 적용한 보안 조치와, **코드 변경만으로는 안전하게 자동 적용할 수 없어 남겨둔 항목**을 정리한다.
상세 근거는 [.planning/codebase/CONCERNS.md](../.planning/codebase/CONCERNS.md) 참고.

## 이번에 적용한 것

| 항목 | 조치 | 위치 |
|---|---|---|
| 시크릿 외부화 | 운영 값(DB/JWT/Redis/CORS)을 환경변수 주입으로 전환, `.env`는 커밋 금지 | `application-prod.yml`, `deploy/.env.example` |
| `/internal/**` 다층 방어 | nginx 403(외부) + 애플리케이션 공유 시크릿 헤더(내부망) | `InternalApiKeyFilter`, `ClientConfig`, nginx |
| FCM 키 취급 | 이미지/저장소 미포함, 서버 마운트 + `@Profile("!test")`로 테스트 분리 | `FcmConfig`, compose |
| nginx 하드닝 | 보안 헤더, `server_tokens off`, rate limit | `deploy/nginx/` |

## 반드시 해야 하는 시크릿 로테이션 (노출 이력 있음)

전부 이미 노출되었으므로 **폐기 후 재발급**한다. git 히스토리·채팅에서 제거해도 유효기간 내엔 위험하다.

1. **JWT 시크릿** — `application-common-local.yml`에 커밋됨. `openssl rand -base64 64`로 재발급, 운영은 env 주입.
2. **DB 자격증명** — `root/1234`가 커밋됨. 운영은 앱 전용 계정 + 강한 비밀번호(`.env`).
3. **WireGuard 개인키/PSK** — 채팅 노출. CI용 피어는 **신규 발급**(개인 노트북 피어 재사용 금지).
4. **SSH 비밀번호(1234)** — 채팅 노출. 키 인증 전환 + `PasswordAuthentication no`.
5. **FCM 서비스 계정 키** — 커밋되진 않았으나(gitignore) 로테이션 권장.

## ⚠ 미적용: 엔드포인트 인증 (Critical, 별도 조율 필요)

### 문제
`@Auth`/`AuthArgumentResolver`/`TokenService` 인증 장치는 구현돼 있으나 **어떤 컨트롤러에도 연결되지 않았다.** 모든 API가 신원을 요청 파라미터로 받는다:
- `@RequestParam Long tutorId` — `LectureController`, `SpecialLectureController`
- `@RequestParam Long studentId` — `StudentController`, `SpecialLectureEnrollmentController`
- `@PathVariable Long memberId` — `SseController`(누구나 타인 스트림 구독), `FcmController`

즉 인가가 전혀 없어 임의 사용자가 타인 자원을 조회·조작할 수 있다.

### 왜 이번에 자동 적용하지 않았나
`@Auth`를 붙이고 신원 파라미터를 제거하면 **프론트엔드 API 계약이 깨진다.** 현재 요청이 JWT를 싣는지 불명(`/login`·JWT 인프라는 존재하나 다른 요청의 토큰 전송 여부 미확인)하고, `cors.allowed-origins`에 프론트(`localhost:3000`)가 있어 조율 없이 강제하면 앱 전체가 즉시 동작 불능이 된다. 로컬에서 검증할 수단도 없다.

### 권장 마이그레이션 (단계적, 무중단)
1. **프론트 토큰 확인/정비**: 로그인 후 모든 요청에 `Authorization: Bearer` 전송하도록 프론트 합의.
2. **리졸버 연결**: 컨트롤러의 `@RequestParam Long tutorId` 등을 `@Auth Long memberId`로 교체(신원은 토큰에서만).
3. **인가 검사**: application 계층에서 소유권 검증(이 회원이 이 자원의 주인인가) 추가.
4. **회귀 테스트**: 신원 불일치 요청이 401/403으로 거부되는지 검증하는 테스트 추가.
5. **점진 적용**: 민감·변경 엔드포인트(SSE 구독, 수강 수락/거절, FCM 토큰)부터 순차 전환.

이 작업은 프론트엔드와의 조율이 선행되어야 하므로 백엔드 단독 커밋으로 진행하지 않았다.

## 기타 후속(운영 안전)
- `ddl-auto`를 운영에서 `validate`로 낮추고 Flyway 도입(스키마 버저닝). 현재 유니크 제약이 JPA 애노테이션에만 존재.
- 입력 검증(`@Valid`)이 3개 컨트롤러에만 있음 — 나머지에 Bean Validation 확대.
