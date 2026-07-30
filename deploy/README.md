# SSA 배포 가이드

서버: Rocky Linux, vCPU 4 / RAM 8GB, WireGuard VPN 뒤 (`10.64.212.20`), Docker 사용 가능.
전략: GitHub Actions → GHCR 이미지 푸시 → WireGuard 터널 → SSH로 `docker compose pull && up -d`.

## 0. 보안 선행 조치 (배포 전 필수)

> ⚠ 아래 자격증명들은 채팅/저장소에 노출된 이력이 있으므로 **전부 재발급** 후 사용한다.

| 항목 | 상태 | 조치 |
|---|---|---|
| WireGuard 개인키/PSK (개인 노트북용) | 채팅에 노출됨 | 서버 관리자(DokaDev)에게 요청해 **피어 재발급**. CI용 피어는 별도로 신규 발급 |
| SSH 비밀번호 (`1234`) | 채팅에 노출됨 | 비밀번호 로그인 비활성화, **ed25519 키 인증 전환**, `PasswordAuthentication no` |
| JWT 시크릿 (`application-common-local.yml`) | git 히스토리에 커밋됨 | 운영용 신규 발급(`openssl rand -base64 64`), 로컬용도 교체 권장 |
| DB root/1234 | git 히스토리에 커밋됨 | 운영은 `.env`의 앱 전용 계정 + 강력한 비밀번호 |
| FCM 서비스 계정 JSON | 로컬에만 존재(커밋 안 됨, gitignore 처리됨) | Firebase 콘솔에서 키 로테이션 후 서버 `/opt/ssa/secrets/`에만 배치 |

## 1. 서버 최초 세팅 (1회)

```bash
# 배포 전용 디렉토리
sudo mkdir -p /opt/ssa/secrets && sudo chown -R $USER /opt/ssa
chmod 700 /opt/ssa/secrets

# 배포 키 등록 (로컬에서 새 키 발급 후 공개키만 서버로)
ssh-keygen -t ed25519 -f ~/.ssh/ssa_deploy -C "ssa-deploy"
ssh-copy-id -i ~/.ssh/ssa_deploy.pub shjung@10.64.212.20

# sshd 강화 (서버에서)
#   /etc/ssh/sshd_config: PasswordAuthentication no, PermitRootLogin no
sudo systemctl restart sshd

# 방화벽: 80/443만 공개, DB/Redis/Kafka 포트는 외부 차단
sudo firewall-cmd --permanent --add-service=http --add-service=https
sudo firewall-cmd --reload
```

`.env` 구성: `deploy/.env.example`을 `/opt/ssa/.env`로 복사해 값 채움 (`chmod 600`).
FCM JSON: `/opt/ssa/secrets/fcm-service-account.json` 배치 (`chmod 600`).

## 2. GitHub 설정 (1회)

1. **Environments**: `prod` 환경 생성(required reviewers 권장).
2. **Secrets** 등록 — `.github/workflows/deploy.yml` 상단 주석의 목록 참고.
   - WireGuard 피어는 **CI 전용으로 신규 발급** (개인 노트북 피어 재사용 금지, IP 충돌 + 키 공유 문제)
   - `WG_ALLOWED_IPS`는 반드시 서버 서브넷만 (예: `10.64.212.0/24`). `0.0.0.0/0` 금지.
   - `DEPLOY_SSH_KNOWN_HOSTS`: 로컬에서 `ssh-keyscan 10.64.212.20` 결과 등록 (호스트키 고정).

## 3. 배포

- 수동: GitHub Actions → `Deploy` 워크플로 → `Run workflow` (환경 선택)
- 자동화하려면 `deploy.yml`의 `on:`에 `push: tags: [ 'v*' ]` 추가

## 4. 과거 "배포 차단 요소" — 현황 (2026-07-30 재확인)

| 항목 | 상태 |
|---|---|
| FcmConfig가 외부 마운트 키를 못 읽음 | ✅ 해소 — `ResourceLoader`로 교체됨(`FcmConfig.java:29,72`) |
| `application-prod.yml` 미작성 | ✅ 해소 — `app/src/main/resources/application-prod.yml` |
| actuator 미포함 | ✅ 해소 — course/notification `build.gradle:12` |
| `ddl-auto` | ⚠️ **미해소** — 운영은 여전히 `update`. Flyway는 도입됐고 정착 후 `validate`로 전환(prod yml에 TODO) |

## 5. develop → main 릴리스 런북 (⚠️ 다음 릴리스는 대점프)

`main` push는 **즉시 자동 배포**다. 현재 `main`은 develop보다 50커밋 이상 뒤처져 있고, 한 번의 병합으로 아래가 **동시에** 나간다:

- 프로세스 토폴로지 교체: 3-프로세스(`ssa-course` + `ssa-notification` + `ssa-geekchat`) → **2-프로세스**(`ssa-app` + `ssa-geekchat`). CD가 `up -d --remove-orphans`로 올리므로 compose에서 사라진 `ssa-course`/`ssa-notification` 컨테이너는 **자동 제거된다**(`deploy.yml:172`) — 수동 정리 불필요.
- Spring Boot 3.4.3 → **4.1** (Hibernate 7 / Jackson 3)
- ADR-0004 도메인 순수화, ADR-0005 인증 기본 차단
- **Flyway 최초 실행** — V1~V3가 운영에서 한 번도 돈 적이 없다

