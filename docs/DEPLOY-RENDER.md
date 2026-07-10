# Deploy — Render (free) + Supabase (free Postgres)

Free-tier deployment of the GeekChat v2 backend: the app runs as a **Render Docker web service**
and talks to a **Supabase managed Postgres**. This replaces the VPN-internal friend-cloud path
(`docker-compose.yml` + `deploy/nginx.conf` + `docs/DEPLOYMENT.md`), which is kept for reference.

Config lives in [`render.yaml`](../render.yaml) (Blueprint) and [`.env.render.example`](../.env.render.example).

## Know the free-tier limits first

| | Free tier reality |
|---|---|
| Render web service | 512 MB RAM / 0.1 CPU · **spins down after 15 min idle** (~1 min cold start) · 750 h/mo · Docker build |
| Supabase Postgres | 500 MB–1 GB · **project pauses after ~1 week of inactivity** (resume from dashboard) · 60 direct connections · SSL required |
| Data durability | Supabase free does NOT expire like Render's own Postgres, but a paused project must be resumed. For anything real, use a paid DB. |

512 MB is tight for Spring Boot 4 + JPA + Spring AI. `JAVA_OPTS` in `render.yaml` sizes the heap as a
% of the container (65%). If it OOMs at startup, lower `MaxRAMPercentage`, trim starters, or move to
Render Starter ($7/mo, no spin-down).

## 1. Supabase — create the database

1. Create a project at <https://supabase.com> (pick a region near you; **Southeast Asia** for KR).
2. Set a strong database password (save it).
3. Dashboard → **Connect** → **Session pooler** (IPv4, Hibernate-friendly). Read off:
   - Host — `aws-0-<region>.pooler.supabase.com`
   - Port — `5432` (session pooler)
   - Database — `postgres`
   - User — `postgres.<project-ref>`
   - Password — the one you set

   > Prefer the **transaction pooler** (port `6543`)? It also works, but append `&prepareThreshold=0`
   > to the JDBC URL so Hibernate's prepared statements don't break. Session pooler (5432) needs no
   > such tweak — start there.

## 2. Render — deploy the web service

1. Push this branch and merge to `main` (Render deploys from a connected branch).
2. Render → **New** → **Blueprint** → connect this GitHub repo. Render reads `render.yaml`.
3. It prompts for every `sync: false` env var — fill them in:
   - `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD` — from Supabase (step 1). `DB_PORT=5432`, `DB_NAME=postgres`, `DB_SSLMODE=require` are preset.
   - `JWT_SECRET` — Render auto-generates it (`generateValue`). Leave as is.
   - `FRONTEND_URL`, `OAUTH_CALLBACK_URL` — you'll finalize these in step 3 (need the service URL).
   - OAuth / AI keys — optional; leave blank to skip (the app boots and human chat works without them).
4. Deploy. The **first boot** runs `SPRING_JPA_HIBERNATE_DDL_AUTO=update` (set in `render.yaml`) and
   creates the schema on the empty Supabase DB. (The app's built-in default is `validate`, which
   would fail on an empty DB.) You may switch this env to `validate` after the first successful boot.

## 3. Wire up URLs (after the service exists)

Your service is at `https://<service-name>.onrender.com`. Now set/confirm in Render → Environment:
- `FRONTEND_URL` = your frontend origin (e.g. `https://geek-chat-web.vercel.app`)
- `OAUTH_CALLBACK_URL` = `https://<service-name>.onrender.com/auth/callback`

Then add that callback URL to the **Google/Naver OAuth console** redirect URIs. Save → Render redeploys.

## 4. Verify

```bash
BASE=https://<service-name>.onrender.com
curl $BASE/health                       # {"status":"ok","db":"connected",...}
curl -X POST $BASE/api/ai/chat -d '{}'  # 401 (admin-gated) → security wired
```
WebSocket is at `wss://<service-name>.onrender.com/ws?token=<JWT>` (Render terminates TLS). Note the
15-min spin-down: the first request after idle takes ~1 min while the instance wakes.

## Notes

- **DB parity**: local dev (`local` profile) and CI/tests all run on Postgres now
  (`docker-compose.dev.yml` = local Postgres on host 5433; tests use H2 in PostgreSQL mode).
- **Secrets**: never commit real values. `render.yaml` only holds non-secret defaults + `sync:false`
  placeholders; fill secrets in the Render dashboard.
- **Friend-cloud path** (`docs/DEPLOYMENT.md`, `docker-compose.yml`, `deploy/`) is retained as an
  alternative/reference and is not used by this Render deployment.
