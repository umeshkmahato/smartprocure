# SmartProcure — Local Docker Run & Validation Guide

> For cloud deployments see **[DEPLOYMENT_AWS.md](./DEPLOYMENT_AWS.md)** and **[DEPLOYMENT_GCP.md](./DEPLOYMENT_GCP.md)**.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Directory Layout](#directory-layout-docker-files)
3. [Environment Variables](#environment-variables)
4. [Start Infrastructure](#step-1--start-infrastructure)
5. [Pull AI Models](#step-2-optional--pull-ai-models)
6. [Build & Start the Application](#step-3--build--start-the-application)
7. [Start the Observability Stack](#step-4--start-the-observability-stack)
8. [Validate the System](#step-5--validate-the-application)
9. [Run Tests](#step-6--run-tests-locally)
10. [Run k6 Load Tests](#step-7--run-k6-load-tests)
11. [Build the GraalVM Native Image](#step-8--build--run-the-graalvm-native-image)
12. [Rebuild After Code Changes](#step-9--rebuild-after-code-changes)
13. [Stop & Reset](#step-10--stop-everything)
14. [Service URLs](#service-urls-quick-reference)
15. [Troubleshooting](#troubleshooting)

---

## Prerequisites

| Tool | Min Version | Check |
|------|-------------|-------|
| Docker Desktop | 24.x | `docker --version` |
| Docker Compose | v2 (bundled) | `docker compose version` |
| Git | any | `git --version` |

> **Windows users:** Use **PowerShell** or **Git Bash**. All commands below work in both.

---

## Environment Variables

```bash
cp .env.example .env.docker   # copy template — never commit this file
```

Key variables:

| Variable | Default (dev) | Notes |
|----------|--------------|-------|
| `DB_PASSWORD` | `smartprocure_dev` | Change for shared environments |
| `JWT_SECRET` | `dev-secret-…` | 256-bit random string in production |
| `SPRING_PROFILES_ACTIVE` | `dev` | Use `prod` for JSON logs + rolling files |
| `OLLAMA_BASE_URL` | `http://ollama:11434` | Comment out the Ollama service to disable AI |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:29092` | Internal listener — use `29092` inside Docker network |
| `MAIL_HOST` | `mailhog` | MailHog catches all outbound email |

---

## Directory Layout (Docker files)

```
smartprocure/
├── docker-compose.yml               ← Infrastructure (Postgres, Redis, Kafka, MailHog, Ollama)
├── docker-compose.app.yml           ← Application (backend, frontend, notification-service)
├── docker-compose.observability.yml ← Prometheus, Grafana, OTel Collector
├── .env.docker                      ← Docker-specific env vars — use this, NOT .env
├── backend/
│   └── Dockerfile                   ← Maven multi-stage build → eclipse-temurin:21-jre-alpine
├── frontend/
│   ├── Dockerfile                   ← Node build → Nginx runtime
│   └── nginx.conf                   ← SPA fallback + /api proxy
├── notification-service/
│   ├── Dockerfile                   ← JVM image (Kafka consumer)
│   └── Dockerfile.native            ← GraalVM → distroless native image (Phase 15)
├── docker/
│   ├── prometheus/
│   │   ├── prometheus.yml           ← Scrape config
│   │   └── alert-rules.yml          ← 15 alert rules: HTTP, JVM, DB, Kafka, business
│   └── grafana/
│       ├── provisioning/
│       │   ├── datasources/prometheus.yml
│       │   └── dashboards/dashboard.yml
│       └── dashboards/
│           ├── smartprocure-overview.json  ← Main observability dashboard
│           └── smartprocure-alerts.json    ← Firing-alerts panel
├── k6/
│   ├── helpers/auth.js              ← Shared login helper
│   └── scenarios/
│       ├── auth-load.js             ← Auth smoke / load / stress
│       ├── purchase-request-load.js ← PR CRUD load test
│       └── full-flow-load.js        ← Full 10-step procurement flow
└── DOCKER_GUIDE.md                  ← This file
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

All containers except **MailHog** (no healthcheck configured) should show **`healthy`**.
MailHog will show **`running`** — that is correct and expected.

---

## Step 2 (optional) — Pull AI Models

> Skip if `OLLAMA_ENABLED=false` or you are not using AI features.

```bash
# Chat model (~4.7 GB on first pull)
docker exec sp-ollama ollama pull llama3.2:8b

# Embedding model (~274 MB)
docker exec sp-ollama ollama pull nomic-embed-text

# Verify
docker exec sp-ollama ollama list
```

---

## Step 3 — Build & Start the Application

```bash
docker compose -f docker-compose.app.yml --env-file .env.docker up --build -d
```

What happens:
1. **Backend** — Maven multi-stage build (dependency cache → compile → JAR); Flyway runs DB migrations on first start.
2. **Notification Service** — same Maven pipeline; Kafka consumer group registers.
3. **Frontend** — `npm ci` → `vite build` → Nginx serves `/` with `/api` proxied to backend.

Watch logs in real-time:

```bash
# All app services
docker compose -f docker-compose.app.yml logs -f

# Backend only (Flyway + Spring context startup)
docker compose -f docker-compose.app.yml logs -f backend

# Notification service consumer group registration
docker compose -f docker-compose.app.yml logs -f notification-service

# Frontend build output
docker compose -f docker-compose.app.yml logs -f frontend
```

---

## Step 4 — Start the Observability Stack

```bash
docker compose -f docker-compose.observability.yml up -d
```

Starts:

| Container | Port (host) | Purpose |
|-----------|-------------|--------|
| `sp-prometheus` | 9090 | Metrics scraping + alert evaluation |
| `sp-grafana` | 3001 | Auto-provisioned dashboards |
| `sp-otel-collector` | 4317 (gRPC), 4318 (HTTP) | OpenTelemetry trace ingestion |

### Verify Prometheus targets

```
http://localhost:9090/targets
```

Both `smartprocure-backend` and `notification-service` should show **State: UP**.

### Open Grafana

```
http://localhost:3001   →  admin / admin123
```

Navigate **Dashboards → SmartProcure — Overview** for HTTP latency, JVM heap, HikariCP pool, Kafka throughput, and business KPI panels (all auto-provisioned from `docker/grafana/dashboards/`).

### View alert rules

```
http://localhost:9090/alerts
```

15 rules across 5 groups (HTTP, JVM, DB, Kafka, business) are pre-loaded from `docker/prometheus/alert-rules.yml`.

---

## Step 5 — Verify All Containers Are Running

```bash
docker compose \
  -f docker-compose.yml \
  -f docker-compose.app.yml \
  -f docker-compose.observability.yml ps
```

Expected output:

```
NAME               STATUS    PORTS
sp-postgres        healthy   0.0.0.0:5432->5432/tcp
sp-redis           healthy   0.0.0.0:6379->6379/tcp
sp-zookeeper       healthy   0.0.0.0:2181->2181/tcp
sp-kafka           healthy   0.0.0.0:9092->9092/tcp
sp-mailhog         running   0.0.0.0:1025->1025/tcp, 0.0.0.0:8025->8025/tcp
sp-ollama          healthy   0.0.0.0:11434->11434/tcp
sp-backend         healthy   0.0.0.0:8080->8080/tcp
sp-notification    healthy   0.0.0.0:8081->8081/tcp
sp-frontend        healthy   0.0.0.0:3000->80/tcp
sp-prometheus      running   0.0.0.0:9090->9090/tcp
sp-grafana         running   0.0.0.0:3001->3000/tcp
sp-otel-collector  running   0.0.0.0:4317->4317/tcp
```

> **Note:** `sp-mailhog` correctly shows `running` — it has no healthcheck by design.

---

## Step 6 — Validate the Application

### 6.1 Frontend

```
http://localhost:3000
```

You should see the **SmartProcure login page**.

---

### 6.2 Backend health

```bash
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
```

Expected:

```json
{ "status": "UP", "components": { "db": { "status": "UP" }, "redis": { "status": "UP" }, "kafka": { "status": "UP" } } }
```

---

### 6.3 Prometheus custom metrics

```bash
curl -s http://localhost:8080/actuator/prometheus | grep smartprocure_
```

Expect business counters: `smartprocure_purchase_requests_created_total`, `smartprocure_auth_login_success_total`, etc.

---

### 6.4 Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

### 6.5 Login & capture JWT

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@smartprocure.internal","password":"Admin@SmartProcure2025!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
echo $TOKEN
```

---

### 6.6 Create a purchase request (smoke test)

```bash
curl -s -X POST http://localhost:8080/api/v1/purchase-requests \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Docker smoke test PR",
    "description": "Validation purchase request",
    "requiredByDate": "2025-12-31",
    "items": [
      { "description": "Laptop", "quantity": 2, "estimatedUnitPrice": 1200.00, "unit": "PCS" }
    ]
  }' | python3 -m json.tool
```

Expect `"status": "DRAFT"` and an `id` in the response.

---

### 6.7 Sorting & pagination

```bash
# Vendors sorted by name
curl -s "http://localhost:8080/api/v1/vendors?sort=name&dir=asc&page=0&size=5" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# SQL-injection attempt → expect HTTP 400
curl -s "http://localhost:8080/api/v1/vendors?sort=;DROP TABLE vendors--" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 6.8 AI vendor recommendation (requires Ollama)

```bash
curl -s -X POST http://localhost:8080/api/v1/ai/recommend \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "requirement": "Office laptops with SSD, 16 GB RAM", "topK": 3 }' \
  | python3 -m json.tool
```

---

### 6.9 MailHog

```
http://localhost:8025
```

Approval notifications, registration emails, and PO alerts appear here.

---

## Step 7 — Run Tests Locally

### Unit tests (no Docker required)

```bash
cd backend && mvn clean test
```

### Integration tests (Testcontainers — Docker required)

```bash
cd backend && mvn clean verify
```

Testcontainers auto-pulls Postgres 16, Kafka 7.6, Redis 7.2 — no manual container management.

### End-to-end integration test only (16-step full flow)

```bash
cd backend
mvn clean verify -Dtest=FullProcurementFlowIntegrationTest -DfailIfNoTests=false
```

---

## Step 8 — Run k6 Load Tests

> k6 must be installed on the host: https://k6.io/docs/get-started/installation/

```bash
# Auth — smoke (1 VU, 30 s)
k6 run -e BASE_URL=http://localhost:8080 -e SCENARIO=smoke \
       k6/scenarios/auth-load.js

# Purchase Request — load (ramp to 60 VUs)
k6 run -e BASE_URL=http://localhost:8080 -e SCENARIO=load \
       k6/scenarios/purchase-request-load.js

# Full procurement flow — load (ramp to 30 VUs, E2E p95 < 12 s)
k6 run -e BASE_URL=http://localhost:8080 -e SCENARIO=load \
       -e OFFICER_EMAIL=alice@smartprocure.dev \
       -e OFFICER_PASSWORD=Test@1234! \
       -e APPROVER_EMAIL=bob@smartprocure.dev \
       -e APPROVER_PASSWORD=Test@1234! \
       -e VENDOR_ID=1 \
       k6/scenarios/full-flow-load.js

# Stress — ramp to 150 VUs (breaking point ~180 VUs)
k6 run -e BASE_URL=http://localhost:8080 -e SCENARIO=stress \
       k6/scenarios/auth-load.js
```

While k6 runs, watch **Grafana → SmartProcure — Overview** at `http://localhost:3001` for real-time HTTP latency, JVM heap, and business KPI panels.

---

## Step 9 — Build & Run the GraalVM Native Image

> Requires **10 GB+** Docker Desktop memory. Build time ≈ 5 minutes.

```bash
cd notification-service

# Build (GraalVM CE 21 → distroless:nonroot runtime)
docker build -f Dockerfile.native \
             -t smartprocure/notification-service:native .

# Run (connected to the already-running sp-network)
docker run --rm \
  --network sp-network \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:29092 \
  -e REDIS_HOST=redis \
  -e REDIS_PORT=6379 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -p 8082:8081 \
  smartprocure/notification-service:native

# Note: port 8081 is already bound by sp-notification from docker-compose.app.yml
# so we map to 8082 on the host to avoid a port conflict.
```

**Verify start-up time (target < 100 ms):**

```bash
docker logs <container_id> | grep "Started NotificationServiceApplication"
# Expected: "Started NotificationServiceApplication in 0.062 seconds"
```

**Compare image sizes:**

```bash
docker images | grep notification-service
# notification-service:native  ~71 MB
# notification-service:latest  ~318 MB  (78 % smaller)
```

---

## Step 10 — Rebuild After Code Changes

```bash
# Backend only
docker compose -f docker-compose.app.yml --env-file .env.docker up --build -d backend

# Frontend only
docker compose -f docker-compose.app.yml --env-file .env.docker up --build -d frontend

# Notification service only
docker compose -f docker-compose.app.yml --env-file .env.docker up --build -d notification-service

# All app services
docker compose -f docker-compose.app.yml --env-file .env.docker up --build -d
```

> Maven's dependency layer is Docker-cached — only changed source files trigger a recompile.

---

## Step 11 — Stop Everything

```bash
# Stop app only (keeps infra running for fast restarts)
docker compose -f docker-compose.app.yml down

# Stop observability
docker compose -f docker-compose.observability.yml down

# Stop everything
docker compose \
  -f docker-compose.yml \
  -f docker-compose.app.yml \
  -f docker-compose.observability.yml \
  down

# Full reset — wipes all data volumes (Postgres, Redis, Kafka offsets)
docker compose \
  -f docker-compose.yml \
  -f docker-compose.app.yml \
  -f docker-compose.observability.yml \
  down -v

# Remove all SmartProcure images (force a clean rebuild)
docker images | grep smartprocure | awk '{print $3}' | xargs docker rmi -f
```

---

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| Backend exits immediately | Postgres / Kafka not yet healthy | Wait for `sp-postgres` and `sp-kafka` to show `healthy`, then restart backend |
| `FlywayException: migration failed` | Schema mismatch from prior run | `docker compose ... down -v` then start fresh |
| Frontend blank page / 502 | Backend still starting | Check `docker logs sp-backend`; wait for `"Started SmartProcureApplication"` |
| `Connection refused` on port 9092 | Kafka still starting | Increase `healthcheck.start_period` to 60 s or restart backend after Kafka is healthy |
| Grafana shows "No data" | Prometheus target DOWN | Check `localhost:9090/targets`; ensure backend exposes `/actuator/prometheus` |
| Grafana dashboards missing | Provisioning path wrong | Confirm `docker/grafana/provisioning/dashboards/dashboard.yml` `path:` matches `docker/grafana/dashboards/` |
| Alert rules not loading | `rule_files:` missing in prometheus.yml | Add `- "alert-rules.yml"` under `rule_files:` in `docker/prometheus/prometheus.yml` |
| `401 Unauthorized` on all calls | Expired JWT | Re-run login curl and export fresh `$TOKEN` |
| Ollama pull hangs | Low Docker memory | Increase Docker Desktop memory to 12 GB+ |
| Native image build OOM | Docker Desktop memory < 8 GB | Increase to 10 GB in Docker Desktop → Settings → Resources |
| `hikaricp_connections_pending > 0` | Pool too small | Increase `maximum-pool-size` to 20 in `application.yml` |
| k6 `ECONNREFUSED` | Backend not running | Confirm `docker compose ... up -d backend` completed |
| MailHog shows no emails | SMTP port misconfigured | Verify `SMTP_HOST=mailhog` and `SMTP_PORT=1025` in `.env.docker` |
| `Invalid sort field` 400 on valid field | Field not in allowlist | Check `SORT_FIELDS` constant in the relevant controller |

---

## Service URLs Quick Reference

| Service | URL | Credentials / Notes |
|---------|-----|---------------------|
| **React Frontend** | http://localhost:3000 | admin@smartprocure.internal / Admin@SmartProcure2025! |
| **Backend REST API** | http://localhost:8080/api/v1 | JWT Bearer |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Interactive API docs |
| **Actuator Health** | http://localhost:8080/actuator/health | — |
| **Actuator Metrics** | http://localhost:8080/actuator/prometheus | Scraped by Prometheus |
| **MailHog UI** | http://localhost:8025 | Dev email inbox |
| **Grafana** | http://localhost:3001 | admin / admin123 |
| **Prometheus** | http://localhost:9090 | — |
| **Prometheus Alerts** | http://localhost:9090/alerts | 15 pre-loaded rules |
| **OTel Collector** | localhost:4317 (gRPC) | Trace ingestion |
| **Ollama API** | http://localhost:11434 | AI inference |
| **PostgreSQL** | localhost:5432 db: `smartprocure` | Use DBeaver / pgAdmin |
| **Redis** | localhost:6379 | No password (dev) |
| **Kafka** | localhost:9092 | Use Offset Explorer |
