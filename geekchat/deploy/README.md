# Deploy — 배포 호스트

> 실제 호스트/SSH/WireGuard 값은 PUBLIC repo 보안상 placeholder 마스킹. 비공개 채널 참조.

빠른 참조. 상세 절차는 `../docs/DEPLOYMENT.md` 참고.

## 1. 첫 배포 (Bootstrap)

```bash
# 개발 머신에서: WireGuard UP
wg-quick up <WG_TUNNEL> || open -a WireGuard

# SSH 접속
ssh geekchat-host

# 호스트에 docker, certbot 설치 (sudo 1회)
sudo apt update && sudo apt install -y certbot
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && newgrp docker

# 코드 + secrets
sudo mkdir -p /srv/geekchat /srv/acme
sudo chown -R $USER:$USER /srv/geekchat /srv/acme
cd /srv/geekchat
git clone https://github.com/geek-chat/server-v2.git
cd server-v2
cp .env.docker.example .env.docker
nano .env.docker  # 시크릿 채우기

# 첫 인증서 (DNS A 레코드 propagation 후)
sudo certbot certonly --standalone -d api.<YOUR_DOMAIN> -m <admin-email> --agree-tos -n

# nginx.conf 도메인 치환
sed -i 's/api.geek-chat.example/api.<YOUR_DOMAIN>/g' deploy/nginx.conf

# 첫 부팅 (스키마 자동 생성)
SPRING_JPA_HIBERNATE_DDL_AUTO=update docker compose --env-file .env.docker up -d --build

# 부팅 확인 + 스키마 검증으로 복귀
curl https://api.<YOUR_DOMAIN>/health
docker compose down
docker compose --env-file .env.docker up -d   # 이제 validate 모드
```

## 2. 일상적 배포

```bash
# 개발 머신
ssh geekchat-host '
  cd /srv/geekchat/server-v2 &&
  git pull &&
  docker compose --env-file .env.docker up -d --build
'

# 헬스체크
curl https://api.<YOUR_DOMAIN>/health
```

## 3. 인증서 갱신 cron (호스트)

```cron
# /etc/cron.d/certbot-renew
0 3 * * * root certbot renew --webroot -w /srv/acme --quiet --post-hook "cd /srv/geekchat/server-v2 && docker compose exec nginx nginx -s reload"
```

## 4. 자주 쓰는 명령

```bash
docker compose ps
docker compose logs -f app
docker compose logs --tail=50 db
docker compose restart app
docker compose exec db mysql -u root -p geekchat
```

## 5. 비상

```bash
# 앱만 재시작
docker compose restart app

# 전체 정지 (DB 데이터 보존)
docker compose down

# DB 백업
docker compose exec db mysqldump -u root -p${DB_ROOT_PASSWORD} geekchat | gzip > /srv/backups/geekchat-$(date +%Y%m%d).sql.gz
```
