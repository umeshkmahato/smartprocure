# SmartProcure — AI-Powered Procurement & Vendor Management System

> Enterprise-grade procurement automation: multi-level approvals, AI vendor recommendations, quotation comparison, purchase orders, invoice matching, and full audit trails.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21 LTS · Spring Boot 4.0.x · Spring Framework 7 · Spring Security 7 |
| Persistence | PostgreSQL 16 + pgVector · Flyway (19 migrations) · Spring Data JPA · Hibernate 7 |
| Messaging | Apache Kafka (Confluent 7.7.1) · Spring Kafka · DLQ pattern |
| Cache | Redis 7 (Lettuce) · Spring Cache · Idempotency store |
| AI | Spring AI · Ollama · llama3.2:8b (chat) · nomic-embed-text (768-dim HNSW embeddings) |
| Frontend | React 18 · TypeScript 5 · Vite · TailwindCSS · Shadcn/ui |
| Notification | Spring Boot (GraalVM Native) · Kafka consumers · Redis dedup · MailHog/SMTP |
| Observability | Spring Actuator · Micrometer · Prometheus · Grafana · OpenTelemetry Collector |
| Performance | Java 21 Virtual Threads (Tomcat + @Async) · GraalVM Native Image |
| Testing | JUnit 5 · Mockito · Testcontainers · Playwright |
| Infrastructure | Docker · Docker Compose (3 compose files) |

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

> **Two ways to run:** Docker (recommended — zero local setup) or manually with Maven + Node.

---

### Option A — Full Stack with Docker *(recommended)*

#### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 24+ running

#### 1 — Clone and configure

```bash
git clone https://github.com/your-org/smartprocure.git
cd smartprocure
```

#### 2 — Start infrastructure (Postgres, Redis, Kafka, MailHog, Ollama)

```bash
docker compose -f docker-compose.yml up -d

# Wait until all services show "healthy"
docker compose -f docker-compose.yml ps
```

#### 3 — Pull AI models (first run only — can be skipped if not using AI features)

```bash
docker exec sp-ollama ollama pull llama3.2:8b
docker exec sp-ollama ollama pull nomic-embed-text
```

#### 4 — Build & start backend + frontend

```bash
docker compose -f docker-compose.app.yml --env-file .env.docker up --build -d

# Watch startup logs
docker compose -f docker-compose.app.yml logs -f
```

#### 5 — Open the app

| Service | URL | Notes |
|---|---|---|
| **Frontend (React)** | http://localhost:3000 | Login page |
| **Backend API** | http://localhost:8080/api/v1 | REST API |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Interactive docs |
| **Health check** | http://localhost:8080/actuator/health | Should return `UP` |
| **MailHog** | http://localhost:8025 | Dev email inbox |

#### Default admin credentials

```
Email:    admin@smartprocure.internal
Password: Admin@SmartProcure2025!
```

#### Rebuild after code changes

```bash
# Backend only
docker compose -f docker-compose.app.yml --env-file .env.docker up --build -d backend

# Frontend only
docker compose -f docker-compose.app.yml --env-file .env.docker up --build -d frontend

# Stop everything
docker compose -f docker-compose.yml -f docker-compose.app.yml down

# Full reset (wipes all data)
docker compose -f docker-compose.yml -f docker-compose.app.yml down -v
```

> See **[DOCKER_GUIDE.md](./DOCKER_GUIDE.md)** for the full run, validate, rebuild, and troubleshooting guide.

---

### Option B — Manual (Backend + Frontend separately)

#### 1 — Clone and configure

```bash
git clone https://github.com/your-org/smartprocure.git
cd smartprocure
cp .env.example .env          # edit values as needed
```

#### 2 — Start infrastructure

```bash
docker compose -f docker-compose.yml up -d
docker compose -f docker-compose.yml ps   # wait until all services are healthy
```

#### 3 — Pull AI models (first run only)

```bash
docker exec sp-ollama ollama pull llama3.2:8b
docker exec sp-ollama ollama pull nomic-embed-text
```

#### 4 — Start the backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### 5 — Start the frontend

```bash
cd frontend
npm install
npm run dev        # starts on http://localhost:3000
```

