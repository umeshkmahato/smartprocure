# SmartProcure — Deployment Index

> Single entry point for all environment guides.  
> Choose the section that matches your target environment.

---

## Environments at a Glance

| Environment | Runtime | Guide |
|-------------|---------|-------|
| **Local / Dev** | Docker Desktop | [DOCKER_GUIDE.md](./DOCKER_GUIDE.md) |
| **AWS Production** | ECS Fargate + managed services | [DEPLOYMENT_AWS.md](./DEPLOYMENT_AWS.md) |
| **GCP Production** | GKE Autopilot + managed services | [DEPLOYMENT_GCP.md](./DEPLOYMENT_GCP.md) |

---

## Quick-Start by Environment

### Local (Docker Desktop)

```bash
cp .env.example .env.docker
docker compose -f docker-compose.yml up -d                              # infra
docker compose -f docker-compose.app.yml --env-file .env.docker up -d  # app
docker compose -f docker-compose.observability.yml up -d               # metrics
```

→ Frontend: http://localhost:3000 · Grafana: http://localhost:3001 · Swagger: http://localhost:8080/swagger-ui.html

---

### AWS (ECS Fargate)

```bash
# 1 — Push images to ECR
aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_BASE
docker build -t $ECR_BASE/smartprocure-backend:latest backend/ && docker push ...

# 2 — Deploy / update ECS services
aws ecs update-service --cluster smartprocure-prod --service backend --force-new-deployment
aws ecs wait services-stable --cluster smartprocure-prod --services backend
```

→ Full guide: [DEPLOYMENT_AWS.md](./DEPLOYMENT_AWS.md)

---

### GCP (GKE Autopilot)

```bash
# 1 — Push images to Artifact Registry
gcloud auth configure-docker us-central1-docker.pkg.dev
docker build -t us-central1-docker.pkg.dev/$PROJECT_ID/smartprocure/smartprocure-backend:latest backend/
docker push us-central1-docker.pkg.dev/$PROJECT_ID/smartprocure/smartprocure-backend:latest

# 2 — Apply Kubernetes manifests
kubectl apply -f k8s/secrets/
kubectl apply -f k8s/backend/
kubectl apply -f k8s/notification/
kubectl apply -f k8s/ingress/
```

→ Full guide: [DEPLOYMENT_GCP.md](./DEPLOYMENT_GCP.md)

---

## Service Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        SmartProcure                             │
│                                                                 │
│  ┌─────────────┐   ┌────────────────┐   ┌──────────────────┐   │
│  │  Frontend   │   │    Backend     │   │  Notification    │   │
│  │  React 18   │──▶│ Spring Boot 4  │──▶│    Service       │   │
│  │  TypeScript │   │   Java 21 VT   │   │ GraalVM Native   │   │
│  └─────────────┘   └───────┬────────┘   └────────┬─────────┘   │
│                            │                     │             │
│               ┌────────────▼─────────────────────▼──────────┐  │
│               │               Data Layer                     │  │
│               │  PostgreSQL 16+pgvector  Redis 7  Kafka      │  │
│               └──────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                  Observability                           │   │
│  │  Prometheus · Grafana · OpenTelemetry · Alert Rules      │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Ports Reference

| Service | Local port | Container port | Notes |
|---------|-----------|---------------|-------|
| Frontend | 3000 | 80 | Nginx + `/api` proxy |
| Backend API | 8080 | 8080 | Spring Boot |
| Notification Service | 8081 | 8081 | Kafka consumer |
| PostgreSQL | 5432 | 5432 | |
| Redis | 6379 | 6379 | |
| Kafka | 9092 | 9092 | |
| MailHog SMTP | 1025 | 1025 | Dev only |
| MailHog UI | 8025 | 8025 | Dev only |
| Prometheus | 9090 | 9090 | |
| Grafana | 3001 | 3000 | |
| OTel Collector (gRPC) | 4317 | 4317 | |
| Ollama | 11434 | 11434 | AI inference |

---

## Credentials

### Default admin (all environments)

```
Email:    admin@smartprocure.internal
Password: Admin@SmartProcure2025!
```

### Grafana (local)

```
URL:      http://localhost:3001
Username: admin
Password: admin123
```

> **Production:** credentials are stored in AWS Secrets Manager / GCP Secret Manager.  
> Never commit secrets to source control — see `.env.example` for required variable names.

---

## Component Image Summary

| Component | Base image (JVM) | Base image (native) | Size |
|-----------|-----------------|---------------------|------|
| Backend | `eclipse-temurin:21-jre-alpine` | — | ~380 MB |
| Notification Service | `eclipse-temurin:21-jre-alpine` | `distroless/static:nonroot` | 71 MB (native) |
| Frontend | `nginx:alpine` | — | ~45 MB |

---

## CI/CD Overview

| Cloud | Pipeline | Trigger | Approval gate |
|-------|----------|---------|---------------|
| **AWS** | GitHub Actions → `deploy-aws.yml` | Push to `main` | ECS `wait services-stable` |
| **GCP** | Cloud Build → Cloud Deploy | Push to `main` | Cloud Deploy approval (prod stage) |

---

## Security & Performance Docs

| Document | Contents |
|----------|---------|
| [SECURITY_AUDIT.md](./SECURITY_AUDIT.md) | 16 resolved findings, OWASP Top 10 coverage, dependency/container scan results |
| [PERFORMANCE_REPORT.md](./PERFORMANCE_REPORT.md) | k6 benchmark results, JVM/native comparison, tuning log |

---

## Related Guides

| Guide | Purpose |
|-------|---------|
| [DOCKER_GUIDE.md](./DOCKER_GUIDE.md) | Full local dev workflow including k6 tests, native image, observability |
| [DEPLOYMENT_AWS.md](./DEPLOYMENT_AWS.md) | ECR · ECS Fargate · RDS · ElastiCache · MSK · ALB · Route 53 · GitHub Actions CI/CD |
| [DEPLOYMENT_GCP.md](./DEPLOYMENT_GCP.md) | Artifact Registry · GKE Autopilot · Cloud SQL · Memorystore · Strimzi Kafka · Cloud Build/Deploy |
| [SECURITY_AUDIT.md](./SECURITY_AUDIT.md) | Security audit report (Phase 16) |
| [PERFORMANCE_REPORT.md](./PERFORMANCE_REPORT.md) | Performance benchmark report (Phase 16) |
