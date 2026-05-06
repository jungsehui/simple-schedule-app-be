# Deployment Runbook — 친구 클라우드 (10.64.212.20)

## 1. 인프라 개요

| 항목 | 값 |
|---|---|
| Host | `10.64.212.20` (private, VPN 내부) |
| 스펙 | 4 vCPU, 8 GB RAM |
| 외부 노출 포트 | **80, 443만** (다른 포트 차단) |
| OS | Linux (Docker 호환) |
| VPN | WireGuard (`shjung.conf`, peer `dokalab.iptime.org:40773`) |
| SSH | 등록된 공개키로만 (id_rsa.pub) |
| TLS | Let's Encrypt (certbot --webroot) |
| DNS | 친구가 A 레코드 → 호스트 public IP로 가리킴 |

## 2. 처음 한 번만 (Bootstrap)

### 2.1 개발 머신에서: WireGuard 연결

macOS:
- App Store에서 **WireGuard** 설치
- `shjung.conf` 파일 import → "shjung" 터널 활성화
- 확인: `ifconfig | grep utun` → wg 인터페이스 존재
- 호스트 ping 테스트: `ping -c 3 10.64.212.20`

### 2.2 SSH 첫 접속

```bash
# ~/.ssh/config 추가
Host geekchat-host
    HostName 10.64.212.20
    User <YOUR_USERNAME>      # 친구가 만들어준 리눅스 계정
    IdentityFile ~/.ssh/id_rsa
    ServerAliveInterval 30    # WireGuard 끊김 대비
    ServerAliveCountMax 3

# 첫 접속
ssh geekchat-host
```

### 2.3 호스트 환경 준비 (sudo 권한 필요)

```bash
# Docker + Compose v2
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker

# certbot
sudo apt update && sudo apt install -y certbot

# UFW 방화벽 (이미 80/443만 열려 있다면 생략 가능)
sudo ufw allow 22/tcp     # SSH (VPN 내부)
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# 작업 디렉토리
sudo mkdir -p /srv/geekchat /srv/acme
sudo chown -R $USER:$USER /srv/geekchat /srv/acme
```

### 2.4 코드 클론

```bash
cd /srv/geekchat
git clone https://github.com/geek-chat/server-v2.git
cd server-v2
cp .env.docker.example .env.docker
nano .env.docker   # 시크릿 채워넣기
```

`.env.docker`에서 채울 항목:
- `DB_PASSWORD`, `DB_ROOT_PASSWORD`: 강력한 랜덤 (예: `openssl rand -base64 24`)
- `JWT_SECRET`: 32자 이상 (예: `openssl rand -base64 48`)
- `FRONTEND_URL`: `https://<frontend-domain>` (Vercel)
- `OAUTH_CALLBACK_URL`: `https://api.<domain>/auth/callback`
- `GOOGLE_CLIENT_ID`/`SECRET`, `NAVER_CLIENT_ID`/`SECRET`: provider 콘솔에서 발급

### 2.5 첫 인증서 발급

DNS A 레코드가 호스트 public IP로 propagation된 후:

```bash
# 임시로 nginx 없이 80 포트로 standalone challenge
sudo certbot certonly --standalone -d api.<domain> -m <admin-email> --agree-tos -n

# 또는 webroot 방식 (이후 갱신과 동일)
sudo certbot certonly --webroot -w /srv/acme -d api.<domain> -m <admin-email> --agree-tos -n
```

발급 후: `/etc/letsencrypt/live/api.<domain>/fullchain.pem`, `privkey.pem` 존재 확인.

### 2.6 첫 부팅 (스키마 생성)

처음 한 번만 `ddl-auto=update` 모드로:
```bash
# .env.docker에 임시로 추가:
SPRING_JPA_HIBERNATE_DDL_AUTO=update

docker compose --env-file .env.docker up -d --build
docker compose logs -f app    # 스키마 생성 로그 확인 (Hibernate: create table users ...)

# 스키마 확정되면 .env.docker에서 SPRING_JPA_HIBERNATE_DDL_AUTO=update 제거 (또는 validate로 변경)
docker compose down
docker compose --env-file .env.docker up -d
```

### 2.7 헬스체크

```bash
curl https://api.<domain>/health
# {"status":"ok","db":"connected","uptime":3.45,"timestamp":"..."}
```