| Endpoint | URL |
|---|---|
| Frontend | http://localhost:3000 |
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
├── backend/                                      ← Spring Boot 4 application (Java 21)
│   ├── src/main/java/com/smartprocure/
│   │   ├── SmartProcureApplication.java
│   │   ├── config/                               ← Cross-cutting configuration
│   │   │   ├── SecurityConfig.java               ← Spring Security + JWT RBAC
│   │   │   ├── JpaConfig.java                    ← JPA auditing setup
│   │   │   ├── CacheConfig.java                  ← Redis cache manager
│   │   │   ├── RedisConfig.java                  ← Lettuce connection factory
│   │   │   ├── KafkaTopicConfig.java             ← Topic declarations
│   │   │   ├── MetricsConfig.java                ← Micrometer custom metrics
│   │   │   ├── OpenApiConfig.java                ← Springdoc / Swagger UI
│   │   │   ├── VirtualThreadConfig.java          ← Java 21 virtual threads
│   │   │   ├── WebConfig.java                    ← CORS configuration
│   │   │   └── DataInitializer.java              ← Admin seed on startup
│   │   ├── common/
│   │   │   ├── audit/                            ← AuditableEntity, AuditLog, AuditService
│   │   │   ├── constants/                        ← SecurityConstants
│   │   │   ├── exception/                        ← ErrorCode, GlobalExceptionHandler, custom exceptions
│   │   │   ├── filter/                           ← CorrelationIdFilter, RequestLoggingFilter
│   │   │   ├── response/                         ← ApiResponse, PageResponse, ErrorResponse
│   │   │   ├── security/                         ← JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal
│   │   │   └── util/                             ← MoneyUtils, DateUtils, SecurityUtils, SortValidator
│   │   ├── user/                                 ← Auth, JWT, RBAC
│   │   │   ├── entity/                           ← User, Role, RoleName, RefreshToken, UserStatus
│   │   │   ├── dto/                              ← Login, Register, ChangePassword, UserDto …
│   │   │   ├── repository/                       ← UserRepository, RoleRepository, RefreshTokenRepository
│   │   │   ├── service/                          ← AuthService, UserService, UserDetailsServiceImpl
│   │   │   └── controller/                       ← AuthController, UserController
│   │   ├── vendor/                               ← Vendor lifecycle & ratings
│   │   │   ├── entity/                           ← Vendor, VendorContact, VendorRating, VendorStatus
│   │   │   ├── dto/                              ← CreateVendor, UpdateVendor, AddContact, AddRating …
│   │   │   ├── repository/                       ← VendorRepository, VendorContactRepository, VendorRatingRepository
│   │   │   ├── service/                          ← VendorService
│   │   │   └── controller/                       ← VendorController
│   │   ├── approval/                             ← Multi-level approval workflow
│   │   │   ├── entity/                           ← ApprovalRequest, ApprovalThreshold, ApprovalDelegation, ApprovalStatus
│   │   │   ├── dto/                              ← ActionApproval, CreateDelegation, ApprovalThresholdDto …
│   │   │   ├── repository/                       ← ApprovalRequestRepository, ThresholdRepository, DelegationRepository
│   │   │   ├── service/                          ← ApprovalWorkflowService
│   │   │   └── controller/                       ← ApprovalWorkflowController
│   │   ├── purchaserequest/                      ← PR lifecycle & line items
│   │   │   ├── entity/                           ← PurchaseRequest, PurchaseRequestItem, PRStatus, PRPriority
│   │   │   ├── dto/                              ← CreatePR, UpdatePR, RejectPR, LineItemRequest …
│   │   │   ├── repository/                       ← PurchaseRequestRepository, PurchaseRequestItemRepository
│   │   │   ├── service/                          ← PurchaseRequestService
│   │   │   └── controller/                       ← PurchaseRequestController
│   │   ├── purchaseorder/                        ← PO generation & delivery tracking
│   │   │   ├── entity/                           ← PurchaseOrder, PurchaseOrderItem, POStatus
│   │   │   ├── dto/                              ← CreatePO, UpdatePO, RecordDelivery, PoLineItem …
│   │   │   ├── repository/                       ← PurchaseOrderRepository, PurchaseOrderItemRepository
│   │   │   ├── service/                          ← PurchaseOrderService
│   │   │   └── controller/                       ← PurchaseOrderController
│   │   ├── quotation/                            ← RFQ & vendor quotation comparison
│   │   │   ├── entity/                           ← QuotationRequest, Quotation, QuotationItem, statuses
│   │   │   ├── dto/                              ← CreateRFQ, SubmitQuotation, QuotationDto …
│   │   │   ├── repository/                       ← QuotationRequestRepository, QuotationRepository
│   │   │   ├── service/                          ← QuotationService
│   │   │   └── controller/                       ← QuotationController
│   │   ├── invoice/                              ← Invoice upload, PO matching & verification
│   │   │   ├── entity/                           ← Invoice, InvoiceStatus
│   │   │   ├── dto/                              ← CreateInvoice, UpdateInvoice, DisputeInvoice, InvoiceDto
│   │   │   ├── repository/                       ← InvoiceRepository
│   │   │   ├── service/                          ← InvoiceService
│   │   │   └── controller/                       ← InvoiceController
│   │   ├── ai/                                   ← Spring AI: RAG, recommendations, chat
│   │   │   ├── dto/                              ← ChatRequest, RecommendRequest
│   │   │   ├── service/                          ← AiChatService, VendorRecommendationService, VendorEmbeddingService
│   │   │   └── controller/                       ← AiController
│   │   └── events/                               ← Kafka event publishing
│   │       ├── KafkaEventPublisher.java
│   │       ├── VendorApprovedEvent.java
│   │       ├── PurchaseOrderIssuedEvent.java
│   │       └── InvoiceVerifiedEvent.java
│   └── src/main/resources/
│       ├── application.yml                       ← Base configuration (all env vars)
│       ├── application-dev.yml                   ← Dev overrides
│       ├── application-prod.yml                  ← Prod overrides
│       ├── application-test.yml                  ← Test overrides
│       ├── logback-spring.xml                    ← Structured JSON logging
│       └── db/migration/                         ← 19 Flyway migrations (V1–V19)
│
├── frontend/                                     ← React 18 + TypeScript + Vite
│   └── src/
│       ├── api/                                  ← Typed API clients
│       │   ├── client.ts                         ← Axios base client
│       │   ├── auth.ts, vendors.ts, approvals.ts
│       │   ├── purchaseRequests.ts, purchaseOrders.ts
│       │   ├── quotations.ts, invoices.ts, ai.ts
│       ├── pages/                                ← Feature pages
│       │   ├── auth/                             ← Login / register
│       │   ├── dashboard/                        ← Summary dashboard
│       │   ├── vendors/                          ← Vendor list & detail
│       │   ├── approvals/                        ← Approval queue
│       │   ├── purchaseRequests/                 ← PR list & form
│       │   ├── purchaseOrders/                   ← PO list & detail
│       │   ├── quotations/                       ← RFQ & comparison
│       │   └── invoices/                         ← Invoice list & upload
│       ├── components/
│       │   ├── ai/                               ← AI chat widget & recommendations
│       │   ├── layout/                           ← Shell, sidebar, navbar
│       │   └── ui/                               ← Shadcn/ui primitives
│       ├── store/                                ← Zustand state management
│       ├── hooks/                                ← Custom React hooks
│       ├── types/                                ← Shared TypeScript types
│       └── lib/                                  ← Utility helpers
│
├── notification-service/                         ← Standalone GraalVM Native microservice
│   └── src/main/java/com/smartprocure/notification/
│       ├── NotificationServiceApplication.java
│       ├── config/
│       │   ├── KafkaConsumerConfig.java           ← Consumer factory + retry/DLQ
│       │   └── NativeHintsRegistrar.java          ← GraalVM reflection hints
│       ├── consumer/
│       │   ├── VendorEventConsumer.java           ← vendor.approved topic
│       │   ├── PurchaseOrderEventConsumer.java    ← purchase-order.issued topic
│       │   ├── InvoiceEventConsumer.java          ← invoice.verified topic
│       │   └── DlqConsumer.java                  ← Dead-letter queue handler
│       ├── model/
│       │   ├── VendorApprovedEvent.java
│       │   ├── PurchaseOrderIssuedEvent.java
│       │   └── InvoiceVerifiedEvent.java
│       └── service/
│           ├── IdempotencyService.java            ← Redis-based deduplication (TTL: 7 days)
│           └── NotificationEmailService.java      ← SMTP email dispatch
│
├── docker/
│   ├── prometheus/prometheus.yml
│   ├── grafana/provisioning/
│   └── otel/otel-collector.yml
├── k6/                                           ← Load test scripts
├── benchmarks/                                   ← Virtual thread benchmarks
├── docs/                                         ← Additional documentation
├── docker-compose.yml                            ← Infrastructure: PostgreSQL, Redis, Kafka, MailHog, Ollama
├── docker-compose.app.yml                        ← Application: Backend, Frontend, Notification Service
├── docker-compose.observability.yml              ← Observability: Prometheus, Grafana, OTel Collector
├── DEPLOYMENT.md                                 ← Deployment index (Local / AWS / GCP)
├── DEPLOYMENT_AWS.md                             ← AWS ECS Fargate production guide
├── DEPLOYMENT_GCP.md                             ← GCP GKE Autopilot production guide
├── DOCKER_GUIDE.md                               ← Docker run, rebuild & troubleshoot guide
├── SECURITY_AUDIT.md                             ← Security audit report
├── PERFORMANCE_REPORT.md                         ← k6 load test & benchmark results
├── .env.example                                  ← All environment variables with defaults
└── README.md
```

---

## Implementation Phase Status

| Phase | Scope | Status |
|---|---|---|
| **1 — Foundation** | Project skeleton, configs, common infra, Docker, tests | ✅ Complete |
| **2 — User Management** | DB schema, JWT auth, RBAC, refresh tokens | ✅ Complete |
| **3 — Vendor Management** | Vendor CRUD, contacts, ratings, approval workflow | ✅ Complete |
| **4 — Purchase Requests** | PR lifecycle, line items, status tracking | ✅ Complete |
| **5 — Approval Workflow** | Multi-level thresholds, delegation, audit | ✅ Complete |
| **6 — Quotation Comparison** | RFQ, vendor responses, comparison engine | ✅ Complete |
| **7 — Purchase Orders** | PO generation, delivery tracking | ✅ Complete |
| **8 — Invoice Management** | Upload, PO matching, verification | ✅ Complete |
| **9 — React Frontend** | All screens, routing, API integration | ✅ Complete |
| **10 — Spring AI** | RAG, vendor recommendations, chat assistant, pgvector embeddings | ✅ Complete |
| **11 — Virtual Threads** | Throughput benchmarks vs platform threads | ✅ Complete |
| **12 — Kafka Events** | Async notifications, retry, DLQ | ✅ Complete |
| **13 — Docker Deployment** | Production containers, health checks | ✅ Complete |
| **14 — Observability** | Structured logging, Micrometer counters, Grafana dashboards, alert rules | ✅ Complete |
| **15 — GraalVM Native** | Native hints, `Dockerfile.native`, distroless runtime image | ✅ Complete |
| **16 — Final Validation** | k6 load tests, E2E integration test, `SECURITY_AUDIT.md`, `PERFORMANCE_REPORT.md` | ✅ Complete |

---

## API Endpoints Reference

### Authentication — `/api/v1/auth`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/auth/register` | Public | Register new user |
| POST | `/api/v1/auth/login` | Public | Login, receive JWT + refresh token |
| POST | `/api/v1/auth/refresh` | Public | Refresh access token |
| POST | `/api/v1/auth/logout` | Authenticated | Invalidate refresh token |

