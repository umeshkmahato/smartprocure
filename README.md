# SmartProcure — AI-Powered Procurement & Vendor Management System

> Enterprise-grade procurement automation: multi-level approvals, AI vendor recommendations, quotation comparison, purchase orders, invoice matching, and full audit trails.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21 LTS · Spring Boot 4.0.x · Spring Framework 7 · Spring Security 7 |
| Persistence | PostgreSQL 16 + PGVector · Flyway · Spring Data JPA · Hibernate 7 |
| Messaging | Apache Kafka · Spring Kafka |
| Cache | Redis 7 · Spring Cache (Lettuce) |
| AI | Spring AI · Ollama (llama3.2:8b chat · nomic-embed-text embeddings) |
| Frontend | React 18 · TypeScript 5 · Vite · TailwindCSS · Shadcn/ui (Phase 9) |
| Observability | Spring Actuator · Micrometer · Prometheus · Grafana · OpenTelemetry |
| Performance | Java 21 Virtual Threads · GraalVM Native Image (Phase 15) |
| Testing | JUnit 5 · Mockito · Testcontainers · Playwright |
| Infrastructure | Docker · Docker Compose |

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 21 LTS |
| Maven | 3.9+ |
| Docker Desktop | Latest stable |
| Node.js | 20+ (Phase 9 frontend) |

---

## Quick Start

### 1 — Clone and configure

```bash
git clone https://github.com/your-org/smartprocure.git
cd smartprocure
cp .env.example .env          # edit values as needed
```

### 2 — Start infrastructure

```bash
docker compose up -d
docker compose ps              # wait until all services are healthy
```

### 3 — Pull AI models (first run only)

```bash
docker exec sp-ollama ollama pull llama3.2:8b
docker exec sp-ollama ollama pull nomic-embed-text
```

### 4 — Start the backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

| Endpoint | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api-docs |
| Health | http://localhost:8080/actuator/health |
| Metrics | http://localhost:8080/actuator/prometheus |
| MailHog UI | http://localhost:8025 |

---

## Observability Stack (optional)

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d
```

| Tool | URL | Credentials |
|---|---|---|
| Grafana | http://localhost:3001 | admin / admin123 |
| Prometheus | http://localhost:9090 | — |
| OTel Collector | localhost:4317 (gRPC) | — |

---

## Backend Development

```bash
cd backend

# ── Compile ──────────────────────────────────────────────────────────
mvn clean compile

# ── Unit tests (no Docker required) ──────────────────────────────────
mvn clean test

# ── Integration tests (Docker required — starts Testcontainer PG) ────
mvn clean verify

# ── Run with dev profile ──────────────────────────────────────────────
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# ── Build Docker image ────────────────────────────────────────────────
docker build -t smartprocure-backend:latest .

