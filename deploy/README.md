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

## 4. 남은 코드 변경 (배포 차단 요소)

1. **FcmConfig 리팩토링 필요**: 현재 `ClassPathResource`로 FCM JSON을 읽어서 외부 마운트 파일을 못 읽는다.
   `ResourceLoader`(`file:` 접두사 지원)로 교체해야 `FCM_KEY_JSON=/app/config/...` 주입이 동작한다.
2. **application-prod.yml 작성**: course/notification 모두 비어 있음 — env 기반 설정 필요 (이 브랜치에서 작성됨).
3. **ddl-auto**: 운영은 `validate` + 마이그레이션 도구(Flyway) 도입 권장.
4. `/actuator/health` 헬스체크를 쓰려면 모듈에 `spring-boot-starter-actuator` 추가 필요.

## 5. 운영 점검

```bash
docker compose -f /opt/ssa/docker-compose.prod.yml ps
docker logs -f ssa-course --tail 100
curl -s localhost/lectures -o /dev/null -w '%{http_code}\n'   # nginx 경유 확인
```

메모리 예산(8GB): course 1.5G + notification 1G + MySQL×2 2G + Kafka 1G + ZK 0.5G + Redis 0.4G + nginx 0.1G ≈ 6.5G (OS 여유 ~1.5G).