### Users — `/api/v1/users`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/users/me` | Authenticated | Get own profile |
| PUT | `/api/v1/users/me` | Authenticated | Update own profile |
| POST | `/api/v1/users/me/change-password` | Authenticated | Change password |
| GET | `/api/v1/users` | ADMIN | List all users (paginated) |
| GET | `/api/v1/users/{id}` | ADMIN | Get user by ID |

### Vendors — `/api/v1/vendors`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/vendors` | ADMIN, PROCUREMENT_OFFICER | Register vendor |
| GET | `/api/v1/vendors` | Authenticated | Search / filter vendors |
| GET | `/api/v1/vendors/{id}` | Authenticated | Get vendor detail |
| PUT | `/api/v1/vendors/{id}` | ADMIN, PROCUREMENT_OFFICER | Update vendor |
| PATCH | `/api/v1/vendors/{id}/approve` | ADMIN | Approve vendor |
| PATCH | `/api/v1/vendors/{id}/suspend` | ADMIN | Suspend vendor |
| POST | `/api/v1/vendors/{id}/contacts` | ADMIN, PROCUREMENT_OFFICER | Add contact |
| POST | `/api/v1/vendors/{id}/ratings` | ADMIN, PROCUREMENT_OFFICER | Add rating |
| GET | `/api/v1/vendors/{id}/ratings` | Authenticated | Get vendor ratings |