### 병합 전 (순서대로)

1. **노트북 WireGuard 터널 Deactivate.** CI 러너가 같은 피어 키를 쓰므로 켜둔 채 배포하면 터널 개설 단계에서 실패한다. (근본 해결: CI 전용 피어 신규 발급 — §0)
2. **Supabase 스키마 생성** (SQL Editor에서 1회):
   ```sql
   CREATE SCHEMA IF NOT EXISTS ssa;
   CREATE SCHEMA IF NOT EXISTS geekchat;
   ```
3. **서버 `.env` 갱신** — CD는 `docker-compose.prod.yml`만 `scp`하고 **`.env`는 복사하지 않는다.** `/opt/ssa/.env`에 직접 반영:
   - `SSA_DB_URL` / `SSA_DB_USER` / `SSA_DB_PASSWORD` (세션 풀러 호스트 + `currentSchema=ssa`)
   - `GEEKCHAT_FRONTEND_ORIGIN_PATTERNS` (CORS/WS 다중 origin — 없으면 채팅이 403)
   - `CORS_ALLOWED_ORIGINS` (운영 프론트 도메인)
4. `.env.example`과 실제 `.env`의 키 목록을 대조해 누락이 없는지 확인한다.

### 병합 후 검증

```bash
# 1) 새 토폴로지가 떴는지 (ssa-app + ssa-geekchat + nginx/redis/kafka)
docker compose -f /opt/ssa/docker-compose.prod.yml ps

# 2) Flyway가 실제로 돌았는지 — 이 릴리스의 최대 미검증 지점
docker logs ssa-app 2>&1 | grep -i flyway
#    Supabase SQL Editor: SELECT version, success FROM ssa.flyway_schema_history ORDER BY installed_rank;
#    기대: V1(baseline) + V2 + V3 전부 success

# 3) 인증 게이트 (ADR-0005) — 무토큰은 401이어야 한다
curl -s -o /dev/null -w '%{http_code}\n' https://<host>/lectures/search?keyword=test   # 401 기대
curl -s -o /dev/null -w '%{http_code}\n' -X POST https://<host>/login                   # 400/401 (게이트 통과 = 공개)

# 4) 구 컨테이너가 정말 사라졌는지 확인 (--remove-orphans가 처리하지만 검증한다)
docker ps -a --filter name=ssa-course --filter name=ssa-notification --format '{{.Names}}'
#    빈 출력이 정상. 남아 있으면 포트/네트워크를 물고 있을 수 있으므로 docker rm -f 로 정리
```

3. **서버 `.env`는 CD가 건드리지 않는다** — `scp`는 `docker-compose.prod.yml`과 `nginx/`만 복사한다(`deploy.yml:163`). 위 §병합 전 3의 값들이 반영돼 있지 않으면 컨테이너가 부팅 실패하거나(DB URL 없음) 조용히 오작동한다(CORS 패턴 없음 → 채팅 403).

> **Flyway가 이 릴리스의 최대 리스크다.** V2·V3는 실 PostgreSQL 16으로 두 pre-state(빈 스키마 / 오타 컬럼 존재)를 검증했지만, **Supabase 세션 풀러의 `search_path`는 로컬에서 재현할 수 없다.** prod Flyway 설정에 `schemas`가 없어 `current_schema()`가 JDBC URL의 `currentSchema=ssa`에 의존하는데, 풀러가 다르게 주면 V2·V3는 **성공을 보고하면서 아무것도 하지 않는다.** 그래서 위 2)의 `flyway_schema_history` 확인이 형식적 절차가 아니다 — 적용 여부를 실제 스키마로 교차 확인할 것:
> ```sql
> SELECT column_name FROM information_schema.columns
>  WHERE table_schema='ssa' AND table_name='consultation_attendee';
> -- consultation_id 가 있어야 한다 (consultaition_id 가 남아 있으면 V2가 no-op으로 지나간 것)
> ```

### 롤백

`docker-compose.prod.yml`의 `IMAGE_TAG`를 이전 성공 SHA로 되돌려 `up -d`. 단 **Flyway 마이그레이션은 자동으로 되돌아가지 않는다** — V2는 컬럼 rename이므로 구버전 앱(오타 컬럼 기대)과 맞지 않는다. 구버전으로 되돌리려면 rename을 수동 역적용해야 한다:
```sql
ALTER TABLE ssa.consultation_attendee RENAME COLUMN consultation_id TO consultaition_id;
```

## 6. 운영 점검

```bash
docker compose -f /opt/ssa/docker-compose.prod.yml ps
docker logs -f ssa-app --tail 100          # 단일 JVM (구 3-프로세스에서는 ssa-course)
curl -s localhost/lectures/search?keyword=a -o /dev/null -w '%{http_code}\n'   # nginx 경유 — 무토큰이면 401
```

메모리 예산(8GB, 2-프로세스 기준): app 2G + geekchat 1G + Kafka 1G + ZK 0.5G + Redis 0.4G + nginx 0.13G ≈ 5G. DB는 외부 관리형(Supabase)이라 서버 메모리를 쓰지 않는다 — 구 3-프로세스 + MySQL×2 구성 대비 여유가 늘었다.
