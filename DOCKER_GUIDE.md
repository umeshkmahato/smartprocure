# SmartProcure — Local Docker Run & Validation Guide

## Prerequisites

| Tool | Min Version | Check |
|------|-------------|-------|
| Docker Desktop | 24.x | `docker --version` |
| Docker Compose | v2 (bundled) | `docker compose version` |
| Git | any | `git --version` |

> **Windows users:** Use **PowerShell** or **Git Bash**. All commands below work in both.

---

## Directory Layout (Docker files)

```
smartprocure/
├── docker-compose.yml          ← Infrastructure (Postgres, Redis, Kafka, MailHog, Ollama)
├── docker-compose.app.yml      ← Application   (Backend API + React Frontend)
├── .env.docker                 ← Docker-specific env vars  ← use this, NOT .env
├── backend/
│   └── Dockerfile              ← Maven multi-stage build → JRE runtime
└── frontend/
    ├── Dockerfile              ← Node build → Nginx runtime
    └── nginx.conf              ← SPA fallback + /api proxy
```

---

## Step 1 — Start Infrastructure

```bash
docker compose -f docker-compose.yml up -d
```

This starts:
- **Postgres 16** on port `5432`
- **Redis 7** on port `6379`
- **Kafka + Zookeeper** on port `9092`
- **MailHog** SMTP `1025`, Web UI `8025`
- **Ollama** on port `11434` *(optional — skip if not using AI)*

Wait until all containers are healthy:

```bash
docker compose -f docker-compose.yml ps
```

All services should show **`healthy`** before proceeding.

---

## Step 2 — Build & Start the Application

```bash
docker compose -f docker-compose.app.yml --env-file .env.docker up --build -d
```

What happens:
1. **Backend** — Maven downloads dependencies, compiles, packages JAR, runs Flyway migrations on first boot
2. **Frontend** — npm installs, Vite builds React app, Nginx serves it with `/api` proxied to backend

Watch logs in real-time:

```bash
# All services
docker compose -f docker-compose.app.yml logs -f

# Backend only
docker compose -f docker-compose.app.yml logs -f backend

# Frontend only
docker compose -f docker-compose.app.yml logs -f frontend
```

---

## Step 3 — Verify All Containers Are Running

```bash
docker compose -f docker-compose.yml -f docker-compose.app.yml ps
```

Expected output:

```
NAME            STATUS          PORTS
sp-postgres     healthy         0.0.0.0:5432->5432/tcp
sp-redis        healthy         0.0.0.0:6379->6379/tcp
sp-zookeeper    healthy         0.0.0.0:2181->2181/tcp
sp-kafka        healthy         0.0.0.0:9092->9092/tcp
sp-mailhog      running         0.0.0.0:1025->1025/tcp, 0.0.0.0:8025->8025/tcp
sp-ollama       healthy         0.0.0.0:11434->11434/tcp
sp-backend      healthy         0.0.0.0:8080->8080/tcp
sp-frontend     healthy         0.0.0.0:3000->80/tcp
```

---

## Step 4 — Validate the Application

### 4.1 Frontend (React App)
Open in browser:
```
http://localhost:3000
```
You should see the **SmartProcure login page**.

---

### 4.2 Backend Health Check
```bash
curl http://localhost:8080/actuator/health
```
Expected:
```json
{ "status": "UP", "components": { "db": { "status": "UP" }, "redis": { "status": "UP" } } }
```

---

### 4.3 Swagger API Docs
```
http://localhost:8080/swagger-ui.html
```
Browse all REST endpoints, try them out directly in the browser.

---

### 4.4 Login & Get JWT Token
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@smartprocure.internal","password":"Admin@SmartProcure2025!"}' \
  | python -m json.tool
```
Expected — you get back an `accessToken` and `refreshToken`.

---

### 4.5 Test Server-Side Sorting & Pagination

Replace `<TOKEN>` with the access token from step 4.4.

**Vendors — sort by name ascending, page 1:**
```bash
curl -s "http://localhost:8080/api/v1/vendors?sort=name&dir=asc&page=0&size=5" \
  -H "Authorization: Bearer <TOKEN>" | python -m json.tool
```

**Purchase Requests — sort by estimatedTotal descending:**
```bash
curl -s "http://localhost:8080/api/v1/purchase-requests?sort=estimatedTotal&dir=desc&page=0&size=5" \
  -H "Authorization: Bearer <TOKEN>" | python -m json.tool
```

**Validate — invalid sort field returns 400 (not 500):**
```bash
curl -s "http://localhost:8080/api/v1/vendors?sort=; DROP TABLE vendors--" \
  -H "Authorization: Bearer <TOKEN>"
```
Expected response: **HTTP 400** with `"Invalid sort field"` message — injection blocked.

---

### 4.6 MailHog — Check Emails
```
http://localhost:8025
```
All emails sent by the app (registration, approval notifications) appear here.

---

## Step 5 — Rebuild After Code Changes

**Backend change:**
```bash
docker compose -f docker-compose.app.yml --env-file .env.docker up --build -d backend
```

**Frontend change:**
```bash
docker compose -f docker-compose.app.yml --env-file .env.docker up --build -d frontend
```

**Both:**
```bash
docker compose -f docker-compose.app.yml --env-file .env.docker up --build -d
```

---

## Step 6 — Stop Everything

```bash
# Stop app only (keep infra running for faster restarts)
docker compose -f docker-compose.app.yml down

# Stop everything
docker compose -f docker-compose.yml -f docker-compose.app.yml down

# Stop everything AND wipe all data volumes (full reset)
docker compose -f docker-compose.yml -f docker-compose.app.yml down -v
```

---

## Troubleshooting

| Problem | Cause | Fix |
|---------|-------|-----|
| Backend exits immediately | Postgres not ready | Wait for `sp-postgres` to be `healthy`, then `docker compose ... up -d backend` |
| `Flyway migration failed` | Schema mismatch | Run `down -v` to wipe DB, then start fresh |
| Frontend shows blank page | Nginx can't reach backend | Check `docker logs sp-backend` for startup errors |
| `Connection refused` on port 9092 | Kafka not ready | Add `start_period: 60s` or wait longer before starting backend |
| Backend `401` on all endpoints | Missing/expired JWT | Re-run login curl and use the new token |
| `Invalid sort field` 400 on valid field | Field not in allowlist | Check `SORT_FIELDS` set in the relevant controller |

---

## Service URLs Quick Reference

| Service | URL | Notes |
|---------|-----|-------|
| **Frontend** | http://localhost:3000 | React SPA |
| **Backend API** | http://localhost:8080/api/v1 | REST API |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Interactive API docs |
| **Actuator Health** | http://localhost:8080/actuator/health | Health check |
| **MailHog** | http://localhost:8025 | Dev email inbox |
| **Postgres** | localhost:5432 / db: `smartprocure` | Use DBeaver/pgAdmin |
| **Redis** | localhost:6379 | Use Redis Insight |
| **Kafka** | localhost:9092 | Use Offset Explorer |
| **Ollama** | http://localhost:11434 | AI inference |