### Purchase Requests — `/api/v1/purchase-requests`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/purchase-requests` | Authenticated | Create PR (DRAFT) |
| GET | `/api/v1/purchase-requests` | Authenticated | List / filter PRs |
| GET | `/api/v1/purchase-requests/{id}` | Authenticated | Get PR by ID |
| PUT | `/api/v1/purchase-requests/{id}` | Authenticated | Update DRAFT PR |
| PATCH | `/api/v1/purchase-requests/{id}/submit` | Authenticated | Submit for approval |
| PATCH | `/api/v1/purchase-requests/{id}/cancel` | Authenticated | Cancel PR |
| POST | `/api/v1/purchase-requests/{id}/reject` | ADMIN, APPROVER | Reject PR |

### Approvals — `/api/v1/approvals`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/approvals` | Authenticated | List approval requests |
| GET | `/api/v1/approvals/{id}` | Authenticated | Get approval detail |
| POST | `/api/v1/approvals/{id}/action` | ADMIN, APPROVER | Approve or reject |
| GET | `/api/v1/approvals/thresholds` | ADMIN | Get approval thresholds |
| POST | `/api/v1/approvals/delegations` | ADMIN, APPROVER | Create delegation |

### Quotations — `/api/v1/quotations`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/quotations/rfqs` | ADMIN, PROCUREMENT_OFFICER | Create RFQ |
| PATCH | `/api/v1/quotations/rfqs/{id}/publish` | ADMIN, PROCUREMENT_OFFICER | Publish RFQ to vendors |
| PATCH | `/api/v1/quotations/rfqs/{id}/close` | ADMIN, PROCUREMENT_OFFICER | Close RFQ |
| POST | `/api/v1/quotations/rfqs/{id}/quotations` | VENDOR | Submit quotation |
| GET | `/api/v1/quotations/rfqs/{id}/compare` | ADMIN, PROCUREMENT_OFFICER | Compare all quotations |
| POST | `/api/v1/quotations/rfqs/{id}/select/{qId}` | ADMIN, PROCUREMENT_OFFICER | Select winning quotation |