### 2.8 인증서 갱신 cron

`/etc/cron.d/certbot-renew`:
```cron
0 3 * * * root certbot renew --webroot -w /srv/acme --quiet --post-hook "cd /srv/geekchat/server-v2 && docker compose exec nginx nginx -s reload"
```

## 3. 일상적 배포 (Continuous)

### 3.1 코드 변경 배포

```bash
# 개발 머신: WireGuard UP 확인
wg show shjung    # 최근 handshake 있는지 확인 (없으면 터널 재시작)

# 배포
ssh geekchat-host '
  cd /srv/geekchat/server-v2
  git pull
  docker compose --env-file .env.docker up -d --build
  docker compose logs --tail=50 app
'
```

### 3.2 환경변수 변경 시

```bash
ssh geekchat-host
cd /srv/geekchat/server-v2
nano .env.docker
docker compose --env-file .env.docker up -d   # 변경된 서비스만 재생성
```

### 3.3 로그 확인

```bash
docker compose logs -f app          # 앱 로그 실시간
docker compose logs --tail=200 db   # DB 로그
docker compose logs nginx           # nginx 액세스/에러
```

### 3.4 DB 백업

```bash
# 호스트에서
docker compose exec db mysqldump -u root -p${DB_ROOT_PASSWORD} geekchat | gzip > /srv/backups/geekchat-$(date +%Y%m%d).sql.gz
```

cron 권장:
```cron
0 4 * * * root cd /srv/geekchat/server-v2 && docker compose exec -T db mysqldump -u root -p$(grep DB_ROOT_PASSWORD .env.docker | cut -d= -f2) geekchat | gzip > /srv/backups/geekchat-$(date +\%Y\%m\%d).sql.gz
```

## 4. 롤백

```bash
ssh geekchat-host
cd /srv/geekchat/server-v2
git log --oneline -10               # 이전 커밋 SHA 확인
git checkout <previous-sha>
docker compose --env-file .env.docker up -d --build
```

DB 마이그레이션이 backward incompatible한 경우: 백업 복원 필요.

## 5. 장애 대응

### 5.1 앱이 시작 안 됨
```bash
docker compose logs app | tail -50
# 흔한 원인:
# - DB_HOST 못 찾음 → docker compose ps에서 db 상태 확인
# - JWT_SECRET 32자 미만 → .env.docker 점검
# - 포트 충돌 → app 컨테이너는 호스트 포트 노출 안 함, nginx만 80/443
```

### 5.2 nginx 502 Bad Gateway
앱이 죽었거나 시작 중. `docker compose ps`로 app 상태 확인 → 로그 → 재시작:
```bash
docker compose restart app
```

### 5.3 인증서 만료
```bash
sudo certbot certificates    # 만료일 확인
sudo certbot renew --force-renewal --webroot -w /srv/acme
docker compose exec nginx nginx -s reload
```

### 5.4 디스크 부족
```bash
df -h
docker system prune -a --volumes    # 미사용 이미지/볼륨 정리 (주의: DB 볼륨은 named라 안전)
```

## 6. 비상 정지

```bash
ssh geekchat-host
cd /srv/geekchat/server-v2
docker compose down                 # 모든 컨테이너 중지 (DB 데이터는 named volume에 보존)
docker compose down -v              # ⚠️ DB 데이터까지 삭제 (백업 후에만)
```

## 7. 모니터링 (간단)

```bash
docker stats                        # CPU/메모리 실시간
df -h                               # 디스크
free -m                             # RAM
docker compose ps                   # 컨테이너 상태
```

권장 메모리 한계 (8GB 호스트):
- app: 1.5GB 이하 (`-Xmx2g` 설정으로 여유)
- db (mysql): 500MB 이하
- nginx: 50MB 이하

## 8. 자주 쓰는 명령 cheat sheet

```bash
# WG 상태
wg show shjung

# 호스트 도커 상태
ssh geekchat-host 'docker compose -f /srv/geekchat/server-v2/docker-compose.yml ps'

# 빠른 재시작
ssh geekchat-host 'cd /srv/geekchat/server-v2 && docker compose restart app'

# DB 콘솔
ssh geekchat-host 'cd /srv/geekchat/server-v2 && docker compose exec db mysql -u root -p geekchat'

# 헬스체크
curl -s https://api.<domain>/health | jq
```