# ── Dependency convergence check ──────────────────────────────────────
mvn dependency:analyze
mvn dependency:tree | grep -i conflict
```

---

## Testing Strategy

| Test Type | Pattern | Runs With | Docker |
|---|---|---|---|
| Unit | `*Test.java` | `mvn test` | ❌ Not required |
| Integration (context load) | `ApplicationContextTest.java` | `mvn verify` | ✅ PostgreSQL via Testcontainers |
| Integration (API) | `*IntegrationTest.java`, `*IT.java` | `mvn verify` | ✅ |

### Phase 1 Tests

| Class | Type | Assertions |
|---|---|---|
| `ApiResponseTest` | Unit | 6 — response envelope factory methods |
| `MoneyUtilsTest` | Unit | 14 — BigDecimal arithmetic & null-safety |
| `CorrelationIdFilterTest` | Unit | 5 — correlation ID injection & MDC cleanup |
| `GlobalExceptionHandlerTest` | Unit | 6 — exception → HTTP status mapping |
| `ApplicationContextTest` | Integration | 1 — full Spring context + Flyway + JPA |

---

## Standard API Response Envelope

All endpoints return a consistent JSON envelope:

```json
{
  "success": true,
  "message": "Success",
  "data": { "...": "..." },
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-06-01T10:30:00"
}
```

Error responses:

```json
{
  "correlationId": "550e8400-...",
  "errorCode": "RESOURCE_NOT_FOUND",
  "status": 404,
  "message": "Purchase Request not found with id: '42'",
  "errors": null,
  "timestamp": "2025-06-01T10:30:00"
}
```

---

## Project Structure

```
smartprocure/
├── backend/                              ← Spring Boot 4 application
│   ├── src/main/java/com/smartprocure/
│   │   ├── SmartProcureApplication.java
│   │   ├── config/                       ← Security, JPA, Cache, OpenAPI, Virtual Threads
│   │   └── common/
│   │       ├── audit/                    ← AuditableEntity, SpringSecurityAuditorAware
│   │       ├── constants/                ← SecurityConstants
│   │       ├── exception/                ← ErrorCode, GlobalExceptionHandler
│   │       ├── filter/                   ← CorrelationIdFilter, RequestLoggingFilter
│   │       ├── response/                 ← ApiResponse, PageResponse, ErrorResponse
│   │       └── util/                     ← MoneyUtils, DateUtils, SecurityUtils
│   └── src/main/resources/
│       ├── application.yml               ← Base configuration
│       ├── application-{dev,prod,test}.yml
│       ├── logback-spring.xml
│       └── db/migration/V1__init_extensions.sql
├── frontend/                             ← React 18 + TypeScript (Phase 9)
├── notification-service/                 ← GraalVM Native candidate (Phase 15)
├── docker/
│   ├── prometheus/prometheus.yml
│   ├── grafana/provisioning/
│   └── otel/otel-collector.yml
├── docker-compose.yml                    ← PostgreSQL, Redis, Kafka, MailHog, Ollama
├── docker-compose.observability.yml      ← Prometheus, Grafana, OTel Collector
├── .env.example
└── README.md
```

---

## Implementation Phase Status

| Phase | Scope | Status |
|---|---|---|
| **1 — Foundation** | Project skeleton, configs, common infra, Docker, tests | ✅ **Complete** |
| 2 — User Management | DB schema, JWT auth, RBAC, refresh tokens | ✅ **Complete**  |
| **3 — Vendor Management** | Vendor CRUD, contacts, ratings, approval workflow | ✅ **Complete** |
| **4 — Purchase Requests** | PR lifecycle, line items, status tracking | ✅ **Complete** |
| **5 — Approval Workflow** | Multi-level thresholds, delegation, audit | ✅ **Complete** |
| **6 — Quotation Comparison** | RFQ, vendor responses, comparison engine | ✅ **Complete** |
| **7 — Purchase Orders** | PO generation, delivery tracking | 🔄 In Progress |
| **8 — Invoice Management** | Upload, PO matching, verification | ⏳ Pending |
| **9 — React Frontend** | All screens, routing, API integration | ⏳ Pending |
| **10 — Spring AI** | RAG, vendor recommendations, tool calling | ⏳ Pending |
| 11 — Virtual Threads | Throughput benchmarks vs platform threads | ⏳ Pending |
| 12 — Kafka Events | Async notifications, retry, DLQ | ⏳ Pending |
| 13 — Docker Deployment | Production containers, health checks | ⏳ Pending |
| 14 — Observability | Grafana dashboards, OTel tracing, alerts | ⏳ Pending |
| 15 — GraalVM Native | Notification Service native executable | ⏳ Pending |
| 16 — Final Validation | Security audit, performance tests, go/no-go | ⏳ Pending |

---

## Security Notes

- JWT stored in **HttpOnly** cookies (never localStorage)
- Passwords hashed with **BCrypt strength 12**
- All endpoints protected by Spring Security RBAC
- Correlation ID propagated in every log line for tracing
- No secrets in source code — use `.env` (gitignored)

---

## License

Proprietary — All Rights Reserved.