### Purchase Orders — `/api/v1/purchase-orders`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/purchase-orders` | ADMIN, PROCUREMENT_OFFICER | Create PO (DRAFT) |
| GET | `/api/v1/purchase-orders` | Authenticated | Search / filter POs |
| GET | `/api/v1/purchase-orders/{id}` | Authenticated | Get PO by ID |
| GET | `/api/v1/purchase-orders/number/{poNumber}` | Authenticated | Get PO by PO number |
| PUT | `/api/v1/purchase-orders/{id}` | ADMIN, PROCUREMENT_OFFICER | Update DRAFT PO |
| PATCH | `/api/v1/purchase-orders/{id}/issue` | ADMIN, PROCUREMENT_OFFICER | Issue PO to vendor |
| PATCH | `/api/v1/purchase-orders/{id}/acknowledge` | VENDOR | Vendor acknowledges PO |
| PATCH | `/api/v1/purchase-orders/{id}/deliver` | ADMIN, PROCUREMENT_OFFICER | Record delivery |
| PATCH | `/api/v1/purchase-orders/{id}/cancel` | ADMIN, PROCUREMENT_OFFICER | Cancel PO |
| PATCH | `/api/v1/purchase-orders/{id}/close` | ADMIN, PROCUREMENT_OFFICER | Close PO |

