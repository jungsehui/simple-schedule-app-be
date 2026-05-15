---
description: 배포 전 사전 점검 (테스트 / 환경변수 / Dockerfile / nginx config)
allowed-tools: Bash, Read, Grep
---

배포 전 사전 점검 체크리스트를 자동 검증한다.

## 점검 항목

### 1. 테스트 통과
```bash
./gradlew clean test
```
모든 테스트 통과해야 배포 가능.

### 2. JAR 빌드 성공
```bash
./gradlew bootJar
ls -lh build/libs/*.jar
```

### 3. Docker 이미지 빌드
```bash
docker build -t geek-chat-server-v2:check .
docker images geek-chat-server-v2:check
```

### 4. 필수 환경변수 점검
`.env.docker`에 다음 항목이 채워져 있는지:
```bash
grep -E "^(DB_|JWT_SECRET|FRONTEND_URL|OAUTH_CALLBACK_URL)=.+$" .env.docker | wc -l
# 최소 7개 이상 있어야 함 (DB_NAME, DB_USERNAME, DB_PASSWORD, DB_ROOT_PASSWORD, JWT_SECRET, FRONTEND_URL, OAUTH_CALLBACK_URL)
```

`.env.docker`에 placeholder가 남아있는지 확인:
```bash
grep -E "(changeme|replace-me|your-)" .env.docker
# 결과 있으면 시크릿 미입력 — 배포 중단
```

### 5. nginx config 도메인 치환 확인
```bash
grep "api.geek-chat.example" deploy/nginx.conf
# 결과 있으면 도메인 치환 안됨 — 실제 도메인으로 sed 필요
```

### 6. v1 코드 수정 안 됐는지
```bash
cd ~/Work/geek-chat/geek-chat-server && git status --porcelain
# 결과 비어있어야 함
```

### 7. WireGuard VPN 연결 (원격 배포 시)
```bash
wg show <WG_TUNNEL> 2>&1 | grep -E "latest handshake|peer"
# 최근 handshake 있어야 ssh 가능
```

### 8. SSH 접속 테스트
```bash
ssh -o ConnectTimeout=5 geekchat-host 'echo ok'
```

## 결과 해석

- 모두 통과 (✅): 배포 OK.
- 일부 실패 (❌): 어떤 단계에서 실패했는지 보고. 사용자 확인 후 진행.

## 배포 명령 (모든 점검 통과 후)

```bash
ssh geekchat-host '
  cd /srv/geekchat/server-v2 &&
  git pull &&
  docker compose --env-file .env.docker up -d --build
'
```