### Invoices — `/api/v1/invoices`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/invoices` | ADMIN, PROCUREMENT_OFFICER | Upload invoice (PDF/JPEG/PNG ≤ 10 MB) |
| GET | `/api/v1/invoices` | Authenticated | List / filter invoices |
| GET | `/api/v1/invoices/{id}` | Authenticated | Get invoice detail |
| PUT | `/api/v1/invoices/{id}` | ADMIN, PROCUREMENT_OFFICER | Update invoice |
| PATCH | `/api/v1/invoices/{id}/verify` | ADMIN, PROCUREMENT_OFFICER | Verify invoice against PO |
| POST | `/api/v1/invoices/{id}/dispute` | ADMIN, PROCUREMENT_OFFICER | Dispute invoice |

### AI — `/api/v1/ai`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/ai/chat` | Authenticated | Procurement chat assistant (llama3.2:8b) |
| POST | `/api/v1/ai/recommend` | Authenticated | AI vendor recommendations (pgvector RAG) |

---

## Database Schema

19 Flyway migrations applied in order:

| Migration | Description |
|-----------|-------------|
| V1 | Enable `uuid-ossp` and `pgcrypto` extensions |
| V2 | `users` table |
| V3 | `roles` table |
| V4 | `refresh_tokens` table |
| V5 | `audit_logs` table |
| V6 | `approval_thresholds` table |
| V7 | Seed roles (`ADMIN`, `PROCUREMENT_OFFICER`, `APPROVER`, `VENDOR`) and default thresholds |
| V8 | `vendors` table |
| V9 | `vendor_contacts` table |
| V10 | `vendor_ratings` table |
| V11 | `purchase_requests` table |
| V12 | `purchase_request_items` table |
| V13 | `approval_requests` table |
| V14 | `approval_delegations` table |
| V15 | `quotation_requests`, `quotations`, `quotation_items` tables |
| V16 | `purchase_orders`, `purchase_order_items` tables |
| V17 | `invoices` table |
| V18 | Enable `vector` extension + `vendor_embeddings` table (768-dim HNSW, cosine) |
| V19 | `published_events_audit` table |

---

## Kafka Topics & Events

| Topic | Producer | Consumer | Payload |
|-------|----------|----------|---------|
| `vendor.approved` | `KafkaEventPublisher` | `VendorEventConsumer` | `VendorApprovedEvent` |
| `purchase-order.issued` | `KafkaEventPublisher` | `PurchaseOrderEventConsumer` | `PurchaseOrderIssuedEvent` |
| `invoice.verified` | `KafkaEventPublisher` | `InvoiceEventConsumer` | `InvoiceVerifiedEvent` |
| `*.DLT` | Kafka retry mechanism | `DlqConsumer` | Original failed payload |

> The Notification Service uses Redis-backed idempotency (TTL: 7 days) to prevent duplicate emails on consumer retries.

---

## Approval Thresholds

| Level | Amount Range | Required Approver |
|-------|-------------|-------------------|
| Level 1 | ≤ $10,000 | APPROVER |
| Level 2 | $10,001 – $50,000 | Senior APPROVER |
| Level 3 | > $50,000 | ADMIN |

Approvers can delegate to others via `ApprovalDelegation` with configurable date ranges.

---

## Actuator Endpoints

All exposed at `/actuator/*`:

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Liveness + DB + Redis component health |
| `/actuator/info` | Application metadata |
| `/actuator/metrics` | Micrometer metrics |
| `/actuator/prometheus` | Prometheus scrape endpoint |
| `/actuator/flyway` | Applied migration status |
| `/actuator/loggers` | Runtime log-level management |
| `/actuator/threaddump` | Virtual thread dump |
| `/actuator/env` | Active environment properties |
| `/actuator/caches` | Redis cache statistics |

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
